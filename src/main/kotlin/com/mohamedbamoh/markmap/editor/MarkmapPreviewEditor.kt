
package com.mohamedbamoh.markmap.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class MarkmapPreviewEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {
    private val browser: JBCefBrowser = JBCefBrowser()
    private var document: Document? = null
    private var isLoaded = false

    init {
        val html = createMarkmapHtml()
        browser.loadHTML(html)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                println("Markmap page loaded with status: $httpStatusCode")
                isLoaded = true
                updatePreview()
            }
        }, browser.cefBrowser)

        ApplicationManager.getApplication().invokeLater({
            document = FileDocumentManager.getInstance().getDocument(file)
            document?.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    if (isLoaded) {
                        updatePreview()
                    }
                }
            })
        }, ModalityState.nonModal())
    }

    private fun createMarkmapHtml(): String {
        // Load the JavaScript files from resources
        val d3Result = loadResourceWithStatus("/d3.min.js")
        val markmapViewResult = loadResourceWithStatus("/markmap-view.min.js")
        val markmapLibResult = loadResourceWithStatus("/markmap-lib.min.js")

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
            font-family: Arial, sans-serif;
            background-color: #ffffff;
            overflow: hidden;
        }
        #markmap {
            width: 100vw;
            height: 100vh;
        }
        .debug-overlay {
            position: absolute;
            top: 10px;
            left: 10px;
            background-color: rgba(255, 255, 255, 0.9);
            padding: 10px;
            border-radius: 5px;
            font-size: 12px;
            max-width: 300px;
            z-index: 1000;
            border: 1px solid #ccc;
        }
    </style>
</head>
<body>
    <div id="debug" class="debug-overlay">Loading...</div>
    <svg id="markmap"></svg>
    
    <!-- Load D3 -->
    <script>
        ${d3Result.content}
        document.getElementById('debug').innerHTML = 'D3 loaded: ' + typeof d3;
    </script>
    
    <!-- Load Markmap View -->
    <script>
        ${markmapViewResult.content}
        document.getElementById('debug').innerHTML += '<br>Markmap View loaded: ' + typeof markmap;
    </script>
    
    <!-- Load Markmap Lib and inspect what's available -->
    <script>
        // Capture global variables before loading markmap-lib
        const beforeGlobals = Object.keys(window);
        
        ${markmapLibResult.content}
        
        // Find new global variables
        const afterGlobals = Object.keys(window);
        const newGlobals = afterGlobals.filter(key => !beforeGlobals.includes(key));
        
        document.getElementById('debug').innerHTML += '<br>New globals: ' + newGlobals.join(', ');
        document.getElementById('debug').innerHTML += '<br>markmapLib: ' + typeof markmapLib;
        
        // Check for other possible names
        const possibleNames = ['markmapLib', 'markmap', 'MarkmapLib', 'Transformer', 'transform'];
        possibleNames.forEach(name => {
            if (window[name]) {
                document.getElementById('debug').innerHTML += '<br>' + name + ': ' + typeof window[name];
            }
        });
        
        // Try to find Transformer class
        let TransformerClass = null;
        if (window.markmapLib && window.markmapLib.Transformer) {
            TransformerClass = window.markmapLib.Transformer;
        } else if (window.markmap && window.markmap.Transformer) {
            TransformerClass = window.markmap.Transformer;
        } else if (window.Transformer) {
            TransformerClass = window.Transformer;
        }
        
        document.getElementById('debug').innerHTML += '<br>Transformer found: ' + (TransformerClass ? 'YES' : 'NO');
        
        // Initialize Markmap
        try {
            if (typeof d3 === 'undefined') throw new Error('D3 not available');
            if (typeof markmap === 'undefined') throw new Error('markmap not available');
            if (!TransformerClass) throw new Error('Transformer class not found');
            
            const { Markmap } = markmap;
            
            const transformer = new TransformerClass();
            const svg = d3.select('#markmap');
            const mm = Markmap.create(svg.node());
            
            document.getElementById('debug').innerHTML += '<br><span style="color: green;">Initialized successfully!</span>';
            
            window.markmapInstance = {
                transformer: transformer,
                mm: mm,
                render: function(content) {
                    try {
                        document.getElementById('debug').innerHTML = 'Rendering...';
                        const { root } = transformer.transform(content);
                        mm.setData(root);
                        mm.fit();
                        document.getElementById('debug').innerHTML = '<span style="color: green;">Ready</span>';
                    } catch (e) {
                        document.getElementById('debug').innerHTML = '<span style="color: red;">Render Error: ' + e.message + '</span>';
                        console.error('Render error:', e);
                    }
                }
            };
            
            // Test render
            window.markmapInstance.render('# Welcome\\n## To Markmap\\n### It works!');
            
        } catch (e) {
            document.getElementById('debug').innerHTML += '<br><span style="color: red;">Init Error: ' + e.message + '</span>';
            console.error('Initialization error:', e);
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    data class ResourceResult(
        val success: Boolean,
        val content: String,
        val size: Int,
        val error: String?
    )

    private fun loadResourceWithStatus(resourcePath: String): ResourceResult {
        return try {
            println("Attempting to load resource: $resourcePath")

            // Try different possible paths
            val possiblePaths = listOf(
                resourcePath,
                "/markmap.static${resourcePath.substring(1)}",
                "/markmap/static${resourcePath.substring(1)}"
            )

            var content: String? = null

            for (path in possiblePaths) {
                try {
                    val stream = javaClass.getResourceAsStream(path)
                    if (stream != null) {
                        content = stream.bufferedReader().readText()
                        println("Successfully loaded resource from path: $path")
                        break
                    }
                } catch (e: Exception) {
                    println("Failed to load from path $path: ${e.message}")
                }
            }

            if (content != null) {
                ResourceResult(
                    success = true,
                    content = content,
                    size = content.length,
                    error = null
                )
            } else {
                ResourceResult(
                    success = false,
                    content = "",
                    size = 0,
                    error = "Resource not found in any of the attempted paths: $possiblePaths"
                )
            }
        } catch (e: Exception) {
            ResourceResult(
                success = false,
                content = "",
                size = 0,
                error = e.message
            )
        }
    }

    private fun updatePreview() {
        val content = document?.text ?: "# Empty Document\\n## No content available"
        println("Updating markmap with content length: ${content.length}")

        val escapedContent = content
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        val script = """
            if (window.markmapInstance && window.markmapInstance.render) {
                window.markmapInstance.render('$escapedContent');
            } else {
                console.warn('Markmap instance not ready yet');
            }
        """.trimIndent()

        ApplicationManager.getApplication().invokeLater({
            browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        }, ModalityState.nonModal())
    }

    @NotNull
    override fun getComponent(): JComponent = browser.component

    @Nullable
    override fun getPreferredFocusedComponent(): JComponent? = browser.component

    @NotNull
    override fun getName(): String = "Markmap"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    @Nullable
    override fun getCurrentLocation(): FileEditorLocation? = null

    @NotNull
    override fun getFile(): VirtualFile = file

    override fun dispose() {
        browser.dispose()
    }
}