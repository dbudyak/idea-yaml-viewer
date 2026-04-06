# YAML Viewer Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an IntelliJ IDEA plugin that renders YAML files as a navigable, human-readable tree/table hybrid view in a split editor alongside the raw text.

**Architecture:** `TextEditorWithPreview` split editor with a custom Swing `JTree` viewer panel. The viewer is built from IntelliJ's YAML PSI tree, with bidirectional sync between editor caret and tree selection. No external dependencies — everything uses IntelliJ Platform SDK and the bundled YAML plugin.

**Tech Stack:** Kotlin, Gradle (IntelliJ Platform Gradle Plugin 2.x), IntelliJ Platform SDK 2024.1+, bundled YAML PSI plugin, JUnit 4 + `BasePlatformTestCase` for integration tests.

**Spec:** `docs/superpowers/specs/2026-04-06-yaml-viewer-plugin-design.md`

---

## File Structure

```
build.gradle.kts                              — Gradle build with intellij-platform plugin
settings.gradle.kts                           — Project settings
gradle.properties                             — IntelliJ platform version config
src/main/kotlin/com/yamlviewer/
  YamlScalarType.kt                           — Enum + scalar type detection from string values
  YamlNodeType.kt                             — Enum: Section, LeafGroup, Sequence, Scalar
  YamlViewerTreeNode.kt                       — Tree node data class (holds type, key, value, PSI range, children)
  YamlTreeModelBuilder.kt                     — Builds tree of YamlViewerTreeNode from YAML PSI
  YamlTreeModel.kt                            — DefaultTreeModel wrapper for JTree
  YamlTreeCellRenderer.kt                     — Custom TreeCellRenderer: section headers, leaf tables, type badges
  YamlPreviewPanel.kt                         — JPanel containing search bar, breadcrumbs, JTree
  EditorPreviewSync.kt                        — Bidirectional sync between editor caret and tree selection
  YamlViewerEditorProvider.kt                 — FileEditorProvider, creates TextEditorWithPreview
src/main/resources/
  META-INF/plugin.xml                         — Plugin descriptor
src/test/kotlin/com/yamlviewer/
  YamlScalarTypeTest.kt                       — Unit tests for scalar type detection
  YamlTreeModelBuilderTest.kt                 — Integration tests: PSI → tree model
  EditorPreviewSyncTest.kt                    — Integration tests: bidirectional sync
  YamlViewerEditorProviderTest.kt             — Integration tests: editor provider registration
src/test/resources/
  testdata/simple.yaml                        — Simple key-value pairs
  testdata/nested.yaml                        — Deeply nested structure
  testdata/sequence.yaml                      — Lists and sequences
  testdata/mixed.yaml                         — Complex real-world-like YAML
  testdata/invalid.yaml                       — YAML with syntax errors
```

---

### Task 1: Project Scaffolding

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `src/main/resources/META-INF/plugin.xml`
- Create: `src/main/kotlin/com/yamlviewer/YamlViewerEditorProvider.kt` (stub)

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "idea-yamlviewer"
```

- [ ] **Step 2: Create `gradle.properties`**

```properties
pluginGroup=com.yamlviewer
pluginName=YAML Viewer
pluginVersion=0.1.0

platformType=IC
platformVersion=2024.1
```

- [ ] **Step 3: Create `build.gradle.kts`**

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("org.jetbrains.plugins.yaml")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        ideaVersion {
            sinceBuild = "241"
        }
    }
}

tasks {
    test {
        useJUnit()
    }
}
```

- [ ] **Step 4: Create `src/main/resources/META-INF/plugin.xml`**

```xml
<idea-plugin>
    <id>com.yamlviewer</id>
    <name>YAML Viewer</name>
    <version>0.1.0</version>
    <vendor>yamlviewer</vendor>
    <description>Human-readable YAML viewer with tree/table hybrid rendering</description>

    <depends>com.intellij.modules.platform</depends>
    <depends>org.jetbrains.plugins.yaml</depends>

    <extensions defaultExtensionNs="com.intellij">
        <fileEditorProvider implementation="com.yamlviewer.YamlViewerEditorProvider"/>
    </extensions>
</idea-plugin>
```

- [ ] **Step 5: Create stub `YamlViewerEditorProvider.kt`**

```kotlin
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
        TODO("Implemented in Task 8")
    }

    override fun getEditorTypeId(): String = "yaml-viewer"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
```

- [ ] **Step 6: Create Gradle wrapper and verify build**

Run: `gradle wrapper --gradle-version 8.5`
Then: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL (plugin compiles, no tests yet)

- [ ] **Step 7: Initialize git and commit**

```bash
git init
echo ".gradle/\nbuild/\n.idea/\n*.iml\n.superpowers/" > .gitignore
git add .gitignore build.gradle.kts settings.gradle.kts gradle.properties gradle/ gradlew gradlew.bat src/
git commit -m "chore: scaffold IntelliJ plugin project with Gradle and plugin.xml"
```

---

### Task 2: Scalar Type Detection

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlScalarType.kt`
- Create: `src/test/kotlin/com/yamlviewer/YamlScalarTypeTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.yamlviewer

import org.junit.Assert.assertEquals
import org.junit.Test

class YamlScalarTypeTest {
    @Test
    fun `detects boolean true`() {
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("true"))
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("True"))
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("TRUE"))
    }

    @Test
    fun `detects boolean false`() {
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("false"))
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("False"))
        assertEquals(YamlScalarType.BOOL, YamlScalarType.detect("FALSE"))
    }

    @Test
    fun `detects null`() {
        assertEquals(YamlScalarType.NULL, YamlScalarType.detect("null"))
        assertEquals(YamlScalarType.NULL, YamlScalarType.detect("Null"))
        assertEquals(YamlScalarType.NULL, YamlScalarType.detect("~"))
        assertEquals(YamlScalarType.NULL, YamlScalarType.detect(""))
    }

    @Test
    fun `detects integer`() {
        assertEquals(YamlScalarType.INT, YamlScalarType.detect("42"))
        assertEquals(YamlScalarType.INT, YamlScalarType.detect("-1"))
        assertEquals(YamlScalarType.INT, YamlScalarType.detect("0"))
        assertEquals(YamlScalarType.INT, YamlScalarType.detect("+100"))
    }

    @Test
    fun `detects float`() {
        assertEquals(YamlScalarType.FLOAT, YamlScalarType.detect("3.14"))
        assertEquals(YamlScalarType.FLOAT, YamlScalarType.detect("-0.5"))
        assertEquals(YamlScalarType.FLOAT, YamlScalarType.detect(".inf"))
        assertEquals(YamlScalarType.FLOAT, YamlScalarType.detect("-.inf"))
        assertEquals(YamlScalarType.FLOAT, YamlScalarType.detect(".nan"))
    }

    @Test
    fun `detects string as fallback`() {
        assertEquals(YamlScalarType.STRING, YamlScalarType.detect("hello"))
        assertEquals(YamlScalarType.STRING, YamlScalarType.detect("hello world"))
        assertEquals(YamlScalarType.STRING, YamlScalarType.detect("123abc"))
        assertEquals(YamlScalarType.STRING, YamlScalarType.detect("v1.2.3"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yamlviewer.YamlScalarTypeTest"`
Expected: FAIL — `YamlScalarType` does not exist

- [ ] **Step 3: Implement `YamlScalarType`**

```kotlin
package com.yamlviewer

enum class YamlScalarType(val label: String) {
    STRING("string"),
    INT("int"),
    FLOAT("float"),
    BOOL("bool"),
    NULL("null");

    companion object {
        private val INT_PATTERN = Regex("^[+-]?\\d+$")
        private val FLOAT_PATTERN = Regex("^[+-]?(\\d+\\.\\d*|\\.\\d+)([eE][+-]?\\d+)?$")
        private val FLOAT_SPECIAL = setOf(".inf", "-.inf", "+.inf", ".nan")
        private val BOOL_VALUES = setOf("true", "false")
        private val NULL_VALUES = setOf("null", "~", "")

        fun detect(value: String): YamlScalarType {
            val lower = value.lowercase()
            return when {
                lower in NULL_VALUES -> NULL
                lower in BOOL_VALUES -> BOOL
                INT_PATTERN.matches(value) -> INT
                FLOAT_PATTERN.matches(value) || lower in FLOAT_SPECIAL -> FLOAT
                else -> STRING
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yamlviewer.YamlScalarTypeTest"`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlScalarType.kt src/test/kotlin/com/yamlviewer/YamlScalarTypeTest.kt
git commit -m "feat: add YamlScalarType enum with type detection from scalar values"
```

---

### Task 3: Tree Node Model & Node Types

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlNodeType.kt`
- Create: `src/main/kotlin/com/yamlviewer/YamlViewerTreeNode.kt`

- [ ] **Step 1: Create `YamlNodeType`**

```kotlin
package com.yamlviewer

enum class YamlNodeType {
    /** Mapping with at least one non-scalar child */
    SECTION,
    /** Mapping where every child is a scalar */
    LEAF_GROUP,
    /** YAML sequence (list) */
    SEQUENCE,
    /** Single scalar value */
    SCALAR
}
```

- [ ] **Step 2: Create `YamlViewerTreeNode`**

```kotlin
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
    val childCount: Int = 0,
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
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlNodeType.kt src/main/kotlin/com/yamlviewer/YamlViewerTreeNode.kt
git commit -m "feat: add YamlNodeType enum and YamlViewerTreeNode data model"
```

---

### Task 4: PSI → Tree Model Builder

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlTreeModelBuilder.kt`
- Create: `src/test/kotlin/com/yamlviewer/YamlTreeModelBuilderTest.kt`
- Create: `src/test/resources/testdata/simple.yaml`
- Create: `src/test/resources/testdata/nested.yaml`
- Create: `src/test/resources/testdata/sequence.yaml`
- Create: `src/test/resources/testdata/mixed.yaml`

- [ ] **Step 1: Create test YAML fixtures**

`src/test/resources/testdata/simple.yaml`:
```yaml
name: my-app
version: "1.0"
debug: true
replicas: 3
```

`src/test/resources/testdata/nested.yaml`:
```yaml
metadata:
  name: my-app
  namespace: default
  labels:
    app: my-app
    env: production
spec:
  replicas: 3
```

`src/test/resources/testdata/sequence.yaml`:
```yaml
items:
  - name: first
    value: 1
  - name: second
    value: 2
tags:
  - alpha
  - beta
```

`src/test/resources/testdata/mixed.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  labels:
    app: my-app
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: app
          image: my-app:latest
          ports:
            - containerPort: 8080
          env:
            - name: DB_HOST
              value: localhost
```

- [ ] **Step 2: Write the failing tests**

```kotlin
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

        // metadata has: name, namespace (scalars) + labels (mapping) → it's a SECTION
        // labels is a leaf group inside metadata
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

        // Find the labels node inside metadata
        val labels = (0 until metadata.childCount)
            .map { metadata.getChildAt(it) as YamlViewerTreeNode }
            .first { it.key == "labels" }
        assertEquals("metadata.labels", labels.yamlPath)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yamlviewer.YamlTreeModelBuilderTest"`
Expected: FAIL — `YamlTreeModelBuilder` does not exist

- [ ] **Step 4: Implement `YamlTreeModelBuilder`**

```kotlin
package com.yamlviewer

import com.intellij.openapi.util.TextRange
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
                childCount = keyValues.size,
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
            childCount = keyValues.size,
        )
        for (kv in keyValues) {
            val childValue = kv.value
            if (childValue != null) {
                val child = buildNode(childValue, kv.keyText, path)
                if (child != null) {
                    node.add(child)
                }
            } else {
                // Key with null value — treat as scalar
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
            childCount = items.size,
        )
        for ((index, item) in items.withIndex()) {
            val itemValue = item.value
            val itemPath = "$path[$index]"
            if (itemValue != null) {
                val child = buildNode(itemValue, null, itemPath)
                if (child != null) {
                    child.let { node.add(it) }
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
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yamlviewer.YamlTreeModelBuilderTest"`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlTreeModelBuilder.kt src/test/kotlin/com/yamlviewer/YamlTreeModelBuilderTest.kt src/test/resources/testdata/
git commit -m "feat: build tree model from YAML PSI with node type classification"
```

---

### Task 5: Tree Model Wrapper

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlTreeModel.kt`

- [ ] **Step 1: Implement `YamlTreeModel`**

```kotlin
package com.yamlviewer

import com.intellij.psi.PsiFile
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class YamlTreeModel(psiFile: PsiFile) : DefaultTreeModel(YamlTreeModelBuilder.build(psiFile)) {

    /** Find a tree node by its YAML path (e.g., "spec.template.containers[0]") */
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

    /** Find a tree node by PSI offset */
    fun findByOffset(offset: Int): YamlViewerTreeNode? {
        return findByOffset(root as DefaultMutableTreeNode, offset)
    }

    private fun findByOffset(node: DefaultMutableTreeNode, offset: Int): YamlViewerTreeNode? {
        if (node is YamlViewerTreeNode) {
            // Check leaf entries first (for leaf group nodes)
            if (node.nodeType == YamlNodeType.LEAF_GROUP) {
                for (entry in node.leafEntries) {
                    if (entry.psiRange.contains(offset)) return node
                }
            }
            if (node.psiRange.contains(offset)) {
                // Try to find a more specific child
                for (i in 0 until node.childCount) {
                    val result = findByOffset(node.getChildAt(i) as DefaultMutableTreeNode, offset)
                    if (result != null) return result
                }
                return node
            }
        } else {
            // root node — check children
            for (i in 0 until node.childCount) {
                val result = findByOffset(node.getChildAt(i) as DefaultMutableTreeNode, offset)
                if (result != null) return result
            }
        }
        return null
    }

    /** Get the TreePath for a given node */
    fun treePath(node: YamlViewerTreeNode): TreePath {
        return TreePath(node.path)
    }

    /** Collect all expanded YAML paths from current tree state */
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

    /** Restore expansion state from a set of YAML paths */
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
```

- [ ] **Step 2: Verify build**

Run: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlTreeModel.kt
git commit -m "feat: add YamlTreeModel with path-based lookup and expansion state management"
```

---

### Task 6: Tree Cell Renderer

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlTreeCellRenderer.kt`

- [ ] **Step 1: Implement `YamlTreeCellRenderer`**

```kotlin
package com.yamlviewer

import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
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
            YamlNodeType.LEAF_GROUP -> renderLeafGroup(node, tree)
            YamlNodeType.SEQUENCE -> renderSequence(node, expanded)
            YamlNodeType.SCALAR -> renderScalar(node)
        }
    }

    private fun renderSection(node: YamlViewerTreeNode, expanded: Boolean): Component {
        val component = SimpleColoredComponent()
        component.append(node.key ?: "document", BOLD_ATTRIBUTES)
        if (!expanded) {
            component.append("  ${node.childCount} keys", GRAY_ATTRIBUTES)
        }
        component.ipad = JBUI.insetsLeft(2)
        return component
    }

    private fun renderLeafGroup(node: YamlViewerTreeNode, tree: JTree): Component {
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
            component.append("  ${node.childCount} items", GRAY_ATTRIBUTES)
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
```

- [ ] **Step 2: Verify build**

Run: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlTreeCellRenderer.kt
git commit -m "feat: add custom tree cell renderer with hybrid table rows and type badges"
```

---

### Task 7: Preview Panel

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/YamlPreviewPanel.kt`

- [ ] **Step 1: Implement `YamlPreviewPanel`**

```kotlin
package com.yamlviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class YamlPreviewPanel(
    private val psiFile: PsiFile,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()), Disposable {

    val tree: Tree
    var treeModel: YamlTreeModel
        private set

    private val searchField = SearchTextField()
    private val breadcrumbPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
    private val statusLabel = JBLabel()
    private val filterAlarm: Alarm

    var onTreeSelectionChanged: ((YamlViewerTreeNode) -> Unit)? = null

    init {
        Disposer.register(parentDisposable, this)
        filterAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

        treeModel = YamlTreeModel(psiFile)
        tree = Tree(treeModel).apply {
            cellRenderer = YamlTreeCellRenderer()
            isRootVisible = false
            showsRootHandles = true
            rowHeight = -1  // Variable row heights for leaf group rendering
        }

        // Expand top-level nodes
        expandTopLevel()

        // Tree selection listener
        tree.addTreeSelectionListener { e: TreeSelectionEvent ->
            val node = tree.lastSelectedPathComponent as? YamlViewerTreeNode ?: return@addTreeSelectionListener
            updateBreadcrumbs(node)
            onTreeSelectionChanged?.invoke(node)
        }

        // Search field
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = scheduleFilter()
            override fun removeUpdate(e: DocumentEvent) = scheduleFilter()
            override fun changedUpdate(e: DocumentEvent) = scheduleFilter()
        })

        // Layout
        val topPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(4)
            add(searchField, BorderLayout.NORTH)
            add(breadcrumbPanel, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
        statusLabel.isVisible = false

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)
    }

    fun rebuild() {
        ApplicationManager.getApplication().assertIsDispatchThread()

        val expandedPaths = treeModel.collectExpandedPaths(tree)
        val selectedPath = (tree.lastSelectedPathComponent as? YamlViewerTreeNode)?.yamlPath

        treeModel = YamlTreeModel(psiFile)
        tree.model = treeModel

        treeModel.restoreExpandedPaths(tree, expandedPaths)

        if (selectedPath != null) {
            val node = treeModel.findByYamlPath(selectedPath)
            if (node != null) {
                val tp = treeModel.treePath(node)
                tree.selectionPath = tp
                tree.scrollPathToVisible(tp)
            }
        }

        statusLabel.isVisible = false
    }

    fun showInvalidYamlStatus() {
        statusLabel.text = "YAML contains errors — showing last valid state"
        statusLabel.isVisible = true
    }

    fun selectByOffset(offset: Int) {
        val node = treeModel.findByOffset(offset) ?: return
        val path = treeModel.treePath(node)
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
    }

    private fun expandTopLevel() {
        val root = treeModel.root as DefaultMutableTreeNode
        for (i in 0 until root.childCount) {
            val docNode = root.getChildAt(i) as DefaultMutableTreeNode
            tree.expandPath(TreePath(docNode.path))
            for (j in 0 until docNode.childCount) {
                val child = docNode.getChildAt(j)
                tree.expandPath(TreePath((child as DefaultMutableTreeNode).path))
            }
        }
    }

    private fun updateBreadcrumbs(node: YamlViewerTreeNode) {
        breadcrumbPanel.removeAll()
        val parts = node.yamlPath.split(".")
        var accumulated = ""
        for ((index, part) in parts.withIndex()) {
            if (index > 0) {
                breadcrumbPanel.add(JBLabel(" > "))
                accumulated += "."
            }
            accumulated += part
            val pathSoFar = accumulated
            val link = JBLabel(part).apply {
                foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        val target = treeModel.findByYamlPath(pathSoFar)
                        if (target != null) {
                            val tp = treeModel.treePath(target)
                            tree.selectionPath = tp
                            tree.scrollPathToVisible(tp)
                        }
                    }
                })
            }
            breadcrumbPanel.add(link)
        }
        breadcrumbPanel.revalidate()
        breadcrumbPanel.repaint()
    }

    private fun scheduleFilter() {
        filterAlarm.cancelAllRequests()
        filterAlarm.addRequest({ applyFilter() }, 200)
    }

    private fun applyFilter() {
        val query = searchField.text.trim().lowercase()
        if (query.isEmpty()) {
            // Reset: rebuild full model
            rebuild()
            return
        }

        // Filter: walk tree and hide non-matching branches
        val root = treeModel.root as DefaultMutableTreeNode
        filterNode(root, query)
        treeModel.reload()

        // Expand everything that's visible after filter
        expandAll(root)
    }

    private fun filterNode(node: DefaultMutableTreeNode, query: String): Boolean {
        if (node is YamlViewerTreeNode) {
            val matches = nodeMatchesQuery(node, query)
            var childMatches = false

            // Process children in reverse to safely remove
            for (i in node.childCount - 1 downTo 0) {
                val child = node.getChildAt(i) as DefaultMutableTreeNode
                if (!filterNode(child, query)) {
                    node.remove(i)
                } else {
                    childMatches = true
                }
            }

            return matches || childMatches
        }

        // Root node — process children
        var anyChild = false
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChildAt(i) as DefaultMutableTreeNode
            if (!filterNode(child, query)) {
                node.remove(i)
            } else {
                anyChild = true
            }
        }
        return anyChild
    }

    private fun nodeMatchesQuery(node: YamlViewerTreeNode, query: String): Boolean {
        if (node.key?.lowercase()?.contains(query) == true) return true
        if (node.value?.lowercase()?.contains(query) == true) return true
        for (entry in node.leafEntries) {
            if (entry.key.lowercase().contains(query)) return true
            if (entry.value.lowercase().contains(query)) return true
        }
        return false
    }

    private fun expandAll(node: DefaultMutableTreeNode) {
        tree.expandPath(TreePath(node.path))
        for (i in 0 until node.childCount) {
            expandAll(node.getChildAt(i) as DefaultMutableTreeNode)
        }
    }

    override fun dispose() {}
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlPreviewPanel.kt
git commit -m "feat: add preview panel with tree view, search filter, and breadcrumbs"
```

---

### Task 8: Bidirectional Sync

**Files:**
- Create: `src/main/kotlin/com/yamlviewer/EditorPreviewSync.kt`
- Create: `src/test/kotlin/com/yamlviewer/EditorPreviewSyncTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.yamlviewer

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EditorPreviewSyncTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testdata"

    fun `test editor caret change selects tree node`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val textEditor = TextEditorProvider.getInstance().createEditor(project, psiFile.virtualFile) as TextEditor
        val panel = YamlPreviewPanel(psiFile, testRootDisposable)
        val sync = EditorPreviewSync(textEditor.editor, panel)

        // Move caret to "namespace: default" line
        val text = psiFile.text
        val offset = text.indexOf("namespace")
        textEditor.editor.caretModel.moveToOffset(offset)

        // Trigger sync manually (in tests, listeners may not fire synchronously)
        sync.syncEditorToTree()

        val selected = panel.tree.lastSelectedPathComponent as? YamlViewerTreeNode
        assertNotNull("A node should be selected", selected)
        // The node should be metadata (since namespace is inside the metadata leaf group or section)
        assertTrue(
            "Selected node path should contain metadata",
            selected!!.yamlPath.startsWith("metadata")
        )
    }

    fun `test tree selection moves editor caret`() {
        val psiFile = myFixture.configureByFile("nested.yaml")
        val textEditor = TextEditorProvider.getInstance().createEditor(project, psiFile.virtualFile) as TextEditor
        val panel = YamlPreviewPanel(psiFile, testRootDisposable)
        val sync = EditorPreviewSync(textEditor.editor, panel)

        // Find the spec node and select it
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yamlviewer.EditorPreviewSyncTest"`
Expected: FAIL — `EditorPreviewSync` does not exist

- [ ] **Step 3: Implement `EditorPreviewSync`**

```kotlin
package com.yamlviewer

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

class EditorPreviewSync(
    private val editor: Editor,
    private val previewPanel: YamlPreviewPanel,
) {
    private var isSyncing = false

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            if (isSyncing) return
            syncEditorToTree()
        }
    }

    private val treeSelectionListener = TreeSelectionListener { e: TreeSelectionEvent ->
        if (isSyncing) return@TreeSelectionListener
        syncTreeToEditor()
    }

    init {
        editor.caretModel.addCaretListener(caretListener)
        previewPanel.tree.addTreeSelectionListener(treeSelectionListener)
        previewPanel.onTreeSelectionChanged = { node ->
            if (!isSyncing) {
                syncTreeToEditor()
            }
        }
    }

    fun syncEditorToTree() {
        if (isSyncing) return
        isSyncing = true
        try {
            val offset = editor.caretModel.offset
            previewPanel.selectByOffset(offset)
        } finally {
            isSyncing = false
        }
    }

    fun syncTreeToEditor() {
        if (isSyncing) return
        isSyncing = true
        try {
            val node = previewPanel.tree.lastSelectedPathComponent as? YamlViewerTreeNode ?: return
            val targetOffset = node.psiRange.startOffset
            editor.caretModel.moveToOffset(targetOffset)
            editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
        } finally {
            isSyncing = false
        }
    }

    fun dispose() {
        editor.caretModel.removeCaretListener(caretListener)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yamlviewer.EditorPreviewSyncTest"`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/EditorPreviewSync.kt src/test/kotlin/com/yamlviewer/EditorPreviewSyncTest.kt
git commit -m "feat: add bidirectional sync between editor caret and tree selection"
```

---

### Task 9: Editor Provider (Wiring Everything Together)

**Files:**
- Modify: `src/main/kotlin/com/yamlviewer/YamlViewerEditorProvider.kt`
- Create: `src/test/kotlin/com/yamlviewer/YamlViewerEditorProviderTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.yamlviewer

import com.intellij.openapi.fileTypes.FileTypeManager
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
        assertNotNull(editor)
        assertEquals("yaml-viewer", provider.editorTypeId)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yamlviewer.YamlViewerEditorProviderTest"`
Expected: FAIL — `createEditor` throws `TODO`

- [ ] **Step 3: Implement the full `YamlViewerEditorProvider`**

Replace the entire content of `YamlViewerEditorProvider.kt`:

```kotlin
package com.yamlviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
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

        // Document change listener with debounce
        val rebuildAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, textEditor)
        textEditor.editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                rebuildAlarm.cancelAllRequests()
                rebuildAlarm.addRequest({
                    val currentPsi = PsiManager.getInstance(project).findFile(file) ?: return@addRequest
                    val hasErrors = com.intellij.psi.util.PsiTreeUtil.hasErrorElements(currentPsi)
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

/**
 * Wraps a JComponent as a FileEditor so it can be used in TextEditorWithPreview.
 */
private class FileEditorWrapper(private val component: YamlPreviewPanel) : FileEditor {
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yamlviewer.YamlViewerEditorProviderTest"`
Expected: ALL PASS

- [ ] **Step 5: Run all tests**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/yamlviewer/YamlViewerEditorProvider.kt src/test/kotlin/com/yamlviewer/YamlViewerEditorProviderTest.kt
git commit -m "feat: wire up TextEditorWithPreview with document change handling"
```

---

### Task 10: Test Data for Invalid YAML & Edge Cases

**Files:**
- Create: `src/test/resources/testdata/invalid.yaml`
- Modify: `src/test/kotlin/com/yamlviewer/YamlTreeModelBuilderTest.kt`

- [ ] **Step 1: Create invalid YAML fixture**

`src/test/resources/testdata/invalid.yaml`:
```yaml
valid_key: value
broken:
  - item1
  - item2
  bad_indent: oops
another: works
```

- [ ] **Step 2: Add edge case tests**

Add to `YamlTreeModelBuilderTest.kt`:

```kotlin
fun `test empty yaml produces empty root`() {
    val psiFile = myFixture.configureByText("empty.yaml", "")
    val root = YamlTreeModelBuilder.build(psiFile)
    assertEquals(0, root.childCount)
}

fun `test yaml with only comments produces empty root`() {
    val psiFile = myFixture.configureByText("comments.yaml", "# just a comment\n# another comment")
    val root = YamlTreeModelBuilder.build(psiFile)
    // May have a document node but no meaningful content
    assertTrue(root.childCount == 0 || (root.getChildAt(0) as? DefaultMutableTreeNode)?.childCount == 0)
}

fun `test multi-document yaml`() {
    val content = """
        name: doc1
        ---
        name: doc2
    """.trimIndent()
    val psiFile = myFixture.configureByText("multi.yaml", content)
    val root = YamlTreeModelBuilder.build(psiFile)
    assertEquals("Should have 2 document nodes", 2, root.childCount)
}

fun `test deeply nested yaml builds correct paths`() {
    val psiFile = myFixture.configureByFile("mixed.yaml")
    val root = YamlTreeModelBuilder.build(psiFile)

    // Walk to spec.template.spec.containers[0]
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

private fun findChild(parent: YamlViewerTreeNode, key: String): YamlViewerTreeNode? {
    for (i in 0 until parent.childCount) {
        val child = parent.getChildAt(i) as? YamlViewerTreeNode
        if (child?.key == key) return child
    }
    return null
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "com.yamlviewer.YamlTreeModelBuilderTest"`
Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/com/yamlviewer/YamlTreeModelBuilderTest.kt src/test/resources/testdata/invalid.yaml
git commit -m "test: add edge case tests for empty, multi-doc, and deeply nested YAML"
```

---

### Task 11: Manual Smoke Test & Plugin Run

**Files:** None (verification only)

- [ ] **Step 1: Run the plugin in a sandbox IDE**

Run: `./gradlew runIde`
Expected: IntelliJ IDEA opens with the plugin loaded

- [ ] **Step 2: Smoke test**

1. Open any `.yaml` file in the sandbox IDE
2. Verify the "YAML Viewer" tab appears in the editor tab bar (alongside the default text editor)
3. Switch to split view — verify the tree panel renders on the right
4. Click nodes in the tree — verify editor scrolls to corresponding line
5. Click in the editor — verify tree selects the corresponding node
6. Type to break YAML syntax — verify "YAML contains errors" status appears
7. Fix the syntax — verify the tree rebuilds
8. Use the search field to filter nodes
9. Verify breadcrumbs update on selection

- [ ] **Step 3: Fix any issues found during smoke test**

Address any rendering, sync, or layout issues discovered.

- [ ] **Step 4: Run full test suite**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "chore: finalize YAML viewer plugin v0.1.0"
```
