package com.testy.plugin

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Key
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class TestyViewerEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        val name = file.name
        return name.endsWith(".testy.yaml") || name.endsWith(".testy.yml")
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val panel = TestyViewerPanel(project, file)
        return object : FileEditor {
            override fun getComponent(): JComponent = panel
            override fun getPreferredFocusedComponent(): JComponent? = null
            override fun getName() = "Testy Viewer"
            override fun dispose() {}
            override fun isModified() = false
            override fun isValid() = true

            override fun getFile(): VirtualFile = file

            override fun <T : Any?> getUserData(key: Key<T>): T? = null
            override fun <T : Any?> putUserData(key: Key<T>, value: T?) {}
            override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
            override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
            override fun setState(state: FileEditorState) {}
        }
    }

    override fun getEditorTypeId(): String = "testy-viewer"
    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
