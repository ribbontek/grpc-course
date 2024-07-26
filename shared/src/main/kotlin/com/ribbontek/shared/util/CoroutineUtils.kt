package com.ribbontek.shared.util

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): List<R> =
    coroutineScope {
        map { async { transform(it) } }.awaitAll()
    }

suspend fun <T> Iterable<T>.forEachAsync(action: (T) -> Unit) =
    coroutineScope {
        forEach { launch { action(it) } }
    }

suspend fun <T, R> Iterable<T>.mapVirtualAsync(transform: suspend (T) -> R): List<R> =
    withContext(Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()) {
        map { async { transform(it) } }.awaitAll()
    }
