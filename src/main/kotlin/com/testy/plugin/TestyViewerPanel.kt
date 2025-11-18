package com.testy.plugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import com.testy.plugin.ui.MethodBadgeRenderer
import com.testy.plugin.export.TestyMarkdownExporter
import com.testy.plugin.export.ExportDialog
import java.awt.Color
import java.awt.Toolkit
import java.io.File
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import java.util.Timer
import java.util.TimerTask

sealed class TreeNodeData {
    data class Root(val fileName: String, val scenarioCount: Int, val errorCount: Int) : TreeNodeData()
    data class Scenario(val scenario: TestyScenario, val errorCount: Int) : TreeNodeData()
    data class Step(val step: TestyStep) : TreeNodeData()
    data class DbChecksGroup(val checks: List<DbCheck>) : TreeNodeData()
    data class DbCheckItem(val check: DbCheck, val index: Int) : TreeNodeData()
    data class FixturesSummary(val count: Int) : TreeNodeData()
    data class MocksSummary(val serverCount: Int, val routeCount: Int) : TreeNodeData()
    data class ErrorsGroup(val errors: List<ValidationError>) : TreeNodeData()
    data class Error(val error: ValidationError) : TreeNodeData()
}

class TestyViewerPanel(project: Project, file: VirtualFile) : JPanel(), Disposable {
    private val tree = Tree()
    private val root = DefaultMutableTreeNode(TreeNodeData.Root(file.name, 0, 0))
    private val projectRef = project
    private val fileRef = file
    private var refreshTimer: Timer? = null
    private val debounceDelay = 500L // milliseconds - increased for better performance
    private val detailsPanel = TestyDetailsPanel()
    private val progressBar = JProgressBar().apply {
        isStringPainted = true
        isVisible = false
    }
    private var isRefreshing = false // Prevent concurrent refreshes

    init {
        layout = java.awt.BorderLayout()
        
        // Toolbar
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        val refreshButton = JButton(AllIcons.Actions.Refresh)
        refreshButton.toolTipText = "Refresh"
        refreshButton.addActionListener { refreshTree() }
        toolbar.add(refreshButton)
        
        val expandAllButton = JButton(AllIcons.Actions.Expandall)
        expandAllButton.toolTipText = "Expand All"
        expandAllButton.addActionListener { expandAll() }
        toolbar.add(expandAllButton)
        
        val collapseAllButton = JButton(AllIcons.Actions.Collapseall)
        collapseAllButton.toolTipText = "Collapse All"
        collapseAllButton.addActionListener { collapseAll() }
        toolbar.add(collapseAllButton)
        
        toolbar.addSeparator()
        toolbar.add(progressBar)
        
        val exportButton = JButton(AllIcons.Actions.Upload)
        exportButton.text = "Export"
        exportButton.toolTipText = "Export to Markdown"
        exportButton.addActionListener { exportToMarkdown() }
        toolbar.add(exportButton)
        
        add(toolbar, java.awt.BorderLayout.NORTH)
        
        // Split pane with tree and details panel
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(tree), detailsPanel)
        splitPane.dividerLocation = 400
        splitPane.isOneTouchExpandable = true
        add(splitPane, java.awt.BorderLayout.CENTER)
        
        tree.cellRenderer = Renderer()
        tree.toolTipText = null // Enable tooltips
        
        // Selection listener for details panel
        tree.selectionModel.addTreeSelectionListener { e ->
            val selectedPath = e.newLeadSelectionPath ?: return@addTreeSelectionListener
            val selectedNode = selectedPath.lastPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val nodeData = selectedNode.userObject as? TreeNodeData ?: return@addTreeSelectionListener
            
            when (nodeData) {
                is TreeNodeData.Step -> detailsPanel.showStep(nodeData.step)
                is TreeNodeData.DbCheckItem -> detailsPanel.showDbCheck(nodeData.check, nodeData.index)
                is TreeNodeData.Scenario -> detailsPanel.showScenario(nodeData.scenario)
                is TreeNodeData.Error -> detailsPanel.showError(nodeData.error)
                is TreeNodeData.MocksSummary -> {
                    // Try to find the scenario
                    val scenarioNode = findParentScenarioNode(selectedPath)
                    if (scenarioNode != null) {
                        val scenarioData = scenarioNode.userObject as? TreeNodeData.Scenario
                        scenarioData?.let { detailsPanel.showScenario(it.scenario) }
                    }
                }
                else -> detailsPanel.showEmptyState()
            }
        }
        
        // Tooltip support
        object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val tooltip = getToolTipForEvent(e)
                tree.toolTipText = tooltip
            }
        }.let { tree.addMouseMotionListener(it) }
        
        // Double click navigation
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    navigateToSelectedNode()
                }
            }
        })
        
        // Context menu
        val popupMenu = createContextMenu()
        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e, popupMenu)
                }
            }
            
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e, popupMenu)
                }
            }
        })

        // Live updates with debounce
        setupLiveUpdates()

        refreshTree()
    }
    
    private fun setupLiveUpdates() {
        val document = FileDocumentManager.getInstance().getDocument(fileRef) ?: return
        val psiDocumentManager = PsiDocumentManager.getInstance(projectRef)
        
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // Debounce refresh
                refreshTimer?.cancel()
                refreshTimer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            // Ensure PSI is up to date
                            psiDocumentManager.commitDocument(document)
                            // Refresh on EDT
                            SwingUtilities.invokeLater {
                                refreshTree()
                            }
                        }
                    }, debounceDelay)
                }
            }
        }, this)
    }
    
    override fun dispose() {
        refreshTimer?.cancel()
        refreshTimer = null
    }
    
    override fun removeNotify() {
        super.removeNotify()
        dispose()
    }
    
    private fun expandAll() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }
    
    private fun collapseAll() {
        var row = tree.rowCount - 1
        while (row > 0) {
            tree.collapseRow(row)
            row--
        }
    }
    
    private fun findParentStepNode(path: TreePath): DefaultMutableTreeNode? {
        val pathArray = path.path
        for (i in pathArray.size - 1 downTo 0) {
            val node = pathArray[i] as? DefaultMutableTreeNode ?: continue
            val nodeData = node.userObject as? TreeNodeData
            if (nodeData is TreeNodeData.Step) {
                return node
            }
        }
        return null
    }
    
    private fun findParentScenarioNode(path: TreePath): DefaultMutableTreeNode? {
        val pathArray = path.path
        for (i in pathArray.size - 1 downTo 0) {
            val node = pathArray[i] as? DefaultMutableTreeNode ?: continue
            val nodeData = node.userObject as? TreeNodeData
            if (nodeData is TreeNodeData.Scenario) {
                return node
            }
        }
        return null
    }
    
    private fun navigateToSelectedNode() {
        val selectedPath = tree.selectionPath ?: return
        val selectedNode = selectedPath.lastPathComponent as? DefaultMutableTreeNode ?: return
        val nodeData = selectedNode.userObject as? TreeNodeData ?: return
        
        val offset = when (nodeData) {
            is TreeNodeData.Scenario -> nodeData.scenario.offset
            is TreeNodeData.Step -> nodeData.step.offset
            is TreeNodeData.DbCheckItem -> {
                // Try to find the step that contains this dbCheck
                val stepNode = findParentStepNode(selectedPath) ?: return
                (stepNode.userObject as? TreeNodeData.Step)?.step?.offset
            }
            is TreeNodeData.Error -> nodeData.error.offset
            else -> null
        }
        
        offset?.let {
            OpenFileDescriptor(projectRef, fileRef, it).navigate(true)
        }
    }
    
    private fun createContextMenu(): JPopupMenu {
        val menu = JPopupMenu()
        
        val revealItem = JMenuItem("Reveal in Editor", AllIcons.General.Locate)
        revealItem.addActionListener { navigateToSelectedNode() }
        menu.add(revealItem)
        
        menu.addSeparator()
        
        val copyPointerItem = JMenuItem("Copy JSON Pointer", AllIcons.Actions.Copy)
        copyPointerItem.addActionListener {
            val selectedPath = tree.selectionPath ?: return@addActionListener
            val selectedNode = selectedPath.lastPathComponent as? DefaultMutableTreeNode ?: return@addActionListener
            val nodeData = selectedNode.userObject as? TreeNodeData ?: return@addActionListener
            
            val pointer = when (nodeData) {
                is TreeNodeData.Error -> nodeData.error.pointer
                else -> null
            }
            
            pointer?.let {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = java.awt.datatransfer.StringSelection(it)
                clipboard.setContents(selection, null)
            }
        }
        menu.add(copyPointerItem)
        
        menu.addSeparator()
        
        val expandItem = JMenuItem("Expand", AllIcons.Actions.Expandall)
        expandItem.addActionListener {
            val selectedPath = tree.selectionPath ?: return@addActionListener
            tree.expandPath(selectedPath)
        }
        menu.add(expandItem)
        
        val collapseItem = JMenuItem("Collapse", AllIcons.Actions.Collapseall)
        collapseItem.addActionListener {
            val selectedPath = tree.selectionPath ?: return@addActionListener
            tree.collapsePath(selectedPath)
        }
        menu.add(collapseItem)
        
        return menu
    }
    
    private fun showContextMenu(e: MouseEvent, menu: JPopupMenu) {
        val path = tree.getPathForLocation(e.x, e.y) ?: return
        tree.selectionPath = path
        menu.show(tree, e.x, e.y)
    }

    fun refreshTree() {
        // PERFORMANCE FIX: Prevent concurrent refreshes that can stack up
        if (isRefreshing) {
            return
        }
        isRefreshing = true
        
        progressBar.isVisible = true
        progressBar.isIndeterminate = true
        progressBar.string = "Parsing..."
        
        // Save expansion state
        val expandedPaths = mutableSetOf<TreePath>()
        var row = 0
        while (row < tree.rowCount) {
            val path = tree.getPathForRow(row)
            if (tree.isExpanded(path)) {
                expandedPaths.add(path)
            }
            row++
        }
        
        // Save selection
        val selectedPath = tree.selectionPath
        
        root.removeAllChildren()
        
        progressBar.string = "Validating..."
        
        val psiFile = try {
            PsiManager.getInstance(projectRef).findFile(fileRef)
        } catch (e: Exception) {
            root.userObject = TreeNodeData.Root(fileRef.name, 0, 1)
            root.add(DefaultMutableTreeNode(TreeNodeData.Error(
                ValidationError(
                    severity = ValidationSeverity.ERROR,
                    pointer = "",
                    message = "Failed to load file: ${e.message}",
                    offset = null
                )
            )))
            tree.model = DefaultTreeModel(root)
            progressBar.isVisible = false
            isRefreshing = false
            return
        }
        
        val scenarios = try {
            psiFile?.let { TestyYamlParser.parse(it) } ?: emptyList()
        } catch (e: Exception) {
            root.userObject = TreeNodeData.Root(fileRef.name, 0, 1)
            root.add(DefaultMutableTreeNode(TreeNodeData.Error(
                ValidationError(
                    severity = ValidationSeverity.ERROR,
                    pointer = "",
                    message = "Failed to parse YAML: ${e.message}",
                    offset = null
                )
            )))
            tree.model = DefaultTreeModel(root)
            progressBar.isVisible = false
            isRefreshing = false
            return
        }
        
        val yamlContent = psiFile?.text ?: ""
        val validationErrors = try {
            psiFile?.let { TestySchemaValidator.validate(yamlContent, it) } ?: emptyList()
        } catch (e: Exception) {
            listOf(
                ValidationError(
                    severity = ValidationSeverity.ERROR,
                    pointer = "",
                    message = "Validation failed: ${e.message}",
                    offset = null
                )
            )
        }
        
        val errorCount = validationErrors.count { it.severity == ValidationSeverity.ERROR }
        root.userObject = TreeNodeData.Root(fileRef.name, scenarios.size, errorCount)
        
        scenarios.forEachIndexed { index, scenario ->
            val scenarioErrors = validationErrors.filter { 
                it.pointer.startsWith("/$index") 
            }
            val scenarioErrorCount = scenarioErrors.count { it.severity == ValidationSeverity.ERROR }
            
            val scenarioNode = DefaultMutableTreeNode(TreeNodeData.Scenario(scenario, scenarioErrorCount))
            
            // Steps
            scenario.steps.forEach { step ->
                val stepNode = DefaultMutableTreeNode(TreeNodeData.Step(step))
                
                // DbChecks for this step
                step.dbChecks?.let { dbChecks ->
                    if (dbChecks.isNotEmpty()) {
                        val dbChecksNode = DefaultMutableTreeNode(TreeNodeData.DbChecksGroup(dbChecks))
                        dbChecks.forEachIndexed { index, check ->
                            dbChecksNode.add(DefaultMutableTreeNode(TreeNodeData.DbCheckItem(check, index)))
                        }
                        stepNode.add(dbChecksNode)
                    }
                }
                
                scenarioNode.add(stepNode)
            }
            
            // Fixtures summary
            scenario.fixtures?.let { fixtures ->
                if (fixtures.isNotEmpty()) {
                    val fixturesNode = DefaultMutableTreeNode(TreeNodeData.FixturesSummary(fixtures.size))
                    scenarioNode.add(fixturesNode)
                }
            }
            
            // Mocks summary
            scenario.mockServers?.let { mockServers ->
                if (mockServers.isNotEmpty()) {
                    val totalRoutes = mockServers.values.sumOf { it.routes.size }
                    val mocksNode = DefaultMutableTreeNode(
                        TreeNodeData.MocksSummary(mockServers.size, totalRoutes)
                    )
                    scenarioNode.add(mocksNode)
                }
            }
            
            // Errors group
            if (scenarioErrors.isNotEmpty()) {
                val errorsNode = DefaultMutableTreeNode(TreeNodeData.ErrorsGroup(scenarioErrors))
                scenarioErrors.forEach { error ->
                    errorsNode.add(DefaultMutableTreeNode(TreeNodeData.Error(error)))
                }
                scenarioNode.add(errorsNode)
            }
            
            root.add(scenarioNode)
        }

        tree.model = DefaultTreeModel(root)
        
        // Restore expansion state (best effort)
        expandedPaths.forEach { path ->
            // Try to find matching path in new tree
            val pathArray = path.path
            if (pathArray.isNotEmpty()) {
                val rootNode = pathArray[0] as? DefaultMutableTreeNode
                if (rootNode == root) {
                    // Try to restore expansion for known paths
                    tree.expandPath(TreePath(pathArray))
                }
            }
        }
        
        // Restore selection (best effort)
        selectedPath?.let {
            tree.selectionPath = it
        }
        
        // Expand root by default
        tree.expandRow(0)
        
        progressBar.isVisible = false
        progressBar.isIndeterminate = false
        isRefreshing = false // Allow next refresh
    }
    
    private fun exportToMarkdown() {
        val psiFile = PsiManager.getInstance(projectRef).findFile(fileRef) ?: return
        val scenarios = TestyYamlParser.parse(psiFile)
        val yamlContent = psiFile.text
        val errors = TestySchemaValidator.validate(yamlContent, psiFile)
        
        val dialog = ExportDialog(projectRef, fileRef.nameWithoutExtension)
        if (dialog.showAndGet()) {
            val format = dialog.getSelectedFormat()
            val file = dialog.getSelectedFile()
            
            try {
                when (format) {
                    "markdown" -> {
                        val markdown = TestyMarkdownExporter.export(scenarios, fileRef.name, errors)
                        file.writeText(markdown)
                        com.intellij.openapi.ui.Messages.showInfoMessage(
                            projectRef,
                            "Exported successfully to:\n${file.absolutePath}",
                            "Export Complete"
                        )
                    }
                }
            } catch (e: Exception) {
                com.intellij.openapi.ui.Messages.showErrorDialog(
                    projectRef,
                    "Failed to export: ${e.message}",
                    "Export Error"
                )
            }
        }
    }
    
    private fun getToolTipForEvent(event: java.awt.event.MouseEvent): String? {
        val path = tree.getPathForLocation(event.x, event.y) ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        val nodeData = node.userObject as? TreeNodeData ?: return null
        
        return when (nodeData) {
            is TreeNodeData.Step -> {
                val formatter = TestyDataFormatter
                val preview = "Request: ${nodeData.step.request.method} ${nodeData.step.request.path}\n" +
                        "Response: ${nodeData.step.response.status}\n" +
                        if (nodeData.step.request.body != null) {
                            "Body: ${formatter.truncate(nodeData.step.request.body.toString(), 100)}"
                        } else {
                            ""
                        }
                preview.trim()
            }
            is TreeNodeData.DbCheckItem -> {
                "Query: ${TestyDataFormatter.truncate(nodeData.check.query, 150)}"
            }
            is TreeNodeData.Error -> {
                nodeData.error.message
            }
            is TreeNodeData.Scenario -> {
                "Scenario: ${nodeData.scenario.name}\n" +
                        "Steps: ${nodeData.scenario.steps.size}\n" +
                        if (nodeData.errorCount > 0) "Errors: ${nodeData.errorCount}" else ""
            }
            else -> null
        }
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
            val node = (value as? DefaultMutableTreeNode)?.userObject as? TreeNodeData ?: return
            
            when (node) {
                is TreeNodeData.Root -> {
                    icon = AllIcons.FileTypes.Text
                    append(node.fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append(" (${node.scenarioCount} scenarios", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    if (node.errorCount > 0) {
                        append(", $node.errorCount errors", SimpleTextAttributes.ERROR_ATTRIBUTES)
                    }
                    append(")", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                
                is TreeNodeData.Scenario -> {
                    icon = AllIcons.Nodes.TestSourceFolder
                    append(node.scenario.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    if (node.errorCount > 0) {
                        append(" ($node.errorCount errors)", SimpleTextAttributes.ERROR_ATTRIBUTES)
                    }
                }
                
                is TreeNodeData.Step -> {
                    val method = node.step.request.method
                    val methodColor = MethodBadgeRenderer.getMethodColor(method)
                    val status = node.step.response.status
                    val statusColor = getStatusColor(status)
                    
                    // Use badge-like appearance with bold colored text
                    append("$method ", SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, methodColor))
                    append(node.step.request.path, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append(" → ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append("$status", SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, statusColor))
                }
                
                is TreeNodeData.DbChecksGroup -> {
                    icon = AllIcons.Nodes.DataSchema
                    append("dbChecks (${node.checks.size})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                
                is TreeNodeData.DbCheckItem -> {
                    icon = AllIcons.Nodes.DataTables
                    val queryPreview = if (node.check.query.length > 50) {
                        node.check.query.take(50) + "..."
                    } else {
                        node.check.query
                    }
                    append("[${node.index}] ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    append(queryPreview, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
                
                is TreeNodeData.FixturesSummary -> {
                    icon = AllIcons.Nodes.Folder
                    append("fixtures: ${node.count}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                
                is TreeNodeData.MocksSummary -> {
                    icon = AllIcons.Nodes.Folder
                    append("mocks: ${node.serverCount} servers, ${node.routeCount} routes", 
                        SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                
                is TreeNodeData.ErrorsGroup -> {
                    icon = AllIcons.General.Error
                    append("Problems (${node.errors.size})", SimpleTextAttributes.ERROR_ATTRIBUTES)
                }
                
                is TreeNodeData.Error -> {
                    icon = when (node.error.severity) {
                        ValidationSeverity.ERROR -> AllIcons.General.Error
                        ValidationSeverity.WARNING -> AllIcons.General.Warning
                        ValidationSeverity.INFO -> AllIcons.General.Information
                    }
                    val attrs = when (node.error.severity) {
                        ValidationSeverity.ERROR -> SimpleTextAttributes.ERROR_ATTRIBUTES
                        ValidationSeverity.WARNING -> SimpleTextAttributes(
                            SimpleTextAttributes.STYLE_PLAIN, 
                            Color.ORANGE
                        )
                        ValidationSeverity.INFO -> SimpleTextAttributes.REGULAR_ATTRIBUTES
                    }
                    append(node.error.message, attrs)
                }
            }
        }
        
        private fun getStatusColor(status: Int): Color {
            return when {
                status in 200..299 -> Color(0x4CAF50) // Green
                status in 400..499 -> Color(0xFF9800) // Orange
                status in 500..599 -> Color(0xF44336) // Red
                else -> JBColor.GRAY
            }
        }
    }
}
