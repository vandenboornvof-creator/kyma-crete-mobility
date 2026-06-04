package com.cretemobility.app.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "CreteMobility/1.0 Android (https://cretemobility.com)")
            .build()
        return chain.proceed(request)
    }
}
