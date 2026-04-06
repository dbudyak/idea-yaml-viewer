package com.yamlviewer

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class YamlTreeModelBuilderTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testdata"

    fun `test simple yaml produces leaf group`() {
        val psiFile = myFixture.configureByFile("simple.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        // Top-level of a simple all-scalar file is a single leaf group
        assertEquals(1, root.childCount)
        val docNode = root.getChildAt(0) as YamlViewerTreeNode
        assertEquals(YamlNodeType.LEAF_GROUP, docNode.nodeType)
        assertEquals(4, docNode.leafEntries.size)
        assertEquals("name", docNode.leafEntries[0].key)
        assertEquals("my-app", docNode.leafEntries[0].value)
        assertEquals(YamlScalarType.STRING, docNode.leafEntries[0].scalarType)
        assertEquals(YamlScalarType.BOOL, docNode.leafEntries[2].scalarType)
        assertEquals(YamlScalarType.INT, docNode.leafEntries[3].scalarType)
    }

    fun `test nested yaml produces sections`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        val doc = root.getChildAt(0) as YamlViewerTreeNode
        // metadata and spec are top-level sections
        assertEquals(2, doc.childCount)
        val metadata = doc.getChildAt(0) as YamlViewerTreeNode
        assertEquals("metadata", metadata.key)
        assertEquals(YamlNodeType.SECTION, metadata.nodeType)
    }

    fun `test sequence yaml produces sequence nodes`() {
        val psiFile = myFixture.configureByFile("sequence.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        val doc = root.getChildAt(0) as YamlViewerTreeNode
        assertEquals(2, doc.childCount)

        val items = doc.getChildAt(0) as YamlViewerTreeNode
        assertEquals("items", items.key)
        assertEquals(YamlNodeType.SEQUENCE, items.nodeType)
        assertEquals(2, items.childCount)

        val tags = doc.getChildAt(1) as YamlViewerTreeNode
        assertEquals("tags", tags.key)
        assertEquals(YamlNodeType.SEQUENCE, tags.nodeType)
    }

    fun `test yaml paths are built correctly`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        val doc = root.getChildAt(0) as YamlViewerTreeNode
        val metadata = doc.getChildAt(0) as YamlViewerTreeNode
        assertEquals("metadata", metadata.yamlPath)

        val labels = (0 until metadata.childCount)
            .map { metadata.getChildAt(it) as YamlViewerTreeNode }
            .first { it.key == "labels" }
        assertEquals("metadata.labels", labels.yamlPath)
    }
}
