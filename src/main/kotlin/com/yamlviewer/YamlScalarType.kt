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
