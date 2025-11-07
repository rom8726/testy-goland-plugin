package com.testy.plugin

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLDocument

data class TestyScenario(
    val name: String,
    val description: String?,
    val offset: Int
)

object TestyYamlParser {
    fun parse(file: PsiFile): List<TestyScenario> {
        if (file !is YAMLFile) return emptyList()

        val result = mutableListOf<TestyScenario>()
        file.documents.forEach { doc ->
            val root = doc.topLevelValue as? YAMLMapping ?: return@forEach
            root.keyValues.forEach { kv ->
                val name = kv.keyText
                val desc = (kv.value as? YAMLMapping)?.getKeyValueByKey("description")?.valueText
                result += TestyScenario(name, desc, kv.textOffset)
            }
        }
        return result
    }
}
