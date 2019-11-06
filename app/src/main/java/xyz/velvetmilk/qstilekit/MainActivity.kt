package xyz.velvetmilk.qstilekit

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_LOCATION = 1
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson
    private lateinit var locationChannel: Channel<LatlngModel?>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate")

        job = Job()
        sharedPreferences = getSharedPreferences(PreferencesHelper.PREFERENCES_QSTILEKIT, Context.MODE_PRIVATE)
        gson = Gson()
        locationChannel = Channel()

        // initialise values
        location_text.text = getString(R.string.custom_location, sharedPreferences.getString(PreferencesHelper.KEY_CUSTOM_LOCATION, null))
        weather_switch.isChecked = sharedPreferences.getBoolean(PreferencesHelper.KEY_WEATHER_ENABLED, true)
        media_switch.isChecked = sharedPreferences.getBoolean(PreferencesHelper.KEY_MUSIC_ENABLED, true)

        // setup click listeners
        choose_button.setOnClickListener {
            setCustomLocationFromMap()
        }

        clear_button.setOnClickListener {
            clearCustomLocation()
        }

        weather_switch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.setComponentState(this, WeatherQSTileService::class.java, isChecked)
            sharedPreferences.edit().putBoolean(PreferencesHelper.KEY_WEATHER_ENABLED, isChecked).apply()
        }

        media_switch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.setComponentState(this, MusicQSTileService::class.java, isChecked)
            sharedPreferences.edit().putBoolean(PreferencesHelper.KEY_MUSIC_ENABLED, isChecked).apply()
        }

        val tempUnits = arrayOf("Celcius", "Kelvin", "Fahrenheit")
        val adapter = ArrayAdapter(
                this,
                R.layout.dropdown_menu_popup_item,
                tempUnits)

        filled_exposed_dropdown.setAdapter(adapter)
        filled_exposed_dropdown.setText(tempUnits[sharedPreferences.getLong(PreferencesHelper.KEY_TEMPURATURE_UNIT, 0).toInt()], false)
        filled_exposed_dropdown.setOnItemClickListener { parent, view, position, id ->
            sharedPreferences.edit().putLong(PreferencesHelper.KEY_TEMPURATURE_UNIT, id).apply()
            // tell weather
        }

        // setup channel observers
        launch {
            for (location in locationChannel) {
                location_text.text = getString(R.string.custom_location, location.toString())

                sharedPreferences
                    .edit()
                    .putString(PreferencesHelper.KEY_CUSTOM_LOCATION, gson.toJson(location))
                    .apply()
            }
        }
    }

    private fun setCustomLocationFromMap() {
        startActivityForResult(ChooseOnMapActivity.buildIntent(this), REQUEST_LOCATION)
    }

    private fun clearCustomLocation() {
        launch {
            locationChannel.send(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        locationChannel.cancel()
        job.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                if (resultCode == RESULT_OK) {
                    // get data
                    val latlng = data?.getParcelableExtra<LatlngModel>(ChooseOnMapActivity.EXTRA_LATLNG)
                    launch {
                        locationChannel.offer(latlng)
                    }
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}
