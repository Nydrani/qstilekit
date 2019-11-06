package xyz.velvetmilk.qstilekit

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class PreferencesHelper {

    companion object {
        const val PREFERENCES_QSTILEKIT = "PREFERENCES_QSTILEKIT"
        const val KEY_CUSTOM_LOCATION = "KEY_CUSTOM_LOCATION"
        const val KEY_WEATHERINFO = "KEY_WEATHERINFO"
        const val KEY_WEATHER_ENABLED = "KEY_WEATHER_ENABLED"
        const val KEY_MUSIC_ENABLED = "KEY_MUSIC_ENABLED"
        const val KEY_TEMPURATURE_UNIT = "KEY_TEMPERATURE_UNIT"

        fun setComponentState(context: Context, cls: Class<*>, enable: Boolean) {
            val componentState = if (enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, cls),
                componentState,
                PackageManager.DONT_KILL_APP)
        }
    }

    enum class TempatureUnit {
        KELVIN,
        CELCIUS,
        FAHRENHEIT
    }
}
