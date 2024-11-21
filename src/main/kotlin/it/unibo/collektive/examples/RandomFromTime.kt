package it.unibo.collektive.examples

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

fun Aggregate<Int>.random(randomGenerator: RandomGenerator) = evolve(randomGenerator.nextRandomDouble()) { it }

fun Aggregate<Int>.randomFromTimeElapsed(
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = when {
    timeSensor.getTimeAsDouble() % 1000 <= 150.0 -> random(randomGenerator)
    else -> random(randomGenerator)
}
