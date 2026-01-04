package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.gossip.NoLoopAgainstNeighborsGossip.noLoopAgainstNeighborsGossip
import it.unibo.collektive.stdlib.spreading.gossip
import it.unibo.collektive.utils.randomFromTimeElapsed
import org.jooq.lambda.Agg

fun Aggregate<Int>.selfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double {
    val selector: (Double, Double) -> Double = if (env["findMax"]) ::maxOf else ::minOf
    return noLoopAgainstNeighborsGossip(
        local = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it },
        selector = selector,
    ).also { env["gossip-value"] = it }
}
