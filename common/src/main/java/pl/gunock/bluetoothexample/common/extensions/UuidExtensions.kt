package pl.gunock.bluetoothexample.client.extensions

import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

fun UUID.order(byteOrder: ByteOrder): UUID {
    val byteBuffer = ByteBuffer.allocate(16)
        .apply {
            putLong(leastSignificantBits)
            putLong(mostSignificantBits)
            rewind()
            order(byteOrder)
        }
    return UUID(byteBuffer.long, byteBuffer.long)
}

fun ParcelUuid.order(byteOrder: ByteOrder): ParcelUuid {
    return ParcelUuid(this.uuid.order(byteOrder))
}