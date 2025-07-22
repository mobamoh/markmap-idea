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
import com.mohamedbamoh.markmap.ui.MarkmapPreviewComponent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class MarkmapPreviewEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val previewComponent = MarkmapPreviewComponent()
    private var document: Document? = null

    init {
        ApplicationManager.getApplication().invokeLater({
            document = FileDocumentManager.getInstance().getDocument(file)
            document?.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    updatePreview()
                }
            })
            // Initial update
            updatePreview()
        }, ModalityState.nonModal())
    }

    private fun updatePreview() {
        val content = document?.text ?: ""
        previewComponent.updateContent(content)
    }

    @NotNull
    override fun getComponent(): JComponent = previewComponent.getComponent()

    @Nullable
    override fun getPreferredFocusedComponent(): JComponent? = previewComponent.getComponent()

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
        previewComponent.dispose()
    }
}