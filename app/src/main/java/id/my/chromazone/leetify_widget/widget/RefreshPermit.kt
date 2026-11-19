package id.my.chromazone.leetify_widget.widget

internal sealed interface RefreshPermit {
    data object Wait : RefreshPermit
    data object Finished : RefreshPermit
}
