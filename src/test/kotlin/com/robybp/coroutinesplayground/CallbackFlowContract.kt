package com.robybp.coroutinesplayground

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.*

class CallbackFlowContract {

    @Test
    fun `should emit value when value is produced`() =
        runTest {
            val producer = EventProducer(1)

            val flow = callbackFlow {
                val callback: (Int) -> Unit = { number -> trySend(number) }
                producer.addNumberProducedListeners(callback)
                awaitClose { producer.removeNumberProducedListener(callback) }
            }

            assertNotNull(flow.first())
        }

    @Test
    fun `should throw IllegalStateException if awaitClose() is not set up`() =
        runTest {
            assertFailsWith(IllegalStateException::class) {
                callbackFlow<Unit> { }.collect {}
            }
        }

    @Test
    fun `should call awaitClose() block when there are no more consumers`() =
        runTest {
            val producer = EventProducer(1000)
            var awaitCloseCalled = false

            val flow = callbackFlow {
                producer.addNumberProducedListeners(::trySend)
                awaitClose { awaitCloseCalled = true }
            }

            val job1 = launch {
                flow.collect()
            }

            delay(100L)

            job1.cancelAndJoin()

            assertTrue(awaitCloseCalled)
        }

    @Test
    fun `should be cold flow`() =
        runTest {
            val producer = EventProducer(1000)
            var value: Int? = null

            val flow = callbackFlow<Unit> {
                val callback: (Int) -> Unit = { number ->
                    value = number
                    trySend(Unit)
                }
                producer.addNumberProducedListeners(callback)
                awaitClose { producer.removeNumberProducedListener(callback) }
            }

            assertNull(value)

            flow.first()

            assertNotNull(value)
        }

    @Test
    fun `should propagate exception if exception is thrown`() =
        runTest {
            val expected = IllegalStateException("I'm expecting this")

            val actual = try {
                callbackFlow<Unit> { throw expected }.first()
                null
            } catch (thr: Throwable) {
                thr
            }

            assertEquals(expected.message, actual!!.message)
        }
}

private class EventProducer(private val count: Int) {

    private val listeners: MutableSet<(Int) -> Unit> = mutableSetOf()

    init {
        thread {
            var counter = 0

            while (counter != count) {
                Thread.sleep(1000)
                listeners.forEach { it.invoke(counter++) }
            }
        }
    }

    fun addNumberProducedListeners(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun removeNumberProducedListener(listener: (Int) -> Unit) {
        listeners.remove(listener)
    }
}
