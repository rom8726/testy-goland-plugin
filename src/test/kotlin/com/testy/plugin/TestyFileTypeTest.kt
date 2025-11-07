package com.testy.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class TestyFileTypeTest {

    @Test
    fun `file type properties are correct`() {
        assertEquals("Testy YAML", TestyFileType.name)
        assertEquals("Testy scenario file", TestyFileType.description)
        assertEquals("testy.yml", TestyFileType.defaultExtension)
    }
}
