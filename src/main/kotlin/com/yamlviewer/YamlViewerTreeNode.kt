package com.yamlviewer

import com.intellij.openapi.util.TextRange
import javax.swing.tree.DefaultMutableTreeNode

class YamlViewerTreeNode(
    val nodeType: YamlNodeType,
    val key: String?,
    val value: String?,
    val scalarType: YamlScalarType?,
    val psiRange: TextRange,
    val yamlPath: String,
    val nodeChildCount: Int = 0,
) : DefaultMutableTreeNode() {

    /** For leaf group nodes: the scalar entries as key/value/type triples */
    val leafEntries: List<LeafEntry> = mutableListOf()

    data class LeafEntry(
        val key: String,
        val value: String,
        val scalarType: YamlScalarType,
        val psiRange: TextRange,
    )

    fun addLeafEntry(entry: LeafEntry) {
        (leafEntries as MutableList).add(entry)
    }

    override fun toString(): String = key ?: "[${parent?.let { (it as YamlViewerTreeNode).getIndex(this) } ?: 0}]"
}
