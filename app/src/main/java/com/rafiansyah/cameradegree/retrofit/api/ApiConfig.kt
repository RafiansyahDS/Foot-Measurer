package com.rafiansyah.cameradegree.retrofit.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ApiConfig {
    fun getApiService(): ApiService {
        val serveoBaseUrl = "https://skripsi.serveo.net/"
        val localTunnelBaseUrl = "https://skripsi.loca.lt"
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                var response = chain.proceed(request)
                if (!response.isSuccessful) {
                    // Retry with the second URL if the first URL fails
                    val newUrl = request.url.newBuilder()
                        .host(localTunnelBaseUrl)
                        .build()
                    val newRequest = request.newBuilder()
                        .url(newUrl)
                        .build()
                    response = chain.proceed(newRequest)
                }
                response
            }
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(serveoBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)



    }
}