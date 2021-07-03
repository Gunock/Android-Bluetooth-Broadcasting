# Android-Bluetooth-Broadcasting

## Overview

Android for broadcasting messages via Bluetooth.
* Written using Kotlin and MVVM pattern
* Minimum Android 6.0 (API 23)

## How to use
Here are some instructions if you would like to use my Bluetooth server and client classes.

### BluetoothServer
Bluetooth server exposes RFCOMM service, manages client connections and allows broadcasting messages to all connected clients.

1. Constructor and operations:
``` kotlin
val server = BluetoothServer(
    bluetoothAdapter,
    SERVICE_NAME,
    SERVICE_UUID
)

// Recommended use on IO thread
server.startloop()
server.stop()

server.broadcastMessage("Something")
```
2. Bluetooth server has several listeners available:
``` kotlin
bluetoothServer.setOnConnectListener { clientSocket: BluetoothSocket ->
    // Do something
}

bluetoothServer.setOnDisconnectListener { clientSocket: BluetoothSocket ->
    // Do something
}

bluetoothServer.setOnStateChangeListener { isStopped: Boolean ->
    // Do something
}
```

### BluetoothClient
Bluetooth client allows connection to server and handles incoming messages.

1. Constructor and operations:
``` kotlin
val client =
  BluetoothClient(
      device,       // Server device
      SERVICE_UUID  // Server service ID
  ) { message: ByteArray ->
      // Do something
  }
  
// Recommended use on IO thread
client.startloop()
client.disconnect()

```
2. Bluetooth client has several listeners available:
``` kotlin
client.setOnConnectionSuccessListener { serverSocket: BluetoothSocket ->
  // Do something
}

client.setOnConnectionFailureListener { serverSocket: BluetoothSocket ->
  // Do something
}

client.setOnDisconnectionListener { serverSocket: BluetoothSocket ->
  // Do something
}

// This replaces the listener given in constructor
client.setOnDataListener { message: ByteArray ->
  // Do something
}
```


### BluetoothServiceDiscoveryManager
A helper class for acquiring bluetooth devices which host desired service.

1. Constructor:
``` kotlin
BluetoothServiceDiscoveryManager(
    context,
    listOf(serviceUuid)
)
```
2. You must register receiver (without doing it manager can't handle *fetchUuidsWithSdp* results):
``` kotlin
activity.registerReceiver(
    serviceDiscoveryManager.receiver,
    IntentFilter(BluetoothDevice.ACTION_UUID)
)
```
3. Discover services on given devices:
``` kotlin
serviceDiscoveryManager.discoverServicesInDevices(pairedDevices)
```
4. You can observe devices with desired services:
``` kotlin
serviceDiscoveryManager.devices.observe(this) { collection: Set<BluetoothDevice> ->
    // Do something
}
```

## Libraries Used
* [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for managing background threads with simplified code and reducing needs for callbacks.
* [ViewBinding](https://developer.android.com/topic/libraries/view-binding) - Easy, type safe, null safe access to layout elements.
* [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Store UI-related data that isn't destroyed on app rotations. Easily schedule asynchronous tasks for optimal execution.
* [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)- Build data objects that notify views when the underlying data changes.
* [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview?gclsrc=aw.ds&gclid=CjwKCAjwrPCGBhALEiwAUl9X03wCNk7bhvoxs_okW86jFVgc92QelSerqKyYmfEM54CbHOsKc3tYyxoCgRcQAvD_BwE) for custom list views.

## License
Copyright (c) 2021 Tomasz Kiljańczyk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
