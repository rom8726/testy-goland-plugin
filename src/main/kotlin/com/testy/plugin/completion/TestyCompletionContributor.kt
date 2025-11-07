package com.testy.plugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.yaml.YAMLLanguage

class TestyCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE),
            TestyCompletionProvider()
        )
    }
}

