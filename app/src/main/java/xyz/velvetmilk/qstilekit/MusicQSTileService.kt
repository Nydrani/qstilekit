package xyz.velvetmilk.qstilekit

import android.content.Context
import android.content.Intent
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

class MusicQSTileService : TileService() {
    companion object {
        private val TAG = MusicQSTileService::class.java.simpleName
    }

    private var playbackState: Int = PlaybackState.STATE_NONE
    private var songName: CharSequence? = null
    private lateinit var defaultNames: Array<String>
    private var currentController: MediaController? = null
    private var tile: Tile? = null


    private var mediaControllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "onMetadataChanged")

            songName = metadata?.description?.title
            statefulUpdateTile(tile)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "onPlaybackStateChanged")

            playbackState = state?.state ?: PlaybackState.STATE_NONE
            statefulUpdateTile(tile)
        }
    }

    private var actionSessionListener = MediaSessionManager.OnActiveSessionsChangedListener {
        Log.d(TAG, "onActiveSessionsChanged")

        clearCurrentSession()

        updateCurrentSession(it)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")

        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        defaultNames = arrayOf(getString(R.string.play), getString(R.string.pause))
    }

    override fun onTileAdded() {
        Log.d(TAG, "onTileAdded")
        Toast.makeText(this, "onTileAdded", Toast.LENGTH_SHORT).show()
    }

    override fun onTileRemoved() {
        Log.d(TAG, "onTileRemoved")
        Toast.makeText(this, "onTileRemoved", Toast.LENGTH_SHORT).show()
    }

    override fun onStartListening() {
        Log.d(TAG, "onStartListening")
        tile = qsTile

        val mediaSessionManager: MediaSessionManager = this.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionManager.addOnActiveSessionsChangedListener(actionSessionListener, null)

        updateCurrentSession(mediaSessionManager.getActiveSessions(null))
    }

    override fun onStopListening() {
        Log.d(TAG, "onStopListening")
        tile = null

        val mediaSessionManager: MediaSessionManager = this.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionManager.removeOnActiveSessionsChangedListener(actionSessionListener)

        clearCurrentSession()
    }

    override fun onClick() {
        Log.d(TAG, "onClick")

        val mediaSessionManager: MediaSessionManager = this.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessions = mediaSessionManager.getActiveSessions(null)

        if (activeSessions.isEmpty()) {
            val audioManager: AudioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        } else {
            activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            activeSessions[0].dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }

    private fun statefulUpdateTile(tile: Tile?) {
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
