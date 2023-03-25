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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.*
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.RemoteInput
import androidx.core.content.res.ResourcesCompat.getFloat
import androidx.recyclerview.widget.LinearLayoutManager
import co.aurasphere.bluetooth.bluetooth.*
import co.aurasphere.bluetooth.bluetooth.BluetoothController.Companion.deviceToString
import co.aurasphere.bluetooth.bluetooth.BluetoothController.Companion.getDeviceName
import co.aurasphere.bluetooth.view.DPadView
import co.aurasphere.bluetooth.view.DeviceRecyclerViewAdapter
import co.aurasphere.bluetooth.view.ListInteractionListener
import co.aurasphere.bluetooth.view.RecyclerViewProgressEmptySupport
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.util.function.IntConsumer


/**
 * Main Activity of this application.
 *
 */
class MainActivity : ComponentActivity(), ListInteractionListener<BluetoothDevice?> {
    /**
     * The controller for Bluetooth functionalities.
     */
    private var bluetoothService: BluetoothController? = null

    private lateinit var keyboardPeripheral: RemoteInput

    /**
     * The Bluetooth discovery button.
     */
    private var fab: FloatingActionButton? = null

    lateinit var vibrator: Vibrator

    /**
     * Progress dialog shown during the pairing process.
     */
    private var bondingProgressDialog: ProgressDialog? = null
    lateinit var buttons: ArrayList<View>

    /**
     * Adapter for the recycler view.
     */
    private var recyclerViewAdapter: DeviceRecyclerViewAdapter? = null
    private var recyclerView: RecyclerViewProgressEmptySupport? = null
    private var controllerLayout: ConstraintLayout? = null
    lateinit var txtInput: EditText
    lateinit var mGatt: BluetoothGatt

    val accessFineLocationPermission
        get() = checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val coarseLocationPermission
        get() = checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val bluetoothScanPermission
        get() = checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    val bluetoothConnectPermission
        get() = checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun requestBlePermissions(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ActivityCompat.requestPermissions(
            activity!!,
            ANDROID_12_BLE_PERMISSIONS,
            requestCode
        ) else {

        }
    }

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

//        setSupportActionBar(toolbar)

        requestBlePermissions(this,123)
        assignButtonActions()


        controllerLayout = findViewById<ConstraintLayout>(R.id.controller_view) as ConstraintLayout


        // Sets up the RecyclerView.
        recyclerViewAdapter = DeviceRecyclerViewAdapter(this)
        recyclerView = findViewById<View>(R.id.list) as RecyclerViewProgressEmptySupport
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        // Sets the view to show when the dataset is empty. IMPORTANT : this method must be called
        // before recyclerView.setAdapter().
        val emptyView = findViewById<View>(R.id.empty_list)
        recyclerView!!.setEmptyView(emptyView)


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

        findViewById<View>(R.id.cancel).setOnClickListener {
            bluetoothService?.mmSocket?.close()
            controllerLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            fab?.visibility= View.VISIBLE
        }


//        findViewById<Button>(R.id.btn_listen).setOnClickListener {
//            Log.d(TAG, "Attempt to receive messages ")
//            listenForConnection()
//        }

        fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab!!.setOnClickListener { view ->
            // If the bluetooth is not enabled, turns it on.
            if (!bluetoothService!!.isBluetoothEnabled) {
                Snackbar.make(view, R.string.enabling_bluetooth, Snackbar.LENGTH_SHORT).show()
//                if (checkForBluetoothAndLocationPermission())
                    bluetoothService!!.turnOnBluetoothAndScheduleDiscovery()
//                else {
//                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
//                }
            } else {
                //Prevents the user from spamming the button and thus glitching the UI.
//                if (checkForBluetoothAndLocationPermission()) {
                    if (!bluetoothService!!.isDiscovering) {
                        // Starts the discovery.
                        Snackbar.make(
                            view,
                            R.string.device_discovery_started,
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        bluetoothService!!.startDiscovery()
                    } else {
                        Snackbar.make(
                            view,
                            R.string.device_discovery_stopped,
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        bluetoothService!!.cancelDiscovery()
                    }
                }

            }
        }


    fun isLocationEnabled(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String

        locationMode = try {
            Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE)
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }


    fun checkForBluetoothAndLocationPermission(): Boolean {
        if (checkForLocationPermissions() && checkForBluetoothPermissions()) {
            return true
        }


        return false
    }

    fun checkForBluetoothPermissions(): Boolean {

        if (bluetoothScanPermission && bluetoothConnectPermission) return true
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            }
            return bluetoothScanPermission && bluetoothConnectPermission
        }

    }

    fun checkForLocationPermissions(): Boolean {

        if (accessFineLocationPermission && coarseLocationPermission && isLocationEnabled(this)) return true
        else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return accessFineLocationPermission && coarseLocationPermission
        }
    }

    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Toast.makeText(this, "Precise location access granted", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                Toast.makeText(this, "Coarse location access granted", Toast.LENGTH_SHORT).show()

            }
            else -> {
                Toast.makeText(this, "No location access granted", Toast.LENGTH_SHORT).show()

                // No location access granted.
            }
        }
    }

    val bluetoothPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false) -> {
                // Precise location access granted.
                Toast.makeText(this, "Bluetooth scan permission granted", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false) -> {
                // Only approximate location access granted.
                Toast.makeText(this, "Bluetooth permission granted", Toast.LENGTH_SHORT).show()

            }
            else -> {
                Toast.makeText(this, "No bluetooth permission granted", Toast.LENGTH_SHORT).show()

                // No location access granted.
            }
        }
    }


    val PERMISSIONS_LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission_group.LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_PRIVILEGED
    )

    fun checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothService!!.turnOnBluetoothAndScheduleDiscovery()
                bluetoothService?.startDiscovery()

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    getRequiredPermissions(), 1
                );
            }
        }

    }


    private fun getRequiredPermissions(): Array<String> {
        val targetSdkVersion = applicationInfo.targetSdkVersion
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == 1) {
//
//            bluetoothService!!.turnOnBluetoothAndScheduleDiscovery()
//            bluetoothService?.startDiscovery()
//
//        } else {
//            checkPermissions()
//        }
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
        return super.onOptionsItemSelected(item)
    }

    fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    25,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
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
//        val serviceIntent = Intent(this, MyService::class.java)
//        stopService(serviceIntent)
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
        setupCustomDpadActions()
        val btnPower: View = findViewById(R.id.ntfBtnPower)
//        val btnMenu: Button = findViewById(R.id.btnMenu)
//        val btnPair: Button = findViewById(R.id.btnPair)
//        btnPair.setOnClickListener { v: View? ->
//            this.pairBtnAction(
//                v
//            )
//        }
//        txtInput = findViewById(R.id.txtInput)
//        val btnLeft: View = findViewById(R.id.ic_chev_left)
//        val btnRight: View = findViewById(R.id.ic_chev_right)
//        val btnUp: View = findViewById(R.id.ic_chev_top)
//        val btnDown: View = findViewById(R.id.ic_chev_down)
        val btnBack: View = findViewById(R.id.ntfBtnBack)

        val circularDpadUp : View = findViewById(R.id.dpad_custom)
        val btnHome: View = findViewById(R.id.home)
        val btnVolInc: View = findViewById(R.id.vol_plus)
        val btnVolDec: View = findViewById(R.id.vol_minus)
        val btnMute: View = findViewById(R.id.mute)
//        val btnPlayPause: Button = findViewById(R.id.btnPlayPause)
//        val btnRewind: Button = findViewById(R.id.btnRewind)
//        val btnForward: Button = findViewById(R.id.btnForward)
        val cancelController: View = findViewById(R.id.cancel)
        val btnChUp: View = findViewById(R.id.ch_up)
        val btnChDown: View = findViewById(R.id.ch_down)
//        val btnRecord: Button = findViewById(R.id.record)
////        val btn2 : Button = findViewById(R.id.btn2)
        val epg: View = findViewById(R.id.guideline)
//        val btnInfo: Button = findViewById(R.id.info)
//        val btnMagenta: Button = findViewById(R.id.btnMagenta)


//        cancelController.setOnClickListener {
//            stopService()
//            updateViewForDeviceList()
//        }

        buttons = ArrayList<View>()
//        buttons.add(btnLeft)
//        buttons.add(btnRight)
//        buttons.add(btnUp)
        buttons.add(cancelController)
        buttons.add(circularDpadUp)
//        buttons.add(btnDown)
        buttons.add(btnHome)
        buttons.add(btnBack)
        buttons.add(btnVolDec)
        buttons.add(btnVolInc)
//        buttons.add(btnPlayPause)
        buttons.add(btnPower)
//        buttons.add(btnMenu)
        buttons.add(btnMute)
//        buttons.add(btnRewind)
//        buttons.add(btnForward)
        buttons.add(btnChUp)
        buttons.add(btnChDown)
        buttons.apply {
//            add(btnRecord)
//            add(btnInfo)
//            add(epg)
//            add(btnMagenta)
        }
        //        addKeyBoardListeners(btnSource, 0x91);
        //        buttons.add(btnSource);

        setButtonsEnabled(true)
        buttons.forEach {

        }
//        addRemoteKeyListeners()
        addRemoteKeyListeners(btnPower, RemoteControlHelper.Key.POWER)
        addRemoteKeyListeners(epg, RemoteControlHelper.Key.EPG)
//        addRemoteKeyListeners(btnMenu, RemoteControlHelper.Key.MENU)
//        addRemoteKeyListeners(btnLeft, RemoteControlHelper.Key.MENU_LEFT)
//        addRemoteKeyListeners(btnRight, RemoteControlHelper.Key.MENU_RIGHT)
//        addRemoteKeyListeners(btnUp, RemoteControlHelper.Key.MENU_UP)
//        addRemoteKeyListeners(btnDown, RemoteControlHelper.Key.MENU_DOWN)
        addRemoteKeyListeners(btnBack, RemoteControlHelper.Key.BACK)
        addRemoteKeyListeners(btnHome, RemoteControlHelper.Key.HOME)
        addRemoteKeyListeners(btnVolInc, RemoteControlHelper.Key.VOLUME_INC)
        addRemoteKeyListeners(btnVolDec, RemoteControlHelper.Key.VOLUME_DEC)
        addRemoteKeyListeners(btnChUp, RemoteControlHelper.Key.CHANNEL_UP)
        addRemoteKeyListeners(btnChDown, RemoteControlHelper.Key.CHANNEL_DOWN)
//        addRemoteKeyListeners(btnInfo, RemoteControlHelper.Key.INFO)
//        addRemoteKeyListeners(btnRecord, RemoteControlHelper.Key.RECORD)
//        addRemoteKeyListeners(btnMagenta, RemoteControlHelper.Key.MAGENTA)
        addRemoteKeyListeners(btnMute, RemoteControlHelper.Key.MUTE)
//        addRemoteKeyListeners(btnPlayPause, RemoteControlHelper.Key.PLAY_PAUSE)
//        addRemoteKeyListeners(btnRewind, RemoteControlHelper.Key.MEDIA_REWIND)
//        addRemoteKeyListeners(btnForward, RemoteControlHelper.Key.MEDIA_FAST_FORWARD)
//        txtInput.setOnKeyListener(this::handleInputText)

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupCustomDpadActions() {

        val dpad = findViewById<DPadView>(R.id.dpad_custom)
        dpad.modify {

            isHapticFeedbackEnabled = true
            normalColor = getColor(R.color.grey)
            pressedColor = getColor(R.color.white_75_opaque)
            padding = 20f
            directionSectionAngle = 90f

            isCenterCircleEnabled = true
            isCenterCirclePressEnabled = true

            centerCircleNormalColor = getColor(R.color.dark_grey)
            centerCirclePressedColor = getColor(R.color.white_75_opaque)

            centerCircleRatio = 5f
            centerIcon = null

            centerText = "OK"

            centerIconTint = 0

            centerTextSize = 36f

            var style = 0
            style = style or DPadView.TextStyle.BOLD.style


            centerTextStyle = style

            centerTextColor = getColor(R.color.white)
        }


        dpad.onDirectionPressListener = { direction, action ->

            when (direction) {

                DPadView.Direction.UP -> addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_UP)
                DPadView.Direction.DOWN -> addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_DOWN)
                DPadView.Direction.LEFT -> addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_LEFT)
                DPadView.Direction.RIGHT -> addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_RIGHT)
                DPadView.Direction.CENTER -> addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_PICK)


            }
            val text = StringBuilder()
            val directionText = direction?.name ?: ""
            if (directionText.isNotEmpty()) {
                text.append("Direction:\t")
            }
            text.append(directionText)
            if (directionText.isNotEmpty()) {
                text.append("\nAction:\t")
                val actionText = when (action) {
                    MotionEvent.ACTION_DOWN -> "Down"
                    MotionEvent.ACTION_UP -> "Up"
                    MotionEvent.ACTION_MOVE -> "Move"
                    else -> action.toString()
                }
                text.append(actionText)
            }
//            findViewById<TextView>(R.id.tv_sample).text = text;

            dpad.onDirectionClickListener = {
                it?.let {
                    Log.i("directionPress", it.name)
                    when (it) {
//                    DPadView.Direction.UP -> addRemoteKeyListenersForDpad()

                    }
                }

                dpad.setOnClickListener {
                    Log.i("Click", "Done")
//                    addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_PICK)
                }

                dpad.onCenterLongClick = {
                    addRemoteKeyListenersForDpad(action,RemoteControlHelper.Key.MENU_PICK)
                }
            }


        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleInputText(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
//            debug("onKey=" + txtInput.getText().toString());
            txtInput?.getText().chars().forEach(IntConsumer { c: Int ->
//                debug((char) c + "");
                if (KeyboardHelper.keyMap.containsKey(c.toChar())) { // Small case letter
                    bluetoothService?.let {
                        KeyboardHelper.sendKeyDown(
                            KeyboardHelper.Modifier.NONE,
                            KeyboardHelper.getKey(c.toChar()),
                            it
                        )
                    }
                    bluetoothService?.let { KeyboardHelper.sendKeyUp(it) }
                } else if (KeyboardHelper.shiftKeyMap.containsKey(c.toChar())) { // Upper case letter
                    bluetoothService?.let {
                        KeyboardHelper.sendKeyDown(
                            KeyboardHelper.Modifier.KEY_MOD_LSHIFT,
                            KeyboardHelper.getShiftKey(c.toChar()),
                            it
                        )
                    }
                    bluetoothService?.let { KeyboardHelper.sendKeyUp(it) }
                }
            })
            //            boolean sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.Key.ENTER);
//            if (sent)
//            MainActivity.vibrate()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && keyEvent.action == KeyEvent.ACTION_UP) {
//            debug("onKey= BACKSPACE");
            val sent: Boolean = bluetoothService?.let {
                KeyboardHelper.sendKeyDown(
                    KeyboardHelper.Modifier.NONE,
                    KeyboardHelper.Key.BACKSPACE,
                    it
                )
            } == true
            bluetoothService?.let { KeyboardHelper.sendKeyUp(it) }
//            if (sent) MainActivity.vibrate()
            return true
        }
        return false
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        for (button in buttons) {
            button.isEnabled = enabled
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ClickableViewAccessibility")
    private fun addRemoteKeyListeners(button: View, keys: ByteArray) {

        button.setOnTouchListener { view: View?, motionEvent: MotionEvent ->
            val now = System.currentTimeMillis()
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val sent = bluetoothService?.let {
                    RemoteControlHelper.sendKeyDown(
                        keys[0].toInt(),
                        keys[1].toInt(), it
                    )
                }
                button.background = AppCompatResources.getDrawable(this@MainActivity,R.drawable.ic_round_white)
                if (sent == true) vibrate()
            }
           else if (motionEvent.action == MotionEvent.ACTION_UP) {
                val sent = bluetoothService?.let { RemoteControlHelper.sendKeyUp(it) }
                if (button.id == R.id.ntfBtnPower) {
                    button.background =
                        AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_round_red)
                } else {
                    button.background =
                        AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_round)
                }
                    return@setOnTouchListener true
            }

            else if (motionEvent.action ==MotionEvent.ACTION_MOVE){

                Log.e(TAG,"Move");
                return@setOnTouchListener true

            }
            false
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun addRemoteKeyListenersForDpad(action: Int, keys: ByteArray) {

            if (action == MotionEvent.ACTION_DOWN) {
                val sent = bluetoothService?.let {
                    RemoteControlHelper.sendKeyDown(
                        keys[0].toInt(),
                        keys[1].toInt(), it
                    )
                }
//                if (sent) MainActivity.vibrate()
            }
            else if (action == MotionEvent.ACTION_UP) {
                val sent = bluetoothService?.let { RemoteControlHelper.sendKeyUp(it) }
            }

            else if (action ==MotionEvent.ACTION_MOVE){

                Log.e(TAG,"Move");

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
            when (msg.what) {

                MessageConstants.CONNECTION_SUCCESSFULL -> {
                    updateView()
                }

                MessageConstants.CONNECTION_FAILED -> {
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
                    Log.d(TAG, "Sent data to remote device");
                }
                MessageConstants.MESSAGE_READ -> {
                    val readBuf: ByteArray = msg.obj as ByteArray
//                      construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
//                    findViewById<TextView>(R.id.et_receive).text = readMessage.toString()
                    Log.d(TAG, "Recevied data from remote device $readMessage");

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
            fab?.visibility = View.VISIBLE
        }
    }


    private fun listenForConnection() {
        Toast.makeText(this, "Listening for available connections ...", Toast.LENGTH_SHORT).show()
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
        updateViewForDeviceList()

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
                bluetoothService!!.cancelDiscovery()
                bluetoothService!!.connectToDevice(device)
//                bluetoothService.sendMessage()
//                if (checkForBluetoothAndLocationPermission()) {
//                    connectToDevice(device)
//                } else {
//                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
//                }
            }
            for (uuid in device?.uuids!!) {
                Log.d(TAG, "$uuid")
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

    fun connectToDevice(device: BluetoothDevice) {
        mGatt = device.connectGatt(this, false, gattCallback)

    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("onConnectionStateChange", "Status: $status")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("gattCallback", "STATE_CONNECTED")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> Log.e("gattCallback", "STATE_DISCONNECTED")
                else -> Log.e("gattCallback", "STATE_OTHER")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val services = gatt.services
            Log.i("onServicesDiscovered", services.toString())
//            displayGattServices(services)
            gatt.readCharacteristic(services[1].characteristics[0])
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            Log.i("onCharacteristicRead", characteristic.toString())
            gatt.disconnect()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun updateView() {

        if (bluetoothService?.mmSocket?.isConnected == true
        ) {
            recyclerView?.visibility = View.GONE
            controllerLayout?.visibility = View.VISIBLE
//            findViewById<Button>(R.id.btn_listen).visibility = View.GONE
            fab?.visibility = View.GONE
        } else {
//            findViewById<Button>(R.id.btn_listen).visibility = View.VISIBLE
            recyclerView?.visibility = View.VISIBLE
            fab?.visibility = View.VISIBLE
            controllerLayout?.visibility = View.GONE
            Toast.makeText(this, "Unable to connect to server", Toast.LENGTH_SHORT).show()
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