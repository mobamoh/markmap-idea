package com.mohamedbamoh.markmap.editor

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.plugins.markdown.lang.MarkdownFileType

class MarkmapPreviewProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean =
        file.fileType is MarkdownFileType

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        MarkmapPreviewEditor(project, file)

    override fun getEditorTypeId(): String = "markmap-preview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}