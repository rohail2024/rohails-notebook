package com.penandpaper.app.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounced autosave helper: each [schedule] call cancels the pending save
 * and starts a new one after [delayMs].
 */
class AutoSaver(
    private val scope: CoroutineScope,
    private val delayMs: Long = 800L
) {
    private var job: Job? = null

    fun schedule(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            block()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
