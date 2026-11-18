package com.nuvio.tv.core.trial

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.*
import org.junit.Test

class TrialSignatureTest {
    @Test fun androidDerSignatureMatchesServerP1363Format() {
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val message = "challenge\n123456".toByteArray()
        repeat(50) {
            val der = Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(message) }.sign()
            val raw = TrialSignature.p1363(der)
            assertEquals(64, raw.size)
            assertTrue(Signature.getInstance("SHA256withECDSAinP1363Format").apply { initVerify(pair.public); update(message) }.verify(raw))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun malformedSignatureFailsClosed() {
        TrialSignature.p1363(byteArrayOf(0x30,0x7f,2,1,1,2,1,1))
    }
}
