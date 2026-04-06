package com.yamlviewer

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EditorPreviewSyncTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testdata"

    fun `test editor caret change selects tree node`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val textEditor = TextEditorProvider.getInstance().createEditor(project, psiFile.virtualFile) as TextEditor
        Disposer.register(testRootDisposable, textEditor)
        val panel = YamlPreviewPanel(psiFile, testRootDisposable)
        val sync = EditorPreviewSync(textEditor.editor, panel)

        val text = psiFile.text
        val offset = text.indexOf("namespace")
        textEditor.editor.caretModel.moveToOffset(offset)

        sync.syncEditorToTree()

        val selected = panel.tree.lastSelectedPathComponent as? YamlViewerTreeNode
        assertNotNull("A node should be selected", selected)
        assertTrue(
            "Selected node path should contain metadata",
            selected!!.yamlPath.startsWith("metadata")
        )
    }

    fun `test tree selection moves editor caret`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val textEditor = TextEditorProvider.getInstance().createEditor(project, psiFile.virtualFile) as TextEditor
        Disposer.register(testRootDisposable, textEditor)
        val panel = YamlPreviewPanel(psiFile, testRootDisposable)
        val sync = EditorPreviewSync(textEditor.editor, panel)

        val specNode = panel.treeModel.findByYamlPath("spec")
        assertNotNull("spec node should exist", specNode)

        panel.tree.selectionPath = panel.treeModel.treePath(specNode!!)
        sync.syncTreeToEditor()

        val caretOffset = textEditor.editor.caretModel.offset
        val text = psiFile.text
        val specOffset = text.indexOf("spec:")
        assertTrue(
            "Caret should be near spec: (at $caretOffset, spec at $specOffset)",
            caretOffset in specOffset..(specOffset + 10)
        )
    }
}
