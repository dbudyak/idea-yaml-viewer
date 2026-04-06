package com.yamlviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Alarm

class YamlViewerEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        val ext = file.extension?.lowercase()
        return ext == "yaml" || ext == "yml"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val psiFile = PsiManager.getInstance(project).findFile(file)
            ?: return textEditor

        val previewPanel = YamlPreviewPanel(psiFile, textEditor)
        val sync = EditorPreviewSync(textEditor.editor, previewPanel)

        val rebuildAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, textEditor)
        textEditor.editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                rebuildAlarm.cancelAllRequests()
                rebuildAlarm.addRequest({
                    val currentPsi = PsiManager.getInstance(project).findFile(file) ?: return@addRequest
                    val hasErrors = PsiTreeUtil.hasErrorElements(currentPsi)
                    if (hasErrors) {
                        previewPanel.showInvalidYamlStatus()
                    } else {
                        previewPanel.rebuild()
                    }
                }, 300)
            }
        }, textEditor)

        Disposer.register(textEditor, Disposable { sync.dispose() })

        return object : TextEditorWithPreview(textEditor, FileEditorWrapper(previewPanel), "YAML Viewer") {
            override fun getName(): String = "YAML Viewer"
        }
    }

    override fun getEditorTypeId(): String = "yaml-viewer"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}

private class FileEditorWrapper(private val component: YamlPreviewPanel) : UserDataHolderBase(), FileEditor {
    override fun getComponent(): javax.swing.JComponent = component
    override fun getPreferredFocusedComponent(): javax.swing.JComponent = component.tree
    override fun getName(): String = "YAML Preview"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    override fun dispose() {}
    override fun getFile(): VirtualFile? = null
}
