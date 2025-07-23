package com.mohamedbamoh.markmap.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.mohamedbamoh.markmap.ui.MarkmapPreviewComponent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Composite editor that combines text editor with markmap preview
 */
class MarkmapCompositeEditor(
    private val textEditor: TextEditor
) : UserDataHolderBase(), FileEditor {

    private val mainPanel: JPanel = JPanel(BorderLayout())
    private val previewComponent = MarkmapPreviewComponent()
    private var currentMode = ViewMode.SPLIT
    private var splitter: JBSplitter? = null

    enum class ViewMode {
        EDITOR_ONLY,
        SPLIT,
        PREVIEW_ONLY
    }

    init {
        setupContentSync()
        updateLayout()
    }

    /**
     * Sync text editor changes with preview component
     */
    private fun setupContentSync() {
        ApplicationManager.getApplication().invokeLater({
            val document = textEditor.editor.document

            // Listen for document changes - IMMEDIATE UPDATES like playground
            document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    ApplicationManager.getApplication().invokeLater({
                        val content = document.text
                        previewComponent.updateContent(content)
                    }, ModalityState.nonModal())
                }
            })

            // Initial sync
            val initialContent = document.text
            previewComponent.updateContent(initialContent)

        }, ModalityState.nonModal())
    }

    private fun createCombinedToolbar(): JComponent {
        val actionGroup = DefaultActionGroup()

        // View mode controls
        actionGroup.add(object :
            ToggleAction("Show Editor Only", "Show editor only", AllIcons.General.LayoutEditorOnly), DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.EDITOR_ONLY
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.EDITOR_ONLY
                    updateLayout()
                }
            }
        })

        actionGroup.add(object :
            ToggleAction("Show Editor and Preview", "Show editor and preview", AllIcons.General.LayoutEditorPreview),
            DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.SPLIT
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.SPLIT
                    updateLayout()
                }
            }
        })

        actionGroup.add(object :
            ToggleAction("Show Preview Only", "Show preview only", AllIcons.General.LayoutPreviewOnly), DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.PREVIEW_ONLY
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.PREVIEW_ONLY
                    updateLayout()
                }
            }
        })

        // Add separator between view controls and map controls
        actionGroup.addSeparator()

        // Mind map controls - only show when preview is visible (simplified to working controls only)
        if (currentMode == ViewMode.SPLIT || currentMode == ViewMode.PREVIEW_ONLY) {
            // Center/Fit action
            actionGroup.add(object : AnAction("Center View", "Center and fit the mind map", AllIcons.Graph.ActualZoom),
                DumbAware {
                override fun actionPerformed(e: AnActionEvent) {
                    executeJavaScript("if (typeof centerMap === 'function') centerMap();")
                }
            })

            // Zoom In
            actionGroup.add(object : AnAction("Zoom In", "Zoom in", AllIcons.Graph.ZoomIn), DumbAware {
                override fun actionPerformed(e: AnActionEvent) {
                    executeJavaScript("if (typeof zoomIn === 'function') zoomIn();")
                }
            })

            // Zoom Out
            actionGroup.add(object : AnAction("Zoom Out", "Zoom out", AllIcons.Graph.ZoomOut), DumbAware {
                override fun actionPerformed(e: AnActionEvent) {
                    executeJavaScript("if (typeof zoomOut === 'function') zoomOut();")
                }
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("MarkmapEditor", actionGroup, true)
        toolbar.targetComponent = mainPanel
        toolbar.component.isOpaque = false
        return toolbar.component
    }

    private fun executeJavaScript(script: String) {
        previewComponent.executeJavaScript(script)
    }

    private fun updateLayout() {
        mainPanel.removeAll()

        when (currentMode) {
            ViewMode.EDITOR_ONLY -> {
                val editorPanel = JPanel(BorderLayout())
                editorPanel.add(textEditor.component, BorderLayout.CENTER)

                val topPanel = JPanel(BorderLayout())
                topPanel.add(createCombinedToolbar(), BorderLayout.EAST)
                topPanel.isOpaque = false

                mainPanel.add(topPanel, BorderLayout.NORTH)
                mainPanel.add(editorPanel, BorderLayout.CENTER)
            }

            ViewMode.PREVIEW_ONLY -> {
                // For preview only, get the raw browser component without additional toolbar
                val previewPanel = JPanel(BorderLayout())
                previewPanel.add(previewComponent.getBrowserComponent(), BorderLayout.CENTER)

                val topPanel = JPanel(BorderLayout())
                topPanel.add(createCombinedToolbar(), BorderLayout.EAST)
                topPanel.isOpaque = false

                mainPanel.add(topPanel, BorderLayout.NORTH)
                mainPanel.add(previewPanel, BorderLayout.CENTER)
            }

            ViewMode.SPLIT -> {
                splitter = JBSplitter(false, 0.5f).apply {
                    firstComponent = textEditor.component
                    // For split mode, get the raw browser component without additional toolbar
                    secondComponent = previewComponent.getBrowserComponent()
                    setDividerWidth(1)
                }

                val topPanel = JPanel(BorderLayout())
                topPanel.add(createCombinedToolbar(), BorderLayout.EAST)
                topPanel.isOpaque = false

                mainPanel.add(topPanel, BorderLayout.NORTH)
                mainPanel.add(splitter, BorderLayout.CENTER)
            }
        }

        mainPanel.revalidate()
        mainPanel.repaint()
    }

    @NotNull
    override fun getComponent(): JComponent = mainPanel

    @Nullable
    override fun getPreferredFocusedComponent(): JComponent? = when (currentMode) {
        ViewMode.EDITOR_ONLY, ViewMode.SPLIT -> textEditor.preferredFocusedComponent
        ViewMode.PREVIEW_ONLY -> previewComponent.getComponent()
    }

    @NotNull
    override fun getName(): String = "Markmap"

    override fun setState(state: FileEditorState) {
        if (state is MarkmapCompositeEditorState) {
            currentMode = state.viewMode
            textEditor.setState(state.textState)
            updateLayout()
        }
    }

    @NotNull
    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return MarkmapCompositeEditorState(currentMode, textEditor.getState(level))
    }

    override fun isModified(): Boolean = textEditor.isModified
    override fun isValid(): Boolean = textEditor.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.removePropertyChangeListener(listener)
    }

    @Nullable
    override fun getCurrentLocation(): FileEditorLocation? = textEditor.currentLocation

    @NotNull
    override fun getFile(): VirtualFile = textEditor.file

    override fun dispose() {
        previewComponent.dispose()
        textEditor.dispose()
    }
}

data class MarkmapCompositeEditorState(
    val viewMode: MarkmapCompositeEditor.ViewMode,
    val textState: FileEditorState
) : FileEditorState {
    override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean {
        return otherState is MarkmapCompositeEditorState &&
                textState.canBeMergedWith(otherState.textState, level)
    }
}