package com.kirkbushman.araw.utils

import okhttp3.Interceptor
import okhttp3.Response

// Reddit rejects the default okhttp UA. Format: <platform>:<app ID>:<version> (by /u/<user>).
// Keep the version in sync with app/build.gradle's versionName if it changes.
internal object UserAgentInterceptor : Interceptor {

    private const val USER_AGENT = "android:rocks.tbog.livewallpaperit:v2.0 (by /u/TBog)"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        return chain.proceed(request)
    }
}
