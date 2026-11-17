package id.my.chromazone.leetify_widget.data

import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class PlayerProfileTest {
    private val steamId = "76561190000000000"

    @Test
    fun mapsExactProfileFieldsAndFiveLatestUniqueResults() {
        val fixture = checkNotNull(javaClass.getResource("/profile-synthetic.json")).readText()
        val profile = parse(fixture)
        assertEquals(steamId, profile.steamId)
        assertEquals("Synthetic Player", profile.name)
        assertEquals(61.25, profile.aim!!, 0.0)
        assertEquals(0.0, profile.utility!!, 0.0)
        assertEquals(-2.12, profile.leetifyRating!!, 0.0)
        assertNull(profile.premierRating)
        assertEquals(listOf("newest", "duplicate", "unknown", "fifth", "old"), profile.recentMatches.map { it.id })
        assertEquals(listOf(MatchOutcome.WIN, MatchOutcome.DRAW, MatchOutcome.UNKNOWN, MatchOutcome.LOSS, MatchOutcome.WIN), profile.recentMatches.map { it.outcome })
        assertEquals("de_nuke", profile.recentMatches.first().mapName)
        assertEquals(1793959200000L, profile.recentMatches.first().finishedAtMillis)
    }

    @Test
    fun missingAndMalformedMetricsStayUnavailable() {
        for (body in listOf(
            """{"ranks":null}""",
            """{"rating":{"aim":"bad","utility":{}},"ranks":{"leetify":1e999,"premier":"19000"}}""",
        )) {
            val profile = parse(body)
            assertNull(profile.aim)
            assertNull(profile.utility)
            assertNull(profile.leetifyRating)
            assertNull(profile.premierRating)
            assertTrue(profile.recentMatches.isEmpty())
        }
        assertEquals(0.0, parse("""{"ranks":{"premier":0}}""").premierRating!!, 0.0)
    }

    @Test
    fun badDatesSortAfterValidDatesAndUnknownOutcomesRemainExplicit() {
        val matches = parse("""{"recent_matches":[
            {"id":"bad","finished_at":"bad","outcome":"tie"},
            {"id":"valid","finished_at":"2026-11-06T10:00:00Z","outcome":"loss"},
            {"id":"missing"}
        ]}""").recentMatches
        assertEquals(listOf("valid", "bad", "missing"), matches.map { it.id })
        assertNull(matches[1].finishedAtMillis)
        assertEquals(MatchOutcome.UNKNOWN, matches[1].outcome)
        assertEquals(MatchOutcome.UNKNOWN, matches[2].outcome)
    }

    @Test
    fun rejectsErrorObjectsAndAnotherAccountsProfile() {
        assertThrows(IOException::class.java) { parse("{}") }
        assertThrows(IOException::class.java) { parse("""{"error":"unavailable"}""") }
        assertThrows(IOException::class.java) { parse("""{"steam64_id":"76561190000000001"}""") }
    }

    private fun parse(body: String) = Json.parseToJsonElement(body).jsonObject.toPlayerProfile(steamId)
}
