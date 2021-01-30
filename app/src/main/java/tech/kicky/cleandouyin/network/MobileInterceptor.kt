package tech.kicky.cleandouyin.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 添加固定头
 * author: yidong
 * 2021/1/30
 */
class MobileInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        request.newBuilder()
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36"
            )
        return chain.proceed(request)
    }
}