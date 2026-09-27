package com.azhar.aillmgallery.ai

/**
 * Builds model-specific chat prompts.
 *
 * Every model family was fine-tuned on its own chat template; feeding Gemma or Phi a ChatML
 * prompt produces rambling or empty output and wastes tokens. This picks the right template
 * from the model file name and packs as much recent conversation history as fits the budget.
 */
object PromptFormatter {

    /** Rough character budget for history — keeps prompts well inside a 1280-token KV cache. */
    const val DEFAULT_HISTORY_CHAR_BUDGET = 2400

    enum class Template { CHATML, GEMMA, ZEPHYR, PHI }

    data class Turn(val isUser: Boolean, val text: String)

    fun templateFor(modelFileName: String?): Template {
        val name = modelFileName?.lowercase().orEmpty()
        val family = ModelCatalog.models.firstOrNull { it.fileName.equals(modelFileName, ignoreCase = true) }?.family
            ?: when {
                "gemma" in name -> ModelFamily.GEMMA
                "phi" in name -> ModelFamily.PHI
                "llama" in name -> ModelFamily.LLAMA
                else -> ModelFamily.OTHER
            }
        return when (family) {
            ModelFamily.GEMMA -> Template.GEMMA
            ModelFamily.PHI -> Template.PHI
            ModelFamily.LLAMA -> Template.ZEPHYR
            else -> Template.CHATML
        }
    }

    /**
     * @param history previous turns, oldest first (excluding [userMessage]).
     */
    fun build(
        template: Template,
        systemPrompt: String,
        userMessage: String,
        history: List<Turn> = emptyList(),
        historyCharBudget: Int = DEFAULT_HISTORY_CHAR_BUDGET
    ): String {
        val turns = trimHistory(history, historyCharBudget) + Turn(isUser = true, text = userMessage)
        return when (template) {
            Template.CHATML -> buildString {
                append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n")
                turns.forEach { t ->
                    append("<|im_start|>").append(if (t.isUser) "user" else "assistant").append('\n')
                    append(t.text).append("<|im_end|>\n")
                }
                append("<|im_start|>assistant\n")
            }

            // Gemma has no system role: fold the system prompt into the first user turn.
            Template.GEMMA -> buildString {
                turns.forEachIndexed { i, t ->
                    append("<start_of_turn>").append(if (t.isUser) "user" else "model").append('\n')
                    if (i == 0) append(systemPrompt).append("\n\n")
                    append(t.text).append("<end_of_turn>\n")
                }
                append("<start_of_turn>model\n")
            }

            Template.ZEPHYR -> buildString {
                append("<|system|>\n").append(systemPrompt).append("</s>\n")
                turns.forEach { t ->
                    append(if (t.isUser) "<|user|>\n" else "<|assistant|>\n")
                    append(t.text).append("</s>\n")
                }
                append("<|assistant|>\n")
            }

            Template.PHI -> buildString {
                append("<|system|>").append(systemPrompt).append("<|end|>")
                turns.forEach { t ->
                    append(if (t.isUser) "<|user|>" else "<|assistant|>")
                    append(t.text).append("<|end|>")
                }
                append("<|assistant|>")
            }
        }
    }

    /** Keeps the most recent turns whose combined length fits in [budget]. */
    internal fun trimHistory(history: List<Turn>, budget: Int): List<Turn> {
        var used = 0
        val kept = ArrayDeque<Turn>()
        for (turn in history.asReversed()) {
            if (turn.text.isBlank()) continue
            used += turn.text.length
            if (used > budget) break
            kept.addFirst(turn)
        }
        // Never start the window with an assistant reply — the model expects a user turn first.
        while (kept.isNotEmpty() && !kept.first().isUser) kept.removeFirst()
        return kept.toList()
    }

    /** Strips template artifacts some small models echo back into their output. */
    fun cleanOutput(raw: String): String = raw
        .replace(Regex("<\\|im_end\\|>|<\\|im_start\\|>(assistant)?|<end_of_turn>|<start_of_turn>(model)?|<\\|end\\|>|</s>|<\\|assistant\\|>"), "")
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
}
