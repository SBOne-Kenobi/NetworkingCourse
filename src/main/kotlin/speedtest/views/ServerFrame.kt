package speedtest.views

import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import speedtest.Settings
import speedtest.controls.Protocol
import speedtest.controls.ServerController
import speedtest.stopThread
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.InetAddress
import javax.swing.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

class ServerFrame(
    private val controller: ServerController
) : JFrame("Speed test server") {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(Settings.windowWidth, Settings.windowHeight)
        setLocationRelativeTo(null)
        isResizable = false
    }

    private val timer = java.util.Timer()

    fun createDialog(
        title: String, message: String,
        lifetime: Long? = null,
        messageType: Int = JOptionPane.ERROR_MESSAGE
    ) {
        val pane = JOptionPane(message, messageType)
        val dialog = pane.createDialog(title)
        lifetime?.let {
            timer.schedule(it) {
                dialog.dispose()
            }
        }
        dialog.isVisible = true
    }

    private fun getFieldVerifier(field: JFormattedTextField) =
        object : InputVerifier() {
            override fun verify(input: JComponent?): Boolean {
                if (!field.isEditValid) {
                    createDialog("Wrong input", "${field.name} is wrong!", 1500)
                }
                return true
            }
        }

    private val ipAddressLabel = JLabel("Enter IP address:")
    private val ipAddressField = JFormattedTextField(
        IPAddressFormatterFactory(),
        byteArrayOf(127, 0, 0, 1)
    ).apply {
        name = "IP address field"
        font = font.deriveFont(font.size + 2f)
        inputVerifier = getFieldVerifier(this)
    }

    private val portLabel = JLabel("Enter port:")
    private val portField = JFormattedTextField(
        PortFormatterFactory(),
        8080
    ).apply {
        name = "Port field"
        font = font.deriveFont(font.size + 2f)
        inputVerifier = getFieldVerifier(this)
    }

    private val numberOfPackagesLabel = JLabel("Number of received packages:")
    private val numberOfPackagesField = JFormattedTextField(
        ReceivedPackagesFormatterFactory(),
        0 to 0
    ).apply {
        name = "Number of packages field"
        font = font.deriveFont(font.size + 2f)
        isEditable = false
    }

    private val transferRateLabel = JLabel("Average transfer rate:")
    private val transferRateField = JFormattedTextField(
        TransferRateFormatterFactory(),
        0f
    ).apply {
        name = "Transfer rate field"
        font = font.deriveFont(font.size + 2f)
        isEditable = false
    }

    private val receiveButton = JButton("Receive packages").apply {
        addActionListener {
            startReceiving(
                InetAddress.getByAddress(ipAddressField.value as ByteArray),
                portField.value as Int
            )
        }
    }

    private var receiverThread: Thread? = null

    private fun startReceiving(address: InetAddress, port: Int) {
        receiveButton.isEnabled = false
        numberOfPackagesField.value = 0 to 0
        transferRateField.value = 0f
        receiverThread = thread {
            runBlocking {
                controller.receive(address, port).cancellable().collect {
                    numberOfPackagesField.value = it.first to it.second
                    transferRateField.value = it.third
                }
                receiveButton.isEnabled = true
                receiverThread = null
            }
        }
    }

    init {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                super.windowClosing(e)
                receiverThread?.stopThread()
            }
        })
    }

    private val protocolBox = JComboBox(Protocol.values()).apply {
        addActionListener {
            controller.setProtocol(selectedItem as Protocol)
        }
        controller.setProtocol(selectedItem as Protocol)
    }

    init {
        val groupLayout = GroupLayout(contentPane)
        contentPane.layout = groupLayout
        groupLayout.apply {
            val offset = 25
            autoCreateContainerGaps = true
            autoCreateGaps = true
            setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup()
                    .addGroup(createParallelGroup()
                        .addComponent(ipAddressLabel)
                        .addComponent(portLabel)
                        .addComponent(transferRateLabel)
                        .addComponent(numberOfPackagesLabel)
                    )
                    .addGap(offset)
                    .addGroup(createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(ipAddressField)
                        .addComponent(portField)
                        .addComponent(transferRateField)
                        .addComponent(numberOfPackagesField)
                    )
                )
                .addGroup(
                    GroupLayout.Alignment.CENTER, createSequentialGroup()
                    .addComponent(receiveButton)
                    .addComponent(
                        protocolBox,
                        GroupLayout.PREFERRED_SIZE,
                        GroupLayout.PREFERRED_SIZE,
                        GroupLayout.PREFERRED_SIZE
                    )
                )
            )
            setVerticalGroup(createSequentialGroup()
                .addGap(offset)
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(ipAddressLabel)
                    .addComponent(ipAddressField)
                )
                .addPreferredGap(
                    LayoutStyle.ComponentPlacement.RELATED,
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(portLabel)
                    .addComponent(portField)
                )
                .addPreferredGap(
                    LayoutStyle.ComponentPlacement.RELATED,
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(transferRateLabel)
                    .addComponent(transferRateField)
                )
                .addPreferredGap(
                    LayoutStyle.ComponentPlacement.RELATED,
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(numberOfPackagesLabel)
                    .addComponent(numberOfPackagesField)
                )
                .addGap(offset)
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(receiveButton, GroupLayout.Alignment.TRAILING)
                    .addComponent(protocolBox, GroupLayout.Alignment.LEADING)
                )
                .addGap(offset)
            )
        }
    }
}