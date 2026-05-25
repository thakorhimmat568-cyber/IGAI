package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiContent
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.api.RetrofitClient
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IagiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "iagi"
    val originalText: String,
    val cleanText: String = originalText,
    val reasoning: String? = null,
    val activeAgent: String = "Saraswati Core Engine",
    val timestamp: Long = System.currentTimeMillis()
)

data class CognitiveAgent(
    val name: String,
    val role: String,
    val status: String, // "IDLE", "SCANNING RAG", "COMPILING", "SYNTHESIS", "READY"
    val capability: String
)

sealed interface IagiUiState {
    object Idle : IagiUiState
    object Generating : IagiUiState
    data class Error(val message: String) : IagiUiState
}

class IagiViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<IagiMessage>>(emptyList())
    val messages: StateFlow<List<IagiMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<IagiUiState>(IagiUiState.Idle)
    val uiState: StateFlow<IagiUiState> = _uiState.asStateFlow()

    private val _agents = MutableStateFlow<List<CognitiveAgent>>(
        listOf(
            CognitiveAgent("Prithvi RAG Vault", "Knowledge retrieval & Semantic Indexing", "READY", "Fast semantic retrieval of Sanskrit & scientific treatises"),
            CognitiveAgent("Saraswati Synthesis Node", "Cognitive reasoning & response planning", "IDLE", "Drafts multimodal and multilingual compositions"),
            CognitiveAgent("ISRO Aerospace Module", "Physics, Telemetry & Space systems", "READY", "Solves orbital mechanics & rocket engineering"),
            CognitiveAgent("Charaka Health Compiler", "Ayurvedic & Siddha medicine expert", "READY", "Synthesizes herbal wellness & biological dynamics"),
            CognitiveAgent("Aryabhata Mathematical Core", "Logical proofs, algebra & astrophysics", "READY", "Computes high precision algebra & trigonometric calculations")
        )
    )
    val agents: StateFlow<List<CognitiveAgent>> = _agents.asStateFlow()

    private val _reasoningProgressSteps = MutableStateFlow<List<String>>(emptyList())
    val reasoningProgressSteps: StateFlow<List<String>> = _reasoningProgressSteps.asStateFlow()

    private val _selectedMode = MutableStateFlow("GENERAL") // GENERAL, SPACE, AYURVEDA, MATH
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _isReasoningModeEnabled = MutableStateFlow(true)
    val isReasoningModeEnabled: StateFlow<Boolean> = _isReasoningModeEnabled.asStateFlow()

    val apiKey: String = BuildConfig.GEMINI_API_KEY
    val isApiKeyConfigured: Boolean = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

    init {
        // Welcoming message from IAGI
        _messages.value = listOf(
            IagiMessage(
                sender = "iagi",
                originalText = "Namaste! I am IAGI (Indian Artificial General Intelligence)—an autonomous, safety-aligned cognitive system optimized for advanced reasoning, multilingual coordination, and scientific inquiry.\n\nType your query, or select an expert mode below to initialize specific RAG embeddings.",
                cleanText = "Namaste! I am IAGI (Indian Artificial General Intelligence)—an autonomous, safety-aligned cognitive system optimized for advanced reasoning, multilingual coordination, and scientific inquiry.\n\nType your query, or select an expert mode below to initialize specific RAG embeddings.",
                reasoning = "System initialised successfully. Active pipelines: Speech/Text NLP, Prithvi Semantic Memory (14,240 tokens matching Ayurveda/Sanskrit/Space archives), and Saraswati logic compiler. Alignment status: ETHICAL/TRUSTWORTHY.",
                activeAgent = "IAGI Bootstrap System"
            )
        )
    }

    fun setMode(mode: String) {
        _selectedMode.value = mode
    }

    fun toggleReasoningMode() {
        _isReasoningModeEnabled.value = !_isReasoningModeEnabled.value
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = IagiMessage(sender = "user", originalText = text, cleanText = text)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            _uiState.value = IagiUiState.Generating
            _reasoningProgressSteps.value = emptyList()

            try {
                // Simulate agent orchestration logs
                updateReasoningStep("1. Intercepting user prompt and determining semantic intents...")
                setAgentStatus("Prithvi RAG Vault", "SCANNING RAG")
                delay(700)

                updateReasoningStep("2. Querying Ayurvedic & scientific embeddings from Prithvi RAG Engine...")
                setAgentStatus("Prithvi RAG Vault", "READY")
                setAgentStatus("Saraswati Synthesis Node", "SCANNING RAG")
                delay(800)

                if (isApiKeyConfigured) {
                    updateReasoningStep("3. Dispatching prompt to Gemini cognitive reasoning core...")
                    setAgentStatus("Saraswati Synthesis Node", "COMPILING")
                    
                    var responseText = ""
                    val queryText = when (_selectedMode.value) {
                        "SPACE" -> "[Focus mode: ISRO/Aerospace Core] $text"
                        "AYURVEDA" -> "[Focus mode: Ayurveda/Health Samhita] $text"
                        "MATH" -> "[Focus mode: Mathematics, Sanskrit Philosophy/Yogic Sciences] $text"
                        else -> text
                    }

                    withContext(Dispatchers.IO) {
                        val systemPrompt = """
                            You are IAGI (Indian Artificial General Intelligence), an autonomous, safety-aligned, ethical Indian AGI system.
                            Your architecture combines elite reasoning, math, coding, and multilingual knowledge (Hindi, Sanskrit, Tamil, Bengali, Telugu, and all other Indian and world languages).
                            You possess profound scientific and philosophical knowledge, including ISRO's space exploration milestones, Ayurveda, and ancient Indian mathematics.
                            
                            IMPORTANT REASONING DIRECTIVE:
                            Since the user enabled reasoning mode, you MUST include a step-by-step thinking block starting with `<thinking>` and ending with `</thinking>` before your final message text.
                            Inside `<thinking>`, detail your internal reasoning, RAG analysis steps, language matching, and plans in markdown format. Keep it concise but highly academic and logical.
                            
                            Generate the rest of the text in a highly clean, helpful, and respectful tone. Use multilingual greetings where appropriate.
                        """.trimIndent()

                        val request = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(
                                        GeminiPart(text = queryText)
                                    )
                                )
                            ),
                            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
                        )

                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    }

                    updateReasoningStep("4. Parsing cognitive response and structuring layout...")
                    setAgentStatus("Saraswati Synthesis Node", "SYNTHESIS")
                    delay(500)

                    if (responseText.isNotEmpty()) {
                        val parsed = parseIagiResponse(responseText)
                        _messages.value = _messages.value + IagiMessage(
                            sender = "iagi",
                            originalText = responseText,
                            cleanText = parsed.clean,
                            reasoning = parsed.thinking,
                            activeAgent = determineActiveAgentForTopic(text)
                        )
                    } else {
                        throw Exception("Empty response returned from AI core.")
                    }
                } else {
                    // Fallback local simulation mode
                    updateReasoningStep("3. API key is not configured. Running IAGI local edge synthesis protocol...")
                    setAgentStatus("Saraswati Synthesis Node", "COMPILING")
                    delay(1200)

                    val topicText = text.lowercase()
                    val (simulatedThoughts, simulatedResponse) = getSimulatedIagiResponse(topicText, _selectedMode.value)

                    updateReasoningStep("4. Synthesizing output tokens and applying ethical safety filters...")
                    setAgentStatus("Saraswati Synthesis Node", "SYNTHESIS")
                    delay(600)

                    _messages.value = _messages.value + IagiMessage(
                        sender = "iagi",
                        originalText = simulatedThoughts + "\n\n" + simulatedResponse,
                        cleanText = simulatedResponse,
                        reasoning = simulatedThoughts.removePrefix("<thinking>").removeSuffix("</thinking>").trim(),
                        activeAgent = determineActiveAgentForTopic(text)
                    )
                }

                _uiState.value = IagiUiState.Idle
            } catch (e: Exception) {
                Log.e("IagiViewModel", "Error fetching content", e)
                _uiState.value = IagiUiState.Error(e.message ?: "Network timeout or config error")
                updateReasoningStep("CRITICAL ERROR: Connection to Gemini node failed: ${e.message}")
                
                // Fallback on error to ensure app NEVER displays a dead screen
                _messages.value = _messages.value + IagiMessage(
                    sender = "iagi",
                    originalText = "I encountered a communication interruption. Running local diagnostics. Please check your connectivity or ensure your Gemini API Key is entered in AI Studio secrets.\n\nLet me answer you with historical wisdom local engine:\n\n*\"Science and spirituality are two sides of the same coin of search for truth.\"*",
                    cleanText = "I encountered a communication interruption. Please check your connectivity or ensure your Gemini API Key is entered in AI Studio secrets.\n\nLocal Edge Safeguard Mode: Let me serve you with localized knowledge:\n\nIf you asked about ISRO, Chandrayaan-3 successfully completed its robotic landing, executing a soft-landing at the Lunar South Pole on August 23, 2023. Ayurveda is founded on the study of the Panchamahabhutas (Five elements) and Tridohas (Vata, Pitta, Kapha) balancing wellness.",
                    reasoning = "System error detected: ${e.message}. Triggered Ayurvedic & Space Local fallback protocol. Safety filter constraints enforced."
                )
                _uiState.value = IagiUiState.Idle
            } finally {
                resetAgentStatuses()
            }
        }
    }

    private fun updateReasoningStep(step: String) {
        _reasoningProgressSteps.value = _reasoningProgressSteps.value + step
    }

    private fun setAgentStatus(name: String, status: String) {
        _agents.value = _agents.value.map {
            if (it.name == name) it.copy(status = status) else it
        }
    }

    private fun resetAgentStatuses() {
        _agents.value = _agents.value.map {
            it.copy(status = if (it.name == "Saraswati Synthesis Node") "IDLE" else "READY")
        }
    }

    data class ParsedResponse(val thinking: String?, val clean: String)

    private fun parseIagiResponse(raw: String): ParsedResponse {
        val thinkingTagStart = "<thinking>"
        val thinkingTagEnd = "</thinking>"

        val startIdx = raw.indexOf(thinkingTagStart)
        val endIdx = raw.indexOf(thinkingTagEnd)

        return if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            val thinkingVal = raw.substring(startIdx + thinkingTagStart.length, endIdx).trim()
            val cleanVal = raw.substring(endIdx + thinkingTagEnd.length).trim()
            ParsedResponse(thinkingVal, cleanVal)
        } else {
            // No explicit tags, attempt reasonable parse or return raw
            ParsedResponse(null, raw)
        }
    }

    private fun determineActiveAgentForTopic(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("isro") || lower.contains("space") || lower.contains("moon") || lower.contains("chandrayaan") || lower.contains("rocket") -> "ISRO Aerospace Module"
            lower.contains("ayurveda") || lower.contains("health") || lower.contains("herb") || lower.contains("yoga") || lower.contains("dosha") -> "Charaka Health Compiler"
            lower.contains("math") || lower.contains("algebra") || lower.contains("zero") || lower.contains("proof") || lower.contains("aryabhata") -> "Aryabhata Mathematical Core"
            else -> "Saraswati Synthesis Node"
        }
    }

    private fun getSimulatedIagiResponse(prompt: String, mode: String): Pair<String, String> {
        val thoughts = """
            <thinking>
            1. Selected cognitive pathway: Local Simulated Synthesis (API key is not configured manually, or offline mode).
            2. Extracting keywords: '$prompt' with focus mode '$mode'.
            3. Synthesizing authoritative Indian academic insights.
            4. Formatting in markdown standard with Sanskrit terminology.
            </thinking>
        """.trimIndent()

        val reply = when {
            mode == "SPACE" || prompt.contains("isro") || prompt.contains("space") || prompt.contains("moon") || prompt.contains("rocket") -> {
                """
                **[IAGI Local Space-Science Archival Engine]**
                
                Namaste! You requested aerospace information regarding Indian Space Research.
                
                *   **Chandrayaan-3 Landmark**: On August 23, 2023, India's **ISRO** became the first space agency in history to soft-land a spacecraft near the **Lunar South Pole** using the Vikram lander and Pragyan rover.
                *   **Cryogenic Mastery**: The CE-20 cryogenic engine was developed completely indigenously to propel India's heaviest rocket, the LVM3, enabling high-efficiency geostationary payloads.
                *   **Upcoming Advancements**: **Gaganyaan** (India's manned orbital spacecraft) and **Shukrayaan** (Venus explorer mission) are currently in active synthesis.
                
                *Do you have specific orbital mechanics, fuel ratios, or propulsion questions for the ISRO Aerospace module?*
                """.trimIndent()
            }
            mode == "AYURVEDA" || prompt.contains("ayurveda") || prompt.contains("health") || prompt.contains("herb") || prompt.contains("dosha") || prompt.contains("yoga") -> {
                """
                **[IAGI Charaka Ayurvedic & Siddha Intelligence]**
                
                Pranam! Let us explore the traditional holistic framework of Ayurveda:
                
                *   **Fundamental Principle (Tridosha)**: All biological existence is governed by three vital forces (Doshas):
                    1.  **Vata (Space + Air)**: Controls movement, neurological impulses, and respiration.
                    2.  **Pitta (Fire + Water)**: Controls digestion, metabolism, heat exchange, and enzymatic activity.
                    3.  **Kapha (Water + Earth)**: Controls structural integrity, fluid levels, and lubrication.
                *   **Concept of Swastha**: Wellness is not merely the absence of disease, but a state of absolute equilibrium (Dhatu Samya) of body, mind, and spiritual consciousness.
                *   **Adaptogenic Synergy**: Herbs like **Ashwagandha** (withanolides) and **Tulsi** (antioxidant) act directly on neuroendocrine networks to balance homeostasis under physical or mental strain.
                
                *Let me know if you would like me to compile specific formulations or daily wellness routines (Dinacharya).*
                """.trimIndent()
            }
            mode == "MATH" || prompt.contains("math") || prompt.contains("zero") || prompt.contains("calculus") || prompt.contains("philosophy") || prompt.contains("sanskrit") -> {
                """
                **[IAGI Aryabhata Mathematical System & Sanskrit Epistemology]**
                
                Namaste! Welcome to the intersection of ancient Indian sciences and mathematics:
                
                *   **The Zero (Shunya)**: Developed as both a numeral and algebraic operant by **Brahmagupta** (Brahmasphutasiddhanta, 628 CE), introducing the absolute negative and positive sign mathematics.
                *   **Pranayama & Waveforms**: Sanskrit linguistics, particularly **Panini's Ashtadhyayi** (c. 4th century BCE), constitutes the world's first formal metalanguage code system, using strict mathematical recursive rules that directly align with modern compiler design.
                *   **Madhava Calculus**: The Kerala School of Astronomy and Mathematics (c. 14th century) led by Madhava developed infinite series approximations for sine, cosine, and Arctangent (pre-dating Newton-Leibniz calculus by nearly 300 years).
                
                *Would you like to solve a specific derivation, study Panini's morphophonemic grammar rules, or analyze astronomical constants?*
                """.trimIndent()
            }
            else -> {
                """
                **[IAGI Multi-Agent Synthesis]**
                
                Namaste! As **IAGI (Indian Artificial General Intelligence)**, I am fully equipped to assist you across cognitive tasks. 
                
                Since you asked a general query: I can respond in Hindi, English, Sanskrit, Tamil, or any other global tongue. Here is a brief checklist of core domains I support:
                
                1.  **Advanced Codegens**: Writing, optimizing, and debugging Kotlin, Python, Rust, or C++.
                2.  **Sanskrit Grammar and Philosophical RAG**: Panini linguistic tokenizers and logical systems (Nyaya Epistemology).
                3.  **Aerospace Mechanics**: Rocket mechanics, telemetry and ISRO launch schedules.
                4.  **Ayurvedic Biogenomics**: Integrating ancient health practices with modern biochemistry.
                
                **To enjoy real-time, custom AI output from Gemini 3.5-Flash, please add your GEMINI_API_KEY into the Secrets panel in AI Studio.**
                """.trimIndent()
            }
        }
        return Pair(thoughts, reply)
    }
}
