package com.github.itube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicFeedFilterTest {

    private fun video(
        id: String,
        title: String,
        channel: String = "Creator",
        durationSec: Long = 210L,
        isShort: Boolean = false,
    ) = VideoItem(
        id = id,
        title = title,
        channel = channel,
        durationSec = durationSec,
        isShort = isShort,
    )

    private fun collection(
        url: String,
        title: String,
        subtitle: String = "",
        isAlbum: Boolean = false,
    ) = PublicCollection(
        url = url,
        title = title,
        subtitle = subtitle,
        thumbnail = null,
        isAlbum = isAlbum,
    )

    @Test fun matchesMusicVideosByTitleAndChannel() {
        assertTrue(MusicFeedFilter.isMusicVideo(video("1", "Midnight Skyline (Official Music Video)")))
        assertTrue(MusicFeedFilter.isMusicVideo(video("2", "Ocean Drive [Official Audio]")))
        assertTrue(MusicFeedFilter.isMusicVideo(video("3", "Late Night Lo-Fi Beats")))
        assertTrue(MusicFeedFilter.isMusicVideo(video("4", "Sunrise", channel = "Artist - Topic")))
        assertTrue(MusicFeedFilter.isMusicVideo(video("5", "Horizon", channel = "ArtistVEVO")))
        assertTrue(MusicFeedFilter.isMusicVideo(video("6", "Acoustic Session", channel = "Indie Records")))
    }

    @Test fun rejectsShortsAndNonMusicVideos() {
        assertFalse(
            MusicFeedFilter.isMusicVideo(
                video("s1", "Catchy Song (Official Audio)", durationSec = 45L, isShort = true),
            ),
        )
        assertFalse(
            MusicFeedFilter.isMusicVideo(
                video("s2", "Quick Remix Clip", durationSec = 30L, isShort = false),
            ),
        )
        assertFalse(
            MusicFeedFilter.isMusicVideo(
                video("v1", "10 Tips for Android TV Performance", channel = "Tech Channel"),
            ),
        )
        assertFalse(
            MusicFeedFilter.isMusicVideo(
                video("v2", "   ", channel = "Music Channel"),
            ),
        )
    }

    @Test fun filterMusicVideosDeduplicatesAndPreservesOrder() {
        val items = listOf(
            video("v1", "Morning Vlog", channel = "Daily Vlogs"),
            video("m1", "Starlight (Official Video)", channel = "Label Records"),
            video("m2", "Night Drive Remix"),
            video("m1", "Starlight (Official Video)", channel = "Label Records"),
            video("s1", "Short Song", durationSec = 25L, isShort = true),
            video("m3", "Calm Piano Instrumental"),
        )
        val filtered = MusicFeedFilter.filterMusicVideos(items)
        assertEquals(listOf("m1", "m2", "m3"), filtered.map { it.id })
    }

    @Test fun matchesAndDeduplicatesMusicCollections() {
        val collections = listOf(
            collection("https://www.youtube.com/playlist?list=RDCLAK5uy_1", "Daily Showcase"),
            collection("https://www.youtube.com/playlist?list=PL_ALBUM", "Debut LP", isAlbum = true),
            collection("https://www.youtube.com/playlist?list=PL_HITS", "Chill Beats & Lo-Fi"),
            collection("https://www.youtube.com/playlist?list=PL_ALBUM", "Debut LP", isAlbum = true),
            collection("https://www.youtube.com/playlist?list=PL_TECH", "Kotlin Tutorials", subtitle = "Coding course"),
        )
        val filtered = MusicFeedFilter.filterMusicCollections(collections)
        assertEquals(
            listOf(
                "https://www.youtube.com/playlist?list=RDCLAK5uy_1",
                "https://www.youtube.com/playlist?list=PL_ALBUM",
                "https://www.youtube.com/playlist?list=PL_HITS",
            ),
            filtered.map { it.url },
        )
    }
}
