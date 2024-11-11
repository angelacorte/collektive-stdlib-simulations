package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

class TimeSensorProperty<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    override val node: Node<T>,
) : TimeSensor, NodeProperty<T> {
    private var timeOfLastReplica = 0

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        TimeSensorProperty(environment, node)

    override fun getTimeAsDouble(): Double =
        environment.simulation.time.toDouble()

    //nano time
    //unix time stamp

    //time stamp

    override fun getTimeAsInt(): Int =
        environment.simulation.time.toDouble().toInt()

    override fun getDeltaTime(): Int {
        val newReplicaTime = getTimeAsInt() - timeOfLastReplica
        updateReplicaTime(newReplicaTime)
        return newReplicaTime
    }

    private fun updateReplicaTime(time: Int) {
        timeOfLastReplica = time
    }
}
