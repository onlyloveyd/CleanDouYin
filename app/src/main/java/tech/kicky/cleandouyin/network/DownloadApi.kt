package tech.kicky.cleandouyin.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Download API
 * author: yidong
 * 2021/1/30
 */
interface DownloadApi {
    @GET
    suspend fun download(@Url url: String): ResponseBody
}