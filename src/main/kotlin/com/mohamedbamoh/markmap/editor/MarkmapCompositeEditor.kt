package com.mohamedbamoh.markmap.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.*

/**
 * Composite editor that provides three viewing modes like the default markdown editor
 */
class MarkmapCompositeEditor(
    private val textEditor: TextEditor,
    private val previewEditor: MarkmapPreviewEditor
) : UserDataHolderBase(), FileEditor {

    private val mainPanel: JPanel = JPanel(BorderLayout())
    private var currentMode = ViewMode.SPLIT
    private var splitter: JBSplitter? = null

    enum class ViewMode {
        EDITOR_ONLY,
        SPLIT,
        PREVIEW_ONLY
    }

    init {
        updateLayout()
    }

    private fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup()

        // Editor only action
        actionGroup.add(object : ToggleAction("Show Editor Only", "Show editor only", AllIcons.General.LayoutEditorOnly), DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.EDITOR_ONLY

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.EDITOR_ONLY
                    updateLayout()
                }
            }
        })

        // Split action
        actionGroup.add(object : ToggleAction("Show Editor and Preview", "Show editor and preview", AllIcons.General.LayoutEditorPreview), DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.SPLIT

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.SPLIT
                    updateLayout()
                }
            }
        })

        // Preview only action
        actionGroup.add(object : ToggleAction("Show Preview Only", "Show preview only", AllIcons.General.LayoutPreviewOnly), DumbAware {
            override fun isSelected(e: AnActionEvent): Boolean = currentMode == ViewMode.PREVIEW_ONLY

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                if (state) {
                    currentMode = ViewMode.PREVIEW_ONLY
                    updateLayout()
                }
            }
        })

        val toolbar = ActionManager.getInstance().createActionToolbar(
            "MarkmapEditor", actionGroup, true
        )

        // Fix the warning by setting target component
        toolbar.targetComponent = mainPanel

        // Position toolbar on the right like default markdown editor
        val toolbarComponent = toolbar.component
        toolbarComponent.isOpaque = false

        return toolbarComponent
    }

    private fun updateLayout() {
        mainPanel.removeAll()

        when (currentMode) {
            ViewMode.EDITOR_ONLY -> {
                // Create wrapper with toolbar in top-right corner
                val editorPanel = JPanel(BorderLayout())
                editorPanel.add(textEditor.component, BorderLayout.CENTER)

                val topPanel = JPanel(BorderLayout())
                topPanel.add(createToolbar(), BorderLayout.EAST)
                topPanel.isOpaque = false

                mainPanel.add(topPanel, BorderLayout.NORTH)
                mainPanel.add(editorPanel, BorderLayout.CENTER)
            }
            ViewMode.PREVIEW_ONLY -> {
                // Create wrapper with toolbar in top-right corner
                val previewPanel = JPanel(BorderLayout())
                previewPanel.add(previewEditor.component, BorderLayout.CENTER)

                val topPanel = JPanel(BorderLayout())
                topPanel.add(createToolbar(), BorderLayout.EAST)
                topPanel.isOpaque = false

                mainPanel.add(topPanel, BorderLayout.NORTH)
                mainPanel.add(previewPanel, BorderLayout.CENTER)
            }
            ViewMode.SPLIT -> {
                // Create splitter
                splitter = JBSplitter(false, 0.5f).apply {
                    firstComponent = textEditor.component
                    secondComponent = previewEditor.component
                    setDividerWidth(1)
                }

                // Add toolbar in top-right corner
                val topPanel = JPanel(BorderLayout())
                topPanel.add(createToolbar(), BorderLayout.EAST)
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
        ViewMode.PREVIEW_ONLY -> previewEditor.preferredFocusedComponent
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
        return MarkmapCompositeEditorState(
            currentMode,
            textEditor.getState(level)
        )
    }

    override fun isModified(): Boolean = textEditor.isModified

    override fun isValid(): Boolean = textEditor.isValid && previewEditor.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.addPropertyChangeListener(listener)
        previewEditor.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        textEditor.removePropertyChangeListener(listener)
        previewEditor.removePropertyChangeListener(listener)
    }

    @Nullable
    override fun getCurrentLocation(): FileEditorLocation? = textEditor.currentLocation

    @NotNull
    override fun getFile(): VirtualFile = textEditor.file

    override fun dispose() {
        textEditor.dispose()
        previewEditor.dispose()
    }

    fun getTextEditor(): TextEditor = textEditor
    fun getPreviewEditor(): MarkmapPreviewEditor = previewEditor
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