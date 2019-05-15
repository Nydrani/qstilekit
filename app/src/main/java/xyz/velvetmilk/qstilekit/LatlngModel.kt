package xyz.velvetmilk.qstilekit

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LatlngModel(val latitude: Double, val longitude: Double) : Parcelable
