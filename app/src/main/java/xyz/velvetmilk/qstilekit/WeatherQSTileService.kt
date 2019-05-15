package xyz.velvetmilk.qstilekit

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.location.Location
import android.location.LocationManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONException
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlin.coroutines.CoroutineContext

@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeatherQSTileService : TileService(), CoroutineScope {
    companion object {
        private val TAG = WeatherQSTileService::class.java.simpleName
        private const val DEBUG = BuildConfig.CUSTOM_DEBUG

        const val API_KEY = "***REMOVED***"
    }


    class WeatherInfo {
        var condition: Int = 0
        var temperature: Float = 0f
        var timestamp: Instant = Instant.MIN
        var town: String = ""

//        private val conditionsImage = mutableMapOf<Int, Int>()
//
//        init {
//            conditionsImage["partly-cloudy"] = R.drawable.weather_partly_cloudy
//            conditionsImage["partly-cloudy-night"] = R.drawable.weather_partly_cloudy_night
//            conditionsImage["mostly-cloudy"] = R.drawable.weather_mostly_cloudy
//            conditionsImage["mostly-cloudy-night"] = R.drawable.weather_mostly_cloudy_night
//            conditionsImage["cloudy"] = R.drawable.weather_cloudy
//            conditionsImage["clear-night"] = R.drawable.weather_clear_night
//            conditionsImage["mostly-clear-night"] = R.drawable.weather_mostly_clear_night
//            conditionsImage["sunny"] = R.drawable.weather_sunny
//            conditionsImage["mostly-sunny"] = R.drawable.weather_mostly_sunny
//            conditionsImage["scattered-showers"] = R.drawable.weather_scattered_showers
//            conditionsImage["scattered-showers-night"] = R.drawable.weather_scattered_showers_night
//            conditionsImage["rain"] = R.drawable.weather_rain
//            conditionsImage["windy"] = R.drawable.weather_windy
//            conditionsImage["snow"] = R.drawable.weather_snow
//            conditionsImage["scattered-thunderstorms"] = R.drawable.weather_isolated_scattered_thunderstorms
//            conditionsImage["scattered-thunderstorms-night"] = R.drawable.weather_isolated_scattered_thunderstorms_night
//            conditionsImage["isolated-thunderstorms"] = R.drawable.weather_isolated_scattered_thunderstorms
//            conditionsImage["isolated-thunderstorms-night"] = R.drawable.weather_isolated_scattered_thunderstorms_night
//            conditionsImage["thunderstorms"] = R.drawable.weather_thunderstorms
//            conditionsImage["foggy"] = R.drawable.weather_foggy
//        }

        val conditionImage: Int
            get() {
                return when (condition) {
                    in 200..299 -> R.drawable.weather_thunderstorms
                    in 300..399 -> R.drawable.weather_scattered_showers
                    in 500..599 -> R.drawable.weather_rain
                    in 600..699 -> R.drawable.weather_snow
                    in 700..799 -> R.drawable.weather_foggy
                    800 -> R.drawable.weather_sunny
                    801 -> R.drawable.weather_partly_cloudy
                    802 -> R.drawable.weather_mostly_cloudy
                    803, 804 -> R.drawable.weather_cloudy
                    else -> 0
                }
            }

        val temperatureCelcius: Float
            get() = temperature - 273.15f

        val temperatureFahrenheit: Float
            get() = temperature * 9 / 5 - 459.67f

        override fun toString(): String {
            return "WeatherInfo: condition=" + condition + ", temperature=" + temperature +
                    ", town=" + town + ", timestamp=" + timestamp
        }
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var locationManager: LocationManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gson: Gson
    private lateinit var weatherChannel: Channel<OpenWeatherMapCurrentModel?>

    private var state: Int = Tile.STATE_INACTIVE
    private var updating = false
    private var weatherModel: OpenWeatherMapCurrentModel? = null

    private var tile: Tile? = null

    private val openWeatherService = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherService::class.java)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        job = Job()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sharedPreferences = getSharedPreferences(PreferencesHelper.PREFERENCES_QSTILEKIT, Context.MODE_PRIVATE)
        gson = Gson()
        weatherChannel = Channel()
        weatherModel = gson.fromJson(sharedPreferences.getString(PreferencesHelper.KEY_WEATHERINFO, null) ?: "", OpenWeatherMapCurrentModel::class.java)

        updating = false
        state = if (sharedPreferences.getBoolean(PreferencesHelper.KEY_WEATHER_ENABLED, true)) { Tile.STATE_ACTIVE } else { Tile.STATE_UNAVAILABLE }

        // listen to weather channel forever
        launch {
            for (weather in weatherChannel) {
                Log.d(TAG, "Updating weather tile")

                weatherModel = weather

                // die on unavailble state
                if (state == Tile.STATE_UNAVAILABLE) {
                    tile?.icon = Icon.createWithResource(this@WeatherQSTileService, R.drawable.ic_launcher_foreground)
                    tile?.label = "Weather disabled"
                    tile?.state = Tile.STATE_UNAVAILABLE
                    tile?.updateTile()
                    continue
                }

                if (weather == null || Duration.between(Instant.ofEpochSecond(weather.dt), Instant.now()).toHours() > 3) {
                    // do something
                    tile?.icon = Icon.createWithResource(this@WeatherQSTileService, R.drawable.ic_launcher_foreground)
                    tile?.label = "No Weather/Location Info"
                    tile?.updateTile()
                    continue
                }

                // lets load weather info into tile
                val weatherInfo = WeatherInfo()
                weatherInfo.temperature = weather.main.temp
                weatherInfo.condition = weather.weather[0].id
                weatherInfo.town = weather.name
                weatherInfo.timestamp = Instant.ofEpochSecond(weather.dt)

                tile?.icon = Icon.createWithResource(this@WeatherQSTileService, weatherInfo.conditionImage)
                tile?.label = weatherInfo.temperatureCelcius.toInt().toString() + "\u00B0C - " + weatherInfo.town +
                        "\n" + DateTimeFormatter.ISO_LOCAL_TIME.withZone(ZoneId.systemDefault()).format(weatherInfo.timestamp)
                tile?.updateTile()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        weatherChannel.cancel()
        job.cancel()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick")

        if (tile?.state == Tile.STATE_ACTIVE) {
            tile?.state = Tile.STATE_INACTIVE
            state = Tile.STATE_INACTIVE
        } else if (tile?.state == Tile.STATE_INACTIVE) {
            tile?.state = Tile.STATE_ACTIVE
            state = Tile.STATE_ACTIVE

            // weatherinfo doesnt exist OR not fresh (1 minute)
//            weatherModel?.let {
//                if (Duration.between(Instant.ofEpochSecond(it.dt), Instant.now()).toMinutes() < 1) {
//                    return@let it
//                }
//                null
//            } ?: run {
//                updateWeatherData()
//            }

            // TODO probably need to throttle in the future
            // update whenever
            updateWeatherData()
        }

        tile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")

        tile = qsTile

        // TODO activity needs to instantly update this field somehow
        val enabled = sharedPreferences.getBoolean(PreferencesHelper.KEY_WEATHER_ENABLED, true)
        if (!enabled) {
            state = Tile.STATE_UNAVAILABLE
        } else if (state == Tile.STATE_UNAVAILABLE) {
            // only update state of old state was unavailable
            state = Tile.STATE_ACTIVE
        }
        tile?.state = state
        tile?.updateTile()

        // update display with current model
        // TODO probably a better way to tell the app to load new weather info
        launch {
            weatherChannel.send(weatherModel)
        }

        // early exit if not active
        if (state != Tile.STATE_ACTIVE) {
            return
        }

        // weather is up to date and exists
        if (weatherModel != null && Duration.between(Instant.ofEpochSecond(weatherModel!!.dt), Instant.now()).toHours() < 1) {
            return
        }

        // update weather otherwise
        updateWeatherData()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening")

        tile = null
    }

//    private fun updateLocation() {
//        val locationListener = object : LocationListener {
//            override fun onLocationChanged(location: Location) {
//                getWeatherData(location)
//            }
//
//            override fun onProviderDisabled(provider: String) {
//                launch(Dispatchers.Main) {
//                    Toast.makeText(this@WeatherQSTileService, provider + " disabled", Toast.LENGTH_SHORT).show()
//                }
//                updating = false
//                locationManager.removeUpdates(this)
//            }
//
//            override fun onProviderEnabled(provider: String) {
//            }
//
//            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//            }
//        }
//
//        // update the location
//        try {
//            locationManager.requestSingleUpdate(LocationManager.PASSIVE_PROVIDER, locationListener, null)
//        } catch (e: SecurityException) {
//            // darn
//            updating = false
//            Toast.makeText(this, "Location permission disabled", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun updateWeatherData() {
        // early exit if updating
        if (updating) {
            return
        }
        updating = true


        Log.d(TAG, "Trying to get new weather data")

        var latlng = gson.fromJson(sharedPreferences.getString(PreferencesHelper.KEY_CUSTOM_LOCATION, null) ?: "", LatlngModel::class.java)
        val location: Location?

        try {
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            Log.d(TAG, "Location: " + location)
        } catch (e: SecurityException) {
            // darn
            Toast.makeText(this, "Location permission disabled", Toast.LENGTH_SHORT).show()
            updating = false
            return
        }

        // update latlng object to use more current location if exists and is fresh
        location?.let {
            if (Duration.between(Instant.ofEpochMilli(location.time), Instant.now()).toHours() < 2) {
                latlng = LatlngModel(location.latitude, location.longitude)
            }
        }

        // location doesnt exist
        if (latlng == null) {
            // Clear weather channel
            launch {
                weatherChannel.send(null)
            }
            updating = false
            return
        }

        getWeatherData(latlng)
    }

    private fun getWeatherData(latlng: LatlngModel) {
        launch(Dispatchers.IO) {
            try {
                val weatherModel = openWeatherService.getWeatherAsync(latlng.latitude, latlng.longitude, API_KEY).await()
                Log.d(TAG, "Got new weather data: " + weatherModel)
                weatherChannel.send(weatherModel)
                sharedPreferences.edit().putString(PreferencesHelper.KEY_WEATHERINFO, gson.toJson(weatherModel)).apply()

                Log.d(TAG, weatherModel.toString())
            } catch (e: JSONException) {
                // json parsing died
                Log.e(TAG, e.toString())
            } catch (e: IOException) {
                // no network
            }
            updating = false
        }
    }
}
