package com.magreader.magreader.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface KomgaApi {
    @GET("api/v1/users/me")
    suspend fun authenticate(): Response<AuthenticationResponse>

    @GET("api/v1/libraries")
    suspend fun getLibraries(): List<Library>

    @GET("api/v1/series")
    suspend fun getSeries(
        @Query("library_id") libraryId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PageResponse<Series>

    @GET("api/v1/books")
    suspend fun getBooks(
        @Query("series_id") seriesId: String? = null,
        @Query("library_id") libraryId: String? = null,
        @Query("unpaged") unpaged: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PageResponse<Book>

    @GET("api/v1/books/{bookId}")
    suspend fun getBook(@Path("bookId") bookId: String): Book

    @GET("api/v1/books/{bookId}/pages")
    suspend fun getBookPages(@Path("bookId") bookId: String): List<Page>

    @GET("api/v1/books/{bookId}/pages/{pageNumber}")
    @Streaming
    suspend fun getPage(
        @Path("bookId") bookId: String,
        @Path("pageNumber") pageNumber: Int
    ): ResponseBody

    @GET("api/v1/books/{bookId}/thumbnail")
    suspend fun getBookThumbnail(@Path("bookId") bookId: String): ResponseBody
}
