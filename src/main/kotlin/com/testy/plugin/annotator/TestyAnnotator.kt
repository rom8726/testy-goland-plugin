package com.testy.plugin.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.elementType
import com.testy.plugin.TestySchemaValidator
import com.testy.plugin.ValidationError
import com.testy.plugin.ValidationSeverity
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.psi.YAMLFile

class TestyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Only run validation once per file, not for every element
        // The annotator is called for EVERY PSI element in the file, which causes
        // massive performance issues when there are many .testy files
        
        val file = element.containingFile as? YAMLFile ?: return
        
        // Only annotate testy files
        val fileName = file.name
        if (!fileName.endsWith(".testy.yaml") && !fileName.endsWith(".testy.yml")) {
            return
        }
        
        // Only validate at the file level (when element is the file itself)
        // This prevents validating the entire file for every single PSI element
        if (element != file) {
            return
        }
        
        // Cache validation results to avoid re-validating on every annotate call
        val validationErrors = CachedValuesManager.getCachedValue(file) {
            val yamlContent = file.text
            val errors = try {
                TestySchemaValidator.validate(yamlContent, file)
            } catch (e: Exception) {
                emptyList<ValidationError>()
            }
            CachedValueProvider.Result.create(errors, PsiModificationTracker.MODIFICATION_COUNT)
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

