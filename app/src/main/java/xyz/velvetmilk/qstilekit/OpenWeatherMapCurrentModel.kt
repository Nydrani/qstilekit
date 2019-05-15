package xyz.velvetmilk.qstilekit

data class OpenWeatherMapCurrentModel(val coord: Coordinates,
                                      val weather: List<Weather>,
                                      val base: String,
                                      val main: Main,
                                      val visibility: Int?,
                                      val wind: Wind,
                                      val clouds: Cloud,
                                      val rain: Rain?,
                                      val snow: Snow?,
                                      val dt: Long,
                                      val sys: Sys,
                                      val id: Int,
                                      val name: String,
                                      val cod: Int) {

    data class Coordinates(val lon: Float, val lat: Float)
    data class Weather(val id: Int, val main: String, val description: String, val icon: String)
    data class Main(val temp: Float, val pressure: Float, val humidity: Int, val temp_min: Float, val temp_max: Float, val sea_level: Float?, val grnd_level: Float?)
    data class Wind(val speed: Float, val deg: Float, val gust: Float?)
    data class Cloud(val all: Int)
    data class Rain(val `1h`: Float?, val `3h`: Float?)
    data class Snow(val `1h`: Float?, val `3h`: Float?)
    data class Sys(val type: Int, val id: Int, val message: Float, val country: String, val sunrise: Int, val sunset: Int)
}
