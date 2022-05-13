package translator.view

import translator.ConfigEntry
import translator.Settings
import translator.control.TranslatorController
import java.net.InetAddress
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.schedule

class TranslatorFrame(
    private val controller: TranslatorController
) : JFrame("Port translator") {

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
    ): JDialog {
        val pane = JOptionPane(message, messageType)
        val dialog = pane.createDialog(title)
        lifetime?.let {
            timer.schedule(it) {
                dialog.dispose()
            }
        }
        dialog.isVisible = true
        return dialog
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

    private val nameLabel = JLabel("Name:")
    private val nameField = JTextField("Name").apply {
        font = font.deriveFont(font.size + 2)
    }

    private val internalIpLabel = JLabel("Internal IP:")
    private val internalIpField = JFormattedTextField(
        IPAddressFormatterFactory(),
        byteArrayOf(127, 0, 0, 1)
    ).apply {
        font = font.deriveFont(font.size + 2)
        inputVerifier = getFieldVerifier(this)
    }

    private val internalPortLabel = JLabel("Internal port:")
    private val internalPortField = JFormattedTextField(
        PortFormatterFactory(),
        8080
    ).apply {
        font = font.deriveFont(font.size + 2)
        inputVerifier = getFieldVerifier(this)
    }

    private val externalIpLabel = JLabel("External IP:")
    private val externalIpField = JFormattedTextField(
        IPAddressFormatterFactory(),
        byteArrayOf(213.toByte(), 180.toByte(), 193.toByte(), 234.toByte())
    ).apply {
        font = font.deriveFont(font.size + 2)
        inputVerifier = getFieldVerifier(this)
    }

    private val externalPortLabel = JLabel("External port:")
    private val externalPortField = JFormattedTextField(
        PortFormatterFactory(),
        80
    ).apply {
        font = font.deriveFont(font.size + 2)
        inputVerifier = getFieldVerifier(this)
    }

    private val useSSLLabel = JLabel("Use SSL:")
    private val useSSLBox = JComboBox(arrayOf(false, true)).apply {
        font = font.deriveFont(font.size + 2)
    }

    private val addButton = JButton("Add").apply {
        addActionListener {
            try {
                controller.updateConfig(ConfigEntry(
                    nameField.text,
                    InetAddress.getByAddress(internalIpField.value as ByteArray),
                    internalPortField.value as Int,
                    InetAddress.getByAddress(externalIpField.value as ByteArray),
                    externalPortField.value as Int,
                    useSSLBox.selectedItem as Boolean
                ))
                updateConfig()
            } catch (e: Exception) {
                createDialog("Error", e.message!!)
            }
        }
    }

    private fun updateConfig() {
        configTableModel.setDataVector(
            controller.config.entries.map {
                arrayOf(
                    it.name,
                    it.internal_ip.hostAddress, it.internal_port,
                    it.external_ip.hostAddress, it.external_port,
                    it.useSSL
                )
            }.toTypedArray(),
            arrayOf(
                "Name",
                "Internal IP", "Internal port",
                "External IP", "External port",
                "use SSL"
            )
        )
        configTableModel.fireTableDataChanged()
    }

    private val configTableModel = DefaultTableModel()
    private val configTable = object : JTable(configTableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }.apply {
        tableHeader.reorderingAllowed = false
        columnSelectionAllowed = false
        rowSelectionAllowed = false
    }

    private val configScroll = JScrollPane(configTable)

    init {
        val groupLayout = GroupLayout(contentPane)
        contentPane.layout = groupLayout

        updateConfig()

        groupLayout.apply {
            val offset = 25
            autoCreateGaps = true
            autoCreateContainerGaps = true
            setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup()
                    .addGroup(createParallelGroup()
                        .addComponent(nameLabel)
                        .addComponent(internalIpLabel)
                        .addComponent(internalPortLabel)
                        .addComponent(externalIpLabel)
                        .addComponent(externalPortLabel)
                        .addComponent(useSSLLabel)
                    )
                    .addGap(offset)
                    .addGroup(createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(nameField)
                        .addComponent(internalIpField)
                        .addComponent(internalPortField)
                        .addComponent(externalIpField)
                        .addComponent(externalPortField)
                        .addGroup(createSequentialGroup()
                            .addComponent(useSSLBox)
                            .addComponent(addButton)
                        )
                    )
                )
                .addComponent(configScroll)
            )
            setVerticalGroup(createSequentialGroup()
                .addGap(offset)
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(nameLabel)
                    .addComponent(nameField)
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(internalIpLabel)
                    .addComponent(internalIpField)
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(internalPortLabel)
                    .addComponent(internalPortField)
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(externalIpLabel)
                    .addComponent(externalIpField)
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(externalPortLabel)
                    .addComponent(externalPortField)
                )
                .addGroup(createParallelGroup(GroupLayout.Alignment.CENTER, false)
                    .addComponent(useSSLLabel)
                    .addComponent(useSSLBox)
                    .addComponent(addButton)
                )
                .addComponent(configScroll)
            )
        }
    }

}