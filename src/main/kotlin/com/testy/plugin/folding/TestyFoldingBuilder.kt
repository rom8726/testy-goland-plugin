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
                
                // Fold variables object
                mapping.getKeyValueByKey("variables")?.value?.let { variablesValue ->
                    if (variablesValue is YAMLMapping && variablesValue.keyValues.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                variablesValue.node,
                                variablesValue.textRange,
                                null,
                                "variables: ${variablesValue.keyValues.size} items"
                            )
                        )
                    }
                }
                
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
                
                // Fold setup array
                mapping.getKeyValueByKey("setup")?.value?.let { setupValue ->
                    if (setupValue is YAMLSequence && setupValue.items.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                setupValue.node,
                                setupValue.textRange,
                                null,
                                "setup: ${setupValue.items.size} hooks"
                            )
                        )
                    }
                }
                
                // Fold teardown array
                mapping.getKeyValueByKey("teardown")?.value?.let { teardownValue ->
                    if (teardownValue is YAMLSequence && teardownValue.items.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                teardownValue.node,
                                teardownValue.textRange,
                                null,
                                "teardown: ${teardownValue.items.size} hooks"
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
                
                // Fold mockCalls array
                mapping.getKeyValueByKey("mockCalls")?.value?.let { mockCallsValue ->
                    if (mockCallsValue is YAMLSequence && mockCallsValue.items.isNotEmpty()) {
                        descriptors.add(
                            FoldingDescriptor(
                                mockCallsValue.node,
                                mockCallsValue.textRange,
                                null,
                                "mockCalls: ${mockCallsValue.items.size} items"
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
                            
                            // Fold loop configuration
                            stepMapping.getKeyValueByKey("loop")?.value?.let { loopValue ->
                                if (loopValue is YAMLMapping && loopValue.textRange.length > 30) {
                                    descriptors.add(
                                        FoldingDescriptor(
                                            loopValue.node,
                                            loopValue.textRange,
                                            null,
                                            "loop: ..."
                                        )
                                    )
                                }
                            }
                            
                            // Fold retry configuration
                            stepMapping.getKeyValueByKey("retry")?.value?.let { retryValue ->
                                if (retryValue is YAMLMapping && retryValue.textRange.length > 30) {
                                    descriptors.add(
                                        FoldingDescriptor(
                                            retryValue.node,
                                            retryValue.textRange,
                                            null,
                                            "retry: ..."
                                        )
                                    )
                                }
                            }
                            
                            // Fold performance configuration
                            stepMapping.getKeyValueByKey("performance")?.value?.let { perfValue ->
                                if (perfValue is YAMLMapping && perfValue.textRange.length > 30) {
                                    descriptors.add(
                                        FoldingDescriptor(
                                            perfValue.node,
                                            perfValue.textRange,
                                            null,
                                            "performance: ..."
                                        )
                                    )
                                }
                            }
                            
                            // Fold HTTP request
                            stepMapping.getKeyValueByKey("request")?.value?.let { requestValue ->
                                if (requestValue is YAMLMapping) {
                                    // Fold request body
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
                                    
                                    // Fold request headers
                                    requestValue.getKeyValueByKey("headers")?.value?.let { headersValue ->
                                        if (headersValue is YAMLMapping && headersValue.keyValues.size > 2) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    headersValue.node,
                                                    headersValue.textRange,
                                                    null,
                                                    "headers: ${headersValue.keyValues.size} items"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Fold HTTP response
                            stepMapping.getKeyValueByKey("response")?.value?.let { responseValue ->
                                if (responseValue is YAMLMapping) {
                                    // Fold response json
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
                                    
                                    // Fold response jsonSchema
                                    responseValue.getKeyValueByKey("jsonSchema")?.value?.let { schemaValue ->
                                        if (schemaValue.textRange.length > 50) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    schemaValue.node,
                                                    schemaValue.textRange,
                                                    null,
                                                    "jsonSchema: ..."
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Fold response assertions
                                    responseValue.getKeyValueByKey("assertions")?.value?.let { assertionsValue ->
                                        if (assertionsValue is YAMLSequence && assertionsValue.items.isNotEmpty()) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    assertionsValue.node,
                                                    assertionsValue.textRange,
                                                    null,
                                                    "assertions: ${assertionsValue.items.size} items"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Fold gRPC request
                            stepMapping.getKeyValueByKey("grpcRequest")?.value?.let { grpcRequestValue ->
                                if (grpcRequestValue is YAMLMapping) {
                                    // Fold message
                                    grpcRequestValue.getKeyValueByKey("message")?.value?.let { messageValue ->
                                        if (messageValue.textRange.length > 50) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    messageValue.node,
                                                    messageValue.textRange,
                                                    null,
                                                    "message: ..."
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Fold metadata
                                    grpcRequestValue.getKeyValueByKey("metadata")?.value?.let { metadataValue ->
                                        if (metadataValue is YAMLMapping && metadataValue.keyValues.size > 2) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    metadataValue.node,
                                                    metadataValue.textRange,
                                                    null,
                                                    "metadata: ${metadataValue.keyValues.size} items"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Fold gRPC response
                            stepMapping.getKeyValueByKey("grpcResponse")?.value?.let { grpcResponseValue ->
                                if (grpcResponseValue is YAMLMapping) {
                                    // Fold message
                                    grpcResponseValue.getKeyValueByKey("message")?.value?.let { messageValue ->
                                        if (messageValue.textRange.length > 50) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    messageValue.node,
                                                    messageValue.textRange,
                                                    null,
                                                    "message: ..."
                                                )
                                            )
                                        }
                                    }
                                    
                                    // Fold assertions
                                    grpcResponseValue.getKeyValueByKey("assertions")?.value?.let { assertionsValue ->
                                        if (assertionsValue is YAMLSequence && assertionsValue.items.isNotEmpty()) {
                                            descriptors.add(
                                                FoldingDescriptor(
                                                    assertionsValue.node,
                                                    assertionsValue.textRange,
                                                    null,
                                                    "assertions: ${assertionsValue.items.size} items"
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
