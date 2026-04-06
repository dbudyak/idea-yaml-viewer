package com.yamlviewer

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class YamlViewerEditorProviderTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testdata"

    fun `test accepts yaml files`() {
        val provider = YamlViewerEditorProvider()
        val yamlFile = myFixture.configureByFile("simple.yaml")
        assertTrue(provider.accept(project, yamlFile.virtualFile))
    }

    fun `test rejects non-yaml files`() {
        val provider = YamlViewerEditorProvider()
        val txtFile = myFixture.configureByText("test.txt", "hello")
        assertFalse(provider.accept(project, txtFile.virtualFile))
    }

    fun `test creates editor for yaml file`() {
        val provider = YamlViewerEditorProvider()
        val yamlFile = myFixture.configureByFile("simple.yaml")
        val editor = provider.createEditor(project, yamlFile.virtualFile)
        Disposer.register(testRootDisposable, editor)
        assertNotNull(editor)
        assertEquals("yaml-viewer", provider.editorTypeId)
    }
}
