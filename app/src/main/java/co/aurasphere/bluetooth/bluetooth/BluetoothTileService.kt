package co.aurasphere.bluetooth.bluetooth

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import co.aurasphere.bluetooth.MainActivity


@RequiresApi(Build.VERSION_CODES.N)
class BluetoothTileService : TileService() {
        override fun onClick() {
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
