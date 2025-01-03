package dev.thomas_kiljanczyk.bluetoothbroadcasting.lib.extensions

import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

internal fun UUID.order(byteOrder: ByteOrder): UUID {
    val byteBuffer = ByteBuffer.allocate(16)
        .apply {
            putLong(leastSignificantBits)
            putLong(mostSignificantBits)
            rewind()
            order(byteOrder)
        }
    return UUID(byteBuffer.long, byteBuffer.long)
}

internal fun ParcelUuid.order(byteOrder: ByteOrder): ParcelUuid {
    return ParcelUuid(this.uuid.order(byteOrder))
}