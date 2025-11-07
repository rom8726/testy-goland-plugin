package com.testy.plugin

object TestyDataFormatter {
    fun formatJson(jsonString: String?): String {
        if (jsonString == null || jsonString.isBlank()) return ""
        
        return try {
            val json = com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonString)
            com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(json)
        } catch (e: Exception) {
            jsonString
        }
    }
    
    fun formatSql(query: String): String {
        // Simple SQL formatting - add line breaks before keywords
        return query
            .replace("(?i)\\bSELECT\\b".toRegex(), "\nSELECT")
            .replace("(?i)\\bFROM\\b".toRegex(), "\nFROM")
            .replace("(?i)\\bWHERE\\b".toRegex(), "\nWHERE")
            .replace("(?i)\\bJOIN\\b".toRegex(), "\nJOIN")
            .replace("(?i)\\bORDER BY\\b".toRegex(), "\nORDER BY")
            .replace("(?i)\\bGROUP BY\\b".toRegex(), "\nGROUP BY")
            .trim()
    }
    
    fun truncate(text: String, maxLength: Int = 200): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength) + "..."
    }
    
    fun formatBody(body: Any?): String {
        if (body == null) return ""
        
        val bodyString = when (body) {
            is String -> body
            else -> body.toString()
        }
        
        // Try to format as JSON if it looks like JSON
        if (bodyString.trim().startsWith("{") || bodyString.trim().startsWith("[")) {
            return formatJson(bodyString)
        }
        
        return bodyString
    }
    
    fun formatHeaders(headers: Map<String, String>?): String {
        if (headers == null || headers.isEmpty()) return "No headers"
        
        return headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }
}

