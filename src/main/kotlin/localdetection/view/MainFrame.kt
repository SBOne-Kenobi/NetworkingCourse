package localdetection.view

import kotlinx.coroutines.runBlocking
import localdetection.Settings
import localdetection.control.MainController
import localdetection.model.Device
import localdetection.stopThread
import java.awt.Color
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.thread

class MainFrame(
    private val controller: MainController
) : JFrame("Local detection") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(Settings.windowWidth, Settings.windowHeight)
        setLocationRelativeTo(null)
        isResizable = false
    }

    private var executor: ExecutorService? = null
    private var processThread: Thread? = null

    private fun initTable() {
        deviceTableModel.dataVector.clear()
        deviceTableModel.addRow(arrayOf("Current device"))
        deviceTableModel.addRow(arrayOf("Network devices"))
        deviceTableModel.fireTableDataChanged()
    }

    private fun Device.toArray() = arrayOf(
        ipAddress.hostAddress,
        macAddress.joinToString("-") { byte ->
            byte.toUByte().toString(16).uppercase().let {
                "0".repeat(2 - it.length) + it
            }
        },
        name ?: "-"
    )

    private fun addCurrentDevice(device: Device) {
        deviceTableModel.insertRow(1, device.toArray())
        executor!!.execute {
            deviceTableModel.dataVector[1][2] = device.ipAddress.hostName
            deviceTableModel.fireTableCellUpdated(1, 2)
        }
        deviceTableModel.fireTableRowsInserted(1, 1)
    }

    private fun addNewDevice(device: Device) {
        val row = deviceTableModel.rowCount
        deviceTableModel.addRow(device.toArray())
        executor!!.execute {
            deviceTableModel.dataVector[row][2] = device.ipAddress.hostName
            deviceTableModel.fireTableCellUpdated(row, 2)
        }
        deviceTableModel.fireTableRowsInserted(row, row)
    }

    private fun processDetection() {
        executor?.shutdownNow()
        executor = Executors.newFixedThreadPool(5)
        initTable()
        processThread = thread {
            try {
                runBlocking {
                    controller.detect().collect { event ->
                        when (event) {
                            is MainController.StartEvent -> {
                                detectionProgress.maximum = event.totalAddresses
                                detectionProgress.value = 0
                            }
                            is MainController.CurrentDevice -> {
                                addCurrentDevice(event.device)
                                ++detectionProgress.value
                            }
                            is MainController.NewDevice -> {
                                addNewDevice(event.device)
                                ++detectionProgress.value
                            }
                            MainController.EmptyIter -> {
                                ++detectionProgress.value
                            }
                        }
                    }
                }
            } catch (_: InterruptedException) {
            } finally {
                detectButton.text = "Detect"
                processThread = null
            }
        }
    }

    private val detectButton = JButton("Detect").apply {
        addActionListener {
            if (processThread == null) {
                processDetection()
                text = "Cancel"
            } else {
                controller.cancelDetection()
            }
        }
    }

    private val detectionProgress = JProgressBar()

    private val deviceTableModel = DefaultTableModel(emptyArray(), arrayOf(
        "IP Address", "MAC Address", "Host name"
    ))
    private val deviceTable = object : JTable(deviceTableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }.apply {
        tableHeader.reorderingAllowed = false
        columnSelectionAllowed = false
        rowSelectionAllowed = false
        initTable()
    }
    private val deviceScroll = JScrollPane(deviceTable).apply {
        viewport.background = Color.white
    }

    init {
        val groupLayout = GroupLayout(contentPane)
        contentPane.layout = groupLayout
        groupLayout.apply {
            autoCreateContainerGaps = true
            autoCreateGaps = true
            setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup()
                    .addComponent(detectButton)
                    .addComponent(detectionProgress)
                )
                .addComponent(deviceScroll)
            )
            setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup()
                    .addComponent(detectButton)
                    .addComponent(
                        detectionProgress,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.DEFAULT_SIZE,
                        Short.MAX_VALUE.toInt()
                    )
                )
                .addComponent(deviceScroll)
            )
        }
    }

    fun close() {
        executor?.shutdownNow()
        executor = null
        processThread?.stopThread()
    }

}