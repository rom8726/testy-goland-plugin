package com.testy.plugin.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.testy.plugin.TestySchemaValidator
import com.testy.plugin.ValidationSeverity
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.psi.YAMLFile

class TestyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile as? YAMLFile ?: return
        
        // Only annotate testy files
        val fileName = file.name
        if (!fileName.endsWith(".testy.yaml") && !fileName.endsWith(".testy.yml")) {
            return
        }
        
        // Validate the file
        val yamlContent = file.text
        val validationErrors = try {
            TestySchemaValidator.validate(yamlContent, file)
        } catch (e: Exception) {
            return
        }
        
        // Map errors to PSI elements
        validationErrors.forEach { error ->
            val offset = error.offset ?: return@forEach
            
            // Find the element at this offset
            var current: PsiElement? = file.findElementAt(offset)
            var annotated = false
            
            while (current != null && current !is YAMLFile && !annotated) {
                // Check if this is a meaningful element (not whitespace)
                if (current.elementType != null && current.textRange.length > 0) {
                    val severity = when (error.severity) {
                        ValidationSeverity.ERROR -> HighlightSeverity.ERROR
                        ValidationSeverity.WARNING -> HighlightSeverity.WARNING
                        ValidationSeverity.INFO -> HighlightSeverity.INFORMATION
                    }
                    
                    try {
                        holder.newAnnotation(severity, error.message)
                            .range(current.textRange)
                            .create()
                        annotated = true
                    } catch (e: Exception) {
                        // Skip if annotation fails
                    }
                }
                current = current.parent
            }
        }
    }
}

