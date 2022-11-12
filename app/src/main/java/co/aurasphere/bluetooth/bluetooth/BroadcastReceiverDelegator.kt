
package co.aurasphere.bluetooth.bluetooth

import co.aurasphere.bluetooth.bluetooth.BluetoothController.Companion.deviceToString

import android.content.BroadcastReceiver
import android.content.Intent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import java.io.Closeable

/**
 * Class used to handle communication with the OS about Bluetooth system events.
 *
 */
class BroadcastReceiverDelegator(
    /**
     * The context of this object.
     */
    private val context: Context,
    /**
     * Callback for Bluetooth events.
     */
    private val listener: BluetoothDiscoveryDeviceListener, bluetooth: BluetoothController?
) : BroadcastReceiver(), Closeable {
    /**
     * Tag string used for logging.
     */
    private val TAG = "BroadcastReceiver"

    /**
     * {@inheritDoc}
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Incoming intent : $action")
        when (action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "Device discovered! " + device?.let { deviceToString(it) })
                listener.onDeviceDiscovered(device)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                // Discovery has ended.
                Log.d(TAG, "Discovery ended.")
                listener.onDeviceDiscoveryEnd()
            }
//            BluetoothAdapter.ACTION_STATE_CHANGED -> {
//                // Discovery state changed.
//                Log.d(TAG, "Bluetooth state changed.")
//                listener.onBluetoothStatusChanged()
//            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                // Pairing state has changed.
                Log.d(TAG, "Bluetooth bonding state changed.")
                listener.onDevicePairingEnded()
            }
            else -> {}
        }
    }

    /**
     * Called when device discovery starts.
     */
    fun onDeviceDiscoveryStarted() {
        listener.onDeviceDiscoveryStarted()
    }

    /**
     * Called when device discovery ends.
     */
    fun onDeviceDiscoveryEnd() {
        listener.onDeviceDiscoveryEnd()
    }

    /**
     * Called when the Bluetooth has been enabled.
     */
    fun onBluetoothTurningOn() {
        listener.onBluetoothTurningOn()
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        context.unregisterReceiver(this)
    }

    /**
     * Instantiates a new BroadcastReceiverDelegator.
     *
     * @param context   the context of this object.
     * @param listener  a callback for handling Bluetooth events.
     * @param bluetooth a controller for the Bluetooth.
     */
    init {
        listener.setBluetoothController(bluetooth)

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(this, filter)
    }
}