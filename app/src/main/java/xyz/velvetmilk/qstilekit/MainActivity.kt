package xyz.velvetmilk.qstilekit

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.quicksettings.TileService
import android.util.Log
import android.view.KeyEvent

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")

        // early exist for dealing with tile preferences
        if (intent.action == TileService.ACTION_QS_TILE_PREFERENCES) {
            val mediaSessionManager: MediaSessionManager = this.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val activeSessions = mediaSessionManager.getActiveSessions(null)

            if (activeSessions.isEmpty()) {
                val audioManager: AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            } else {
                activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
            }

            finish()
            return
        }

        setContentView(R.layout.activity_main)
    }
}
