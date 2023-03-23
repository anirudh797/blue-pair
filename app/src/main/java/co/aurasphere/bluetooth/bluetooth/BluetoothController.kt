/*
 * MIT License
 * <p>
 * Copyright (c) 2017 Donato Rimenti
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package co.aurasphere.bluetooth.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.os.Bundle
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat.checkSelfPermission
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Class for handling Bluetooth connection.
 *
 */
class BluetoothController(
    /**
     * The activity which is using this controller.
     */
    private val context: Activity,
    /**
     * Interface for Bluetooth OS services.
     */
    val bluetooth: BluetoothAdapter?,
    listener: BluetoothDiscoveryDeviceListener?,
    handler: Handler
) : Closeable {

    companion object {
        /**
         * Tag string used for logging.
         */
        private const val TAG = "BluetoothManager"

        /**
         * Converts a BluetoothDevice to its String representation.
         *
         * @param device the device to convert to String.
         * @return a String representation of the device.
         */
        @JvmStatic
        fun deviceToString(device: BluetoothDevice): String {
            return "[Address: " + device.address + ", Name: " + device.name + "]"
        }

        /**
         * Gets the name of a device. If the device name is not available, returns the device address.
         *
         * @param device the device whose name to return.
         * @return the name of the device or its address if the name is not available.
         */
        @JvmStatic
        fun getDeviceName(device: BluetoothDevice?): String? {
            var deviceName = device!!.name
            if (deviceName == null) {
                deviceName = device.address
            }
            return deviceName
        }
    }

    /**
     * Instantiates a new BluetoothController.
     *
     * @param context  the activity which is using this controller.
     * @param listener a callback for handling Bluetooth events.
     */


     var mmDevice : BluetoothDevice? = null
    /**
     * Class used to handle communication with OS about Bluetooth system events.
     */
    lateinit var broadcastReceiverDelegator: BroadcastReceiverDelegator


    /**
     * Used as a simple way of synchronization between turning on the Bluetooth and starting a
     * device discovery.
     */
    private var bluetoothDiscoveryScheduled = false
    /**
     * Gets the currently bounding device.
     *
     * @return the [.boundingDevice].
     */
    /**
     * Used as a temporary field for the currently bounding device. This field makes this whole
     * class not Thread Safe.
     */
    var boundingDevice: BluetoothDevice? = null
        private set
    private var ct: ConnectThread? = null
    private var at : AcceptThread? = null
    private var readWriteThread  : ReadWriteThread? = null
    private var handler // handler that gets info from Bluetooth service
            : Handler = handler

    init {
        broadcastReceiverDelegator = listener?.let { BroadcastReceiverDelegator(context, it, this) }!!
        this.handler = handler
    }


     var mmSocket: BluetoothSocket? = null
    var serverSocket : BluetoothServerSocket?= null

    // Defines several constants used when transmitting messages between the
    // service and the UI.


    /**
     * Checks if the Bluetooth is already enabled on this device.
     *
     * @return true if the Bluetooth is on, false otherwise.
     */
    val isBluetoothEnabled: Boolean
        get() = bluetooth!!.isEnabled

    /**
     * Starts the discovery of new Bluetooth devices nearby.
     */
    fun startDiscovery() {
        broadcastReceiverDelegator.onDeviceDiscoveryStarted()

        // This line of code is very important. In Android >= 6.0 you have to ask for the runtime
        // permission as well in order for the discovery to get the devices ids. If you don't do
        // this, the discovery won't find any device.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            if (checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(context,android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                turnOnBluetoothAndScheduleDiscovery()
//
//            } else {
//                ActivityCompat.requestPermissions(context, arrayOf(
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1);
//            }
//        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (bluetooth!!.isDiscovering) {
            bluetooth.cancelDiscovery()
        }

        // Tries to start the discovery. If the discovery returns false, this means that the
        // bluetooth has not started yet.
        Log.d(TAG, "Bluetooth starting discovery.")
        if (!bluetooth.startDiscovery()) {
            Toast.makeText(context, "Error while starting device discovery!", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?")

            // Ends the discovery.
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }

    /**
     * Turns on the Bluetooth.
     */
    private fun turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.")
        broadcastReceiverDelegator.onBluetoothTurningOn()
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
        if(bluetooth?.isEnabled == false)
        bluetooth!!.enable()
    }

    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to remote Bluetooth Device.")
        if (mmSocket == null || mmSocket?.isConnected == false ) {
            ct = ConnectThread(device)
            ct!!.start()
        }
        else
            ct?.manageMyConnectedSocket(mmSocket)

        boundingDevice = device
    }


    /**
     * Performs the device pairing.
     *
     * @param device the device to pair with.
     * @return true if the pairing was successful, false otherwise.
     */
    fun pair(device: BluetoothDevice): Boolean {
        // Stops the discovery and then creates the pairing.
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.BLUETOOTH_SCAN
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return false
//        }
        if (bluetooth!!.isDiscovering) {
            Log.d(TAG, "Bluetooth cancelling discovery.")
            bluetooth.cancelDiscovery()
        }
        Log.d(TAG, "Bluetooth bonding with device: " + deviceToString(device))
        val outcome = device.createBond()
        Log.d(TAG, "Bounding outcome : $outcome")

        // If the outcome is true, we are bounding with this device.
        if (outcome == true) {
            boundingDevice = device
        }
        return outcome
    }

    /**
     * Checks if a device is already paired.
     *
     * @param device the device to check.
     * @return true if it is already paired, false otherwise.
     */
    fun isAlreadyPaired(device: BluetoothDevice?): Boolean {
        return bluetooth!!.bondedDevices.contains(device)
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        broadcastReceiverDelegator.close()
    }

    /**
     * Checks if a deviceDiscovery is currently running.
     *
     * @return true if a deviceDiscovery is currently running, false otherwise.
     */
    val isDiscovering: Boolean
        @SuppressLint("MissingPermission")
        get() = bluetooth!!.isDiscovering

    /**
     * Cancels a device discovery.
     */
    fun cancelDiscovery() {
        if (bluetooth != null) {
            bluetooth.cancelDiscovery()
            broadcastReceiverDelegator.onDeviceDiscoveryEnd()
        }
    }

    /**
     * Turns on the Bluetooth and executes a device discovery when the Bluetooth has turned on.
     */
    fun turnOnBluetoothAndScheduleDiscovery() {
        bluetoothDiscoveryScheduled = true
        turnOnBluetooth()
    }

    /**
     * Called when the Bluetooth status changed.
     */
    fun onBluetoothStatusChanged() {
        // Does anything only if a device discovery has been scheduled.
        if (bluetoothDiscoveryScheduled) {
            val bluetoothState = bluetooth!!.state
            when (bluetoothState) {
                BluetoothAdapter.STATE_ON -> {
                    // Bluetooth is ON.
                    Log.d(TAG, "Bluetooth succesfully enabled, starting discovery")
                    startDiscovery()
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false
                }
                BluetoothAdapter.STATE_OFF -> {
                    // Bluetooth is OFF.
                    Log.d(TAG, "Error while turning Bluetooth on.")
                    Toast.makeText(context, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT)
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false
                }
                else -> {}
            }
        }
    }// If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.

    /**
     * Returns the status of the current pairing and cleans up the state if the pairing is done.
     *
     * @return the current pairing status.
     * @see BluetoothDevice.getBondState
     */
    val pairingDeviceStatus: Int
        get() {
            checkNotNull(boundingDevice) { "No device currently bounding" }
            val bondState = boundingDevice!!.bondState
            // If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.
            if (bondState != BluetoothDevice.BOND_BONDING) {
                boundingDevice = null
            }
            return bondState
        }


    fun sendMessage( message : ByteArray)
    {
        Log.e(TAG,"Attempting to send message : $message")
        readWriteThread?.write(message)
        KeyEvent.keyCodeToString(KeyEvent.KEYCODE_1)
    }

    fun startListening() {
        at = AcceptThread()
        at?.start()

    }

    /**
     * Gets the name of the currently pairing device.
     *
     * @return the name of the currently pairing device.
     */
    val pairingDeviceName: String?
        get() = getDeviceName(boundingDevice)

    /**
     * Returns if there's a pairing currently being done through this app.
     *
     * @return true if a pairing is in progress through this app, false otherwise.
     */
    val isPairingInProgress: Boolean
        get() = boundingDevice != null

    private inner class ReadWriteThread(private val mmSocket: BluetoothSocket?) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private lateinit var mmBuffer: ByteArray

        override fun run() {
            mmBuffer = ByteArray(1024)
            var numBytes: Int // bytes returned from read()

//             Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream!!.read(mmBuffer)
                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_READ, numBytes, -1,
                        mmBuffer
                    )
                    readMsg.sendToTarget()
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    val readMsg = handler.obtainMessage(
                        MessageConstants.CONNECTION_FAILED, -1, -1,
                        mmBuffer
                    )
                    readMsg.sendToTarget()
                    break
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray?) {
            try {
                mmOutStream?.write(bytes)

                // Share the sent message with the UI activity.
//                val writtenMsg = handler.obtainMessage(
//                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer
//                )
//                writtenMsg.sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MessageConstants.MESSAGE_TOAST)
                val bundle = Bundle()
                bundle.putString(
                    "toast",
                    "Couldn't send data to the other device"
                )
                writeErrorMsg.data = bundle
                val msg = handler.obtainMessage(MessageConstants.CONNECTION_FAILED)
                handler.sendMessage(msg)
            }
            catch (e : Exception){
                e.printStackTrace()
                val msg = handler.obtainMessage(MessageConstants.CONNECTION_FAILED)
                handler.sendMessage(msg)
            }
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket!!.close()

            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket!!.inputStream
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when creating input stream", e)
            }
            try {
                tmpOut = mmSocket!!.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when creating output stream", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

     inner class ConnectThread(device: BluetoothDevice) : Thread() {


        init {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            var tmp: BluetoothSocket? = null
            mmDevice = device
            //         UUID : 0000110e-0000-1000-8000-00805f9b34fb
            //             UUID : 00000000-0000-1000-8000-00805f9b34fb
            //             UUID : 00000000-0000-1000-8000-00805f9b34fb
            //             UUID : cbbfe0e2-f7f3-4206-84e0-84cbb3d09dfc //correct one
            //        00001101-0000-1000-8000-00805F9B34FB
            var MY_UUID = UUID.fromString("cbbfe0e2-f7f3-4206-84e0-84cbb3d09dfc")
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's create() method failed", e)
            }
            mmSocket = tmp
        }


        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetooth!!.cancelDiscovery()
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.e(TAG, "Close the socket")
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException)
                }
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket)
        }


        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmDevice = null
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }

         fun manageMyConnectedSocket(mmSocket: BluetoothSocket?) {
            Log.e(TAG, "Connection attempt successfull")
             isHidDeviceConnected = true
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(TAG, "Connection Type " + mmSocket!!.connectionType)
                Log.e(TAG, mmSocket.remoteDevice.name)
                Log.e(TAG, mmSocket.remoteDevice.bondState.toString())
                Log.e(TAG, mmSocket.isConnected.toString())
            }

             bluetooth?.getProfileProxy(context,listener,BluetoothProfile.HID_DEVICE)
             val msg = handler.obtainMessage(
                 MessageConstants.CONNECTION_SUCCESSFULL)
             msg.sendToTarget()

        }
    }


    var isHidDeviceConnected = false
    var bluetoothHidDevice : BluetoothHidDevice? = null

    @RequiresApi(Build.VERSION_CODES.P)
    fun send(byte1: Int, byte2: Int): Boolean {


        return if (bluetoothHidDevice != null && isHidDeviceConnected) {
            bluetoothHidDevice!!.sendReport(
                mmDevice,
                Constants.ID_REMOTE_CONTROL.toInt(), RemoteControlReport.getReport(byte1, byte2)
            )
        } else false
    }

    val listener  = object  : BluetoothProfile.ServiceListener{
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                Log.d(TAG,"onServiceConnected profile == BluetoothProfile.HID_DEVICE")
                val callback: BluetoothHidDevice.Callback = object : BluetoothHidDevice.Callback() {
                    override fun onAppStatusChanged(
                        pluggedDevice: BluetoothDevice?,
                        registered: Boolean
                    ) {
                        super.onAppStatusChanged(pluggedDevice, registered)
                        Log.d(TAG,"onAppStatusChanged registered=$registered")
                        val deviceConnected = bluetoothHidDevice?.connect(
                            mmDevice
                        )
                        if (deviceConnected == true) {
                            Log.d(TAG,"Connected to " + mmDevice!!.name)
                        }
                    }

                    override fun onGetReport(
                        device: BluetoothDevice,
                        type: Byte,
                        id: Byte,
                        bufferSize: Int
                    ) {
                        super.onGetReport(device, type, id, bufferSize)
                        Log.d(TAG,"onGetReport")
                    }

                    override fun onSetReport(
                        device: BluetoothDevice,
                        type: Byte,
                        id: Byte,
                        data: ByteArray
                    ) {
                        super.onSetReport(device, type, id, data)
                        Log.d(TAG,"onSetReport")
                    }

                    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                        var stateStr = ""
                        when (state) {
                            BluetoothHidDevice.STATE_CONNECTED -> {
                                stateStr = "STATE_CONNECTED"
                                isHidDeviceConnected = true
//                                sendMessage(BluetoothHidService.WHAT.BLUETOOTH_CONNECTED)
                            }
                            BluetoothHidDevice.STATE_DISCONNECTED -> {
                                stateStr = "STATE_DISCONNECTED"
                                isHidDeviceConnected = false
//                                sendMessage(BluetoothHidService.WHAT.BLUETOOTH_DISCONNECTED)
//                                this@BluetoothHidService.stopSelf()
                            }
                            BluetoothHidDevice.STATE_CONNECTING -> {
                                stateStr = "STATE_CONNECTING"
//                                sendMessage(BluetoothHidService.WHAT.BLUETOOTH_CONNECTING)
//                                startAsForeground()
                            }
                            BluetoothHidDevice.STATE_DISCONNECTING -> stateStr =
                                "STATE_DISCONNECTING"
                        }
                        val isProfileSupported: Boolean = HidUtils.isProfileSupported(device)
                        Log.d(TAG,"isProfileSupported $isProfileSupported")
                        Log.d(TAG,"HID " + device.name + " " + device.address + " " + stateStr)
                    }

                    override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
                        super.onSetProtocol(device, protocol)
                        Log.d(TAG,"onSetProtocol")
                    }
                }

//            bluetoothHidDevice.registerApp(Constants.SDP_RECORD, null, Constants.QOS_OUT, Runnable::run, callback);
                bluetoothHidDevice!!.registerApp(
                    Constants.SDP_RECORD,
                    null,
                    null,
                    { obj: Runnable -> obj.run() },
                    callback
                )
            }

        }

        override fun onServiceDisconnected(profile: Int) {

        }


    }

     inner class AcceptThread : Thread() {

         val UUID : UUID = java.util.UUID.fromString("cbbfe0e2-f7f3-4206-84e0-84cbb3d09dfc")
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetooth?.listenUsingInsecureRfcommWithServiceRecord("Magenta Companion", UUID)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    val msg = handler.obtainMessage(MessageConstants.CONNECTION_FAILED)
                    msg.sendToTarget()
                    null

                }
                socket?.also {
                    mmSocket = it
                    manageMyConnecteServerdSocket(it)
                    shouldLoop = false
                }
            }
        }

         private fun manageMyConnecteServerdSocket(serverSocket: BluetoothSocket) {
            Log.d(TAG,"Connected Server Socket")
             readWriteThread = ReadWriteThread(serverSocket)
             readWriteThread?.start()
             val msg = handler.obtainMessage(MessageConstants.CONNECTION_SUCCESSFULL)
             msg.sendToTarget()

         }

         // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


}