package rip

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock

class MyChannel<T> {
    private val data = mutableListOf<T>()
    private val lock = ReentrantLock()
    private val hasElement = lock.newCondition()

    suspend fun send(value: T) {
        coroutineScope {
            launch {
                lock.withInterruptiblyLock {
                    data.add(value)
                    if (data.size == 1)
                        hasElement.signalAll()
                }
            }
        }
    }

    suspend fun receive() = coroutineScope {
        lock.withInterruptiblyLock {
            while (data.isEmpty())
                hasElement.await()
            data.removeFirst()
        }
    }

}