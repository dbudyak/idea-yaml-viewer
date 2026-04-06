package com.yamlviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class YamlViewerEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        val ext = file.extension?.lowercase()
        return ext == "yaml" || ext == "yml"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        TODO("Implemented in Task 9")
    }

    override fun getEditorTypeId(): String = "yaml-viewer"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
