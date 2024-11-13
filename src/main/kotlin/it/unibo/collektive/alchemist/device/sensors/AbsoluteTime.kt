package it.unibo.collektive.alchemist.device.sensors

import kotlinx.datetime.Instant
import kotlin.time.Duration

interface AbsoluteTime {
    fun getAbsoluteTime(): Instant

    fun getDeltaTime(): Duration
}
