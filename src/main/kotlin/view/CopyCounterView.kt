package view

import control.CopyCounterController
import domain.Copy
import kotlinx.coroutines.*
import java.awt.EventQueue
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class CopyCounterView(
    private val controller: CopyCounterController
) {
    private val copies = mutableSetOf<Copy>()
    private lateinit var eventsJob: Job
    private lateinit var frame: CopyCounterFrame

    @DelicateCoroutinesApi
    private fun subscribeToEvents() =
        GlobalScope.launch {
            controller.eventFlow.collect {
                when (it) {
                    is CopyCounterController.Event.NewCopy -> copies.add(it.copy)
                    is CopyCounterController.Event.RemoveCopy -> copies.remove(it.copy)
                }
                dataUpdated()
            }
        }

    private fun dataUpdated() {
        runOnUI {
            frame.updateCopies(copies.toList())
        }
    }

    private fun runOnUI(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }

    private fun initFrame(onExit: () -> Unit) {
        runOnUI {
            frame = CopyCounterFrame(controller)
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    super.windowClosed(e)
                    onExit()
                }
            })
            frame.isVisible = true
        }
    }

    @DelicateCoroutinesApi
    fun run(onExit: () -> Unit) {
        initFrame(onExit)
        dataUpdated()
        eventsJob = subscribeToEvents()
    }

    fun stop() {
        runBlocking {
            eventsJob.cancelAndJoin()
        }
    }
}