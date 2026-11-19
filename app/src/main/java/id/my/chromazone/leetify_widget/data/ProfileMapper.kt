package id.my.chromazone.leetify_widget.data

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

internal const val MAX_RECENT_MATCHES = 10

@OptIn(ExperimentalTime::class)
internal fun JsonObject.toPlayerProfile(steamId: String): PlayerProfile {
    if (keys.none { it in setOf("steam64_id", "rating", "ranks", "recent_matches", "privacy_mode") }) {
        throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
    }
    val responseId = this["steam64_id"].text()
    if (responseId != null && responseId != steamId) throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
    val rating = this["rating"] as? JsonObject
    val ranks = this["ranks"] as? JsonObject
    val matches = (this["recent_matches"] as? JsonArray).orEmpty().mapNotNull { element ->
        val match = element as? JsonObject ?: return@mapNotNull null
        val id = match["id"].text()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val finishedAt = match["finished_at"].text()?.let {
            try {
                Instant.parse(it).toEpochMilliseconds()
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        RecentMatch(
            id = id,
            finishedAtMillis = finishedAt,
            mapName = match["map_name"].text(),
            outcome = when (match["outcome"].text()) {
                "win" -> MatchOutcome.WIN
                "loss" -> MatchOutcome.LOSS
                "draw" -> MatchOutcome.DRAW
                else -> MatchOutcome.UNKNOWN
            },
        )
    }.sortedWith(compareByDescending<RecentMatch> { it.finishedAtMillis })
        .distinctBy { it.id }.take(MAX_RECENT_MATCHES)
    return PlayerProfile(
        steamId = steamId,
        name = this["name"].text(),
        aim = rating?.get("aim").number(),
        utility = rating?.get("utility").number(),
        leetifyRating = ranks?.get("leetify").number(),
        premierRating = ranks?.get("premier").number(),
        recentMatches = matches,
    )
}

private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)
    ?.takeUnless { it.isString }?.doubleOrNull?.takeIf { it.isFinite() }

internal fun JsonElement?.text(): String? = (this as? JsonPrimitive)
    ?.takeIf { it.isString }?.content
