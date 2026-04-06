package com.yamlviewer

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.*
import javax.swing.tree.DefaultMutableTreeNode

object YamlTreeModelBuilder {

    fun build(psiFile: PsiFile): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode("root")
        val yamlFile = psiFile as? YAMLFile ?: return root

        for (document in yamlFile.documents) {
            val topValue = document.topLevelValue ?: continue
            val docNode = buildNode(topValue, null, "")
            if (docNode != null) {
                root.add(docNode)
            }
        }
        return root
    }

    private fun buildNode(element: YAMLValue, key: String?, parentPath: String): YamlViewerTreeNode? {
        val currentPath = when {
            key != null && parentPath.isEmpty() -> key
            key != null -> "$parentPath.$key"
            else -> parentPath
        }

        return when (element) {
            is YAMLMapping -> buildMappingNode(element, key, currentPath)
            is YAMLSequence -> buildSequenceNode(element, key, currentPath)
            is YAMLScalar -> buildScalarNode(element, key, currentPath)
            else -> null
        }
    }

    private fun buildMappingNode(mapping: YAMLMapping, key: String?, path: String): YamlViewerTreeNode {
        val keyValues = mapping.keyValues.toList()
        val allScalar = keyValues.all { it.value is YAMLScalar || it.value == null }

        if (allScalar) {
            val node = YamlViewerTreeNode(
                nodeType = YamlNodeType.LEAF_GROUP,
                key = key,
                value = null,
                scalarType = null,
                psiRange = mapping.textRange,
                yamlPath = path,
                nodeChildCount = keyValues.size,
            )
            for (kv in keyValues) {
                val text = (kv.value as? YAMLScalar)?.textValue ?: ""
                node.addLeafEntry(
                    YamlViewerTreeNode.LeafEntry(
                        key = kv.keyText,
                        value = text,
                        scalarType = YamlScalarType.detect(text),
                        psiRange = kv.textRange,
                    )
                )
            }
            return node
        }

        val node = YamlViewerTreeNode(
            nodeType = YamlNodeType.SECTION,
            key = key,
            value = null,
            scalarType = null,
            psiRange = mapping.textRange,
            yamlPath = path,
            nodeChildCount = keyValues.size,
        )
        for (kv in keyValues) {
            val childValue = kv.value
            if (childValue != null) {
                val child = buildNode(childValue, kv.keyText, path)
                if (child != null) {
                    node.add(child)
                }
            } else {
                val child = YamlViewerTreeNode(
                    nodeType = YamlNodeType.SCALAR,
                    key = kv.keyText,
                    value = "",
                    scalarType = YamlScalarType.NULL,
                    psiRange = kv.textRange,
                    yamlPath = if (path.isEmpty()) kv.keyText else "$path.${kv.keyText}",
                )
                node.add(child)
            }
        }
        return node
    }

    private fun buildSequenceNode(sequence: YAMLSequence, key: String?, path: String): YamlViewerTreeNode {
        val items = sequence.items
        val node = YamlViewerTreeNode(
            nodeType = YamlNodeType.SEQUENCE,
            key = key,
            value = null,
            scalarType = null,
            psiRange = sequence.textRange,
            yamlPath = path,
            nodeChildCount = items.size,
        )
        for ((index, item) in items.withIndex()) {
            val itemValue = item.value
            val itemPath = "$path[$index]"
            if (itemValue != null) {
                val child = buildNode(itemValue, null, itemPath)
                if (child != null) {
                    node.add(child)
                }
            }
        }
        return node
    }

    private fun buildScalarNode(scalar: YAMLScalar, key: String?, path: String): YamlViewerTreeNode {
        val text = scalar.textValue
        return YamlViewerTreeNode(
            nodeType = YamlNodeType.SCALAR,
            key = key,
            value = text,
            scalarType = YamlScalarType.detect(text),
            psiRange = scalar.textRange,
            yamlPath = path,
        )
    }
}
