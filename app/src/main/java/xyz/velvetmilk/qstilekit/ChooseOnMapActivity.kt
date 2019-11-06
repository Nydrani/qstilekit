package xyz.velvetmilk.qstilekit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.maps.GoogleMap
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_choose_location.*

@Suppress("ConstantConditionIf")
class ChooseOnMapActivity : AppCompatActivity() {

    companion object {
        private val TAG = ChooseOnMapActivity::class.java.simpleName
        private const val DEBUG = BuildConfig.CUSTOM_DEBUG
        const val EXTRA_LATLNG = "EXTRA_LATLNG"


        fun buildIntent(context: Context): Intent {
            return Intent(context, ChooseOnMapActivity::class.java)
        }
    }

    private lateinit var googleMap: GoogleMap
    private lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_location)

        gson = Gson()

        map.onCreate(savedInstanceState)
        map.getMapAsync { googleMap ->
            this.googleMap = googleMap
            googleMap.isBuildingsEnabled = false
            googleMap.isIndoorEnabled = false
            googleMap.isMyLocationEnabled = false
            googleMap.isTrafficEnabled = false
        }

        select_location_button.setOnClickListener {
            // set location
            if (DEBUG) Log.d(TAG, googleMap.cameraPosition.target.toString())

            val latlng = LatlngModel(googleMap.cameraPosition.target.latitude, googleMap.cameraPosition.target.longitude)
            getSharedPreferences(PreferencesHelper.PREFERENCES_QSTILEKIT, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PreferencesHelper.KEY_CUSTOM_LOCATION, gson.toJson(latlng))
                    .apply()

            val data = Intent()
            data.putExtra(EXTRA_LATLNG, latlng)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        map.onStart()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onStop() {
        super.onStop()
        map.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.onLowMemory()
    }
}
