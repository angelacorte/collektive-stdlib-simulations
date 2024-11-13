package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.collektive.alchemist.device.sensors.AbsoluteTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

class AbsoluteTimeProperty<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    override val node: Node<T>,
) : AbsoluteTime, NodeProperty<T> {
    private lateinit var timeOfLastReplica: Instant

    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> =
        AbsoluteTimeProperty(environment, node)

    override fun getAbsoluteTime(): Instant =
        Clock.System.now()

    override fun getDeltaTime(): Duration {
        val newReplicaTime = getAbsoluteTime()
        val delta = if (this::timeOfLastReplica.isInitialized) {
            (newReplicaTime - timeOfLastReplica)
        } else {
            ZERO
        }
        timeOfLastReplica = newReplicaTime
        return delta
    }
}
