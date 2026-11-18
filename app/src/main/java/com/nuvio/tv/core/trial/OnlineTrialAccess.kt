package com.nuvio.tv.core.trial

import android.content.Context
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.URL
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.net.ssl.HttpsURLConnection

/** The installation private key never leaves Android Keystore. No server secret is in the APK. */
internal class OnlineTrialAccess(
    context: Context,
    private val elapsedNow: () -> Long = SystemClock::elapsedRealtime,
    private val transport: ((String, String) -> JSONObject)? = null
) {
    private val prefs = context.getSharedPreferences("epislate_online_trial", Context.MODE_PRIVATE)
    private val alias = "epislate_trial_installation_v1"
    @Volatile private var checkedAt = -1L
    @Volatile private var serverTime = 0L
    @Volatile private var end = 0L
    @Volatile private var state = TrialStatus.CONNECTION_REQUIRED
    @Volatile private var retryAt = 0L

    @Synchronized fun snapshot(): TrialSnapshot {
        if (!prefs.getBoolean("redeemed", false)) return TrialSnapshot(TrialStatus.NEEDS_CODE)
        val elapsed = elapsedNow()
        if (state == TrialStatus.EXPIRED) return TrialSnapshot(state, end)
        if (checkedAt < 0 || elapsed - checkedAt > 60_000) return TrialSnapshot(TrialStatus.CONNECTION_REQUIRED)
        val remaining = (end - serverTime - (elapsed - checkedAt)).coerceAtLeast(0)
        return TrialSnapshot(if (remaining > 0) TrialStatus.ACTIVE else TrialStatus.EXPIRED, end, remaining)
    }

    fun refresh() {
        if (!prefs.getBoolean("redeemed", false)) return
        val elapsed = elapsedNow()
        synchronized(this) {
            if (elapsed < retryAt || (checkedAt >= 0 && elapsed - checkedAt < 50_000)) return
            retryAt = elapsed + 30_000
        }
        runCatching { accept(request("status")) }
            .onFailure { checkedAt = -1; state = TrialStatus.CONNECTION_REQUIRED }
    }

    fun redeem(code: String): RedemptionResult {
        if (!TrialPolicy.isCodeFormatValid(code)) return RedemptionResult.INVALID_CODE
        return try {
            val reply = request("redeem", code)
            if (reply.optString("status") != "active") return RedemptionResult.ALREADY_STARTED
            accept(reply)
            if (!prefs.edit().putBoolean("redeemed", true).commit()) {
                checkedAt = -1
                return RedemptionResult.STORAGE_ERROR
            }
            RedemptionResult.ACTIVATED
        } catch (e: TrialRequestFailure) {
            when (e.status) {
                400, 403 -> RedemptionResult.INVALID_CODE
                429 -> RedemptionResult.RATE_LIMITED
                else -> RedemptionResult.NETWORK_ERROR
            }
        } catch (_: Exception) { RedemptionResult.NETWORK_ERROR }
    }

    @Synchronized private fun accept(reply: JSONObject) {
        val start = reply.getLong("activated_at") * 1000
        end = reply.getLong("expires_at") * 1000
        serverTime = reply.getLong("server_time") * 1000
        require(TrialPolicy.validRecord(start, end) && serverTime >= start)
        state = if (reply.getString("status") == "active" && serverTime < end) TrialStatus.ACTIVE else TrialStatus.EXPIRED
        checkedAt = elapsedNow()
    }

    private fun request(action: String, code: String = ""): JSONObject {
        transport?.let { return it(action, code) }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(alias)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build())
            }.generateKeyPair()
        }
        val public = store.getCertificate(alias).publicKey as ECPublicKey
        fun coordinate(value: java.math.BigInteger): String {
            val raw = value.toByteArray().takeLast(32).toByteArray()
            return Base64.encodeToString(ByteArray(32 - raw.size) + raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
        val jwk = JSONObject().put("kty", "EC").put("crv", "P-256")
            .put("x", coordinate(public.w.affineX)).put("y", coordinate(public.w.affineY))
        val challengeReply = post("challenge", JSONObject().put("public_key", jwk).put("action", action))
        val challenge = challengeReply.getString("challenge")
        val build = TrialBuildReport.negotiate(challengeReply, TrialBuildReport.current())
        val der = Signature.getInstance("SHA256withECDSA").apply {
            initSign(store.getKey(alias, null) as java.security.PrivateKey)
            update(TrialBuildReport.message(challenge, action, code, build).toByteArray(Charsets.UTF_8))
        }.sign()
        val signature = TrialSignature.p1363(der).joinToString("") { "%02x".format(it.toInt() and 255) }
        return post(action, JSONObject().put("public_key", jwk).put("challenge", challenge)
            .put("signature", signature).apply {
                if (action == "redeem") put("code", code)
                build?.attach(this)
            })
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val connection = URL("https://epislate-trial-activation.episl8-admin.workers.dev/v1/$path").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status != 200) throw TrialRequestFailure(status)
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            require(text.length <= 4096)
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
    private class TrialRequestFailure(val status: Int) : Exception()
}

