package com.mohamedbamoh.markmap.utils

import com.intellij.util.ui.UIUtil
import java.io.IOException

class MarkmapTemplateManager {
    private val resourceLoader = MarkmapResourceLoader()

    fun createMarkmapHtml(): String {
        return try {
            val template = resourceLoader.loadResource("/markmap/template.html")

            // Get proper IntelliJ theme colors
            val backgroundColor = UIUtil.getPanelBackground()
            val textColor = UIUtil.getLabelForeground()

            val bgColorHex = String.format("#%02x%02x%02x",
                backgroundColor.red, backgroundColor.green, backgroundColor.blue)
            val textColorHex = String.format("#%02x%02x%02x",
                textColor.red, textColor.green, textColor.blue)

            // Apply theme colors to body, container, and markmap text
            val processedTemplate = template
                .replace("background: #ffffff;", "background: $bgColorHex;")
                .replace("color: #000000;", "color: $textColorHex;")
                .replace(".markmap-svg text {\n            fill: #000000 !important;\n        }",
                    ".markmap-svg text { fill: $textColorHex !important; }")
                .replace(".markmap-svg .markmap-foreign {\n            color: #000000 !important;\n        }",
                    ".markmap-svg .markmap-foreign { color: $textColorHex !important; }")

            processedTemplate

        } catch (e: IOException) {
            createFallbackHtml(e.message ?: "Unknown error")
        }
    }

    private fun createFallbackHtml(error: String): String {
        val backgroundColor = UIUtil.getPanelBackground()
        val textColor = UIUtil.getLabelForeground()
        val bgColorHex = String.format("#%02x%02x%02x",
            backgroundColor.red, backgroundColor.green, backgroundColor.blue)
        val textColorHex = String.format("#%02x%02x%02x",
            textColor.red, textColor.green, textColor.blue)

        return """
            <!DOCTYPE html>
            <html>
            <head><title>Markmap Error</title>
            <style>body { background: $bgColorHex; color: $textColorHex; font-family: system-ui; padding: 20px; }</style>
            </head>
            <body>
                <h1>Failed to Load Markmap</h1>
                <p>Error: $error</p>
                <p>Please check that the markmap resources are properly loaded.</p>
            </body>
            </html>
        """.trimIndent()
    }
}