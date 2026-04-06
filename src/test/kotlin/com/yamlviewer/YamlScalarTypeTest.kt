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
