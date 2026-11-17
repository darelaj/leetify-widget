package id.my.chromazone.leetify_widget.data

import kotlinx.coroutines.sync.Mutex

// Account writes and widget publication share this lock; HTTP calls never hold it.
internal object AccountMutationLock {
    val mutex = Mutex()
}
