package com.testy.plugin

import org.junit.Assert.*
import org.junit.Test

class TestySchemaValidatorTest {
    
    @Test
    fun `validate valid yaml scenario`() {
        val yaml = """
            - name: Test Scenario
              steps:
                - name: step1
                  request:
                    method: GET
                    path: /api/test
                  response:
                    status: 200
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("Valid YAML should have no errors", errors.isEmpty())
    }
    
    @Test
    fun `validate yaml with missing required field`() {
        val yaml = """
            - name: Test Scenario
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("YAML missing required 'steps' field should have errors", errors.isNotEmpty())
        assertTrue("Should have error about missing steps", 
            errors.any { it.message.contains("steps", ignoreCase = true) })
    }
    
    @Test
    fun `validate yaml with invalid status code`() {
        val yaml = """
            - name: Test Scenario
              steps:
                - name: step1
                  request:
                    method: GET
                    path: /api/test
                  response:
                    status: 999
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("YAML with invalid status code should have errors", errors.isNotEmpty())
    }
    
    @Test
    fun `validate yaml with invalid http method`() {
        val yaml = """
            - name: Test Scenario
              steps:
                - name: step1
                  request:
                    method: INVALID
                    path: /api/test
                  response:
                    status: 200
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("YAML with invalid HTTP method should have errors", errors.isNotEmpty())
    }
    
    @Test
    fun `validate yaml with missing step name`() {
        val yaml = """
            - name: Test Scenario
              steps:
                - request:
                    method: GET
                    path: /api/test
                  response:
                    status: 200
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("YAML with missing step name should have errors", errors.isNotEmpty())
    }
    
    @Test
    fun `validate yaml with multiple scenarios`() {
        val yaml = """
            - name: Scenario 1
              steps:
                - name: step1
                  request:
                    method: GET
                    path: /api/test1
                  response:
                    status: 200
            - name: Scenario 2
              steps:
                - name: step2
                  request:
                    method: POST
                    path: /api/test2
                  response:
                    status: 201
        """.trimIndent()
        
        val errors = TestySchemaValidator.validate(yaml)
        assertTrue("Valid YAML with multiple scenarios should have no errors", errors.isEmpty())
    }
}

