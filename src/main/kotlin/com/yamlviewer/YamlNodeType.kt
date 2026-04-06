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
