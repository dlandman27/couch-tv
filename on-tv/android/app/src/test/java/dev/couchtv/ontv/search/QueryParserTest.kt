package dev.couchtv.ontv.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The natural-language queries Orbit's search promises to understand, pinned to their structure. */
class QueryParserTest {

    @Test fun horrorMoviesForHalloween() {
        val q = QueryParser.parse("Show me horror movies for Halloween.")
        assertEquals(listOf("horror"), q.genres)
        assertEquals(setOf(Kind.Movie), q.kinds)
        assertEquals(emptyList<String>(), q.words)   // "halloween" is the genre, not a title word
    }

    @Test fun sciFiFromThe2010s() {
        val q = QueryParser.parse("Find sci-fi movies from the 2010s")
        assertEquals(listOf("sci-fi"), q.genres)
        assertEquals(setOf(Kind.Movie), q.kinds)
        assertEquals(2010..2019, q.years)
        assertEquals(emptyList<String>(), q.words)
    }

    @Test fun islandersGame() {
        val q = QueryParser.parse("Find the Islanders game")
        assertEquals(setOf(Kind.Game), q.kinds)
        assertEquals(listOf("islanders"), q.words)
    }

    @Test fun nbaGamesOnEspn() {
        val q = QueryParser.parse("Show me NBA games on ESPN")
        assertEquals(listOf("nba"), q.leagues)
        assertEquals(listOf("espn"), q.networks)
        assertEquals(setOf(Kind.Game), q.kinds)
        assertEquals(emptyList<String>(), q.words)
    }

    @Test fun openYoutube() {
        val q = QueryParser.parse("Open YouTube")
        assertEquals("youtube", q.openApp)
        assertEquals(setOf(Kind.App), q.kinds)
    }

    @Test fun liveSportsChannels() {
        val q = QueryParser.parse("Show me all live sports channels")
        assertTrue(q.sportsChannels)
        assertEquals(setOf(Kind.Channel), q.kinds)
        assertEquals(emptyList<String>(), q.words)
    }

    @Test fun plainTitleIsPlainText() {
        val q = QueryParser.parse("the last of us")
        assertFalse(q.structured)
        assertEquals(listOf("the", "last", "of", "us"), q.words)
        assertNull(q.summary())
    }

    @Test fun showMeIsNotAskingForTvShows() {
        val q = QueryParser.parse("show me comedies")
        assertEquals(listOf("comedy"), q.genres)
        assertFalse(Kind.Show in q.kinds && Kind.Movie !in q.kinds)   // genre alone → films and shows
    }

    @Test fun nineties() {
        assertEquals(1990..1999, QueryParser.parse("action movies from the 90s").years)
    }
}
