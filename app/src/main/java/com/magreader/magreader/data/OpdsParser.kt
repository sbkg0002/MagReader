package com.magreader.magreader.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class OpdsParser {
    fun parse(xml: String): OpdsFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        parser.nextTag()
        return readFeed(parser)
    }

    private fun readFeed(parser: XmlPullParser): OpdsFeed {
        var title = ""
        val entries = mutableListOf<OpdsEntry>()

        parser.require(XmlPullParser.START_TAG, null, "feed")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "entry" -> entries.add(readEntry(parser))
                else -> skip(parser)
            }
        }
        return OpdsFeed(title, entries)
    }

    private fun readEntry(parser: XmlPullParser): OpdsEntry {
        var title = ""
        var id = ""
        var summary: String? = null
        var thumbnailUrl: String? = null
        var acquisitionUrl: String? = null
        var type: String? = null

        parser.require(XmlPullParser.START_TAG, null, "entry")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "id" -> id = readText(parser)
                "summary" -> summary = readText(parser)
                "link" -> {
                    val rel = parser.getAttributeValue(null, "rel")
                    val href = parser.getAttributeValue(null, "href")
                    val linkType = parser.getAttributeValue(null, "type")
                    
                    if (rel != null) {
                        if (rel.contains("thumbnail") || rel.contains("image")) {
                            thumbnailUrl = href
                        } else if (rel.contains("acquisition")) {
                            acquisitionUrl = href
                            type = linkType
                        } else if (rel.contains("subsection") || rel.contains("alternate")) {
                            if (linkType?.contains("atom+xml") == true) {
                                acquisitionUrl = href
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
