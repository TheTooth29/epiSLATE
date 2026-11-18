package com.nuvio.tv.core.trial

/** Android's ECDSA DER integers converted to Web Crypto's fixed-width r||s format. */
internal object TrialSignature {
    fun p1363(der: ByteArray): ByteArray {
        require(der.size in 8..72 && der[0] == 0x30.toByte() && (der[1].toInt() and 255) == der.size - 2)
        var offset = 2
        fun integer(): ByteArray {
            require(der[offset++].toInt() == 2)
            val length = der[offset++].toInt() and 255
            require(length in 1..33 && offset + length <= der.size)
            val value = der.copyOfRange(offset, offset + length); offset += length
            val positive = if (value.size == 33) { require(value[0] == 0.toByte()); value.drop(1).toByteArray() } else value
            return ByteArray(32 - positive.size) + positive
        }
        val result = integer() + integer()
        require(offset == der.size)
        return result
    }
}
