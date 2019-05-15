package xyz.velvetmilk.qstilekit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

@Suppress("ConstantConditionIf")
class MusicQSTileService : TileService() {
    companion object {
        private val TAG = MusicQSTileService::class.java.simpleName
        private const val DEBUG = BuildConfig.CUSTOM_DEBUG
    }

    private var playbackState: Int = PlaybackState.STATE_NONE
    private var songName: CharSequence? = null
    private lateinit var defaultNames: Array<String>
    private var currentController: MediaController? = null
    private var tile: Tile? = null

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var mediaSessionManager: MediaSessionManager

    private var mediaControllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (DEBUG) Log.d(TAG, "onMetadataChanged")

            songName = metadata?.description?.title
            statefulUpdateTile(tile)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (DEBUG) Log.d(TAG, "onPlaybackStateChanged")

            playbackState = state?.state ?: PlaybackState.STATE_NONE
            statefulUpdateTile(tile)
        }
    }

    private var actionSessionListener = MediaSessionManager.OnActiveSessionsChangedListener {
        if (DEBUG) Log.d(TAG, "onActiveSessionsChanged")

        clearCurrentSession()
        updateCurrentSession(it)
    }


    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "onCreate")

        defaultNames = arrayOf(getString(R.string.play), getString(R.string.pause))
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        sharedPreferences = getSharedPreferences(PreferencesHelper.PREFERENCES_QSTILEKIT, Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (DEBUG) Log.d(TAG, "onBind")

        return super.onBind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DEBUG) Log.d(TAG, "onDestroy")
    }

    override fun onTileAdded() {
        if (DEBUG) Log.d(TAG, "onTileAdded")
    }

    override fun onTileRemoved() {
        if (DEBUG) Log.d(TAG, "onTileRemoved")
    }

    override fun onStartListening() {
        if (DEBUG) Log.d(TAG, "onStartListening")

        tile = qsTile

        // Enabled/disabled tile
        val enabled = sharedPreferences.getBoolean(PreferencesHelper.KEY_MUSIC_ENABLED, true)
        if (!enabled) {
            tile?.state = Tile.STATE_UNAVAILABLE
        } else {
            tile?.state = Tile.STATE_ACTIVE
        }

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(actionSessionListener, null)
            updateCurrentSession(mediaSessionManager.getActiveSessions(null))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onStopListening() {
        if (DEBUG) Log.d(TAG, "onStopListening")
        tile = null

        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(actionSessionListener)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        clearCurrentSession()
    }

    override fun onClick() {
        if (DEBUG) Log.d(TAG, "onClick")

        try {
            val activeSessions = mediaSessionManager.getActiveSessions(null)
            if (activeSessions.isEmpty()) {
                val audioManager: AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            } else {
                activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            }
        } catch (e: SecurityException) {
            e.printStackTrace()

            // tell user that this needs to be in /system/priv-app
            Toast.makeText(this, "This needs to be a privileged app to function properly",
                    Toast.LENGTH_SHORT).show()

            // no sessions, lets try to reboot the most recent
            val audioManager: AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }


    private fun statefulUpdateTile(tile: Tile?) {
        if (tile?.state == Tile.STATE_UNAVAILABLE) {
            tile.icon = Icon.createWithResource(this@MusicQSTileService, android.R.drawable.ic_media_play)
            tile.label = defaultNames[0]
            tile.updateTile()
            return
        }

        if (playbackState == PlaybackState.STATE_PLAYING) {
            tile?.icon = Icon.createWithResource(this@MusicQSTileService, android.R.drawable.ic_media_pause)
            tile?.label = songName ?: defaultNames[1]
        } else {
            tile?.icon = Icon.createWithResource(this@MusicQSTileService, android.R.drawable.ic_media_play)
            tile?.label = songName ?: defaultNames[0]
        }

        tile?.updateTile()
    }

    private fun updateCurrentSession(sessions: MutableList<MediaController>?) {
        if (!sessions.isNullOrEmpty()) {
            currentController = sessions[0]
            currentController!!.registerCallback(mediaControllerCallback)
        }

        playbackState = currentController?.playbackState?.state ?: PlaybackState.STATE_NONE
        songName = currentController?.metadata?.description?.title
        statefulUpdateTile(tile)
    }

    private fun clearCurrentSession() {
        currentController?.unregisterCallback(mediaControllerCallback)
        currentController = null
    }
}
