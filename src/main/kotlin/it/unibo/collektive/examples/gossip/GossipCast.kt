package it.unibo.collektive.examples.gossip

import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.DelicateCollektiveApi
import it.unibo.collektive.aggregate.api.exchange
import it.unibo.collektive.aggregate.api.mapNeighborhood
import it.unibo.collektive.stdlib.util.Reducer
import it.unibo.collektive.stdlib.util.coerceIn
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(DelicateCollektiveApi::class)
@JvmOverloads
inline fun <reified ID : Any, reified Value, reified Distance : Comparable<Distance>> Aggregate<ID>.gossipCast(
    local: Value,
    bottom: Distance,
    top: Distance,
    metric: Field<ID, Distance>,
    maxDiameter: Int = Int.MAX_VALUE,
    noinline accumulateData: (fromSource: Distance, toNeighbor: Distance, neighborData: Value) -> Value =
        { _, _, data -> data },
    crossinline accumulateDistance: Reducer<Distance>,
): Value {
    val coercedMetric = metric.coerceIn(bottom, top)
    val fromLocal = GradientPath<ID, Value, Distance>(bottom, local, emptyList())
    return exchange(fromLocal) { neighborData: Field<ID, GradientPath<ID, Value, Distance>> ->
        val neighbors = neighborData.neighbors
        // Accumulated distances with neighbors, to be used to exclude invalid paths
        val accDistances = neighborData.alignedMapValues(coercedMetric) { path, distance ->
            accumulateDistance(path.distance, distance)
        }
        val neighborAccumulatedDistances = accDistances.excludeSelf()
        val nonLoopingPaths = neighborData.alignedMap(accDistances, coercedMetric) { id, path, accDist, distance ->
            path.takeUnless { id == localId }
                .takeUnless { path.length > maxDiameter }
                .takeUnless { localId in path.hops }
                .takeUnless { path.isInvalidViaShortcut(accDist, neighbors, neighborAccumulatedDistances) }
                ?.run { accDist to lazy { update(id, distance, bottom, top, accumulateDistance, accumulateData) } }
        }.excludeSelf().values.asSequence().filterNotNull().sortedBy { it.first }.map { it.second.value }
//        val best = when {
//            fromLocal != null -> sequenceOf(fromLocal)
//            else -> {
        val pathsHopSets by lazy { nonLoopingPaths.associate { it.nextHop to it.hops.toSet() } }
        val best = nonLoopingPaths.filter { reference ->
            /*
             * Path-coherence: paths that contain inconsistent information must be removed.
             * In particular, if some path passes through A and then B, and another reaches the source
             * through B and then A, we must keep only the shortest
             * (unless they have the same path-length, namely the network is symmetric).
             */
            val refSize = reference.length
            refSize <= 1 ||
                nonLoopingPaths.all { other ->
                    other.length > refSize ||
                            // the current reference is shorter
                            // same hop count, same distance (symmetric network or same path)
                            (other.length == refSize && other.distance == reference.distance) ||
                            // all common hops appear in the same order
                            reference.allCommonHopsAppearInTheSameOrderOf(other, pathsHopSets)
                }
        }
//            }
//        }
        val bestLazyList = best.map { lazy { it } }.toList()
//        mapNeighborhood { neighborID ->
//            val gp: GradientPath<ID, Value, Distance> = bestLazyList.firstOrNull { it.value.hops.lastOrNull() != neighborID }?.value ?: fromLocal
//            gp
//        }
        mapNeighborhood { neighbor -> bestLazyList.firstOrNull { it.value.hops.lastOrNull() != neighbor }?.value ?: fromLocal }
    }.local.value.data// ?: local
}

@PublishedApi
internal data class GradientPath<ID : Any, Value, Distance : Comparable<Distance>>(
    @JvmField
    val distance: Distance,
    @JvmField
    val data: Value,
    @JvmField
    val hops: List<ID> = emptyList(),
) : Comparable<GradientPath<ID, Value, Distance>> {

    /**
     * The ID of the original source node where this path begins.
     */
    val source: ID get() = hops.first()

    /**
     * The ID of the neighbor device on the next hop toward this device.
     */
    val nextHop: ID get() = hops.last()

    /**
     * The number of hops (segments) in this path.
     */
    val length: Int get() = hops.size

    /**
     * Returns true if this path includes the specified device ID.
     *
     * @param id The device identifier to check.
     * @return True if the path hops contain the given ID.
     */
    operator fun contains(id: ID): Boolean = hops.contains(id)

    /**
     * Creates a new path by extending this one with a neighbor hop.
     *
     * @param neighbor The ID of the next hop (neighbor device).
     * @param distanceToNeighbor The edge distance to the neighbor.
     * @param bottom The minimum allowed distance (source base).
     * @param top The maximum allowed distance (distance clamp).
     * @param accumulateDistance Reducer to combine two distances and enforce bounds.
     * @param accumulateData Function to update the carried data when crossing the edge.
     * @return A new [GradientPath] including the neighbor hop, updated distance, and data.
     */
    @OptIn(DelicateCollektiveApi::class)
    inline fun update(
        neighbor: ID,
        distanceToNeighbor: Distance,
        bottom: Distance,
        top: Distance,
        crossinline accumulateDistance: Reducer<Distance>,
        crossinline accumulateData: (fromSource: Distance, toNeighbor: Distance, data: Value) -> Value,
    ): GradientPath<ID, Value, Distance> {
        val totalDistance = accumulate(bottom, top, distance, distanceToNeighbor, accumulateDistance)
        val updatedData = accumulateData(distance, distanceToNeighbor, data)
        return GradientPath(totalDistance, updatedData, hops + neighbor)
    }

    override fun compareTo(other: GradientPath<ID, Value, Distance>) =
        compareBy<GradientPath<ID, Value, Distance>> { it.distance }.compare(this, other)

    // Check if the path is invalid because there is a shortcut through an intermediate neighboring hop
    @PublishedApi
    internal fun isInvalidViaShortcut(
        accDist: Distance?,
        neighbors: Set<ID>,
        neighborAccumulatedDistances: Map<ID, Distance?>,
    ): Boolean = accDist != null &&
            hops.asSequence()
                .filter { it in neighbors }
                .map { neighborAccumulatedDistances[it] }
                .any { it == null || it < accDist }

    // Check if all hops that appear in both paths have the same order
    @PublishedApi
    internal fun allCommonHopsAppearInTheSameOrderOf(
        other: GradientPath<ID, Value, Distance>,
        pathsHopSets: Map<ID, Set<ID>>,
    ): Boolean {
        val otherHops = pathsHopSets[other.nextHop].orEmpty()
        val commonHops = hops.filter { it in otherHops }
        return when (commonHops.size) {
            0, 1 -> true
            else -> {
                // otherHops and commonHops must have the same order for all elements
                val commonIterator = commonHops.iterator()
                val otherIterator = otherHops.iterator()
                var matches = 0
                while (commonIterator.hasNext() && otherIterator.hasNext()) {
                    val common = commonIterator.next()
                    val matchesSoFar = matches
                    while (otherIterator.hasNext() && matchesSoFar == matches) {
                        if (common == otherIterator.next()) {
                            matches++
                        }
                    }
                }
                matches == commonHops.size
            }
        }
    }
}

/**
 * Combines two distances with bounds checking and triangle-inequality enforcement.
 *
 * Uses the provided accumulator to sum `distance` and `distanceToNeighbor`, clamps the result
 * between `bottom` and `top`, and verifies the triangle inequality.
 *
 * @param D The comparable type used for distances.
 * @param bottom The minimum allowed distance.
 * @param top The maximum allowed distance.
 * @param distance The current accumulated distance.
 * @param distanceToNeighbor The edge distance to add.
 * @param accumulator Reducer function to combine two distances.
 * @return The new clamped distance result.
 * @throws IllegalStateException if the accumulator violates the triangle inequality.
 */
@OptIn(ExperimentalContracts::class)
@PublishedApi
internal inline fun <D : Comparable<D>> accumulate(
    bottom: D,
    top: D,
    distance: D,
    distanceToNeighbor: D,
    accumulator: Reducer<D>,
): D {
    contract {
        callsInPlace(accumulator, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val totalDistance = accumulator(distance, distanceToNeighbor).coerceIn(bottom, top)
    check(totalDistance >= distance && totalDistance >= distanceToNeighbor) {
        "The provided distance accumulation function violates the triangle inequality: " +
                "accumulating $distance and $distanceToNeighbor produced $totalDistance"
    }
    return totalDistance
}