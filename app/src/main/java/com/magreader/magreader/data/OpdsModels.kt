package com.magreader.magreader.data

data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry>,
    val nextUrl: String? = null
)

data class OpdsEntry(
    val title: String,
    val id: String,
    val summary: String? = null,
    val thumbnailUrl: String? = null,
    val acquisitionUrl: String? = null,
    val type: String? = null
)
