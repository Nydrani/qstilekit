package xyz.velvetmilk.qstilekit

import android.app.Application
import android.content.Context
import com.jakewharton.threetenabp.AndroidThreeTen

class QSTileKitApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidThreeTen.init(this)

        val sharedPreferences = getSharedPreferences(PreferencesHelper.PREFERENCES_QSTILEKIT, Context.MODE_PRIVATE)

        PreferencesHelper.setComponentState(this, MusicQSTileService::class.java, sharedPreferences.getBoolean(PreferencesHelper.KEY_MUSIC_ENABLED, true))
        PreferencesHelper.setComponentState(this, WeatherQSTileService::class.java, sharedPreferences.getBoolean(PreferencesHelper.KEY_WEATHER_ENABLED, true))
    }
}
