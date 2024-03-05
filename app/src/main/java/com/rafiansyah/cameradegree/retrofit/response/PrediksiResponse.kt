package com.rafiansyah.cameradegree.retrofit.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class PrediksiResponse(

	@field:SerializedName("PrediksiResponse")
	val prediksiResponse: ArrayList<PrediksiResponseItem>? = null
)

@Parcelize
data class PrediksiResponseItem(

	@field:SerializedName("panjang")
	val panjang: Double,

	@field:SerializedName("kaki")
	val kaki: String,

	@field:SerializedName("gambar")
	val gambar: String
): Parcelable
