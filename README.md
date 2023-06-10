# Android-Bluetooth-Broadcasting

## Overview

Android app for broadcasting messages via Bluetooth.

* Written using Kotlin and MVVM pattern
* Minimum Android 6.0 (API 23)

## How to use

Here are some instructions if you would like to use my Bluetooth server and client classes.

### BluetoothServer

Bluetooth server exposes RFCOMM service, manages client connections and allows broadcasting messages
to all connected clients.

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
// Create bluetooth socket using server device and server service ID
val bluetoothSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
val client = BluetoothClient(bluetoothSocket)
client.bluetoothClient.setOnDataListener { message: ByteArray ->
    // Do something
}
  
// Recommended use on IO thread
client.startloop()
client.disconnect()

```

2. Bluetooth client has several listeners available:

``` kotlin
client.setOnDataListener { message: ByteArray ->
  // Do something
}

client.setOnConnectionSuccessListener { serverSocket: BluetoothSocket ->
  // Do something
}

client.setOnConnectionFailureListener { serverSocket: BluetoothSocket ->
  // Do something
}

client.setOnDisconnectionListener { serverSocket: BluetoothSocket ->
  // Do something
}
```

### BluetoothServiceDiscoveryManager

A service class used for acquiring paired bluetooth devices which host desired service.

1. Constructor:

``` kotlin
BluetoothServiceDiscoveryManagerImpl(context)
```

2. You must set which service UUIDs to expect:

```
serviceDiscoveryManager.setExpectedUuids(/* collection of UUIDs */)
```

3. You must register a receiver (without doing it the manager cannot handle *fetchUuidsWithSdp*
   results):

``` kotlin
activity.registerReceiver(
    serviceDiscoveryManager.getBroadcastReceiver(),
    IntentFilter(BluetoothDevice.ACTION_UUID)
)
```

4. Discover services on given devices:

``` kotlin
serviceDiscoveryManager.discoverServicesInDevices(pairedDevices)
```

5. You can observe devices with desired services:

``` kotlin
serviceDiscoveryManager.getBluetoothDevices()
    .onEach { collection -> /* Do something */ }
    .flowOn(Dispatchers.Default)
    .launchIn(lifecycleScope)
```

## Libraries Used

* [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - for managing
  background threads with simplified code and reducing needs for callbacks.
* [ViewBinding](https://developer.android.com/topic/libraries/view-binding) - Easy, type safe, null
  safe access to layout elements.
* [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Store
  UI-related data that isn't destroyed on app rotations. Easily schedule asynchronous tasks for
  optimal execution.
* [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview?gclsrc=aw.ds&gclid=CjwKCAjwrPCGBhALEiwAUl9X03wCNk7bhvoxs_okW86jFVgc92QelSerqKyYmfEM54CbHOsKc3tYyxoCgRcQAvD_BwE)
  - for custom list views.
* [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Dependency
  injection library recommended by Google.

## Stay in touch

- Author - Tomasz Kiljańczyk
- Mail - [asz.czyk.dev@gmail.com](mailto:asz.czyk.dev@gmail.com)
- LinkedIn
  - [https://www.linkedin.com/in/tomasz-kilja%C5%84czyk-6b6ba3130](https://www.linkedin.com/in/tomasz-kilja%C5%84czyk-6b6ba3130)

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
