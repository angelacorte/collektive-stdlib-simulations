package it.unibo.collektive.examples

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.examples.gossip.FirstImplementationGossip.firstGossip
import it.unibo.collektive.examples.gossip.SecondImplementationGossip.secondGossip
import it.unibo.collektive.examples.gossip.SelfStabilizingGossip.gossip
import it.unibo.collektive.examples.gossip.gossipCast
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.consensus.globalElection
import it.unibo.collektive.stdlib.ints.FieldedInts.toDouble
import it.unibo.collektive.stdlib.spreading.gradientCast
import it.unibo.collektive.stdlib.spreading.hopGradientCast
import it.unibo.collektive.stdlib.spreading.isHappeningAnywhere
import it.unibo.collektive.stdlib.util.hops
import kotlin.math.max
import kotlin.math.min

fun Aggregate<Int>.genericGossipEntrypoint(env: EnvironmentVariables): Int {
    return gossipCast(
        local = localId,
        bottom = 0.0,
        top = Double.POSITIVE_INFINITY,
        metric = hops().toDouble(),
        accumulateData = { _, _, data ->
            env["data"] = data
            min(data, localId).also { env["best-value"] = it }
        },
        accumulateDistance = Double::plus,
    )
}

/**
 * Entrypoint for the gossip simulation that uses the gossipMax function defined into Collektive's DSl.
 */
fun Aggregate<Int>.gossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = gossip(
    localId,
//    randomFromTimeElapsed(timeSensor, randomGenerator)
//        .also { env["local-value"] = it },
) { first, second ->
    second.compareTo(first)
}.also {
    env["best-value"] = it
}

/**
 * Entrypoint for the simulation of the first implementation of the gossip algorithm with Collektive.
 */
fun Aggregate<Int>.firstGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = firstGossip(
    env,
    randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it },
) { first, second ->
    first <= second
}.also { env["best-value"] = it }

/**
 * Entrypoint for the simulation of the second implementation of the gossip algorithm with Collektive.
 */
fun Aggregate<Int>.secondGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = secondGossip(
    env,
    randomFromTimeElapsed(timeSensor, randomGenerator)
        .also { env["local-value"] = it },
) { first, second ->
    second.compareTo(first)
}.also { env["best-value"] = it }

/**
 * Entrypoint for the simulation of the `isHappening` gossip function defined into Collektive's DSl.
 */
fun Aggregate<Int>.isHappeningGossipEntrypoint(
    timeSensor: TimeSensor,
) = isHappeningAnywhere {
    (timeSensor.getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
}

/**
 * Entrypoint for the simulation of the `sharingTime` function defined into Collektive's DSl.
 */
//fun Aggregate<Int>.sharedTimerEntrypoint(absoluteTime: AbsoluteTime) =
//    sharedTimer(5.toDuration(DurationUnit.SECONDS), absoluteTime.getDeltaTime())

/**
 * Entrypoint for the simulation of the `timeReplicated` function defined into Collektive's DSl,
 * it replicates the non-self-stabilizing gossip algorithm defined into Collektive's DSl.
 */
//fun Aggregate<Int>.timeReplicationEntrypoint(
//    absoluteTime: AbsoluteTime,
//    env: EnvironmentVariables,
//    randomGenerator: RandomGenerator,
//    timeSensor: TimeSensor,
//) {
//    val rand = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
//    timeReplicated(
//        absoluteTime = absoluteTime,
//        process = {
//                  nssg(rand) { first, second -> if (first >= second) first else second }
//                      .also { env["best-value"] = it }
////        nonSelfStabilizingGossip(
////            randomFromTimeElapsed(timeSensor, randomGenerator)
////                .also { env["local-value"] = it },
////        ) { first, second -> if (first <= second) first else second }
////            .also { env["best-value"] = it }
//        },
//        default = 42,
//        timeToLive = 5.toDuration(DurationUnit.SECONDS),
//        maxReplicas = 7,
//    )
//}
