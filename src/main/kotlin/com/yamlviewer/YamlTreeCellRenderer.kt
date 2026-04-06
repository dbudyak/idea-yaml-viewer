package com.yamlviewer

import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.tree.TreeCellRenderer

class YamlTreeCellRenderer : TreeCellRenderer {

    companion object {
        private val KEY_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.namedColor("YAML.key", JBColor(0x0033B3, 0x6897BB)))
        private val VALUE_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.namedColor("YAML.value", JBColor(0x067D17, 0x6A8759)))
        private val BOLD_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, null)
        private val GRAY_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY)

        private val BADGE_COLORS = mapOf(
            YamlScalarType.STRING to JBColor(0x0033B3, 0x6897BB),
            YamlScalarType.INT to JBColor(0x7B1FA2, 0xB39DDB),
            YamlScalarType.FLOAT to JBColor(0x7B1FA2, 0xB39DDB),
            YamlScalarType.BOOL to JBColor(0x067D17, 0x6A8759),
            YamlScalarType.NULL to JBColor(0x808080, 0x808080),
        )
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): Component {
        val node = value as? YamlViewerTreeNode
            ?: return defaultLabel(value.toString())

        return when (node.nodeType) {
            YamlNodeType.SECTION -> renderSection(node, expanded)
            YamlNodeType.LEAF_GROUP -> renderLeafGroup(node)
            YamlNodeType.SEQUENCE -> renderSequence(node, expanded)
            YamlNodeType.SCALAR -> renderScalar(node)
        }
    }

    private fun renderSection(node: YamlViewerTreeNode, expanded: Boolean): Component {
        val component = SimpleColoredComponent()
        component.append(node.key ?: "document", BOLD_ATTRIBUTES)
        if (!expanded) {
            component.append("  ${node.nodeChildCount} keys", GRAY_ATTRIBUTES)
        }
        component.ipad = JBUI.insetsLeft(2)
        return component
    }

    private fun renderLeafGroup(node: YamlViewerTreeNode): Component {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(2, 0)
        }

        if (node.key != null) {
            val header = SimpleColoredComponent()
            header.append(node.key, BOLD_ATTRIBUTES)
            header.ipad = JBUI.insetsLeft(2)
            header.alignmentX = Component.LEFT_ALIGNMENT
            panel.add(header)
        }

        for (entry in node.leafEntries) {
            val row = JPanel(FlowLayout(FlowLayout.LEFT, 4, 1)).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
            }

            val keyLabel = SimpleColoredComponent()
            keyLabel.append(entry.key, KEY_ATTRIBUTES)
            row.add(keyLabel)

            val valueLabel = SimpleColoredComponent()
            val displayValue = if (entry.value.length > 60) entry.value.take(60) + "..." else entry.value
            valueLabel.append(displayValue, VALUE_ATTRIBUTES)
            row.add(valueLabel)

            row.add(createBadge(entry.scalarType))

            panel.add(row)
        }

        return panel
    }

    private fun renderSequence(node: YamlViewerTreeNode, expanded: Boolean): Component {
        val component = SimpleColoredComponent()
        component.append(node.key ?: "list", BOLD_ATTRIBUTES)
        if (!expanded) {
            component.append("  ${node.nodeChildCount} items", GRAY_ATTRIBUTES)
        }
        component.ipad = JBUI.insetsLeft(2)
        return component
    }

    private fun renderScalar(node: YamlViewerTreeNode): Component {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = false
        }

        if (node.key != null) {
            val keyLabel = SimpleColoredComponent()
            keyLabel.append(node.key, KEY_ATTRIBUTES)
            panel.add(keyLabel)
        }

        val valueLabel = SimpleColoredComponent()
        val displayValue = node.value ?: ""
        valueLabel.append(if (displayValue.length > 60) displayValue.take(60) + "..." else displayValue, VALUE_ATTRIBUTES)
        panel.add(valueLabel)

        if (node.scalarType != null) {
            panel.add(createBadge(node.scalarType))
        }

        return panel
    }

    private fun createBadge(type: YamlScalarType): JLabel {
        val color = BADGE_COLORS[type] ?: JBColor.GRAY
        return object : JLabel(type.label) {
            init {
                font = font.deriveFont(font.size2D - 2f)
                foreground = JBColor.WHITE
                border = JBUI.Borders.empty(1, 4)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = color
                g2.fillRoundRect(0, 0, width, height, 8, 8)
                g2.dispose()
                super.paintComponent(g)
            }
        }
    }

    private fun defaultLabel(text: String): JLabel = JLabel(text)
}
