package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

context(RandomGenerator)
fun Aggregate<Int>.random() = repeat(nextRandomDouble()) { it }

context(EnvironmentVariables, TimeSensor, RandomGenerator)
fun Aggregate<Int>.randomFromTimeElapsed() =
    if (getTimeAsDouble() % 200 < 100) random() else random()
