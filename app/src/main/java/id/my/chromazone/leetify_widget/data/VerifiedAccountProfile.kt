package id.my.chromazone.leetify_widget.data

internal data class VerifiedAccountProfile(
    val accountRevision: String,
    val profile: PlayerProfile,
    val fetchedAtMillis: Long,
)
