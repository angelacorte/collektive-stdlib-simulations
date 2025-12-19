package it.unibo.collektive.examples

import it.unibo.alchemist.util.RandomGenerators.nextDouble
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import org.apache.commons.math3.random.RandomGenerator

fun Aggregate<Int>.keepRandom(
    randomGenerator: RandomGenerator,
    startInclusive: Double,
    endInclusive: Double,
) = evolve(randomGenerator.nextDouble(startInclusive, endInclusive)) { it }

fun Aggregate<Int>.randomFromTimeElapsed(
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = when {
    timeSensor.getTimeAsDouble() <= 400.0 -> keepRandom(randomGenerator, -1.0, 1.0)
    else -> keepRandom(randomGenerator, -0.5, 0.5)
}
