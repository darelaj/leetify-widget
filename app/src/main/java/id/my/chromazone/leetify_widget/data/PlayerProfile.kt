package id.my.chromazone.leetify_widget.data

internal data class PlayerProfile(
    val steamId: String,
    val name: String?,
    val aim: Double?,
    val utility: Double?,
    val leetifyRating: Double?,
    val premierRating: Double?,
    val recentMatches: List<RecentMatch>,
)
