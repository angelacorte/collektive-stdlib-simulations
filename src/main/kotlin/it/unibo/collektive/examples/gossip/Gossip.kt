package it.unibo.collektive.examples.gradient

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.operators.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.field.Field.Companion.hood
import it.unibo.collektive.field.operations.min
import it.unibo.collektive.stdlib.doubles.FieldedDoubles.plus
import kotlin.Double.Companion.POSITIVE_INFINITY

fun Aggregate<Int>.gossipEntrypoint() = gossip(localId)

data class ValueCheckedBy<V: Any, ID: Any>(val value: V, val checkedBy: List<ID>)

fun <ID : Any, V: Comparable<V>> Aggregate<ID>.gossip(
    value: V,
) = share(ValueCheckedBy(value, listOf(localId))) {
    it.hood(ValueCheckedBy(value, listOf(localId))) { actual, checking ->
        when(actual.value > checking.value) {
            true -> actual
            else -> ValueCheckedBy(checking.value, checking.checkedBy + localId)
        }
    }
}