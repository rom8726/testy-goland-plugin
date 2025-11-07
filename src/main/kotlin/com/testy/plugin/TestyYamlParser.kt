package com.testy.plugin

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.*

data class TestyScenario(
    val name: String,
    val fixtures: List<String>? = null,
    val mockServers: Map<String, MockServer>? = null,
    val mockCalls: List<MockCall>? = null,
    val steps: List<TestyStep> = emptyList(),
    val offset: Int
)

data class TestyStep(
    val name: String,
    val request: Request,
    val response: Response,
    val dbChecks: List<DbCheck>? = null,
    val offset: Int
)

data class Request(
    val method: String,
    val path: String,
    val headers: Map<String, String>? = null,
    val body: Any? = null
)

data class Response(
    val status: Int,
    val headers: Map<String, String>? = null,
    val json: String? = null
)

data class MockServer(
    val routes: List<MockRoute>
)

data class MockRoute(
    val method: String,
    val path: String,
    val response: MockResponse
)

data class MockResponse(
    val status: Int,
    val headers: Map<String, String>? = null,
    val json: String? = null
)

data class MockCall(
    val mock: String,
    val count: Int,
    val expect: MockExpect? = null
)

data class MockExpect(
    val method: String? = null,
    val path: String? = null,
    val body: MockBodyExpect? = null
)

data class MockBodyExpect(
    val contains: String? = null
)

data class DbCheck(
    val query: String,
    val result: Any?
)

object TestyYamlParser {
    fun parse(file: PsiFile): List<TestyScenario> {
        if (file !is YAMLFile) return emptyList()

        val result = mutableListOf<TestyScenario>()
        file.documents.forEach { doc ->
            // Top level is an array of scenarios (Sequence)
            val root = doc.topLevelValue as? YAMLSequence ?: return@forEach
            
            root.items.forEach { item ->
                val scenarioMapping = item.value as? YAMLMapping ?: return@forEach
                val scenarioOffset = item.textOffset
                
                // Extract name (required field)
                val nameKv = scenarioMapping.getKeyValueByKey("name") ?: return@forEach
                val name = nameKv.valueText ?: return@forEach
                
                // Extract fixtures
                val fixturesKv = scenarioMapping.getKeyValueByKey("fixtures")
                val fixtures = fixturesKv?.value?.let { extractStringList(it) }
                
                // Extract mockServers
                val mockServersKv = scenarioMapping.getKeyValueByKey("mockServers")
                val mockServers = mockServersKv?.value?.let { extractMockServers(it as? YAMLMapping) }
                
                // Extract mockCalls
                val mockCallsKv = scenarioMapping.getKeyValueByKey("mockCalls")
                val mockCalls = mockCallsKv?.value?.let { extractMockCalls(it) }
                
                // Extract steps (required field)
                val stepsKv = scenarioMapping.getKeyValueByKey("steps")
                val steps = stepsKv?.value?.let { extractSteps(it) } ?: emptyList()
                
                result += TestyScenario(
                    name = name,
                    fixtures = fixtures,
                    mockServers = mockServers,
                    mockCalls = mockCalls,
                    steps = steps,
                    offset = scenarioOffset
                )
            }
        }
        return result
    }
    
    private fun extractStringList(value: YAMLValue): List<String>? {
        return when (value) {
            is YAMLSequence -> {
                value.items.mapNotNull { it.value?.text?.trim('"', '\'') }
            }
            else -> null
        }
    }
    
    private fun extractMockServers(mapping: YAMLMapping?): Map<String, MockServer>? {
        if (mapping == null) return null
        
        val result = mutableMapOf<String, MockServer>()
        mapping.keyValues.forEach { kv ->
            val serverName = kv.keyText
            val serverMapping = kv.value as? YAMLMapping ?: return@forEach
            val routesKv = serverMapping.getKeyValueByKey("routes") ?: return@forEach
            val routes = extractRoutes(routesKv.value) ?: return@forEach
            result[serverName] = MockServer(routes)
        }
        return result.ifEmpty { null }
    }
    
    private fun extractRoutes(value: YAMLValue?): List<MockRoute>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            val routeMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            val methodKv = routeMapping.getKeyValueByKey("method") ?: return@mapNotNull null
            val pathKv = routeMapping.getKeyValueByKey("path") ?: return@mapNotNull null
            val responseKv = routeMapping.getKeyValueByKey("response") ?: return@mapNotNull null
            
            val method = methodKv.valueText ?: return@mapNotNull null
            val path = pathKv.valueText ?: return@mapNotNull null
            val response = extractMockResponse(responseKv.value as? YAMLMapping) ?: return@mapNotNull null
            
            MockRoute(method, path, response)
        }
    }
    
    private fun extractMockResponse(mapping: YAMLMapping?): MockResponse? {
        if (mapping == null) return null
        
        val statusKv = mapping.getKeyValueByKey("status") ?: return null
        val status = statusKv.valueText?.toIntOrNull() ?: return null
        
        val headersKv = mapping.getKeyValueByKey("headers")
        val headers = headersKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val jsonKv = mapping.getKeyValueByKey("json")
        val json = jsonKv?.valueText
        
        return MockResponse(status, headers, json)
    }
    
    private fun extractMockCalls(value: YAMLValue?): List<MockCall>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            val callMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            val mockKv = callMapping.getKeyValueByKey("mock") ?: return@mapNotNull null
            val countKv = callMapping.getKeyValueByKey("count") ?: return@mapNotNull null
            
            val mock = mockKv.valueText ?: return@mapNotNull null
            val count = countKv.valueText?.toIntOrNull() ?: return@mapNotNull null
            
            val expectKv = callMapping.getKeyValueByKey("expect")
            val expect = expectKv?.value?.let { extractMockExpect(it as? YAMLMapping) }
            
            MockCall(mock, count, expect)
        }
    }
    
    private fun extractMockExpect(mapping: YAMLMapping?): MockExpect? {
        if (mapping == null) return null
        
        val methodKv = mapping.getKeyValueByKey("method")
        val method = methodKv?.valueText
        
        val pathKv = mapping.getKeyValueByKey("path")
        val path = pathKv?.valueText
        
        val bodyKv = mapping.getKeyValueByKey("body")
        val body = bodyKv?.value?.let { extractMockBodyExpect(it as? YAMLMapping) }
        
        return MockExpect(method, path, body)
    }
    
    private fun extractMockBodyExpect(mapping: YAMLMapping?): MockBodyExpect? {
        if (mapping == null) return null
        
        val containsKv = mapping.getKeyValueByKey("contains")
        val contains = containsKv?.valueText
        
        return MockBodyExpect(contains)
    }
    
    private fun extractSteps(value: YAMLValue?): List<TestyStep> {
        if (value !is YAMLSequence) return emptyList()
        
        return value.items.mapNotNull { item ->
            val stepMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            val stepOffset = item.textOffset
            
            val nameKv = stepMapping.getKeyValueByKey("name") ?: return@mapNotNull null
            val name = nameKv.valueText ?: return@mapNotNull null
            
            val requestKv = stepMapping.getKeyValueByKey("request") ?: return@mapNotNull null
            val request = extractRequest(requestKv.value as? YAMLMapping) ?: return@mapNotNull null
            
            val responseKv = stepMapping.getKeyValueByKey("response") ?: return@mapNotNull null
            val response = extractResponse(responseKv.value as? YAMLMapping) ?: return@mapNotNull null
            
            val dbChecksKv = stepMapping.getKeyValueByKey("dbChecks")
            val dbChecks = dbChecksKv?.value?.let { extractDbChecks(it) }
            
            TestyStep(name, request, response, dbChecks, stepOffset)
        }
    }
    
    private fun extractRequest(mapping: YAMLMapping?): Request? {
        if (mapping == null) return null
        
        val methodKv = mapping.getKeyValueByKey("method") ?: return null
        val method = methodKv.valueText ?: return null
        
        val pathKv = mapping.getKeyValueByKey("path") ?: return null
        val path = pathKv.valueText ?: return null
        
        val headersKv = mapping.getKeyValueByKey("headers")
        val headers = headersKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val bodyKv = mapping.getKeyValueByKey("body")
        val body = bodyKv?.value?.let { extractBody(it) }
        
        return Request(method, path, headers, body)
    }
    
    private fun extractResponse(mapping: YAMLMapping?): Response? {
        if (mapping == null) return null
        
        val statusKv = mapping.getKeyValueByKey("status") ?: return null
        val status = statusKv.valueText?.toIntOrNull() ?: return null
        
        val headersKv = mapping.getKeyValueByKey("headers")
        val headers = headersKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val jsonKv = mapping.getKeyValueByKey("json")
        val json = jsonKv?.valueText
        
        return Response(status, headers, json)
    }
    
    private fun extractDbChecks(value: YAMLValue?): List<DbCheck>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            val checkMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            val queryKv = checkMapping.getKeyValueByKey("query") ?: return@mapNotNull null
            val resultKv = checkMapping.getKeyValueByKey("result") ?: return@mapNotNull null
            
            val query = queryKv.valueText ?: return@mapNotNull null
            val result = extractBody(resultKv.value)
            
            DbCheck(query, result)
        }
    }
    
    private fun extractStringMap(mapping: YAMLMapping?): Map<String, String>? {
        if (mapping == null) return null
        
        val result = mutableMapOf<String, String>()
        mapping.keyValues.forEach { kv ->
            val key = kv.keyText
            val value = kv.valueText
            if (key != null && value != null) {
                result[key] = value
            }
        }
        return result.ifEmpty { null }
    }
    
    private fun extractBody(value: YAMLValue?): Any? {
        return when (value) {
            is YAMLMapping -> {
                // Return text representation for body
                value.text
            }
            is YAMLSequence -> {
                value.text
            }
            else -> value?.text
        }
    }
}
