/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

interface ScriptDefinitionProvider {
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?
    fun isScript(fileName: String): Boolean
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider? =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}

fun PsiFile.scriptDefinition(project: Project): KotlinScriptDefinition? {
    if (this !is KtFile || this.script == null) return null
    if (virtualFile.isNonScript()) return null

    return scriptDefinitionByFileName(project, virtualFile.name)
}

fun findScriptDefinition(file: VirtualFile, project: Project): KotlinScriptDefinition? {
    if (file.isNonScript()) return null
    if ((PsiManager.getInstance(project).findFile(file) as? KtFile)?.script == null) return null

    return scriptDefinitionByFileName(project, file.name)
}

fun scriptDefinitionByFileName(project: Project, fileName: String): KotlinScriptDefinition {
    val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) ?: return null
        ?: throw IllegalStateException("Unable to get script definition: ScriptDefinitionProvider is not configured.")

    return scriptDefinitionProvider.findScriptDefinition(fileName) ?: scriptDefinitionProvider.getDefaultScriptDefinition()
}

private fun VirtualFile.isNonScript(): Boolean =
    isDirectory ||
            extension == KotlinFileType.EXTENSION ||
            extension == JavaClassFileType.INSTANCE.defaultExtension ||
            !this.isKotlinFileType()

private fun VirtualFile.isKotlinFileType(): Boolean {
    val typeRegistry = FileTypeRegistry.getInstance()
    return typeRegistry.getFileTypeByFile(this) == KotlinFileType.INSTANCE ||
            typeRegistry.getFileTypeByFileName(name) == KotlinFileType.INSTANCE
}

