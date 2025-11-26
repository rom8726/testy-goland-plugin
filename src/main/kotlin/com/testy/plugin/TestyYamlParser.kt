package com.testy.plugin

import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.*

data class TestyScenario(
    val name: String,
    val variables: Map<String, Any?>? = null,
    val fixtures: List<String>? = null,
    val setup: List<Hook>? = null,
    val teardown: List<Hook>? = null,
    val mockServers: Map<String, MockServer>? = null,
    val mockCalls: List<MockCall>? = null,
    val steps: List<TestyStep> = emptyList(),
    val offset: Int
)

data class Hook(
    val name: String? = null,
    val sql: String? = null,
    val http: HookHttp? = null
)

data class HookHttp(
    val method: String,
    val path: String,
    val headers: Map<String, String>? = null,
    val body: Any? = null
)

sealed class TestyStep {
    abstract val name: String
    abstract val condition: String?
    abstract val loop: Loop?
    abstract val retry: Retry?
    abstract val performance: Performance?
    abstract val dbChecks: List<DbCheck>?
    abstract val offset: Int

    data class HttpStep(
        override val name: String,
        override val condition: String? = null,
        override val loop: Loop? = null,
        override val retry: Retry? = null,
        override val performance: Performance? = null,
        val request: Request,
        val response: Response,
        override val dbChecks: List<DbCheck>? = null,
        override val offset: Int
    ) : TestyStep()

    data class GrpcStep(
        override val name: String,
        override val condition: String? = null,
        override val loop: Loop? = null,
        override val retry: Retry? = null,
        override val performance: Performance? = null,
        val grpcRequest: GrpcRequest,
        val grpcResponse: GrpcResponse,
        override val dbChecks: List<DbCheck>? = null,
        override val offset: Int
    ) : TestyStep()
}

data class Loop(
    val items: List<Any>? = null,
    val variable: String,
    val range: LoopRange? = null
)

data class LoopRange(
    val from: Int,
    val to: Int,
    val step: Int = 1
)

data class Retry(
    val attempts: Int,
    val backoff: String? = null,
    val initialDelay: String? = null,
    val maxDelay: String? = null,
    val retryOn: List<Int>? = null,
    val retryOnError: Boolean = false
)

data class Performance(
    val maxDuration: String? = null,
    val warnDuration: String? = null,
    val failOnWarning: Boolean = false,
    val maxMemory: Int? = null,
    val minThroughput: Int? = null
)

data class Request(
    val method: String,
    val path: String,
    val headers: Map<String, String>? = null,
    val body: Any? = null,
    val bodyFile: String? = null,
    val bodyRaw: String? = null
)

data class Response(
    val status: Int,
    val headers: Map<String, String>? = null,
    val json: String? = null,
    val text: String? = null,
    val schema: String? = null,
    val jsonSchema: Any? = null,
    val assertions: List<Assertion>? = null
)

data class GrpcRequest(
    val service: String,
    val method: String,
    val message: Map<String, Any?>? = null,
    val metadata: Map<String, String>? = null
)

data class GrpcResponse(
    val code: String,
    val message: String? = null,
    val metadata: Map<String, String>? = null,
    val assertions: List<Assertion>? = null
)

data class Assertion(
    val path: String,
    val operator: String,
    val value: Any? = null,
    val message: String? = null
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
                
                val variablesKv = scenarioMapping.getKeyValueByKey("variables")
                val variables = variablesKv?.value?.let { extractAnyMap(it as? YAMLMapping) }
                
                val fixturesKv = scenarioMapping.getKeyValueByKey("fixtures")
                val fixtures = fixturesKv?.value?.let { extractStringList(it) }
                
                val setupKv = scenarioMapping.getKeyValueByKey("setup")
                val setup = setupKv?.value?.let { extractHooks(it) }
                
                val teardownKv = scenarioMapping.getKeyValueByKey("teardown")
                val teardown = teardownKv?.value?.let { extractHooks(it) }
                
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
                    variables = variables,
                    fixtures = fixtures,
                    setup = setup,
                    teardown = teardown,
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
    
    private fun extractHooks(value: YAMLValue?): List<Hook>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            val hookMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            
            val nameKv = hookMapping.getKeyValueByKey("name")
            val name = nameKv?.valueText
            
            val sqlKv = hookMapping.getKeyValueByKey("sql")
            val sql = sqlKv?.valueText
            
            val httpKv = hookMapping.getKeyValueByKey("http")
            val http = httpKv?.value?.let { extractHookHttp(it as? YAMLMapping) }
            
            Hook(name, sql, http)
        }
    }
    
    private fun extractHookHttp(mapping: YAMLMapping?): HookHttp? {
        if (mapping == null) return null
        
        val methodKv = mapping.getKeyValueByKey("method") ?: return null
        val method = methodKv.valueText ?: return null
        
        val pathKv = mapping.getKeyValueByKey("path") ?: return null
        val path = pathKv.valueText ?: return null
        
        val headersKv = mapping.getKeyValueByKey("headers")
        val headers = headersKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val bodyKv = mapping.getKeyValueByKey("body")
        val body = bodyKv?.value?.let { extractBody(it) }
        
        return HookHttp(method, path, headers, body)
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
            
            val conditionKv = stepMapping.getKeyValueByKey("when")
            val condition = conditionKv?.valueText
            
            val loopKv = stepMapping.getKeyValueByKey("loop")
            val loop = loopKv?.value?.let { extractLoop(it as? YAMLMapping) }
            
            val retryKv = stepMapping.getKeyValueByKey("retry")
            val retry = retryKv?.value?.let { extractRetry(it as? YAMLMapping) }
            
            val performanceKv = stepMapping.getKeyValueByKey("performance")
            val performance = performanceKv?.value?.let { extractPerformance(it as? YAMLMapping) }
            
            val dbChecksKv = stepMapping.getKeyValueByKey("dbChecks")
            val dbChecks = dbChecksKv?.value?.let { extractDbChecks(it) }
            
            val requestKv = stepMapping.getKeyValueByKey("request")
            val responseKv = stepMapping.getKeyValueByKey("response")
            val grpcRequestKv = stepMapping.getKeyValueByKey("grpcRequest")
            val grpcResponseKv = stepMapping.getKeyValueByKey("grpcResponse")
            
            when {
                requestKv != null && responseKv != null -> {
                    val request = extractRequest(requestKv.value as? YAMLMapping) ?: return@mapNotNull null
                    val response = extractResponse(responseKv.value as? YAMLMapping) ?: return@mapNotNull null
                    TestyStep.HttpStep(name, condition, loop, retry, performance, request, response, dbChecks, stepOffset)
                }
                grpcRequestKv != null && grpcResponseKv != null -> {
                    val grpcRequest = extractGrpcRequest(grpcRequestKv.value as? YAMLMapping) ?: return@mapNotNull null
                    val grpcResponse = extractGrpcResponse(grpcResponseKv.value as? YAMLMapping) ?: return@mapNotNull null
                    TestyStep.GrpcStep(name, condition, loop, retry, performance, grpcRequest, grpcResponse, dbChecks, stepOffset)
                }
                else -> null
            }
        }
    }
    
    private fun extractLoop(mapping: YAMLMapping?): Loop? {
        if (mapping == null) return null
        
        val varKv = mapping.getKeyValueByKey("var") ?: return null
        val variable = varKv.valueText ?: return null
        
        val itemsKv = mapping.getKeyValueByKey("items")
        val items = itemsKv?.value?.let { extractAnyList(it) }
        
        val rangeKv = mapping.getKeyValueByKey("range")
        val range = rangeKv?.value?.let { extractLoopRange(it as? YAMLMapping) }
        
        return Loop(items, variable, range)
    }
    
    private fun extractLoopRange(mapping: YAMLMapping?): LoopRange? {
        if (mapping == null) return null
        
        val fromKv = mapping.getKeyValueByKey("from") ?: return null
        val from = fromKv.valueText?.toIntOrNull() ?: return null
        
        val toKv = mapping.getKeyValueByKey("to") ?: return null
        val to = toKv.valueText?.toIntOrNull() ?: return null
        
        val stepKv = mapping.getKeyValueByKey("step")
        val step = stepKv?.valueText?.toIntOrNull() ?: 1
        
        return LoopRange(from, to, step)
    }
    
    private fun extractRetry(mapping: YAMLMapping?): Retry? {
        if (mapping == null) return null
        
        val attemptsKv = mapping.getKeyValueByKey("attempts") ?: return null
        val attempts = attemptsKv.valueText?.toIntOrNull() ?: return null
        
        val backoffKv = mapping.getKeyValueByKey("backoff")
        val backoff = backoffKv?.valueText
        
        val initialDelayKv = mapping.getKeyValueByKey("initialDelay")
        val initialDelay = initialDelayKv?.valueText
        
        val maxDelayKv = mapping.getKeyValueByKey("maxDelay")
        val maxDelay = maxDelayKv?.valueText
        
        val retryOnKv = mapping.getKeyValueByKey("retryOn")
        val retryOn = retryOnKv?.value?.let { extractIntList(it) }
        
        val retryOnErrorKv = mapping.getKeyValueByKey("retryOnError")
        val retryOnError = retryOnErrorKv?.valueText?.toBooleanStrictOrNull() ?: false
        
        return Retry(attempts, backoff, initialDelay, maxDelay, retryOn, retryOnError)
    }
    
    private fun extractPerformance(mapping: YAMLMapping?): Performance? {
        if (mapping == null) return null
        
        val maxDurationKv = mapping.getKeyValueByKey("maxDuration")
        val maxDuration = maxDurationKv?.valueText
        
        val warnDurationKv = mapping.getKeyValueByKey("warnDuration")
        val warnDuration = warnDurationKv?.valueText
        
        val failOnWarningKv = mapping.getKeyValueByKey("failOnWarning")
        val failOnWarning = failOnWarningKv?.valueText?.toBooleanStrictOrNull() ?: false
        
        val maxMemoryKv = mapping.getKeyValueByKey("maxMemory")
        val maxMemory = maxMemoryKv?.valueText?.toIntOrNull()
        
        val minThroughputKv = mapping.getKeyValueByKey("minThroughput")
        val minThroughput = minThroughputKv?.valueText?.toIntOrNull()
        
        return Performance(maxDuration, warnDuration, failOnWarning, maxMemory, minThroughput)
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
        
        val bodyFileKv = mapping.getKeyValueByKey("bodyFile")
        val bodyFile = bodyFileKv?.valueText
        
        val bodyRawKv = mapping.getKeyValueByKey("bodyRaw")
        val bodyRaw = bodyRawKv?.valueText
        
        return Request(method, path, headers, body, bodyFile, bodyRaw)
    }
    
    private fun extractResponse(mapping: YAMLMapping?): Response? {
        if (mapping == null) return null
        
        val statusKv = mapping.getKeyValueByKey("status") ?: return null
        val status = statusKv.valueText?.toIntOrNull() ?: return null
        
        val headersKv = mapping.getKeyValueByKey("headers")
        val headers = headersKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val jsonKv = mapping.getKeyValueByKey("json")
        val json = jsonKv?.valueText
        
        val textKv = mapping.getKeyValueByKey("text")
        val text = textKv?.valueText
        
        val schemaKv = mapping.getKeyValueByKey("schema")
        val schema = schemaKv?.valueText
        
        val jsonSchemaKv = mapping.getKeyValueByKey("jsonSchema")
        val jsonSchema = jsonSchemaKv?.value?.let { extractBody(it) }
        
        val assertionsKv = mapping.getKeyValueByKey("assertions")
        val assertions = assertionsKv?.value?.let { extractAssertions(it) }
        
        return Response(status, headers, json, text, schema, jsonSchema, assertions)
    }
    
    private fun extractGrpcRequest(mapping: YAMLMapping?): GrpcRequest? {
        if (mapping == null) return null
        
        val serviceKv = mapping.getKeyValueByKey("service") ?: return null
        val service = serviceKv.valueText ?: return null
        
        val methodKv = mapping.getKeyValueByKey("method") ?: return null
        val method = methodKv.valueText ?: return null
        
        val messageKv = mapping.getKeyValueByKey("message")
        val message = messageKv?.value?.let { extractAnyMap(it as? YAMLMapping) }
        
        val metadataKv = mapping.getKeyValueByKey("metadata")
        val metadata = metadataKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        return GrpcRequest(service, method, message, metadata)
    }
    
    private fun extractGrpcResponse(mapping: YAMLMapping?): GrpcResponse? {
        if (mapping == null) return null
        
        val codeKv = mapping.getKeyValueByKey("code") ?: return null
        val code = codeKv.valueText ?: return null
        
        val messageKv = mapping.getKeyValueByKey("message")
        val message = messageKv?.valueText
        
        val metadataKv = mapping.getKeyValueByKey("metadata")
        val metadata = metadataKv?.value?.let { extractStringMap(it as? YAMLMapping) }
        
        val assertionsKv = mapping.getKeyValueByKey("assertions")
        val assertions = assertionsKv?.value?.let { extractAssertions(it) }
        
        return GrpcResponse(code, message, metadata, assertions)
    }
    
    private fun extractAssertions(value: YAMLValue?): List<Assertion>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            val assertionMapping = item.value as? YAMLMapping ?: return@mapNotNull null
            
            val pathKv = assertionMapping.getKeyValueByKey("path") ?: return@mapNotNull null
            val path = pathKv.valueText ?: return@mapNotNull null
            
            val operatorKv = assertionMapping.getKeyValueByKey("operator") ?: return@mapNotNull null
            val operator = operatorKv.valueText ?: return@mapNotNull null
            
            val valueKv = assertionMapping.getKeyValueByKey("value")
            val assertionValue = valueKv?.value?.let { extractBody(it) }
            
            val messageKv = assertionMapping.getKeyValueByKey("message")
            val message = messageKv?.valueText
            
            Assertion(path, operator, assertionValue, message)
        }
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
            if (key.isNotEmpty() && value != null) {
                result[key] = value
            }
        }
        return result.ifEmpty { null }
    }
    
    private fun extractAnyMap(mapping: YAMLMapping?): Map<String, Any?>? {
        if (mapping == null) return null
        
        val result = mutableMapOf<String, Any?>()
        mapping.keyValues.forEach { kv ->
            val key = kv.keyText ?: return@forEach
            val value = kv.value?.let { extractBody(it) }
            result[key] = value
        }
        return result.ifEmpty { null }
    }
    
    private fun extractAnyList(value: YAMLValue?): List<Any>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            item.value?.let { extractBody(it) }
        }
    }
    
    private fun extractIntList(value: YAMLValue?): List<Int>? {
        if (value !is YAMLSequence) return null
        
        return value.items.mapNotNull { item ->
            item.value?.text?.trim()?.toIntOrNull()
        }
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
