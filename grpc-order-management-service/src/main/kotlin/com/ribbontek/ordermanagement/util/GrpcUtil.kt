package com.ribbontek.ordermanagement.util

import com.google.protobuf.MessageLite
import java.util.Base64

fun MessageLite.toBase64String(): String = toByteArray().toBase64String()

fun String.decodeBase64(): ByteArray = Base64.getDecoder().decode(this)

fun ByteArray.toBase64String(): String = Base64.getEncoder().encodeToString(this)
