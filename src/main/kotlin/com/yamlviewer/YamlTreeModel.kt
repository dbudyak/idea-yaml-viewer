package com.yamlviewer

import com.intellij.psi.PsiFile
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class YamlTreeModel(psiFile: PsiFile) : DefaultTreeModel(YamlTreeModelBuilder.build(psiFile)) {

    fun findByYamlPath(path: String): YamlViewerTreeNode? {
        return findByYamlPath(root as DefaultMutableTreeNode, path)
    }

    private fun findByYamlPath(node: DefaultMutableTreeNode, path: String): YamlViewerTreeNode? {
        if (node is YamlViewerTreeNode && node.yamlPath == path) return node
        for (i in 0 until node.childCount) {
            val result = findByYamlPath(node.getChildAt(i) as DefaultMutableTreeNode, path)
            if (result != null) return result
        }
        return null
    }

    fun findByOffset(offset: Int): YamlViewerTreeNode? {
        return findByOffset(root as DefaultMutableTreeNode, offset)
    }

    private fun findByOffset(node: DefaultMutableTreeNode, offset: Int): YamlViewerTreeNode? {
        if (node is YamlViewerTreeNode) {
            if (node.nodeType == YamlNodeType.LEAF_GROUP) {
                for (entry in node.leafEntries) {
                    if (entry.psiRange.contains(offset)) return node
                }
            }
            if (node.psiRange.contains(offset)) {
                for (i in 0 until node.childCount) {
                    val result = findByOffset(node.getChildAt(i) as DefaultMutableTreeNode, offset)
                    if (result != null) return result
                }
                return node
            }
        } else {
            for (i in 0 until node.childCount) {
                val result = findByOffset(node.getChildAt(i) as DefaultMutableTreeNode, offset)
                if (result != null) return result
            }
        }
        return null
    }

    fun treePath(node: YamlViewerTreeNode): TreePath {
        return TreePath(node.path)
    }

    fun collectExpandedPaths(tree: javax.swing.JTree): Set<String> {
        val expanded = mutableSetOf<String>()
        collectExpanded(tree, root as DefaultMutableTreeNode, expanded)
        return expanded
    }

    private fun collectExpanded(tree: javax.swing.JTree, node: DefaultMutableTreeNode, result: MutableSet<String>) {
        if (node is YamlViewerTreeNode && tree.isExpanded(TreePath(node.path))) {
            result.add(node.yamlPath)
        }
        for (i in 0 until node.childCount) {
            collectExpanded(tree, node.getChildAt(i) as DefaultMutableTreeNode, result)
        }
    }

    fun restoreExpandedPaths(tree: javax.swing.JTree, paths: Set<String>) {
        restoreExpanded(tree, root as DefaultMutableTreeNode, paths)
    }

    private fun restoreExpanded(tree: javax.swing.JTree, node: DefaultMutableTreeNode, paths: Set<String>) {
        if (node is YamlViewerTreeNode && node.yamlPath in paths) {
            tree.expandPath(TreePath(node.path))
        }
        for (i in 0 until node.childCount) {
            restoreExpanded(tree, node.getChildAt(i) as DefaultMutableTreeNode, paths)
        }
    }
}
