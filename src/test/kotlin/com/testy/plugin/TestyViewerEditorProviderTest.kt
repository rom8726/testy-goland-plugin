package com.testy.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class TestyViewerEditorProviderTest {

    private fun mockVf(name: String): VirtualFile {
        val vf = Mockito.mock(VirtualFile::class.java)
        Mockito.`when`(vf.name).thenReturn(name)
        return vf
    }

    @Test
    fun `accept recognizes testy yaml extensions`() {
        val provider = TestyViewerEditorProvider()
        val project = Mockito.mock(Project::class.java)
        assertTrue(provider.accept(project, mockVf("a.testy.yml")))
        assertTrue(provider.accept(project, mockVf("a.testy.yaml")))
        assertFalse(provider.accept(project, mockVf("a.yaml")))
        assertFalse(provider.accept(project, mockVf("a.txt")))
    }
}
