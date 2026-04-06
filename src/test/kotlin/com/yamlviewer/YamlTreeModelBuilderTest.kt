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

    fun `test empty yaml produces empty root`() {
        val psiFile = myFixture.configureByText("empty.yaml", "")
        val root = YamlTreeModelBuilder.build(psiFile)
        assertEquals(0, root.childCount)
    }

    fun `test yaml with only comments produces empty root`() {
        val psiFile = myFixture.configureByText("comments.yaml", "# just a comment\n# another comment")
        val root = YamlTreeModelBuilder.build(psiFile)
        assertTrue(root.childCount == 0 || (root.getChildAt(0) as? javax.swing.tree.DefaultMutableTreeNode)?.childCount == 0)
    }

    fun `test multi-document yaml`() {
        val content = "name: doc1\n---\nname: doc2"
        val psiFile = myFixture.configureByText("multi.yaml", content)
        val root = YamlTreeModelBuilder.build(psiFile)
        assertEquals("Should have 2 document nodes", 2, root.childCount)
    }

    fun `test deeply nested yaml builds correct paths`() {
        val psiFile = myFixture.configureByFile("mixed.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        val doc = root.getChildAt(0) as YamlViewerTreeNode
        val spec = findChild(doc, "spec")
        assertNotNull(spec)

        val template = findChild(spec!!, "template")
        assertNotNull(template)

        val innerSpec = findChild(template!!, "spec")
        assertNotNull(innerSpec)

        val containers = findChild(innerSpec!!, "containers")
        assertNotNull(containers)
        assertEquals(YamlNodeType.SEQUENCE, containers!!.nodeType)
    }

    fun `test sequence item paths use bracket indices`() {
        val psiFile = myFixture.configureByFile("sequence.yaml")
        val root = YamlTreeModelBuilder.build(psiFile)

        val doc = root.getChildAt(0) as YamlViewerTreeNode
        val items = findChild(doc, "items")
        assertNotNull(items)

        val firstItem = items!!.getChildAt(0) as YamlViewerTreeNode
        assertEquals("items[0]", firstItem.yamlPath)

        val secondItem = items.getChildAt(1) as YamlViewerTreeNode
        assertEquals("items[1]", secondItem.yamlPath)
    }

    private fun findChild(parent: YamlViewerTreeNode, key: String): YamlViewerTreeNode? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) as? YamlViewerTreeNode
            if (child?.key == key) return child
        }
        return null
    }
}
