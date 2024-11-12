package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

fun Aggregate<Int>.random(randomGenerator: RandomGenerator) = repeat(randomGenerator.nextRandomDouble()) { it }

fun Aggregate<Int>.randomFromTimeElapsed(
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = if (timeSensor.getTimeAsDouble() % 200 in 10.0 .. 50.0) {
    random(randomGenerator)
} else {
    random(randomGenerator)
}
