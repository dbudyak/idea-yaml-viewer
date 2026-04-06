package com.yamlviewer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import javax.swing.event.TreeSelectionListener

class EditorPreviewSync(
    private val editor: Editor,
    private val previewPanel: YamlPreviewPanel,
) {
    private var isSyncing = false

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            if (isSyncing) return
            syncEditorToTree()
        }
    }

    private val treeSelectionListener = TreeSelectionListener {
        if (!isSyncing) {
            syncTreeToEditor()
        }
    }

    init {
        editor.caretModel.addCaretListener(caretListener)
        previewPanel.tree.addTreeSelectionListener(treeSelectionListener)
    }

    fun syncEditorToTree() {
        if (isSyncing) return
        isSyncing = true
        try {
            val offset = editor.caretModel.offset
            previewPanel.selectByOffset(offset)
        } finally {
            isSyncing = false
        }
    }

    fun syncTreeToEditor() {
        if (isSyncing) return
        isSyncing = true
        try {
            val node = previewPanel.tree.lastSelectedPathComponent as? YamlViewerTreeNode ?: return
            val targetOffset = node.psiRange.startOffset
            editor.caretModel.moveToOffset(targetOffset)
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        } finally {
            isSyncing = false
        }
    }

    fun dispose() {
        editor.caretModel.removeCaretListener(caretListener)
    }
}
