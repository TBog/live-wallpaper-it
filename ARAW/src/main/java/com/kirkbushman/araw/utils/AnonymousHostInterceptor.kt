package com.kirkbushman.araw.utils

import okhttp3.Interceptor
import okhttp3.Response

// Requests with no Authorization header can't use oauth.reddit.com (returns 401).
// Rewrite them to www.reddit.com, which serves the public .json endpoints without auth.
internal object AnonymousHostInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) return chain.proceed(original)
        if (original.url.host != "oauth.reddit.com") return chain.proceed(original)

        val rewritten = original.newBuilder()
            .url(original.url.newBuilder().host("www.reddit.com").build())
            .build()
        return chain.proceed(rewritten)
    }
}
