package com.magreader.magreader.data

import com.google.gson.annotations.SerializedName

data class Library(
    val id: String,
    val name: String
)

data class Series(
    val id: String,
    val libraryId: String,
    val name: String,
    val metadata: SeriesMetadata
)

data class SeriesMetadata(
    val title: String,
    val status: String
)

data class Book(
    val id: String,
    val seriesId: String,
    val name: String,
    val number: Int,
    val metadata: BookMetadata,
    val sizeBytes: Long,
    val media: BookMedia
)

data class BookMetadata(
    val title: String,
    val summary: String,
    val number: String
)

data class BookMedia(
    val status: String,
    val mediaType: String,
    val pagesCount: Int
)

data class Page(
    val number: Int,
    val fileName: String,
    val mediaType: String
)

data class AuthenticationResponse(
    val id: String? = null,
    val username: String,
    val roles: List<String>
)

data class PageResponse<T>(
    val content: List<T>,
    val last: Boolean,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)
