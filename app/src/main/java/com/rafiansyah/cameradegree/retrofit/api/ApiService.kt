package com.rafiansyah.cameradegree.retrofit.api

import com.rafiansyah.cameradegree.retrofit.response.PrediksiResponse
import com.rafiansyah.cameradegree.retrofit.response.PrediksiResponseItem
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload")
    fun uploadImage(@Part image: MultipartBody.Part): Call<ArrayList<PrediksiResponseItem>>
}