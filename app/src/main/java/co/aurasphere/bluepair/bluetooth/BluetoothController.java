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
package co.aurasphere.bluepair.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Class for handling Bluetooth connection.
 *
 * @author Donato Rimenti
 */
public class BluetoothController implements Closeable {

    /**
     * Tag string used for logging.
     */
    private static final String TAG = "BluetoothManager";

    /**
     * Interface for Bluetooth OS services.
     */
    private final BluetoothAdapter bluetooth;

    /**
     * Class used to handle communication with OS about Bluetooth system events.
     */
    private final BroadcastReceiverDelegator broadcastReceiverDelegator;

    /**
     * The activity which is using this controller.
     */
    private final Activity context;

    /**
     * Used as a simple way of synchronization between turning on the Bluetooth and starting a
     * device discovery.
     */
    private boolean bluetoothDiscoveryScheduled;

    /**
     * Used as a temporary field for the currently bounding device. This field makes this whole
     * class not Thread Safe.
     */
    private BluetoothDevice boundingDevice;

    private ConnectThread ct = null;

    private Handler handler; // handler that gets info from Bluetooth service

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }



    /**
     * Instantiates a new BluetoothController.
     *
     * @param context  the activity which is using this controller.
     * @param listener a callback for handling Bluetooth events.
     */
    public BluetoothController(Activity context,BluetoothAdapter adapter, BluetoothDiscoveryDeviceListener listener, Handler handler) {
        this.context = context;
        this.bluetooth = adapter;
        this.broadcastReceiverDelegator = new BroadcastReceiverDelegator(context, listener, this);
        this.handler = handler;
    }

    /**
     * Checks if the Bluetooth is already enabled on this device.
     *
     * @return true if the Bluetooth is on, false otherwise.
     */
    public boolean isBluetoothEnabled() {
        return bluetooth.isEnabled();
    }

    /**
     * Starts the discovery of new Bluetooth devices nearby.
     */
    public void startDiscovery() {
        broadcastReceiverDelegator.onDeviceDiscoveryStarted();

        // This line of code is very important. In Android >= 6.0 you have to ask for the runtime
        // permission as well in order for the discovery to get the devices ids. If you don't do
        // this, the discovery won't find any device.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (bluetooth.isDiscovering()) {
            bluetooth.cancelDiscovery();
        }

        // Tries to start the discovery. If the discovery returns false, this means that the
        // bluetooth has not started yet.
        Log.d(TAG, "Bluetooth starting discovery.");
        if (!bluetooth.startDiscovery()) {
            Toast.makeText(context, "Error while starting device discovery!", Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "StartDiscovery returned false. Maybe Bluetooth isn't on?");

            // Ends the discovery.
            broadcastReceiverDelegator.onDeviceDiscoveryEnd();
        }
    }

    /**
     * Turns on the Bluetooth.
     */
    public void turnOnBluetooth() {
        Log.d(TAG, "Enabling Bluetooth.");
        broadcastReceiverDelegator.onBluetoothTurningOn();
        bluetooth.enable();
    }

    public void connectToDevice(BluetoothDevice device){
        Log.d(TAG, "Connecting to remote Bluetooth Device.");

//        if(ct==null)
        ct = new ConnectThread(device);
        ct.start();
        if(!isAlreadyPaired(device)){
            //pair first
        }
        else{
//            ct.run();
//            if(ct.mmSocket==null)
//            {
//                Log.d(TAG, "Attempt to get socket .");
//                ct.run();
//
//            }

//            else{
//                Log.d(TAG, "Already socket available .");
//                Log.d(TAG, "Socket connected "+ct.mmSocket.isConnected());
//
//                closeConnection();
//            }
        }
    }

    public void closeConnection(){
        if(ct.mmSocket!=null){
            Log.d(TAG, "Closing an existing connection");
            ct.cancel();

        }
    }

    /**
     * Performs the device pairing.
     *
     * @param device the device to pair with.
     * @return true if the pairing was successful, false otherwise.
     */
    public boolean pair(BluetoothDevice device) {
        // Stops the discovery and then creates the pairing.
        if (bluetooth.isDiscovering()) {
            Log.d(TAG, "Bluetooth cancelling discovery.");
            bluetooth.cancelDiscovery();
        }
        Log.d(TAG, "Bluetooth bonding with device: " + deviceToString(device));
        boolean outcome = device.createBond();
        Log.d(TAG, "Bounding outcome : " + outcome);

        // If the outcome is true, we are bounding with this device.
        if (outcome == true) {
            this.boundingDevice = device;
        }
        return outcome;
    }

    /**
     * Checks if a device is already paired.
     *
     * @param device the device to check.
     * @return true if it is already paired, false otherwise.
     */
    public boolean isAlreadyPaired(BluetoothDevice device) {
        return bluetooth.getBondedDevices().contains(device);
    }

    /**
     * Converts a BluetoothDevice to its String representation.
     *
     * @param device the device to convert to String.
     * @return a String representation of the device.
     */
    public static String deviceToString(BluetoothDevice device) {
        return "[Address: " + device.getAddress() + ", Name: " + device.getName() + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.broadcastReceiverDelegator.close();
    }

    /**
     * Checks if a deviceDiscovery is currently running.
     *
     * @return true if a deviceDiscovery is currently running, false otherwise.
     */
    public boolean isDiscovering() {
        return bluetooth.isDiscovering();
    }

    /**
     * Cancels a device discovery.
     */
    public void cancelDiscovery() {
        if(bluetooth != null) {
            bluetooth.cancelDiscovery();
            broadcastReceiverDelegator.onDeviceDiscoveryEnd();
        }
    }

    /**
     * Turns on the Bluetooth and executes a device discovery when the Bluetooth has turned on.
     */
    public void turnOnBluetoothAndScheduleDiscovery() {
        this.bluetoothDiscoveryScheduled = true;
        turnOnBluetooth();
    }

    /**
     * Called when the Bluetooth status changed.
     */
    public void onBluetoothStatusChanged() {
        // Does anything only if a device discovery has been scheduled.
        if (bluetoothDiscoveryScheduled) {

            int bluetoothState = bluetooth.getState();
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_ON:
                    // Bluetooth is ON.
                    Log.d(TAG, "Bluetooth succesfully enabled, starting discovery");
                    startDiscovery();
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false;
                    break;
                case BluetoothAdapter.STATE_OFF:
                    // Bluetooth is OFF.
                    Log.d(TAG, "Error while turning Bluetooth on.");
                    Toast.makeText(context, "Error while turning Bluetooth on.", Toast.LENGTH_SHORT);
                    // Resets the flag since this discovery has been performed.
                    bluetoothDiscoveryScheduled = false;
                    break;
                default:
                    // Bluetooth is turning ON or OFF. Ignore.
                    break;
            }
        }
    }

    /**
     * Returns the status of the current pairing and cleans up the state if the pairing is done.
     *
     * @return the current pairing status.
     * @see BluetoothDevice#getBondState()
     */
    public int getPairingDeviceStatus() {
        if (this.boundingDevice == null) {
            throw new IllegalStateException("No device currently bounding");
        }
        int bondState = this.boundingDevice.getBondState();
        // If the new state is not BOND_BONDING, the pairing is finished, cleans up the state.
        if (bondState != BluetoothDevice.BOND_BONDING) {
            this.boundingDevice = null;
        }
        return bondState;
    }

    /**
     * Gets the name of the currently pairing device.
     *
     * @return the name of the currently pairing device.
     */
    public String getPairingDeviceName() {
        return getDeviceName(this.boundingDevice);
    }



    /**
     * Gets the name of a device. If the device name is not available, returns the device address.
     *
     * @param device the device whose name to return.
     * @return the name of the device or its address if the name is not available.
     */
    public static String getDeviceName(BluetoothDevice device) {
        String deviceName = device.getName();
        if (deviceName == null) {
            deviceName = device.getAddress();
        }
        return deviceName;
    }

    /**
     * Returns if there's a pairing currently being done through this app.
     *
     * @return true if a pairing is in progress through this app, false otherwise.
     */
    public boolean isPairingInProgress() {
        return this.boundingDevice != null;
    }

    /**
     * Gets the currently bounding device.
     *
     * @return the {@link #boundingDevice}.
     */
    public BluetoothDevice getBoundingDevice() {
        return boundingDevice;
    }


    private class ReadWriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ReadWriteThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

//         UUID : 0000110e-0000-1000-8000-00805f9b34fb
//             UUID : 00000000-0000-1000-8000-00805f9b34fb
//             UUID : 00000000-0000-1000-8000-00805f9b34fb
//             UUID : cbbfe0e2-f7f3-4206-84e0-84cbb3d09dfc

//        00001101-0000-1000-8000-00805F9B34FB
         UUID MY_UUID = UUID.fromString("cbbfe0e2-f7f3-4206-84e0-84cbb3d09dfc");
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;


            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetooth.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.e(TAG, "Close the socket");
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
            Log.e(TAG, "Connection attempt successfull");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(TAG, "Connection Type "+mmSocket.getConnectionType());
                Log.e(TAG,mmSocket.getRemoteDevice().getName());
                Log.e(TAG, String.valueOf(mmSocket.getRemoteDevice().getBondState()));
                Log.e(TAG, String.valueOf(mmSocket.isConnected()));
            }
            ReadWriteThread rt = new ReadWriteThread(mmSocket);
            rt.start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

}
