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
package co.aurasphere.scanner

import android.app.ProgressDialog
import co.aurasphere.scanner.bluetooth.BluetoothController.Companion.deviceToString
import co.aurasphere.scanner.bluetooth.BluetoothController.Companion.getDeviceName
import android.support.v7.app.AppCompatActivity
import co.aurasphere.scanner.view.ListInteractionListener
import android.bluetooth.BluetoothDevice
import co.aurasphere.scanner.bluetooth.BluetoothController
import android.support.design.widget.FloatingActionButton
import co.aurasphere.scanner.view.DeviceRecyclerViewAdapter
import co.aurasphere.scanner.view.RecyclerViewProgressEmptySupport
import android.support.v7.widget.LinearLayoutManager
import android.content.pm.PackageManager
import android.bluetooth.BluetoothAdapter
import android.content.res.Configuration
import android.os.*
import android.provider.SyncStateContract
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import co.aurasphere.scanner.bluetooth.MessageConstants

/**
 * Main Activity of this application.
 *
 * @author Donato Rimenti
 */
class MainActivity : AppCompatActivity(), ListInteractionListener<BluetoothDevice?> {
    /**
     * The controller for Bluetooth functionalities.
     */
    private var bluetoothService: BluetoothController? = null

    /**
     * The Bluetooth discovery button.
     */
    private var fab: FloatingActionButton? = null

    /**
     * Progress dialog shown during the pairing process.
     */
    private var bondingProgressDialog: ProgressDialog? = null

    /**
     * Adapter for the recycler view.
     */
    private var recyclerViewAdapter: DeviceRecyclerViewAdapter? = null
    private var recyclerView: RecyclerViewProgressEmptySupport? = null
    private var deviceConnectedLayout : RelativeLayout ? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Changes the theme back from the splashscreen. It's very important that this is called
        // BEFORE onCreate.
        SystemClock.sleep(resources.getInteger(R.integer.splashscreen_duration).toLong())
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        deviceConnectedLayout = findViewById<RelativeLayout>(R.id.layout_connected) as RelativeLayout

        // Sets up the RecyclerView.
        recyclerViewAdapter = DeviceRecyclerViewAdapter(this)
        recyclerView = findViewById<View>(R.id.list) as RecyclerViewProgressEmptySupport
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        // Sets the view to show when the dataset is empty. IMPORTANT : this method must be called
        // before recyclerView.setAdapter().
        val emptyView = findViewById<View>(R.id.empty_list)
        recyclerView!!.setEmptyView(emptyView)

        findViewById<Button>(R.id.btn_send).setOnClickListener{
            bluetoothService?.sendMessage(findViewById<EditText>(R.id.et_send).toString())
        }
        // Sets the view to show during progress.
        val progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        recyclerView!!.setProgressView(progressBar)
        recyclerView!!.adapter = recyclerViewAdapter

        // [#11] Ensures that the Bluetooth is available on this device before proceeding.
        val hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        if (!hasBluetooth) {
            val dialog = AlertDialog.Builder(this@MainActivity).create()
            dialog.setTitle(getString(R.string.bluetooth_not_available_title))
            dialog.setMessage(getString(R.string.bluetooth_not_available_message))
            dialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, which -> // Closes the dialog and terminates the activity.
                dialog.dismiss()
                finish()
            }
            dialog.setCancelable(false)
            dialog.show()
        }

        // Sets up the bluetooth controller.
        bluetoothService = BluetoothController(
            this,
            BluetoothAdapter.getDefaultAdapter(),
            recyclerViewAdapter,
            mHandler
        )

        findViewById<View>(R.id.btn_cancel).setOnClickListener {
            deviceConnectedLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }




        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab!!.setOnClickListener { view ->
            // If the bluetooth is not enabled, turns it on.
            if (!bluetoothService!!.isBluetoothEnabled) {
                Snackbar.make(view, R.string.enabling_bluetooth, Snackbar.LENGTH_SHORT).show()
                bluetoothService!!.turnOnBluetoothAndScheduleDiscovery()
            } else {
                //Prevents the user from spamming the button and thus glitching the UI.
                if (!bluetoothService!!.isDiscovering) {
                    // Starts the discovery.
                    Snackbar.make(view, R.string.device_discovery_started, Snackbar.LENGTH_SHORT)
                        .show()
                    bluetoothService!!.startDiscovery()
                } else {
                    Snackbar.make(view, R.string.device_discovery_stopped, Snackbar.LENGTH_SHORT)
                        .show()
                    bluetoothService!!.cancelDiscovery()
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * {@inheritDoc}
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_about) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }



    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {

            Log.d("Message", msg.what.toString())
            //            FragmentActivity activity = getActivity();
            when(msg.what) {

                MessageConstants.CONNECTION_SUCCESSFULL ->{
                    updateView()
                }
//                case Constants.MESSAGE_STATE_CHANGE:
//                    switch (msg.arg1) {
//                        case BluetoothChatService.STATE_CONNECTED:
//                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
//                            break;
//                        case BluetoothChatService.STATE_CONNECTING:
//                            setStatus(R.string.title_connecting);
//                            break;
//                        case BluetoothChatService.STATE_LISTEN:
//                        case BluetoothChatService.STATE_NONE:
//                            setStatus(R.string.title_not_connected);
//                            break;
//                    }
//                    break;
//                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
//                    break;
//                case Constants.MESSAGE_READ:
//                    byte[] readBuf = (byte[]) msg.obj;
//                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
//                    break;
//                case Constants.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
//                    break;
//                case Constants.MESSAGE_TOAST:
//                    if (null != this) {
//                        Toast.makeText(this, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                    break;
//            }
        }
        }
    }

    /**
     * {@inheritDoc}
     */


    /**
     * {@inheritDoc}
     */
    override fun startLoading() {
        recyclerView!!.startLoading()

        // Changes the button icon.
        fab!!.setImageResource(R.drawable.ic_bluetooth_searching_white_24dp)
    }

    /**
     * {@inheritDoc}
     */
    override fun endLoading(partialResults: Boolean) {
        recyclerView!!.endLoading()

        // If discovery has ended, changes the button icon.
        if (!partialResults) {
            fab!!.setImageResource(R.drawable.ic_bluetooth_white_24dp)
        }
    }


    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        bluetoothService!!.close()
        super.onDestroy()
    }

    /**
     * {@inheritDoc}
     */
    override fun onRestart() {
        super.onRestart()
        // Stops the discovery.
        if (bluetoothService != null) {
            bluetoothService!!.cancelDiscovery()
        }
        // Cleans the view.
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.cleanView()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        super.onStop()
        // Stoops the discovery.
        if (bluetoothService != null) {
            bluetoothService!!.cancelDiscovery()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
    companion object {
        /**
         * Tag string used for logging.
         */
        private const val TAG = "MainActivity"
    }

    override fun onItemClick(device: BluetoothDevice?) {
        Log.d(TAG, "Item clicked : " + device?.let { deviceToString(it) })
        if (bluetoothService!!.isAlreadyPaired(device)) {
            if (device != null) {
                bluetoothService!!.connectToDevice(device)
//                Thread.sleep(1000)
            }
//            Log.d(TAG, "Device already paired!")
//            Toast.makeText(this, R.string.device_already_paired, Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Device not paired. Pairing.")
            val outcome = device?.let { bluetoothService!!.pair(it) }

            // Prints a message to the user.
            val deviceName = getDeviceName(device)
            if (outcome == true) {
                // The pairing has started, shows a progress dialog.
                Log.d(TAG, "Showing pairing dialog")
                bondingProgressDialog =
                    ProgressDialog.show(this, "", "Pairing with device $deviceName...", true, false)
            } else {
                Log.d(TAG, "Error while pairing with device $deviceName!")
                Toast.makeText(
                    this,
                    "Error while pairing with device $deviceName!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun updateView() {

//        Log.d(TAG, "UUID : " + device.uuids.toString())
//        for (p in device.uuids) {
//            Log.d(TAG, "UUID : $p")
//        }
        if(bluetoothService?.mmSocket?.isConnected == true
        ){
            recyclerView?.visibility = View.GONE
            deviceConnectedLayout?.visibility = View.VISIBLE
        }

        else{
            Toast.makeText(this,"Unable to connect to server",Toast.LENGTH_SHORT).show()
        }

    }

    override fun endLoadingWithDialog(error: Boolean, device: BluetoothDevice?) {
        if (bondingProgressDialog != null) {
            val view = findViewById<View>(R.id.main_content)
            val message: String
            val deviceName = getDeviceName(device)

            // Gets the message to print.
            message = if (error) {
                "Failed pairing with device $deviceName!"
            } else {
                "Succesfully paired with device $deviceName!"
            }

            // Dismisses the progress dialog and prints a message to the user.
            bondingProgressDialog!!.dismiss()
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()

            // Cleans up state.
            bondingProgressDialog = null
        }
    }

}