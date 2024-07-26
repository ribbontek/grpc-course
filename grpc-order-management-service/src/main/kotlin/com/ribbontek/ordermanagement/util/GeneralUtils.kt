package com.ribbontek.ordermanagement.util

import java.util.UUID

fun String.toUUID(): UUID = UUID.fromString(this)

@Suppress("UNCHECKED_CAST")
fun <T> Class<T>.newInstanceOf(): T? {
    return constructors.singleOrNull { it.parameterCount == 0 }?.newInstance() as T?
}
