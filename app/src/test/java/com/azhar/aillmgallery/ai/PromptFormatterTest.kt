package com.azhar.aillmgallery.ai

import com.azhar.aillmgallery.ai.PromptFormatter.Template
import com.azhar.aillmgallery.ai.PromptFormatter.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptFormatterTest {

    @Test
    fun templateFor_detectsFamilyFromCatalogAndFileName() {
        assertEquals(Template.CHATML, PromptFormatter.templateFor("qwen25-05b-q8.task"))
        assertEquals(Template.CHATML, PromptFormatter.templateFor("deepseek-r1-15b-q8.task"))
        assertEquals(Template.GEMMA, PromptFormatter.templateFor("gemma3-1b-q4.task"))
        assertEquals(Template.ZEPHYR, PromptFormatter.templateFor("tinyllama-1b-q8.task"))
        assertEquals(Template.PHI, PromptFormatter.templateFor("phi4-mini-q8.task"))
        // Unknown files pushed via adb fall back to name matching, then ChatML.
        assertEquals(Template.GEMMA, PromptFormatter.templateFor("my-Gemma-custom.bin"))
        assertEquals(Template.CHATML, PromptFormatter.templateFor("mystery.task"))
        assertEquals(Template.CHATML, PromptFormatter.templateFor(null))
    }

    @Test
    fun chatml_includesSystemHistoryAndOpenAssistantTurn() {
        val prompt = PromptFormatter.build(
            Template.CHATML,
            systemPrompt = "SYS",
            userMessage = "second",
            history = listOf(Turn(true, "first"), Turn(false, "reply"))
        )
        assertEquals(
            "<|im_start|>system\nSYS<|im_end|>\n" +
                "<|im_start|>user\nfirst<|im_end|>\n" +
                "<|im_start|>assistant\nreply<|im_end|>\n" +
                "<|im_start|>user\nsecond<|im_end|>\n" +
                "<|im_start|>assistant\n",
            prompt
        )
    }

    @Test
    fun gemma_foldsSystemPromptIntoFirstUserTurn() {
        val prompt = PromptFormatter.build(Template.GEMMA, "SYS", "hi")
        assertEquals("<start_of_turn>user\nSYS\n\nhi<end_of_turn>\n<start_of_turn>model\n", prompt)
    }

    @Test
    fun zephyrAndPhi_endWithAssistantTag() {
        assertTrue(PromptFormatter.build(Template.ZEPHYR, "S", "u").endsWith("<|user|>\nu</s>\n<|assistant|>\n"))
        assertEquals("<|system|>S<|end|><|user|>u<|end|><|assistant|>", PromptFormatter.build(Template.PHI, "S", "u"))
    }

    @Test
    fun trimHistory_keepsNewestTurnsWithinBudgetAndStartsWithUser() {
        val history = listOf(
            Turn(true, "a".repeat(50)),
            Turn(false, "b".repeat(50)),
            Turn(true, "c".repeat(50)),
            Turn(false, "d".repeat(50))
        )
        // Budget fits the last 3 turns, but the window must not open on an assistant reply.
        val kept = PromptFormatter.trimHistory(history, budget = 160)
        assertEquals(listOf(history[2], history[3]), kept)

        assertTrue(PromptFormatter.trimHistory(history, budget = 10).isEmpty())
        assertEquals(history, PromptFormatter.trimHistory(history, budget = 1000))
    }

    @Test
    fun cleanOutput_stripsTemplateTokensAndEscapes() {
        val cleaned = PromptFormatter.cleanOutput("Hello\\nworld<|im_end|><end_of_turn>")
        assertEquals("Hello\nworld", cleaned)
        assertFalse(cleaned.contains("<|"))
    }
}
