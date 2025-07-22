
package com.mohamedbamoh.markmap.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.UIUtil
import com.mohamedbamoh.markmap.utils.MarkmapResourceLoader
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

class MarkmapPreviewComponent {
    private val browser: JBCefBrowser = JBCefBrowser()
    private var isLoaded = false
    private var pendingContent: String? = null
    private var messageBusConnection: MessageBusConnection? = null
    private var currentContent: String = ""

    init {
        setupBrowser()
        setupThemeListener()
    }

    private fun setupBrowser() {
        val html = createMarkmapHtml()
        browser.loadHTML(html)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                isLoaded = true
                pendingContent?.let { content ->
                    updateContent(content)
                    pendingContent = null
                }
            }
        }, browser.cefBrowser)
    }

    private fun setupThemeListener() {
        // Only set up theme listener after application is fully loaded
        ApplicationManager.getApplication().invokeLater({
            try {
                messageBusConnection = ApplicationManager.getApplication().messageBus.connect()
                messageBusConnection?.subscribe(LafManagerListener.TOPIC, LafManagerListener {
                    ApplicationManager.getApplication().invokeLater({
                        reloadWithNewTheme()
                    }, ModalityState.nonModal())
                })
            } catch (e: Exception) {
                // Ignore theme listener setup errors during early initialization
                println("Theme listener setup failed (this is OK during early init): ${e.message}")
            }
        }, ModalityState.nonModal())
    }

    private fun reloadWithNewTheme() {
        isLoaded = false
        val html = createMarkmapHtml()
        browser.loadHTML(html)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                isLoaded = true
                if (currentContent.isNotEmpty()) {
                    updateContent(currentContent)
                }
            }
        }, browser.cefBrowser)
    }

    private fun createMarkmapHtml(): String {
        val resources = MarkmapResourceLoader()
        val d3Content = resources.loadResource("/markmap/d3.min.js")
        val markmapViewContent = resources.loadResource("/markmap/markmap-view.min.js")
        val markmapLibContent = resources.loadResource("/markmap/markmap-lib.min.js")

        // Get IDE theme colors
        val backgroundColor = UIUtil.getPanelBackground()
        val textColor = UIUtil.getLabelForeground()
        val bgColorHex = String.format("#%02x%02x%02x", backgroundColor.red, backgroundColor.green, backgroundColor.blue)
        val textColorHex = String.format("#%02x%02x%02x", textColor.red, textColor.green, textColor.blue)

        // Check if it's dark theme
        val brightness = (backgroundColor.red * 0.299 + backgroundColor.green * 0.587 + backgroundColor.blue * 0.114)
        val isDark = brightness < 128

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Markmap</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
            background-color: $bgColorHex;
            color: $textColorHex;
            overflow: hidden;
        }
        #markmap {
            width: 100vw;
            height: 100vh;
        }
        
        /* Enhanced markmap styles for better visibility */
        .markmap-node > circle {
            stroke: ${if (isDark) "#666" else "#999"};
            stroke-width: 2;
        }
        .markmap-link {
            stroke: ${if (isDark) "#555" else "#bbb"};
            stroke-width: 2;
        }
        .markmap-node text {
            fill: $textColorHex;
        }
        .markmap-node rect {
            fill: ${if (isDark) "#2d2d2d" else "#ffffff"};
            stroke: ${if (isDark) "#555" else "#ddd"};
            stroke-width: 1;
        }
        
        /* Rich content styling */
        .markmap-foreign img {
            max-width: 200px;
            max-height: 150px;
        }
        
        .markmap-foreign table {
            border-collapse: collapse;
            font-size: 12px;
            background: ${if (isDark) "#2d2d2d" else "#ffffff"};
        }
        
        .markmap-foreign th,
        .markmap-foreign td {
            border: 1px solid ${if (isDark) "#555" else "#ddd"};
            padding: 4px 8px;
            color: $textColorHex;
        }
        
        .markmap-foreign th {
            background-color: ${if (isDark) "#3a3a3a" else "#f5f5f5"};
            font-weight: bold;
        }
        
        .markmap-foreign input[type="checkbox"] {
            margin-right: 6px;
        }
        
        .markmap-foreign pre {
            background-color: ${if (isDark) "#1e1e1e" else "#f8f8f8"};
            border: 1px solid ${if (isDark) "#444" else "#e1e1e1"};
            border-radius: 4px;
            padding: 8px;
            font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
            font-size: 12px;
            overflow-x: auto;
            color: $textColorHex;
        }
        
        .markmap-foreign code {
            background-color: ${if (isDark) "#2d2d2d" else "#f1f1f1"};
            border-radius: 2px;
            padding: 2px 4px;
            font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
            color: $textColorHex;
        }
    </style>
</head>
<body>
    <svg id="markmap"></svg>
    
    <script>$d3Content</script>
    <script>$markmapViewContent</script>
    <script>$markmapLibContent</script>
    
    <script>
        try {
            // Find Transformer class
            let TransformerClass = null;
            if (window.markmapLib && window.markmapLib.Transformer) {
                TransformerClass = window.markmapLib.Transformer;
            } else if (window.markmap && window.markmap.Transformer) {
                TransformerClass = window.markmap.Transformer;
            } else if (window.Transformer) {
                TransformerClass = window.Transformer;
            }
            
            if (!TransformerClass) {
                throw new Error('Transformer class not found');
            }
            
            const { Markmap } = markmap;
            const transformer = new TransformerClass();
            const svg = d3.select('#markmap');
            const mm = Markmap.create(svg.node(), {
                colorFreezeLevel: 6,
                duration: 500,
                maxWidth: 300,
                spacingVertical: 8,
                spacingHorizontal: 80,
                autoFit: true,
                pan: true,
                zoom: true,
            });
            
            window.markmapInstance = {
                transformer: transformer,
                mm: mm,
                render: function(content) {
                    try {
                        if (!content || content.trim() === '') {
                            content = '# Welcome to Markmap\\n## Start editing your markdown\\n### The mind map will update automatically';
                        }
                        
                        const { root } = transformer.transform(content);
                        mm.setData(root);
                        mm.fit();
                    } catch (e) {
                        console.error('Render error:', e);
                        // Show error in markmap
                        const errorRoot = {
                            type: 'heading',
                            depth: 1,
                            payload: { lines: [0, 1] },
                            content: 'Error',
                            children: [{
                                type: 'heading',
                                depth: 2,
                                payload: { lines: [1, 2] },
                                content: 'Failed to render: ' + e.message,
                                children: []
                            }]
                        };
                        mm.setData(errorRoot);
                        mm.fit();
                    }
                }
            };
            
        } catch (e) {
            console.error('Initialization error:', e);
            document.body.innerHTML = '<div style="padding: 20px; text-align: center; color: #ff6b6b;">Failed to initialize Markmap: ' + e.message + '</div>';
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    fun updateContent(content: String) {
        currentContent = content

        if (!isLoaded) {
            pendingContent = content
            return
        }

        val escapedContent = content
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        val script = """
            if (window.markmapInstance && window.markmapInstance.render) {
                window.markmapInstance.render('$escapedContent');
            }
        """.trimIndent()

        ApplicationManager.getApplication().invokeLater({
            browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        }, ModalityState.nonModal())
    }

    fun getComponent(): JComponent = browser.component

    fun dispose() {
        messageBusConnection?.disconnect()
        browser.dispose()
    }
}