package com.ribbontek.ordermanagement.util

fun String.replaceTokens(properties: Map<String, String>): String =
    properties.entries.fold(this) { acc, (key, value) ->
        acc.replace("{{$key}}", value)
    }
