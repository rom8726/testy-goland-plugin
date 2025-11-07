package com.testy.plugin.export

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.ui.components.JBTextField
import java.io.File
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel

class ExportDialog(project: Project?, private val defaultFileName: String) : DialogWrapper(project) {
    private val formatComboBox = JComboBox<String>(arrayOf("Markdown (.md)"))
    private val fileNameField = JBTextField("$defaultFileName.md")
    private var selectedDirectory: File? = null
    private val projectRef = project
    private val directoryLabel = JBLabel("Not selected")
    
    init {
        title = "Export Testy Scenarios"
        init()
    }
    
    override fun createCenterPanel(): JPanel {
        val panel = JPanel()
        panel.layout = java.awt.GridBagLayout()
        val gbc = java.awt.GridBagConstraints()
        gbc.insets = java.awt.Insets(5, 5, 5, 5)
        
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = java.awt.GridBagConstraints.WEST
        panel.add(JBLabel("Format:"), gbc)
        
        gbc.gridx = 1
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(formatComboBox, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = java.awt.GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("Directory:"), gbc)
        
        gbc.gridx = 1
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(directoryLabel, gbc)
        
        gbc.gridx = 2
        gbc.fill = java.awt.GridBagConstraints.NONE
        gbc.weightx = 0.0
        val chooseDirButton = javax.swing.JButton("Choose Directory...")
        chooseDirButton.addActionListener {
            val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
            descriptor.title = "Select Export Directory"
            descriptor.description = "Choose directory where to save the exported file"
            
            val baseDir = projectRef?.let { 
                val roots = ProjectRootManager.getInstance(it).contentRoots
                if (roots.isNotEmpty()) roots[0] else null
            }
            val file = FileChooser.chooseFile(descriptor, projectRef, baseDir)
            if (file != null) {
                selectedDirectory = File(file.path)
                directoryLabel.text = file.path
            }
        }
        panel.add(chooseDirButton, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.fill = java.awt.GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("File name:"), gbc)
        
        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(fileNameField, gbc)
        
        return panel
    }
    
    fun getSelectedFormat(): String {
        return when (formatComboBox.selectedItem as String) {
            "Markdown (.md)" -> "markdown"
            else -> "markdown"
        }
    }
    
    fun getSelectedFile(): File {
        val directory = selectedDirectory ?: run {
            // Default to project directory or home directory
            val basePath = projectRef?.let {
                val roots = ProjectRootManager.getInstance(it).contentRoots
                if (roots.isNotEmpty()) roots[0].path else null
            }
            if (basePath != null) {
                File(basePath)
            } else {
                File(System.getProperty("user.home"))
            }
        }
        
        val fileName = fileNameField.text.trim()
        val finalFileName = if (fileName.isEmpty()) {
            "$defaultFileName.md"
        } else if (!fileName.endsWith(".md")) {
            "$fileName.md"
        } else {
            fileName
        }
        
        return File(directory, finalFileName)
    }
}

