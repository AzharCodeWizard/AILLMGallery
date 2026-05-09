package com.azhar.aillmgallery.ai

/**
 * Catalog of downloadable on-device AI models.
 * These are publicly available models from the official litert-community on HuggingFace,
 * converted to MediaPipe .task format for direct use with LlmInference API.
 */
data class DownloadableModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeDisplay: String,
    val sizeBytes: Long,
    val url: String,
    val fileName: String,
    val quantization: String,
    val parameters: String,
    val category: ModelCategory,
    val family: ModelFamily = ModelFamily.OTHER,
    val isGated: Boolean = false
)

enum class ModelCategory(val displayName: String) {
    SMALL("Compact (< 500 MB)"),
    MEDIUM("Standard (500 MB – 1.5 GB)"),
    LARGE("Performance (1.5 – 3 GB)"),
    EXTRA_LARGE("Flagship (3+ GB)")
}

enum class ModelFamily(val displayName: String, val emoji: String) {
    SMOLLM("SmolLM", "🤗"),
    QWEN("Qwen", "🔮"),
    DEEPSEEK("DeepSeek", "🧠"),
    GEMMA("Gemma", "💎"),
    LLAMA("Llama", "🦙"),
    PHI("Phi", "Φ"),
    OTHER("Other", "🤖")
}

/**
 * Registry of known downloadable models.
 * All URLs point to publicly accessible (non-gated) models from litert-community
 * in MediaPipe .task format — the only format compatible with MediaPipe LlmInference.
 */
object ModelCatalog {
    val models = listOf(
        // ═══════════════════════════════════════════
        // COMPACT MODELS (< 500 MB) — Fast & lightweight
        // ═══════════════════════════════════════════

        DownloadableModel(
            id = "smollm-135m-q8",
            name = "SmolLM 135M",
            description = "Ultra-lightweight 135M model. Blazing fast inference with basic Q&A. Perfect for quick testing on any device.",
            sizeDisplay = "~159 MB",
            sizeBytes = 166_754_726L,
            url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "smollm-135m-q8.task",
            quantization = "Q8",
            parameters = "135M",
            category = ModelCategory.SMALL,
            family = ModelFamily.SMOLLM
        ),

        DownloadableModel(
            id = "gemma3-270m-q8",
            name = "Gemma 3 270M",
            description = "Google's tiniest Gemma 3. Surprisingly capable for its size — great for simple tasks and rapid prototyping.",
            sizeDisplay = "~290 MB",
            sizeBytes = 303_950_933L,
            url = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.task",
            fileName = "gemma3-270m-q8.task",
            quantization = "Q8",
            parameters = "270M",
            category = ModelCategory.SMALL,
            family = ModelFamily.GEMMA
        ),

        // ═══════════════════════════════════════════
        // STANDARD MODELS (500 MB – 1.5 GB) — Balanced
        // ═══════════════════════════════════════════

        DownloadableModel(
            id = "qwen25-05b-q8",
            name = "Qwen 2.5 0.5B",
            description = "Alibaba's Qwen 2.5 with 0.5B params. Solid reasoning, multilingual support, and great coding skills for its size.",
            sizeDisplay = "~521 MB",
            sizeBytes = 546_660_344L,
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen25-05b-q8.task",
            quantization = "Q8",
            parameters = "0.5B",
            category = ModelCategory.MEDIUM,
            family = ModelFamily.QWEN
        ),

        DownloadableModel(
            id = "smollm-135m-f32",
            name = "SmolLM 135M (F32)",
            description = "Full-precision 135M model. Higher accuracy than Q8 variant but 3x larger. Best quality at the smallest scale.",
            sizeDisplay = "~528 MB",
            sizeBytes = 553_281_294L,
            url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_f32_ekv1280.task",
            fileName = "smollm-135m-f32.task",
            quantization = "F32",
            parameters = "135M",
            category = ModelCategory.MEDIUM,
            family = ModelFamily.SMOLLM
        ),

        DownloadableModel(
            id = "gemma3-1b-q4",
            name = "Gemma 3 1B (Q4) ⭐",
            description = "Google's Gemma 3 1B quantized to 4-bit. Excellent quality-to-size ratio — compact yet very capable for on-device use.",
            sizeDisplay = "~529 MB",
            sizeBytes = 554_661_243L,
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task",
            fileName = "gemma3-1b-q4.task",
            quantization = "Q4",
            parameters = "1B",
            category = ModelCategory.MEDIUM,
            family = ModelFamily.GEMMA
        ),

        DownloadableModel(
            id = "gemma3-1b-q8",
            name = "Gemma 3 1B",
            description = "Google's latest Gemma 3 with 1B params. Strong reasoning and instruction following. Optimized for mobile.",
            sizeDisplay = "~1.0 GB",
            sizeBytes = 1_054_012_582L,
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv1280.task",
            fileName = "gemma3-1b-q8.task",
            quantization = "Q8",
            parameters = "1B",
            category = ModelCategory.MEDIUM,
            family = ModelFamily.GEMMA
        ),

        DownloadableModel(
            id = "tinyllama-1b-q8",
            name = "TinyLlama 1.1B",
            description = "Community-favorite TinyLlama trained on 3T tokens. Punches above its weight in general chat and coding tasks.",
            sizeDisplay = "~1.1 GB",
            sizeBytes = 1_148_331_545L,
            url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            fileName = "tinyllama-1b-q8.task",
            quantization = "Q8",
            parameters = "1.1B",
            category = ModelCategory.MEDIUM,
            family = ModelFamily.LLAMA
        ),

        // ═══════════════════════════════════════════
        // PERFORMANCE MODELS (1.5 – 3 GB) — Powerful
        // ═══════════════════════════════════════════

        DownloadableModel(
            id = "qwen25-15b-q8",
            name = "Qwen 2.5 1.5B ⭐",
            description = "Top-tier Qwen 2.5 with 1.5B params. Superior logic, coding, math reasoning. Best overall model for on-device use.",
            sizeDisplay = "~1.5 GB",
            sizeBytes = 1_597_913_616L,
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen25-15b-q8.task",
            quantization = "Q8",
            parameters = "1.5B",
            category = ModelCategory.LARGE,
            family = ModelFamily.QWEN
        ),

        DownloadableModel(
            id = "qwen25-15b-q8-4k",
            name = "Qwen 2.5 1.5B (4K ctx)",
            description = "Same powerful Qwen 2.5 1.5B with extended 4096-token context window. Handles longer conversations and documents.",
            sizeDisplay = "~1.5 GB",
            sizeBytes = 1_598_556_720L,
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task",
            fileName = "qwen25-15b-q8-4k.task",
            quantization = "Q8",
            parameters = "1.5B",
            category = ModelCategory.LARGE,
            family = ModelFamily.QWEN
        ),

        DownloadableModel(
            id = "deepseek-r1-15b-q8",
            name = "DeepSeek R1 1.5B ⭐",
            description = "DeepSeek's R1 reasoning model distilled into Qwen 1.5B. Chain-of-thought reasoning for complex problem solving.",
            sizeDisplay = "~1.8 GB",
            sizeBytes = 1_861_094_737L,
            url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
            fileName = "deepseek-r1-15b-q8.task",
            quantization = "Q8",
            parameters = "1.5B",
            category = ModelCategory.LARGE,
            family = ModelFamily.DEEPSEEK
        ),

        DownloadableModel(
            id = "deepseek-r1-15b-q8-4k",
            name = "DeepSeek R1 1.5B (4K ctx)",
            description = "DeepSeek R1 with extended 4K context. Perfect for detailed reasoning chains and complex multi-step problems.",
            sizeDisplay = "~1.7 GB",
            sizeBytes = 1_834_078_546L,
            url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.task",
            fileName = "deepseek-r1-15b-q8-4k.task",
            quantization = "Q8",
            parameters = "1.5B",
            category = ModelCategory.LARGE,
            family = ModelFamily.DEEPSEEK
        ),

        DownloadableModel(
            id = "gemma4-e2b-web",
            name = "Gemma 4 E2B 🆕",
            description = "Google's latest Gemma 4 multimodal model. State-of-the-art reasoning with text, vision & audio understanding.",
            sizeDisplay = "~1.9 GB",
            sizeBytes = 2_003_697_664L,
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-web.task",
            fileName = "gemma4-e2b.task",
            quantization = "Q8",
            parameters = "2B",
            category = ModelCategory.LARGE,
            family = ModelFamily.GEMMA
        ),

        DownloadableModel(
            id = "qwen25-05b-f32",
            name = "Qwen 2.5 0.5B (F32)",
            description = "Full-precision Qwen 0.5B. Maximum accuracy with no quantization loss. Great for tasks requiring precision.",
            sizeDisplay = "~1.9 GB",
            sizeBytes = 1_991_044_308L,
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_f32_ekv1280.task",
            fileName = "qwen25-05b-f32.task",
            quantization = "F32",
            parameters = "0.5B",
            category = ModelCategory.LARGE,
            family = ModelFamily.QWEN
        ),

        DownloadableModel(
            id = "gemma2-2b-q8",
            name = "Gemma 2 2B",
            description = "Google's Gemma 2 with 2B params. Strong general-purpose conversational AI with solid instruction following.",
            sizeDisplay = "~2.6 GB",
            sizeBytes = 2_713_274_466L,
            url = "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task",
            fileName = "gemma2-2b-q8.task",
            quantization = "Q8",
            parameters = "2B",
            category = ModelCategory.LARGE,
            family = ModelFamily.GEMMA
        ),

        // ═══════════════════════════════════════════
        // FLAGSHIP MODELS (3+ GB) — Maximum quality
        // ═══════════════════════════════════════════

        DownloadableModel(
            id = "gemma4-e4b-web",
            name = "Gemma 4 E4B 🆕",
            description = "Google's flagship Gemma 4 E4B. The most capable on-device Gemma model — exceptional reasoning, coding & analysis.",
            sizeDisplay = "~2.8 GB",
            sizeBytes = 2_964_324_352L,
            url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it-web.task",
            fileName = "gemma4-e4b.task",
            quantization = "Q8",
            parameters = "4B",
            category = ModelCategory.EXTRA_LARGE,
            family = ModelFamily.GEMMA
        ),

        DownloadableModel(
            id = "phi4-mini-q8",
            name = "Phi-4 Mini 3.8B",
            description = "Microsoft's Phi-4 Mini with 3.8B params. State-of-the-art reasoning, math & coding among open models.",
            sizeDisplay = "~3.7 GB",
            sizeBytes = 3_944_275_882L,
            url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "phi4-mini-q8.task",
            quantization = "Q8",
            parameters = "3.8B",
            category = ModelCategory.EXTRA_LARGE,
            family = ModelFamily.PHI
        ),

        DownloadableModel(
            id = "phi4-mini-q8-4k",
            name = "Phi-4 Mini 3.8B (4K ctx)",
            description = "Phi-4 Mini with extended 4096-token context. Handles long documents, detailed code analysis & extended conversations.",
            sizeDisplay = "~3.7 GB",
            sizeBytes = 3_910_050_199L,
            url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task",
            fileName = "phi4-mini-q8-4k.task",
            quantization = "Q8",
            parameters = "3.8B",
            category = ModelCategory.EXTRA_LARGE,
            family = ModelFamily.PHI
        )
    )
}
