package xyz.velvetmilk.qstilekit

import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherService {
    @GET("weather")
    fun getWeatherAsync(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("appid") apiKey: String): Deferred<OpenWeatherMapCurrentModel>
}
