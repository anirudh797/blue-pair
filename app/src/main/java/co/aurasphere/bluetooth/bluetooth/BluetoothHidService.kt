package co.aurasphere.bluetooth.bluetooth

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.content.ComponentName
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.RemoteViews
import co.aurasphere.bluetooth.MainActivity
import co.aurasphere.bluetooth.R

//class BluetoothHidService : Service(), BluetoothProfile.ServiceListener {
//    annotation class WHAT {
//        companion object {
//            var BLUETOOTH_CONNECTING = 1
//            var BLUETOOTH_CONNECTED = 2
//            var BLUETOOTH_DISCONNECTED = 3
//        }
//    }
//
//    private var bluetoothAdapter: BluetoothAdapter? = null
//    private fun debug(msg: String) {
//        Log.e(TAG, "------------------------- $msg")
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private fun init() {
////        val bluetoothManager = getSystemService(
////            BluetoothManager::class.java
////        )
////        bluetoothAdapter = bluetoothManager.adapter
////        bluetoothAdapter?.getProfileProxy(this, this, BluetoothProfile.HID_DEVICE)
////        startAsForeground()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.M)
//    override fun onBind(intent: Intent): IBinder {
//        debug("onBind")
//        val binder = Binder()
//        init()
//        return binder
//    }
//
//    @Nullable
//    override fun startForegroundService(service: Intent): ComponentName? {
//        debug("startForegroundService")
//        return super.startForegroundService(service)
//    }
//
//    override fun onCreate() {
//        init()
//        super.onCreate()
//        debug("onCreate")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    @SuppressLint("MissingPermission")
//    private fun startAsForeground() {
//        val CHANNEL_ID = "Bluetooth Remote Service"
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Bluetooth Remote Service",
//            NotificationManager.IMPORTANCE_MIN
//        )
//        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            Intent(this, MainActivity::class.java),
//            PendingIntent.FLAG_IMMUTABLE
//        )
//        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
//            .setContentTitle("Bluetooth Remote")
//            .setContentIntent(pendingIntent)
//            .setCustomBigContentView(notificationButtonsRemoteViews)
//            .build()
//        startForeground(1, notification)
//        isRunning = true
//    }
//
//    private val notificationButtonsRemoteViews: RemoteViews
//        @SuppressLint("RemoteViewLayout") private get() {
//            val powerIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            powerIntent.action = ACTION_POWER
//            val powerPI =
//                PendingIntent.getBroadcast(this, 0, powerIntent, PendingIntent.FLAG_IMMUTABLE)
//            val muteIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            muteIntent.action = ACTION_MUTE
//            val mutePI =
//                PendingIntent.getBroadcast(this, 0, muteIntent, PendingIntent.FLAG_IMMUTABLE)
//            val upIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            upIntent.action = ACTION_UP
//            val upPI = PendingIntent.getBroadcast(this, 0, upIntent, PendingIntent.FLAG_IMMUTABLE)
//            val menuIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            menuIntent.action = ACTION_MENU
//            val menuPI =
//                PendingIntent.getBroadcast(this, 0, menuIntent, PendingIntent.FLAG_IMMUTABLE)
//            val homeIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            homeIntent.action = ACTION_HOME
//            val homePI =
//                PendingIntent.getBroadcast(this, 0, homeIntent, PendingIntent.FLAG_IMMUTABLE)
//            val leftIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            leftIntent.action = ACTION_LEFT
//            val leftPI =
//                PendingIntent.getBroadcast(this, 0, leftIntent, PendingIntent.FLAG_IMMUTABLE)
//            val middleIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            middleIntent.action = ACTION_MIDDLE
//            val middlePI =
//                PendingIntent.getBroadcast(this, 0, middleIntent, PendingIntent.FLAG_IMMUTABLE)
//            val rightIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            rightIntent.action = ACTION_RIGHT
//            val rightPI =
//                PendingIntent.getBroadcast(this, 0, rightIntent, PendingIntent.FLAG_IMMUTABLE)
//            val backIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            backIntent.action = ACTION_BACK
//            val backPI =
//                PendingIntent.getBroadcast(this, 0, backIntent, PendingIntent.FLAG_IMMUTABLE)
//            val downIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            downIntent.action = ACTION_DOWN
//            val downPI =
//                PendingIntent.getBroadcast(this, 0, downIntent, PendingIntent.FLAG_IMMUTABLE)
//            val rewindIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            rewindIntent.action = ACTION_REWIND
//            val rewindPI =
//                PendingIntent.getBroadcast(this, 0, rewindIntent, PendingIntent.FLAG_IMMUTABLE)
//            val forwardIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            forwardIntent.action = ACTION_FORWARD
//            val forwardPI =
//                PendingIntent.getBroadcast(this, 0, forwardIntent, PendingIntent.FLAG_IMMUTABLE)
//            val playPauseIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            playPauseIntent.action = ACTION_PLAY_PAUSE
//            val playPausePI =
//                PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
//            val volIncIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            volIncIntent.action = ACTION_VOL_INC
//            val volIncPI =
//                PendingIntent.getBroadcast(this, 0, volIncIntent, PendingIntent.FLAG_IMMUTABLE)
//            val volDecIntent = Intent(this, NotificationBroadcastReceiver::class.java)
//            volDecIntent.action = ACTION_VOL_DEC
//            val volDecPI =
//                PendingIntent.getBroadcast(this, 0, volDecIntent, PendingIntent.FLAG_IMMUTABLE)
//            val remoteViews = RemoteViews(packageName, R.layout.notification_buttons)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnPower, powerPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnMute, mutePI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnUp, upPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnMenu, menuPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnHome, homePI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnLeft, leftPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnMiddle, middlePI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnRight, rightPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnBack, backPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnDown, downPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnVolInc, volIncPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnRewind, rewindPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnForward, forwardPI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnPlayPause, playPausePI)
//            remoteViews.setOnClickPendingIntent(R.id.ntfBtnVolDec, volDecPI)
//            return remoteViews
//        }
//
//    @SuppressLint("MissingPermission")
//    override fun onDestroy() {
//        super.onDestroy()
//        debug("onDestroy")
//        releaseBluetooth()
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun releaseBluetooth() {
//        bluetoothHidDevice!!.unregisterApp()
//        bluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
//        isRunning = false
//
//        //Send notification to activity
//        sendMessage(WHAT.BLUETOOTH_DISCONNECTED)
//    }
//
//    private fun sendMessage(@WHAT what: Int) {
////        val message = Message()
////        message.what = what
////        message.sendToTarget()
//    }
//
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        debug("onStartCommand")
//
////        val pendingIntent : PendingIntent = Intent (this, MainActivity::class.java).let { notificationIntent ->
////            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
////            // If we get killed, after returning from here, restart
////        }
//
//            return START_STICKY
//        }
//
//
//        @SuppressLint("MissingPermission")
//        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
//            if (profile == BluetoothProfile.HID_DEVICE) {
//                bluetoothHidDevice = proxy as BluetoothHidDevice
//                debug("onServiceConnected profile == BluetoothProfile.HID_DEVICE")
//                val callback: BluetoothHidDevice.Callback = object : BluetoothHidDevice.Callback() {
//                    override fun onAppStatusChanged(
//                        pluggedDevice: BluetoothDevice,
//                        registered: Boolean
//                    ) {
//                        super.onAppStatusChanged(pluggedDevice, registered)
//                        debug("onAppStatusChanged registered=$registered")
//                        val deviceConnected = bluetoothHidDevice!!.connect(bluetoothDevice)
//                        if (deviceConnected) {
//                            debug("Connected to " + bluetoothDevice!!.name)
//                        }
//                    }
//
//                    override fun onGetReport(
//                        device: BluetoothDevice,
//                        type: Byte,
//                        id: Byte,
//                        bufferSize: Int
//                    ) {
//                        super.onGetReport(device, type, id, bufferSize)
//                        debug("onGetReport")
//                    }
//
//                    override fun onSetReport(
//                        device: BluetoothDevice,
//                        type: Byte,
//                        id: Byte,
//                        data: ByteArray
//                    ) {
//                        super.onSetReport(device, type, id, data)
//                        debug("onSetReport")
//                    }
//
//                    override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
//                        var stateStr = ""
//                        when (state) {
//                            BluetoothHidDevice.STATE_CONNECTED -> {
//                                stateStr = "STATE_CONNECTED"
//                                isHidDeviceConnected = true
//                                sendMessage(WHAT.BLUETOOTH_CONNECTED)
//                            }
//                            BluetoothHidDevice.STATE_DISCONNECTED -> {
//                                stateStr = "STATE_DISCONNECTED"
//                                isHidDeviceConnected = false
//                                sendMessage(WHAT.BLUETOOTH_DISCONNECTED)
//                                this@BluetoothHidService.stopSelf()
//                            }
//                            BluetoothHidDevice.STATE_CONNECTING -> {
//                                stateStr = "STATE_CONNECTING"
//                                sendMessage(WHAT.BLUETOOTH_CONNECTING)
//                                startAsForeground()
//                            }
//                            BluetoothHidDevice.STATE_DISCONNECTING -> stateStr =
//                                "STATE_DISCONNECTING"
//                        }
//                        val isProfileSupported: Boolean = HidUtils.isProfileSupported(device)
//                        debug("isProfileSupported $isProfileSupported")
//                        debug("HID " + device.name + " " + device.address + " " + stateStr)
//                    }
//
//                    override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
//                        super.onSetProtocol(device, protocol)
//                        debug("onSetProtocol")
//                    }
//                }
//
////            bluetoothHidDevice.registerApp(Constants.SDP_RECORD, null, Constants.QOS_OUT, Runnable::run, callback);
//                bluetoothHidDevice!!.registerApp(
//                    Constants.SDP_RECORD,
//                    null,
//                    null,
//                    { obj: Runnable -> obj.run() },
//                    callback
//                )
//            }
//        }
//
//        override fun onServiceDisconnected(profile: Int) {
//            if (profile == BluetoothProfile.HID_DEVICE) {
////            bluetoothHidDevice = null;
//                debug("HID onServiceDisconnected")
//            }
//        }

//        companion object {
//            private const val TAG = "BluetoothHIDService"
//            const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
//            const val ACTION_VOL_INC = "ACTION_VOL_INC"
//            const val ACTION_VOL_DEC = "ACTION_VOL_DEC"
//            const val ACTION_MUTE = "ACTION_MUTE"
//            const val ACTION_POWER = "ACTION_POWER"
//            const val ACTION_REWIND = "ACTION_REWIND"
//            const val ACTION_FORWARD = "ACTION_FORWARD"
//            const val ACTION_UP = "ACTION_UP"
//            const val ACTION_DOWN = "ACTION_DOWN"
//            const val ACTION_LEFT = "ACTION_LEFT"
//            const val ACTION_RIGHT = "ACTION_RIGHT"
//            const val ACTION_MIDDLE = "ACTION_MIDDLE"
//            const val ACTION_MENU = "ACTION_MENU"
//            const val ACTION_HOME = "ACTION_HOME"
//            const val ACTION_BACK = "ACTION_BACK"
//            var bluetoothHidDevice: BluetoothHidDevice? = null
//            var bluetoothDevice: BluetoothDevice? = null
//            var isRunning = false
//            var isHidDeviceConnected = false
//        }
//    }