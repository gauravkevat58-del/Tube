package com.github.itube

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelOriginFilterTest {

    private fun video(id: String, title: String, channel: String, channelUrl: String) = VideoItem(
        id = id,
        title = title,
        channel = channel,
        channelUrl = channelUrl,
    )

    @Test fun identifiesIndiaCountryValuesStrictly() {
        assertTrue(ChannelOriginResolver.isIndiaCountry("India"))
        assertTrue(ChannelOriginResolver.isIndiaCountry("india"))
        assertTrue(ChannelOriginResolver.isIndiaCountry("IN"))
        assertTrue(ChannelOriginResolver.isIndiaCountry("Republic of India"))
        assertTrue(ChannelOriginResolver.isIndiaCountry("Bharat"))

        assertFalse(ChannelOriginResolver.isIndiaCountry("United States"))
        assertFalse(ChannelOriginResolver.isIndiaCountry("Indonesia"))
        assertFalse(ChannelOriginResolver.isIndiaCountry("United Kingdom"))
        assertFalse(ChannelOriginResolver.isIndiaCountry(""))
        assertFalse(ChannelOriginResolver.isIndiaCountry(null))
    }

    @Test fun extractsCountryOnlyFromChannelAboutStructures() {
        val aboutVmJson = JSONObject(
            """
            {
              "onResponseReceivedEndpoints": [
                {
                  "showEngagementPanelEndpoint": {
                    "engagementPanel": {
                      "engagementPanelSectionListRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "itemSectionRenderer": {
                                  "contents": [
                                    {
                                      "aboutChannelRenderer": {
                                        "metadata": {
                                          "aboutChannelViewModel": {
                                            "country": "India"
                                          }
                                        }
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
              ]
            }
            """.trimIndent(),
        )
        assertEquals("India", ChannelOriginResolver.extractCountryFromChannelJson(aboutVmJson))

        val legacyAboutJson = JSONObject(
            """
            {
              "channelAboutFullMetadataRenderer": {
                "country": { "simpleText": "United States" }
              }
            }
            """.trimIndent(),
        )
        assertEquals("United States", ChannelOriginResolver.extractCountryFromChannelJson(legacyAboutJson))

        // Title keywords or descriptions without explicit About country must yield null -> UNKNOWN.
        val missingCountryJson = JSONObject(
            """
            {
              "videoDetails": {
                "title": "Top Hindi News India Today",
                "shortDescription": "Welcome to Bharat"
              }
            }
            """.trimIndent(),
        )
        assertNull(ChannelOriginResolver.extractCountryFromChannelJson(missingCountryJson))
    }

    @Test fun extractsCountryFromAboutPageHtml() {
        val html = """
            <html><script>var ytInitialData = {"metadata":{"channelMetadataRenderer":{"country":"India"}}};</script></html>
        """.trimIndent()
        assertEquals("India", ChannelOriginResolver.extractCountryFromAboutHtml(html))
    }

    @Test fun filtersByCreatorOriginNotLanguageOrKeywords() {
        val indianCreatorEnglishVideo = video(
            id = "v_in_en",
            title = "Deep Dive Into Distributed Systems",
            channel = "Bengaluru Tech Talks",
            channelUrl = "https://www.youtube.com/channel/UCindianEng01",
        )
        val indianCreatorHindiVideo = video(
            id = "v_in_hi",
            title = "Aaj Ki Taaza Khabar",
            channel = "Desh Samachar",
            channelUrl = "https://www.youtube.com/channel/UCindianHin02",
        )
        val foreignCreatorIndiaKeywords = video(
            id = "v_us_kw",
            title = "India Travel Vlog in Hindi & Bharat Tour",
            channel = "Global Nomads USA",
            channelUrl = "https://www.youtube.com/channel/UCforeignUS03",
        )
        val foreignCreatorEnglish = video(
            id = "v_uk_en",
            title = "London Street Food Review",
            channel = "UK Eats",
            channelUrl = "https://www.youtube.com/channel/UCforeignUK04",
        )
        val unverifiedCreator = video(
            id = "v_unknown",
            title = "Mystery Channel Upload India",
            channel = "No Country Channel",
            channelUrl = "https://www.youtube.com/channel/UCunknown005",
        )

        val originMap = mapOf(
            "UCindianEng01" to ChannelOriginResolver.classifyCountryString("India"),
            "UCindianHin02" to ChannelOriginResolver.classifyCountryString("IN"),
            "UCforeignUS03" to ChannelOriginResolver.classifyCountryString("United States"),
            "UCforeignUK04" to ChannelOriginResolver.classifyCountryString("United Kingdom"),
            "UCunknown005" to ChannelOriginResolver.classifyCountryString(null),
        )

        val filtered = ChannelOriginResolver.filterVideosByOrigin(
            videos = listOf(
                indianCreatorEnglishVideo,
                indianCreatorHindiVideo,
                foreignCreatorIndiaKeywords,
                foreignCreatorEnglish,
                unverifiedCreator,
            ),
            statusByVideoOrChannelKey = originMap,
            includeUnknownIfEmpty = false,
        )

        assertEquals(listOf("v_in_en", "v_in_hi"), filtered.map { it.id })
    }
}
