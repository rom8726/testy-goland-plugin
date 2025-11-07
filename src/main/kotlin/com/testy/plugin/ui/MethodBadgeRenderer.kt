package com.testy.plugin.ui

import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.JComponent

object MethodBadgeRenderer {
    private val methodColors = mapOf(
        "GET" to Color(0x2196F3),
        "POST" to Color(0x4CAF50),
        "PUT" to Color(0x8BC34A),
        "PATCH" to Color(0x9C27B0),
        "DELETE" to Color(0xF44336),
        "HEAD" to JBColor.GRAY,
        "OPTIONS" to JBColor.GRAY,
        "TRACE" to JBColor.GRAY,
        "CONNECT" to JBColor.GRAY
    )
    
    fun getMethodColor(method: String): Color {
        return methodColors[method.uppercase()] ?: JBColor.GRAY
    }
    
    fun paintBadge(g: Graphics2D, method: String, x: Int, y: Int, height: Int): Int {
        val methodUpper = method.uppercase()
        val color = getMethodColor(methodUpper)
        val textColor = Color.WHITE
        
        val fm = g.fontMetrics
        val textWidth = fm.stringWidth(methodUpper)
        val padding = 6
        val badgeWidth = textWidth + padding * 2
        val badgeHeight = height - 2
        val arc = 4
        
        // Draw rounded rectangle
        g.color = color
        g.fillRoundRect(x, y + 1, badgeWidth, badgeHeight, arc, arc)
        
        // Draw text
        g.color = textColor
        g.font = g.font.deriveFont(Font.BOLD)
        val textY = y + (height + fm.ascent - fm.descent) / 2
        g.drawString(methodUpper, x + padding, textY)
        
        return badgeWidth + 4 // Return width used
    }
}

