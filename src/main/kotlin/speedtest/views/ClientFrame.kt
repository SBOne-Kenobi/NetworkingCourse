package speedtest.views

import speedtest.Settings
import speedtest.controls.ClientController
import speedtest.controls.Protocol
import java.net.InetAddress
import javax.swing.*
import kotlin.concurrent.schedule

class ClientFrame(
    private val controller: ClientController
) : JFrame("Speed test client") {

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

    private val ipAddressLabel = JLabel("Enter receiver's IP address:")
    private val ipAddressField = JFormattedTextField(
        IPAddressFormatterFactory(),
        byteArrayOf(127, 0, 0, 1)
    ).apply {
        name = "IP address field"
        font = font.deriveFont(font.size + 2f)
        inputVerifier = getFieldVerifier(this)
    }

    private val portLabel = JLabel("Enter receiver's port:")
    private val portField = JFormattedTextField(
        PortFormatterFactory(),
        8080
    ).apply {
        name = "Port field"
        font = font.deriveFont(font.size + 2f)
        inputVerifier = getFieldVerifier(this)
    }

    private val numberOfPackagesLabel = JLabel("Enter number of packages for sending:")
    private val numberOfPackagesField = JFormattedTextField(
        IntWithLimitsFormatterFactory(1), 5
    ).apply {
        name = "Number of packages field"
        font = font.deriveFont(font.size + 2f)
        inputVerifier = getFieldVerifier(this)
    }

    private val sendButton = JButton("Send packages").apply {
        addActionListener {
            controller.send(
                InetAddress.getByAddress(ipAddressField.value as ByteArray),
                portField.value as Int,
                numberOfPackagesField.value as Int
            )
            sendingStarted()
        }
    }

    private fun sendingStarted() {
        sendButton.isEnabled = false
    }

    fun sendingFinished() {
        sendButton.isEnabled = true
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
                        .addComponent(numberOfPackagesLabel)
                    )
                    .addGap(offset)
                    .addGroup(createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(ipAddressField)
                        .addComponent(portField)
                        .addComponent(numberOfPackagesField)
                    )
                )
                .addGroup(GroupLayout.Alignment.CENTER, createSequentialGroup()
                    .addComponent(sendButton)
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
                    .addComponent(numberOfPackagesLabel)
                    .addComponent(numberOfPackagesField)
                )
                .addGap(offset)
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(sendButton, GroupLayout.Alignment.TRAILING)
                    .addComponent(protocolBox, GroupLayout.Alignment.LEADING)
                )
                .addGap(offset)
            )
        }
    }

}