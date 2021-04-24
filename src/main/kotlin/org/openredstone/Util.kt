package org.openredstone


import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

fun UUID.toBin() : ByteArray {
    val uuidBytes = ByteArray(16)
    ByteBuffer.wrap(uuidBytes)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(this.mostSignificantBits)
        .putLong(this.leastSignificantBits)
    return uuidBytes
}

fun ByteArray.toUuid() : UUID {
    val wrap = ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN)
    val msb = wrap.long
    val lsb = wrap.long
    return UUID(msb, lsb)
}
