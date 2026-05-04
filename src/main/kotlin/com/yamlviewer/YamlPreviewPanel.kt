package com.yamlviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class YamlPreviewPanel(
    private val psiFile: PsiFile,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()), Disposable {

    val tree: Tree
    var treeModel: YamlTreeModel
        private set

    private val searchField = SearchTextField()
    private val breadcrumbPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
    private val statusLabel = JBLabel()
    private val filterAlarm: Alarm

    init {
        Disposer.register(parentDisposable, this)
        filterAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

        treeModel = YamlTreeModel(psiFile)
        tree = Tree(treeModel).apply {
            cellRenderer = YamlTreeCellRenderer()
            isRootVisible = false
            showsRootHandles = true
            rowHeight = -1
        }

        expandTopLevel()

        tree.addTreeSelectionListener { _: TreeSelectionEvent ->
            val node = tree.lastSelectedPathComponent as? YamlViewerTreeNode ?: return@addTreeSelectionListener
            updateBreadcrumbs(node)
        }

        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = scheduleFilter()
            override fun removeUpdate(e: DocumentEvent) = scheduleFilter()
            override fun changedUpdate(e: DocumentEvent) = scheduleFilter()
        })

        val topPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(4)
            add(searchField, BorderLayout.NORTH)
            add(breadcrumbPanel, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
        statusLabel.isVisible = false

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)

        // Cmd+F to focus search
        val focusSearchAction = "focusSearch"
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx),
            focusSearchAction
        )
        getActionMap().put(focusSearchAction, object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                searchField.textEditor.requestFocusInWindow()
            }
        })

        // Escape to clear search
        searchField.textEditor.let { editor ->
            editor.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                "clearSearch"
            )
            editor.actionMap.put("clearSearch", object : AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent) {
                    searchField.text = ""
                    tree.requestFocusInWindow()
                }
            })
        }
    }

    fun rebuild() {
        ApplicationManager.getApplication().assertIsDispatchThread()

        val expandedPaths = treeModel.collectExpandedPaths(tree)

        treeModel = YamlTreeModel(psiFile)
        tree.model = treeModel

        treeModel.restoreExpandedPaths(tree, expandedPaths)

        statusLabel.isVisible = false
    }

    fun showInvalidYamlStatus() {
        statusLabel.text = "YAML contains errors — showing last valid state"
        statusLabel.isVisible = true
    }

    fun selectByOffset(offset: Int) {
        val node = treeModel.findByOffset(offset) ?: return
        val path = treeModel.treePath(node)
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
    }

    private fun expandTopLevel() {
        val root = treeModel.root as DefaultMutableTreeNode
        for (i in 0 until root.childCount) {
            val docNode = root.getChildAt(i) as DefaultMutableTreeNode
            tree.expandPath(TreePath(docNode.path))
            for (j in 0 until docNode.childCount) {
                val child = docNode.getChildAt(j)
                tree.expandPath(TreePath((child as DefaultMutableTreeNode).path))
            }
        }
    }

    private fun updateBreadcrumbs(node: YamlViewerTreeNode) {
        breadcrumbPanel.removeAll()
        val parts = node.yamlPath.split(".")
        var accumulated = ""
        for ((index, part) in parts.withIndex()) {
            if (index > 0) {
                breadcrumbPanel.add(JBLabel(" > "))
                accumulated += "."
            }
            accumulated += part
            val pathSoFar = accumulated
            val link = JBLabel(part).apply {
                foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        val target = treeModel.findByYamlPath(pathSoFar)
                        if (target != null) {
                            val tp = treeModel.treePath(target)
                            tree.selectionPath = tp
                            tree.scrollPathToVisible(tp)
                        }
                    }
                })
            }
            breadcrumbPanel.add(link)
        }
        breadcrumbPanel.revalidate()
        breadcrumbPanel.repaint()
    }

    private fun scheduleFilter() {
        filterAlarm.cancelAllRequests()
        filterAlarm.addRequest({ applyFilter() }, 200)
    }

    private fun applyFilter() {
        val query = searchField.text.trim().lowercase()
        if (query.isEmpty()) {
            rebuild()
            return
        }

        // Build a fresh copy to filter (don't mutate the working model)
        val filteredModel = YamlTreeModel(psiFile)
        val root = filteredModel.root as DefaultMutableTreeNode
        filterNode(root, query)

        tree.model = filteredModel
        treeModel = filteredModel
        filteredModel.reload()
        expandAll(root)
    }

    private fun filterNode(node: DefaultMutableTreeNode, query: String): Boolean {
        if (node is YamlViewerTreeNode) {
            val matches = nodeMatchesQuery(node, query)
            var childMatches = false

            for (i in node.childCount - 1 downTo 0) {
                val child = node.getChildAt(i) as DefaultMutableTreeNode
                if (!filterNode(child, query)) {
                    node.remove(i)
                } else {
                    childMatches = true
                }
            }

            return matches || childMatches
        }

        var anyChild = false
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChildAt(i) as DefaultMutableTreeNode
            if (!filterNode(child, query)) {
                node.remove(i)
            } else {
                anyChild = true
            }
        }
        return anyChild
    }

    private fun nodeMatchesQuery(node: YamlViewerTreeNode, query: String): Boolean {
        if (node.key?.lowercase()?.contains(query) == true) return true
        if (node.value?.lowercase()?.contains(query) == true) return true
        for (entry in node.leafEntries) {
            if (entry.key.lowercase().contains(query)) return true
            if (entry.value.lowercase().contains(query)) return true
        }
        return false
    }

    private fun expandAll(node: DefaultMutableTreeNode) {
        tree.expandPath(TreePath(node.path))
        for (i in 0 until node.childCount) {
            expandAll(node.getChildAt(i) as DefaultMutableTreeNode)
        }
    }

    override fun dispose() {}
}
