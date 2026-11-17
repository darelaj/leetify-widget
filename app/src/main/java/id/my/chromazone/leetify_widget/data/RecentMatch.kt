package id.my.chromazone.leetify_widget.data

internal data class RecentMatch(
    val id: String,
    val finishedAtMillis: Long?,
    val mapName: String?,
    val outcome: MatchOutcome,
)
