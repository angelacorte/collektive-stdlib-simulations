package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.gossip.NoLoopAgainstNeighborsGossip.noLoopAgainstNeighborsGossip
import it.unibo.collektive.examples.randomFromTimeElapsed
import org.apache.commons.math3.random.RandomGenerator

/**
 * Split and Merge experiments by using self-stabilizing gossip that avoids loops against neighbors.
 */
fun Aggregate<Int>.selfStabGossip(
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
    selector: (Double, Double) -> Double,
): Double = noLoopAgainstNeighborsGossip(
    local = randomFromTimeElapsed(timeSensor, randomGenerator),
    selector = selector,
)

fun Aggregate<Int>.minSelfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    selfStabGossip(randomGenerator, timeSensor, ::minOf).also { env["best-value"] = it }

fun Aggregate<Int>.maxSelfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    selfStabGossip(randomGenerator, timeSensor, ::maxOf).also { env["best-value"] = it }

fun Aggregate<Int>.avgSelfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    selfStabGossip(randomGenerator, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

fun Aggregate<Int>.setUnionSelfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double =
    selfStabGossip(randomGenerator, timeSensor, TODO("Not yet implemented")).also { env["best-value"] = it }

