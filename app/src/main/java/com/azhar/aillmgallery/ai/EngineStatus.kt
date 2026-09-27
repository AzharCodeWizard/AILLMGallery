package com.azhar.aillmgallery.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Snapshot of the engine for status chips and headers. */
data class EngineStatus(
    val modelName: String = "",
    val backend: String = "",
    val isLoading: Boolean = true,
    val isDemo: Boolean = false
) {
    val isReady: Boolean get() = !isLoading
}

fun LlmInferenceManager.engineStatusIn(scope: CoroutineScope): StateFlow<EngineStatus> = combine(
    activeModelName,
    activeBackend,
    isInitialized,
    isLoading,
    isDemoMode
) { name, backend, ready, loading, demo ->
    EngineStatus(
        modelName = name.substringBeforeLast('.'),
        backend = backend,
        isLoading = loading || !ready,
        isDemo = demo
    )
}.stateIn(scope, SharingStarted.WhileSubscribed(5_000), EngineStatus())
