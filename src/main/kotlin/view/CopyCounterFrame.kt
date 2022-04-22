package view

import Settings
import control.CopyCounterController
import domain.Copy
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.GroupLayout.Alignment
import javax.swing.SwingConstants.CENTER
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class CopyCounterFrame(
    private val controller: CopyCounterController
) : JFrame("Copy Counter ${ProcessHandle.current().pid()}") {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(Settings.windowWidth, Settings.windowHeight)
        setLocationRelativeTo(null)
        isResizable = false
    }

    fun updateCopies(copies: List<Copy>) {
        copiesNumberAvailableText.text = "Alive copies: ${copies.size}"
        copiesListTableModel.setDataVector(
            copies.map { arrayOf("${it.address.hostAddress}:${it.port}", it.pid) }.toTypedArray(),
            arrayOf("Address", "PID")
        )
        copiesListTableModel.fireTableDataChanged()
    }

    private val copiesNumberAvailableText = JLabel().apply {
        horizontalTextPosition = CENTER
        verticalTextPosition = CENTER
        font = Font(null, Font.PLAIN, 16)
    }

    private val waitingTimeText = JLabel().apply {
        text = "Waiting time, ms:"
        font = Font(null, Font.PLAIN, 14)
    }
    private val waitingTimeField = JTextField().apply {
        text = controller.timeout.toString()
        font = Font(null, Font.PLAIN, 14)
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                validate()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                validate()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                validate()
            }

            fun validate() {
                var errorMessage: String? = null
                if (text.isNotBlank()) {
                    text.toIntOrNull()?.let {
                        if (it > Settings.minTimoutValue) {
                            controller.timeout = it
                        } else {
                            errorMessage = "time must be greater than ${Settings.minTimoutValue}"
                        }
                    } ?: run {
                        errorMessage = "text must be number"
                    }
                } else {
                    errorMessage = "text is blank"
                }
                border = errorMessage?.let {
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.red),
                        BorderFactory.createTitledBorder(
                            BorderFactory.createEmptyBorder(),
                            errorMessage,
                            TitledBorder.CENTER,
                            TitledBorder.BOTTOM,
                            Font(null, Font.ITALIC, 8),
                            Color.red
                        )
                    )
                }
            }
        })
    }

    private val copiesListTableModel = DefaultTableModel().apply {
        font = Font(null, Font.PLAIN, 10)
    }
    private val copiesListTable = object : JTable(copiesListTableModel) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }
    private val copiesListScroll = JScrollPane(copiesListTable)

    private val group = GroupLayout(contentPane).apply {
        contentPane.layout = this
        autoCreateContainerGaps = true
        setHorizontalGroup(createParallelGroup()
            .addComponent(copiesNumberAvailableText, Alignment.CENTER)
            .addGroup(createSequentialGroup()
                .addComponent(waitingTimeText)
                .addGap(5)
                .addComponent(waitingTimeField)
            )
            .addComponent(copiesListScroll)
        )
        setVerticalGroup(createSequentialGroup()
            .addComponent(copiesNumberAvailableText)
            .addGap(5)
            .addGroup(createParallelGroup(Alignment.CENTER)
                .addComponent(waitingTimeText)
                .addComponent(waitingTimeField, 30, 30, 30)
            )
            .addGap(5)
            .addComponent(copiesListScroll)
        )
    }

}