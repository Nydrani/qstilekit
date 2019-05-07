package xyz.velvetmilk.qstilekit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.quicksettings.TileService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // early exist for dealing with tile preferences
        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES) {
            // onlongclick --> (mediacontroller go next)
            finish()
            return
        }

        setContentView(R.layout.activity_main)
    }
}
