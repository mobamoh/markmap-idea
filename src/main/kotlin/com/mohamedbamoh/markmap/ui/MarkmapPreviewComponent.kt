package com.mohamedbamoh.markmap.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.messages.MessageBusConnection
import com.mohamedbamoh.markmap.utils.MarkmapTemplateManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.util.*
import javax.swing.JComponent

class MarkmapPreviewComponent {
    private val browser: JBCefBrowser = JBCefBrowser()
    private val templateManager = MarkmapTemplateManager()
    private var isLoaded = false
    private var pendingContent: String? = null
    private var messageBusConnection: MessageBusConnection? = null
    private var lastContent: String = ""

    init {
        setupBrowser()
        setupThemeListener()
    }

    private fun setupBrowser() {
        val html = templateManager.createMarkmapHtml()

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                isLoaded = true

                ApplicationManager.getApplication().invokeLater({
                    Thread.sleep(800)

                    val contentToProcess = pendingContent ?: lastContent
                    if (contentToProcess.isNotEmpty()) {
                        updateContentInternal(contentToProcess)
                    }
                    pendingContent = null
                }, ModalityState.nonModal())
            }
        }, browser.cefBrowser)

        browser.loadHTML(html)
    }

    private fun setupThemeListener() {
        ApplicationManager.getApplication().invokeLater({
            try {
                messageBusConnection = ApplicationManager.getApplication().messageBus.connect()
                messageBusConnection?.subscribe(LafManagerListener.TOPIC, LafManagerListener {
                    ApplicationManager.getApplication().invokeLater({
                        reloadWithNewTheme()
                    }, ModalityState.nonModal())
                })
            } catch (e: Exception) {
                // Silent fail
            }
        }, ModalityState.nonModal())
    }

    private fun reloadWithNewTheme() {
        isLoaded = false
        val html = templateManager.createMarkmapHtml()
        browser.loadHTML(html)
    }

    fun updateContent(content: String) {
        lastContent = content

        if (!isLoaded) {
            pendingContent = content
            return
        }

        updateContentInternal(content)
    }

    private fun updateContentInternal(content: String) {
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val base64Content = Base64.getEncoder().encodeToString(contentBytes)

        val script = """
            try {
                if (typeof updateMarkmap === 'function') {
                    const decodedContent = atob('$base64Content');
                    updateMarkmap(decodedContent);
                }
            } catch (error) {
                console.error('Error updating content:', error);
            }
        """.trimIndent()

        ApplicationManager.getApplication().invokeLater({
            try {
                browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
            } catch (e: Exception) {
                // Silent fail
            }
        }, ModalityState.nonModal())
    }

    fun executeJavaScript(script: String) {
        if (!isLoaded) return

        ApplicationManager.getApplication().invokeLater({
            try {
                browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
            } catch (e: Exception) {
                // Silent fail
            }
        }, ModalityState.nonModal())
    }

    fun getComponent(): JComponent = browser.component
    fun getBrowserComponent(): JComponent = browser.component

    fun dispose() {
        messageBusConnection?.disconnect()
        browser.dispose()
    }
}