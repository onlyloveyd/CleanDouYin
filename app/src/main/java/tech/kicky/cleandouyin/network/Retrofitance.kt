package tech.kicky.cleandouyin.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.kicky.cleandouyin.BuildConfig

/**
 * Retrofit instance
 * author: yidong
 * 2021/1/30
 */
object Retrofitance {
    private const val BASE_URL = "http://fultter.club:8000/";

    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor()
        if (BuildConfig.DEBUG) {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(MobileInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofitance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val downloadApi: DownloadApi by lazy {
        retrofitance.create(DownloadApi::class.java)
    }
}