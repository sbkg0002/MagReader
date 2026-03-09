package com.magreader.magreader.data

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URL

class OpdsParser {
    fun parse(xml: String, baseUrl: String): OpdsFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && localName(parser.name) == "feed") {
                    return readFeed(parser, baseUrl)
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("OpdsParser", "Error parsing OPDS", e)
        }
        return OpdsFeed("Error", emptyList())
    }

    private fun localName(name: String): String = name.substringAfter(":")

    private fun resolveUrl(baseUrl: String, relativeUrl: String?): String? {
        if (relativeUrl == null) return null
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) return relativeUrl
        
        return try {
            val base = URL(baseUrl)
            URL(base, relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }

    private fun readFeed(parser: XmlPullParser, baseUrl: String): OpdsFeed {
        var title = ""
        val entries = mutableListOf<OpdsEntry>()
        var nextUrl: String? = null

        parser.require(XmlPullParser.START_TAG, null, parser.name)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val name = localName(parser.name)
            when (name) {
                "title" -> title = readText(parser)
                "entry" -> entries.add(readEntry(parser, baseUrl))
                "link" -> {
                    val rel = parser.getAttributeValue(null, "rel")
                    val href = parser.getAttributeValue(null, "href")
                    if (rel == "next") {
                        nextUrl = resolveUrl(baseUrl, href)
                    }
                    parser.nextTag()
                }
                else -> skip(parser)
            }
        }
        return OpdsFeed(title, entries, nextUrl)
    }

    private fun readEntry(parser: XmlPullParser, baseUrl: String): OpdsEntry {
        var title = ""
        var id = ""
        var summary: String? = null
        var thumbnailUrl: String? = null
        var acquisitionUrl: String? = null
        var type: String? = null

        parser.require(XmlPullParser.START_TAG, null, parser.name)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val name = localName(parser.name)
            when (name) {
                "title" -> title = readText(parser)
                "id" -> id = readText(parser)
                "summary" -> summary = readText(parser)
                "link" -> {
                    val rel = parser.getAttributeValue(null, "rel")
                    val href = parser.getAttributeValue(null, "href")
                    val linkType = parser.getAttributeValue(null, "type")
                    
                    if (rel != null) {
                        // Log.d("OpdsParser", "Found link: rel=$rel, type=$linkType")
                        if (rel.contains("thumbnail") || rel.contains("image") || rel.contains("cover")) {
                            thumbnailUrl = resolveUrl(baseUrl, href)
                        } else if (rel.contains("acquisition")) {
                            acquisitionUrl = resolveUrl(baseUrl, href)
                            type = linkType
                        } else if (rel.contains("subsection") || rel.contains("alternate")) {
                            if (linkType?.contains("atom+xml") == true) {
                                acquisitionUrl = resolveUrl(baseUrl, href)
                                type = linkType
                            }
                        }
                    }
                    parser.nextTag()
                }
                else -> skip(parser)
            }
        }
        return OpdsEntry(title, id, summary, thumbnailUrl, acquisitionUrl, type)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
