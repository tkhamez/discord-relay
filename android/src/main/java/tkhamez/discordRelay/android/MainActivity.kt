package tkhamez.discordRelay.android

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import tkhamez.discordRelay.common.App
import tkhamez.discordRelay.common.appOnDestroy
import tkhamez.discordRelay.common.stopGateway

class MainActivity : AppCompatActivity() {
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            stopGateway(this)
        }
        appOnDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // >= 12, API 31
            // Back button behaves now the same as the home button.
            super.onBackPressed()
            return
        }

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        }
        Toast.makeText(baseContext, getString(R.string.press_back), Toast.LENGTH_SHORT).show()
        backPressedTime = System.currentTimeMillis()
    }
}
