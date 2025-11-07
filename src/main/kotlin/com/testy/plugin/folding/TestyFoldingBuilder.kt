package com.testy.plugin.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.psi.*

class TestyFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val file = root as? YAMLFile ?: return emptyArray()
        
        // Only fold testy files
        val fileName = file.name
        if (!fileName.endsWith(".testy.yaml") && !fileName.endsWith(".testy.yml")) {
            return emptyArray()
        }
        
        val descriptors = mutableListOf<FoldingDescriptor>()
        
        file.documents.forEach { doc ->
            val rootValue = doc.topLevelValue as? YAMLSequence ?: return@forEach
            
            rootValue.items.forEach { item ->
                val mapping = item.value as? YAMLMapping ?: return@forEach
                
                // Fold fixtures array
                mapping.getKeyValueByKey("fixtures")?.value?.let { fixturesValue ->
                    if (fixturesValue is YAMLSequence && fixturesValue.items.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                fixturesValue.node,
                                fixturesValue.textRange,
                                null,
                                "fixtures: ${fixturesValue.items.size} items"
                            )
                        )
                    }
                }
                
                // Fold mockServers object
                mapping.getKeyValueByKey("mockServers")?.value?.let { mocksValue ->
                    if (mocksValue is YAMLMapping && mocksValue.keyValues.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                mocksValue.node,
                                mocksValue.textRange,
                                null,
                                "mockServers: ${mocksValue.keyValues.size} servers"
                            )
                        )
                    }
                }
                
                // Fold steps array
                mapping.getKeyValueByKey("steps")?.value?.let { stepsValue ->
                    if (stepsValue is YAMLSequence && stepsValue.items.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                stepsValue.node,
                                stepsValue.textRange,
                                null,
                                "steps: ${stepsValue.items.size} items"
                            )
                        )
                    }
                }
                
                // Fold individual step details
                mapping.getKeyValueByKey("steps")?.value?.let { stepsValue ->
                    if (stepsValue is YAMLSequence) {
                        stepsValue.items.forEach { stepItem ->
                            val stepMapping = stepItem.value as? YAMLMapping ?: return@forEach
                            
                            // Fold request body
                            stepMapping.getKeyValueByKey("request")?.value?.let { requestValue ->
                                if (requestValue is YAMLMapping) {
                                    requestValue.getKeyValueByKey("body")?.value?.let { bodyValue ->
                                        if (bodyValue.textRange.length > 50) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    bodyValue.node,
                                                    bodyValue.textRange,
                                                    null,
                                                    "body: ..."
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Fold response json
                            stepMapping.getKeyValueByKey("response")?.value?.let { responseValue ->
                                if (responseValue is YAMLMapping) {
                                    responseValue.getKeyValueByKey("json")?.value?.let { jsonValue ->
                                        if (jsonValue.textRange.length > 50) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    jsonValue.node,
                                                    jsonValue.textRange,
                                                    null,
                                                    "json: ..."
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Fold dbChecks array
                            stepMapping.getKeyValueByKey("dbChecks")?.value?.let { dbChecksValue ->
                                if (dbChecksValue is YAMLSequence && dbChecksValue.items.isNotEmpty()) {
                                    descriptors.add(
                                        FoldingDescriptor(
                                            dbChecksValue.node,
                                            dbChecksValue.textRange,
                                            null,
                                            "dbChecks: ${dbChecksValue.items.size} items"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return descriptors.toTypedArray()
    }
    
    override fun getPlaceholderText(node: ASTNode): String? {
        return "..."
    }
    
    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }
}

