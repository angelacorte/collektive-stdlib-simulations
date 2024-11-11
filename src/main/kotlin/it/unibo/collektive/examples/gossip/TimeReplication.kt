package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.field.operations.maxBy
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

fun <ID : Comparable<ID>, Type : Any> Aggregate<ID>.timeReplicated(
    timeSensor: TimeSensor,
    process: () -> Type,
    default: Type,
    timeToLive: Duration,
    maxReplicas: Int,
): Type {
    // time elapsed without a new replica being created
    val timeElapsed = ZERO //sharedTimer(timeToLive, timeSensor.getDeltaTime())
    val result = repeat(emptyList<Replica<Type>>()) { replicas ->
        // kill the oldest one if there are more than maxReplicas, or if enough time has passed
        val applyReplicas = when {
            replicas.isEmpty() -> listOf(Replica(0u, process, ZERO))
            else -> {
                val maxID = replicas.maxBy { it.id }.id
                val oldest = replicas.maxBy { r -> r.timeAlive }
                when {
                    oldest.timeAlive >= timeToLive || replicas.size == maxReplicas ->
                        replicas.filter { it.id == oldest.id } + Replica(maxID + 1u, process, timeElapsed)
                    else ->
                        when {
                            timeElapsed > ZERO -> replicas + Replica(maxID + 1u, process, timeElapsed)
                            else -> replicas
                        }
                }
            }
        }
        applyReplicas.forEach {
            alignedOn(it.id) { it.process() }
        }
        applyReplicas
    }
    return result.firstOrNull()?.process?.let { it() } ?: default
}

// implementation as in protelis
fun <ID : Comparable<ID>> Aggregate<ID>.sharedTimer(timeToLive: Double, processTime: Double): Int {
    return share(0) { clocks ->
        val clockPerceived = clocks.maxBy(clocks.localValue) { it }
        if (clockPerceived <= clocks.localValue) { // currently as fast as the fastest device in the neighborhood, so keep on counting time
            clocks.localValue + if (cyclicTimerWithDecay(timeToLive, processTime)) 1 else 0
        } else {
            clockPerceived
        }
    }
}

data class Replica<Type>(val id: ULong, val process: () -> Type, val timeAlive: Duration)

private fun <ID : Comparable<ID>> Aggregate<ID>.cyclicTimerWithDecay(timeout: Double, decayRate: Double): Boolean =
    share(timeout) { timer ->
        if (timer.localValue <= 0) {
            timeout
        } else {
            countDownWithDecay(timeout, decayRate)
        }
    } == timeout

fun <ID : Comparable<ID>> Aggregate<ID>.countDownWithDecay(timeout: Double, decayRate: Double): Double =
    timer(timeout, 0.0) { time -> time - decayRate }

fun <ID : Comparable<ID>> Aggregate<ID>.timer(initial: Double, lowerBound: Double, decayRate: (Double) -> Double): Double =
    repeat(initial) { time ->
        min(initial, max(lowerBound, decayRate(time)))
    }

/*
val oldest = replicas.maxByOrNull { r -> r.timeAlive }
//        var filteredReplica = if (replicas.size == maxReplicas && oldest != null) {
//            replicas.filter { it.id == oldest.id }
//        } else if (replicas.isNotEmpty() && replicas.maxBy { it.timeAlive }.timeAlive >= timeToLive) {
//
//        } else {
//            replicas
//        }
//        val maxId: ULong = replicas.maxBy { it.id }.id // the max replica id in the network
//        filteredReplica = if (timeElapsed > 0) replicas + Replica(maxId + 1u, process, timeElapsed) else replicas // todo timeElapsed should be something else but I can't understand what
//        filteredReplica.forEach {
//            alignedOn(it.id) { it.process() } //(re-)start the processes
//        }
//        filteredReplica
 */