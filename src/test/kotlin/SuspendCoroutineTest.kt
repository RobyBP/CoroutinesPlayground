import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SuspendCoroutineTest {

    @Test
    fun `should return expected value if no errors`() =
        runTest {
            val expected = "I'm expecting this"

            val actual = suspendCoroutine<String> { continuation -> continuation.resume(expected) }

            assertEquals(expected, actual)
        }

    @Test
    fun `should throw expected throwable in case of error`() =
        runTest {
            val expected = RuntimeException("I'm expecting this")

            val actual =
                try {
                    suspendCoroutine<String> { continuation -> continuation.resumeWithException(expected) }
                    null
                } catch (thr: Throwable) {
                    thr
                }

            assertEquals(expected, actual)
        }

    @Test
    fun `even if the suspend coroutine returns an item on a new thread, the execution should continue on the initial one`() =
        runTest {
            val expected = Thread.currentThread().id

            suspendCoroutine<String> { continuation -> thread(name = "New thread") { continuation.resume("") } }

            val actual = Thread.currentThread().id

            assertEquals(expected, actual)
        }

    @Test
    fun `should throw IllegalStateException in case of multiple Resume attempts`() =
        runTest {
            val result = try {
                suspendCoroutine<String> { continuation ->
                    continuation.resume("")
                    continuation.resumeWithException(RuntimeException(""))
                }
                null
            } catch (thr: Throwable) {
                thr
            }

            assertIs<IllegalStateException>(result)
        }
}
