package com.testy.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class TestyViewerPanel(project: Project, file: VirtualFile) : JPanel() {
    private val tree = Tree()
    private val root = DefaultMutableTreeNode(file.name)

    init {
        layout = java.awt.BorderLayout()
        add(JScrollPane(tree), java.awt.BorderLayout.CENTER)
        tree.cellRenderer = Renderer()

        val psiFile = PsiManager.getInstance(project).findFile(file)
        val scenarios = psiFile?.let { TestyYamlParser.parse(it) } ?: emptyList()

        scenarios.forEach { scenario ->
            root.add(DefaultMutableTreeNode("${scenario.name}${scenario.description?.let { " — $it" } ?: ""}"))
        }

        tree.model = DefaultTreeModel(root)
        tree.expandRow(0)
    }

    private class Renderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: javax.swing.JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val text = (value as? DefaultMutableTreeNode)?.userObject?.toString() ?: ""
            append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
    }
}
