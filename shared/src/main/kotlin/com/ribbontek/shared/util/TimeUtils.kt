package com.ribbontek.shared.util

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun ZonedDateTime.toUtc(): ZonedDateTime = if (isUTC()) this else this.withZoneSameInstant(ZoneOffset.UTC)

fun ZonedDateTime.atStartOfDay(): ZonedDateTime = truncatedTo(ChronoUnit.DAYS)

fun ZonedDateTime.isUTC(): Boolean = zone == ZoneOffset.UTC
