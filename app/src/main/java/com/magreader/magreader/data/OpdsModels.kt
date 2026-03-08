package com.magreader.magreader.data

data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry>
)

data class OpdsEntry(
    val title: String,
    val id: String,
    val summary: String? = null,
    val thumbnailUrl: String? = null,
    val acquisitionUrl: String? = null,
    val type: String? = null // e.g. application/pdf or application/atom+xml;profile=opds-catalog;kind=navigation
)
