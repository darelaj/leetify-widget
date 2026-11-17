package id.my.chromazone.leetify_widget.widget

import id.my.chromazone.leetify_widget.data.PlayerProfile
import id.my.chromazone.leetify_widget.data.ProfileFailure

internal data class WidgetSnapshot(
    val status: WidgetStatus = WidgetStatus.SETUP,
    val profile: PlayerProfile? = null,
    val fetchedAtMillis: Long? = null,
    val failure: ProfileFailure? = null,
)
