package com.testy.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.PsiFile
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import org.yaml.snakeyaml.Yaml

object TestySchemaValidator {
    private var schema: JsonSchema? = null
    private var schemaLoadError: String? = null
    private val objectMapper = ObjectMapper()
    private val yaml = Yaml()
    
    init {
        try {
            loadSchema()
        } catch (e: Exception) {
            schemaLoadError = "Failed to load schema: ${e.message}"
        }
    }
    
    private fun loadSchema() {
        val schemaStream = javaClass.classLoader.getResourceAsStream("testy.json")
            ?: throw IllegalStateException("Cannot load testy.json schema from resources")
        
        try {
            val schemaJson = objectMapper.readTree(schemaStream)
            val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
            schema = factory.getSchema(schemaJson)
            schemaLoadError = null
        } catch (e: Exception) {
            schemaLoadError = "Failed to parse schema: ${e.message}"
            throw e
        } finally {
            schemaStream.close()
        }
    }
    
    fun validate(yamlContent: String, psiFile: PsiFile? = null): List<ValidationError> {
        return try {
            // Check if there was an error during schema loading
            schemaLoadError?.let { error ->
                return listOf(
                    ValidationError(
                        severity = ValidationSeverity.ERROR,
                        pointer = "",
                        message = error,
                        offset = null
                    )
                )
            }
            
            val jsonSchema = schema ?: run {
                try {
                    loadSchema()
                    schema ?: return emptyList()
                } catch (e: Exception) {
                    // Return error about schema loading failure
                    return listOf(
                        ValidationError(
                            severity = ValidationSeverity.ERROR,
                            pointer = "",
                            message = "Failed to load schema: ${e.message}",
                            offset = null
                        )
                    )
                }
            }
            
            // Convert YAML to JSON for validation
            val yamlObject = try {
                yaml.load<Any>(yamlContent)
            } catch (e: Exception) {
                // Return error about YAML parsing failure
                return listOf(
                    ValidationError(
                        severity = ValidationSeverity.ERROR,
                        pointer = "",
                        message = "Failed to parse YAML: ${e.message}",
                        offset = null
                    )
                )
            }
            
            val jsonNode = try {
                objectMapper.valueToTree<JsonNode>(yamlObject)
            } catch (e: Exception) {
                // Return error about JSON conversion failure
                return listOf(
                    ValidationError(
                        severity = ValidationSeverity.ERROR,
                        pointer = "",
                        message = "Failed to convert YAML to JSON: ${e.message}",
                        offset = null
                    )
                )
            }
            
            // Validate
            val validationMessages = try {
                jsonSchema.validate(jsonNode)
            } catch (e: Exception) {
                // Return error about validation failure
                return listOf(
                    ValidationError(
                        severity = ValidationSeverity.ERROR,
                        pointer = "",
                        message = "Validation error: ${e.message}",
                        offset = null
                    )
                )
            }
            
            validationMessages.map { message ->
                val pointer = message.instanceLocation.toString()
                val offset = try {
                    psiFile?.let { JsonPointerToPsiMapper.mapPointerToOffset(pointer, it) }
                } catch (e: Exception) {
                    null
                }
                
                ValidationError(
                    severity = when {
                        message.type.contains("error", ignoreCase = true) -> ValidationSeverity.ERROR
                        message.type.contains("warning", ignoreCase = true) -> ValidationSeverity.WARNING
                        else -> ValidationSeverity.INFO
                    },
                    pointer = pointer,
                    message = message.message,
                    offset = offset
                )
            }
        } catch (e: Exception) {
            // Catch any unexpected errors and return them as validation errors
            listOf(
                ValidationError(
                    severity = ValidationSeverity.ERROR,
                    pointer = "",
                    message = "Unexpected validation error: ${e.message}",
                    offset = null
                )
            )
        }
    }
}

