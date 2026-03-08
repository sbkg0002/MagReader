package com.magreader.magreader.data

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface OpdsApi {
    @GET
    suspend fun getFeed(@Url url: String): String

    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}
