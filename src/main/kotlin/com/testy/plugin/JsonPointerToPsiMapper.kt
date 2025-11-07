package com.testy.plugin

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.*

object JsonPointerToPsiMapper {
    fun mapPointerToOffset(pointer: String, file: PsiFile): Int? {
        if (file !is YAMLFile) return null
        
        val parts = pointer.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        
        file.documents.forEach { doc ->
            val root = doc.topLevelValue as? YAMLSequence ?: return@forEach
            val offset = findOffsetByPath(parts, root, 0)
            if (offset != null) return offset
        }
        
        return null
    }
    
    private fun findOffsetByPath(parts: List<String>, current: YAMLValue, index: Int): Int? {
        if (index >= parts.size) return current.textOffset
        
        val part = parts[index]
        
        return when (current) {
            is YAMLSequence -> {
                val arrayIndex = part.toIntOrNull() ?: return null
                if (arrayIndex < 0 || arrayIndex >= current.items.size) return null
                val item = current.items[arrayIndex]
                findOffsetByPath(parts, item.value ?: return null, index + 1)
            }
            is YAMLMapping -> {
                val keyValue = current.getKeyValueByKey(part) ?: return null
                if (index + 1 >= parts.size) {
                    // Last part - return the key-value offset
                    return keyValue.textOffset
                }
                findOffsetByPath(parts, keyValue.value ?: return null, index + 1)
            }
            else -> {
                // Try to find nearest parent
                current.textOffset
            }
        }
    }
}

