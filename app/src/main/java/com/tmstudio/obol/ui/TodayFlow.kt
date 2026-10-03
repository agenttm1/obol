package com.tmstudio.obol.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Današnji datum koji se sam osvježi u ponoć, ako ekran ostane otvoren preko noći. */
fun todayFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now(clock)
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, nextMidnight).toMillis() + 1)
    }
}.distinctUntilChanged()
