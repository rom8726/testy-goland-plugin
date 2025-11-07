package com.testy.plugin

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import org.jetbrains.yaml.YAMLLanguage
import javax.swing.Icon

object TestyFileType : LanguageFileType(YAMLLanguage.INSTANCE) {
    override fun getName() = "Testy YAML"
    override fun getDescription() = "Testy scenario file"
    override fun getDefaultExtension() = "testy.yml"
    override fun getIcon(): Icon = IconLoader.getIcon("/icons/testy.svg", javaClass)
}
