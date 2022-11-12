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
package co.aurasphere.bluetooth

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.*
import android.support.annotation.RequiresApi
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import co.aurasphere.bluetooth.bluetooth.*
import co.aurasphere.bluetooth.bluetooth.BluetoothController.Companion.deviceToString
import co.aurasphere.bluetooth.bluetooth.BluetoothController.Companion.getDeviceName
import co.aurasphere.bluetooth.view.DeviceRecyclerViewAdapter
import co.aurasphere.bluetooth.view.ListInteractionListener
import co.aurasphere.bluetooth.view.RecyclerViewProgressEmptySupport
import org.json.JSONObject


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

    lateinit var vibrator: Vibrator
    /**
     * Progress dialog shown during the pairing process.
     */
    private var bondingProgressDialog: ProgressDialog? = null
    lateinit var buttons : ArrayList<Button>

    /**
     * Adapter for the recycler view.
     */
    private var recyclerViewAdapter: DeviceRecyclerViewAdapter? = null
    private var recyclerView: RecyclerViewProgressEmptySupport? = null
    private var deviceConnectedLayout : RelativeLayout ? = null
    private var controllerLayout : TableLayout? = null

    /**
     * {@inheritDoc}
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        // Changes the theme back from the splashscreen. It's very important that this is called
        // BEFORE onCreate.
        SystemClock.sleep(resources.getInteger(R.integer.splashscreen_duration).toLong())
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vibrator = getSystemService(Vibrator::class.java)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        assignButtonActions()


        controllerLayout = findViewById<RelativeLayout>(R.id.controller_view) as TableLayout

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
            val obj = JSONObject()
            try {
                obj.put("type", "KEY")
                obj.put("keylist", findViewById<EditText>(R.id.et_send).text.toString())
                obj.put("pressed",true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bluetoothService?.sendMessage(obj.toString())
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
            bluetoothService?.mmSocket?.close()
            controllerLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }


        findViewById<Button>(R.id.btn_listen).setOnClickListener {
            Log.d(TAG,"Attempt to receive messages ")
            listenForConnection()
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

    fun vibrate() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
//        } else {
//            vibrator.vibrate(
//                VibrationEffect.createOneShot(
//                    25,
//                    VibrationEffect.DEFAULT_AMPLITUDE
//                )
//            )
//        }
    }

    fun prepareAndStartForeground(){

    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun startService() {
//
////        if(BluetoothHidService.isRunning)
////            return
////
////        val serviceIntent = Intent(this, BluetoothHidService::class.java)
//////        createUIHandler()
////        BluetoothHidService.bluetoothDevice = bluetoothService?.boundingDevice
////        startForegroundService(serviceIntent)
//
//        val serviceIntent = Intent(this,BluetoothHidService::class.java)
//        startForegroundService(serviceIntent)
//    }





    private fun stopService() {
//        val serviceIntent = Intent(this, BluetoothHidService::class.java)
        val serviceIntent = Intent(this,MyService::class.java)
        stopService(serviceIntent)
    }

    //    @SuppressLint("MissingPermission")
    //    @Override
    //    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    //        super.onActivityResult(requestCode, resultCode, data);
    //        debug("onActivityResult requestCode" + requestCode + " resultCode=" + resultCode);
    //        if (requestCode == REQUEST_CODE_BT_DEVICE_SELECTED) {
    //            if (resultCode == Activity.RESULT_OK && data != null) {
    //                BluetoothDevice deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
    //                if (deviceToPair != null) {
    //
    //                    IntentFilter intentFilter = new IntentFilter();
    //                    intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    //                    registerReceiver(getBroadcastReceiver(), intentFilter);
    //
    //                    debug("will now pair with " + deviceToPair.getName());
    //                    deviceToPair.createBond();
    //                }
    //            }
    //
    //        }
    //        if (requestCode == REQUEST_CODE_NOTIFICATION_TEST) {
    //            debug("NOTIFICATION!");
    //        }
    //    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun assignButtonActions() {
        val btnPower: Button = findViewById(R.id.btnPower)
        val btnMenu: Button = findViewById(R.id.btnMenu)
//        val btnPair: Button = findViewById(R.id.btnPair)
//        btnPair.setOnClickListener { v: View? ->
//            this.pairBtnAction(
//                v
//            )
//        }
        val btnLeft: Button = findViewById(R.id.btnLeft)
        val btnRight: Button = findViewById(R.id.btnRight)
        val btnUp: Button = findViewById(R.id.btnUp)
        val btnDown: Button = findViewById(R.id.btnDown)
        val btnMiddle: Button = findViewById(R.id.btnMiddle)
        val btnBack: Button = findViewById(R.id.btnBack)
        val btnHome: Button = findViewById(R.id.btnHome)
        val btnVolInc: Button = findViewById(R.id.btnVolInc)
        val btnVolDec: Button = findViewById(R.id.btnVolDec)
        val btnMute: Button = findViewById(R.id.btnMute)
        val btnPlayPause: Button = findViewById(R.id.btnPlayPause)
        val btnRewind: Button = findViewById(R.id.btnRewind)
        val btnForward: Button = findViewById(R.id.btnForward)
        val cancelController : Button = findViewById(R.id.btn_cancel_controller)


        cancelController.setOnClickListener {
            stopService()
            updateViewForDeviceList()
        }

         buttons = ArrayList<Button>()
        buttons.add(btnLeft)
        buttons.add(btnRight)
        buttons.add(btnUp)
        buttons.add(btnDown)
        buttons.add(btnMiddle)
        buttons.add(btnHome)
        buttons.add(btnBack)
        buttons.add(btnVolDec)
        buttons.add(btnVolInc)
        buttons.add(btnPlayPause)
        buttons.add(btnPower)
        buttons.add(btnMenu)
        buttons.add(btnMute)
        buttons.add(btnRewind)
        buttons.add(btnForward)
        //        buttons.add(btnSource);
        setButtonsEnabled(true)
        addRemoteKeyListeners(btnPower, RemoteControlHelper.Key.POWER)
//
//        btnUp.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.MENU_UP.get(0).toInt(),
//                RemoteControlHelper.Key.MENU_UP.get(1).toInt())
//        }
//
//        btnLeft.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.MENU_LEFT.get(0).toInt(),
//                RemoteControlHelper.Key.MENU_LEFT.get(1).toInt())
//        }
//        btnRight.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.MENU_RIGHT.get(0).toInt(),
//                RemoteControlHelper.Key.MENU_RIGHT.get(1).toInt())
//        }
//
//        btnDown.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.MENU_DOWN.get(0).toInt(),
//                RemoteControlHelper.Key.MENU_DOWN.get(1).toInt())
//        }

//        btnHome.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.HOME.get(0).toInt(),
//                RemoteControlHelper.Key.HOME.get(1).toInt())
//        }
//
//        btnVolInc.setOnClickListener {
//            bluetoothService?.send( RemoteControlHelper.Key.VOLUME_INC.get(0).toInt(),
//                RemoteControlHelper.Key.VOLUME_INC.get(1).toInt())
//        }
//
//        btnVolDec.setOnClickListener{
//            bluetoothService?.send( RemoteControlHelper.Key.VOLUME_DEC.get(0).toInt(),
//                RemoteControlHelper.Key.VOLUME_DEC.get(1).toInt())
//        }
//
//        btnBack.setOnClickListener{
//            bluetoothService?.send( RemoteControlHelper.Key.BACK.get(0).toInt(),
//                RemoteControlHelper.Key.BACK.get(1).toInt())
//        }
//
//        btnMiddle.setOnClickListener{
//            bluetoothService?.send( RemoteControlHelper.Key.MEDIA_SELECT_CD.get(0).toInt(),
//                RemoteControlHelper.Key.MEDIA_SELECT_CD.get(1).toInt())
//        }








//        addRemoteKeysListeners(btnSource, RemoteControlHelper.Key.ASSIGN_SELECTION, RemoteControlHelper.Key.MEDIA_SELECT_CD);
//        addRemoteKeyListeners(btnPair, RemoteControlHelper.Key.MEDIA_SELECT_CD);

//        addKeyBoardListeners(btnSource, 0x91);
//
        addRemoteKeyListeners(btnMenu, RemoteControlHelper.Key.MENU)
        addRemoteKeyListeners(btnLeft, RemoteControlHelper.Key.MENU_LEFT)
        addRemoteKeyListeners(btnRight, RemoteControlHelper.Key.MENU_RIGHT)
        addRemoteKeyListeners(btnUp, RemoteControlHelper.Key.MENU_UP)
        addRemoteKeyListeners(btnDown, RemoteControlHelper.Key.MENU_DOWN)
        addRemoteKeyListeners(btnMiddle, RemoteControlHelper.Key.MENU_PICK)
        addRemoteKeyListeners(btnBack, RemoteControlHelper.Key.BACK)
        addRemoteKeyListeners(btnHome, RemoteControlHelper.Key.HOME)
        addRemoteKeyListeners(btnVolInc, RemoteControlHelper.Key.VOLUME_INC)
        addRemoteKeyListeners(btnVolDec, RemoteControlHelper.Key.VOLUME_DEC)
//        addRemoteKeyListeners(btnMute, RemoteControlHelper.Key.MUTE)
//        addRemoteKeyListeners(btnPlayPause, RemoteControlHelper.Key.PLAY_PAUSE)
//        addRemoteKeyListeners(btnRewind, RemoteControlHelper.Key.MEDIA_REWIND)
//        addRemoteKeyListeners(btnForward, RemoteControlHelper.Key.MEDIA_FAST_FORWARD)
//        txtInput.setOnKeyListener(View.OnKeyListener { view: View?, keyCode: Int, keyEvent: KeyEvent? ->
//            this.handleInputText(
//                view,
//                keyCode,
//                keyEvent
//            )
//        })
//        txtInput.setOnKeyListener(this::handleRealtimeInputText);
//        txtInput.addTextChangedListener(getKeyTextWatcher());

    }

    private fun setButtonsEnabled(enabled: Boolean) {
        for (button in buttons) {
            button.isEnabled = enabled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addRemoteKeyListeners(button: Button,  keys: ByteArray) {
        button.setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val sent = bluetoothService?.let {
                    RemoteControlHelper.sendKeyDown(
                        keys[0].toInt(),
                        keys[1].toInt(), it
                    )
                }
//                if (sent) MainActivity.vibrate()
            }
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                val sent = bluetoothService?.let { RemoteControlHelper.sendKeyUp(it) }
            }
            false
        }
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
     val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun handleMessage(msg: Message) {

            Log.d("Message", msg.what.toString())
            //            FragmentActivity activity = getActivity();
            when(msg.what) {

                MessageConstants.CONNECTION_SUCCESSFULL ->{
                    updateView()
                }

                MessageConstants.CONNECTION_FAILED->{
                    updateViewForDeviceList()
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
                 MessageConstants.MESSAGE_WRITE -> {
//                    val writeBuf : ByteArray =  msg.obj as ByteArray
                    // construct a string from the buffer
//                    val writeMessage : String = String(writeBuf,0,msg.arg1)
                    Log.d(TAG,"Sent data to remote device");
                 }
                 MessageConstants.MESSAGE_READ -> {
                     val readBuf : ByteArray =  msg.obj as ByteArray
//                      construct a string from the valid bytes in the buffer
                     val readMessage =  String(readBuf, 0, msg.arg1)
                     findViewById<TextView>(R.id.et_receive).text = readMessage.toString()
                     Log.d(TAG,"Recevied data from remote device $readMessage");

                 }
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

    private fun updateViewForDeviceList() {
        runOnUiThread {
            controllerLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }




    private fun listenForConnection(){
        Toast.makeText(this,"Listening for available connections ...", Toast.LENGTH_SHORT).show()
        bluetoothService?.startListening()
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
        stopService()
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
            }
            for(uuid in device?.uuids!!){
                Log.d(TAG,"$uuid")
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateView() {

//        Log.d(TAG, "UUID : " + device.uuids.toString())
//        for (p in device.uuids) {
//            Log.d(TAG, "UUID : $p")
//        }
        if(bluetoothService?.mmSocket?.isConnected == true
        ){
            recyclerView?.visibility = View.GONE
            controllerLayout?.visibility = View.VISIBLE
            findViewById<Button>(R.id.btn_listen).visibility = View.GONE
        }

        else{
            findViewById<Button>(R.id.btn_listen).visibility = View.VISIBLE
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