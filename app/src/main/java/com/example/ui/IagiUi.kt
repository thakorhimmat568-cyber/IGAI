package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CognitiveAgent
import com.example.viewmodel.IagiMessage
import com.example.viewmodel.IagiUiState
import com.example.viewmodel.IagiViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IagiDashboardScreen(
    viewModel: IagiViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isReasoningEnabled by viewModel.isReasoningModeEnabled.collectAsState()
    val reasoningSteps by viewModel.reasoningProgressSteps.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Track selected agent detail modal/card
    var selectedAgentDetail by remember { mutableStateOf<CognitiveAgent?>(null) }
    
    // Track if user wants to view "Console" tab vs "Agent Mind" visualizer
    var activeTab by remember { mutableStateOf("CONSOLE") } // "CONSOLE" or "AGENT_MIND"

    // Auto scroll list when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DeepIndigoBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            IagiTopBar(
                isReasoningEnabled = isReasoningEnabled,
                onReasoningToggled = { viewModel.toggleReasoningMode() }
            )
        },
        bottomBar = {
            // Elegant single entry field
            if (activeTab == "CONSOLE") {
                IagiInputArea(
                    textValue = textInput,
                    onValueChange = { textInput = it },
                    isGenerating = uiState is IagiUiState.Generating,
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_input_area")
                )
            } else {
                // Return to Console bar in AGENT_MIND view
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SpaceCard,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    border = BorderStroke(1.dp, ThinBorderGrey)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { activeTab = "CONSOLE" },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSaffron),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("back_to_console_button")
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Console")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deploy Response in Chat Console", fontWeight = FontWeight.Bold, color = CosmicDark)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepIndigoBackground)
        ) {
            
            // Warnings / Config alerts
            if (!viewModel.isApiKeyConfigured) {
                LocalModeAlertBanner()
            }

            // Quick Tabs: Chat Console vs Autonomous Architecture
            TabSelector(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )

            HorizontalDivider(color = ThinBorderGrey, thickness = 1.dp)

            Box(modifier = Modifier.weight(1f)) {
                if (activeTab == "CONSOLE") {
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        // Active Topic Focus Selector
                        FocusModeSelector(
                            selectedMode = selectedMode,
                            onModeSelected = { viewModel.setMode(it) }
                        )

                        // Real-time Chat Dialog Scroll
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { message ->
                                IagiMessageCard(
                                    message = message,
                                    onPresetUse = { preset ->
                                        viewModel.sendMessage(preset)
                                    }
                                )
                            }

                            // Dynamic Generation & Reasoning State Box
                            if (uiState is IagiUiState.Generating) {
                                item {
                                    GeneratingIndicatorBox(
                                        reasoningSteps = reasoningSteps
                                    )
                                }
                            }
                        }

                        // Knowledge vault pre-set query chips showing RAG entry points
                        if (messages.size <= 2 && uiState !is IagiUiState.Generating) {
                            KnowledgeVaultSection(
                                onChipClicked = { presetText ->
                                    textInput = presetText
                                }
                            )
                        }
                    }
                } else {
                    // "AGENT_MIND" Architecture Node Diagram
                    AgentMindVisualizer(
                        agents = agents,
                        selectedAgent = selectedAgentDetail,
                        onAgentSelect = { selectedAgentDetail = if (selectedAgentDetail?.name == it.name) null else it }
                    )
                }
            }
        }
    }
}

@Composable
fun TabSelector(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(42.dp)
            .background(CosmicDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.dp, ThinBorderGrey, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(if (activeTab == "CONSOLE") SpaceCard else Color.Transparent)
                .clickable { onTabSelected("CONSOLE") }
                .testTag("tab_console"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = if (activeTab == "CONSOLE") DeepSaffron else SubtitleGrey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Chat Console",
                    color = if (activeTab == "CONSOLE") HighContrastWhite else SubtitleGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(if (activeTab == "AGENT_MIND") SpaceCard else Color.Transparent)
                .clickable { onTabSelected("AGENT_MIND") }
                .testTag("tab_agent_mind"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Hub,
                    contentDescription = null,
                    tint = if (activeTab == "AGENT_MIND") AccentTeal else SubtitleGrey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Agentic Core System",
                    color = if (activeTab == "AGENT_MIND") HighContrastWhite else SubtitleGrey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun IagiTopBar(
    isReasoningEnabled: Boolean,
    onReasoningToggled: () -> Unit
) {
    Surface(
        color = SpaceCard,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, ThinBorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(DeepSaffron, GoldGlow)),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, HighContrastWhite.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = "IAGI Cognitive Core",
                        tint = CosmicDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "IAGI",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = HighContrastWhite,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentTeal.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, AccentTeal, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "v3.5-EDGE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                    }
                    Text(
                        "Indian Artificial General Intelligence System",
                        fontSize = 10.sp,
                        color = SubtitleGrey,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Reasoning Toggle with Gold Glow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onReasoningToggled() }
                    .background(if (isReasoningEnabled) ThinBorderGrey else Color.Transparent)
                    .border(
                        1.dp,
                        if (isReasoningEnabled) GoldGlow.copy(alpha = 0.6f) else ThinBorderGrey,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("reasoning_mode_toggle_switch")
            ) {
                Icon(
                    Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = if (isReasoningEnabled) GoldGlow else SubtitleGrey,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "REASONING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReasoningEnabled) GoldGlow else SubtitleGrey
                )
            }
        }
    }
}

@Composable
fun LocalModeAlertBanner() {
    Surface(
        color = DeepSaffron.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, DeepSaffron.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "Info",
                tint = DeepSaffron,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Running local simulation. Insert your GEMINI_API_KEY in the Secrets panel for fully autonomous real-time intelligence.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = DeepSaffron.copy(alpha = 0.9f),
                lineHeight = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val modes = listOf(
            Triple("GENERAL", "Universal Core", Icons.Filled.Hub),
            Triple("SPACE", "ISRO / Space Mechanics", Icons.Filled.Science),
            Triple("AYURVEDA", "Charaka Samhita", Icons.Filled.Eco),
            Triple("MATH", "Vedic Math / Philosophy", Icons.Filled.Calculate)
        )

        modes.forEach { (modeKey, label, icon) ->
            val isSelected = selectedMode == modeKey
            val accentColor = when (modeKey) {
                "SPACE" -> AccentTeal
                "AYURVEDA" -> Color(0xFF81C784)
                "MATH" -> GoldGlow
                else -> DeepSaffron
            }

            Surface(
                onClick = { onModeSelected(modeKey) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.15f) else SpaceCard,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) accentColor else ThinBorderGrey
                ),
                tonalElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("mode_$modeKey")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) accentColor else SubtitleGrey,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) accentColor else HighContrastWhite
                    )
                }
            }
        }
    }
}

@Composable
fun IagiMessageCard(
    message: IagiMessage,
    onPresetUse: (String) -> Unit
) {
    val isUser = message.sender == "user"
    var isThinkingExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(DeepSaffron.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, DeepSaffron, CircleShape)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = DeepSaffron,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Optional Active Agent indicator
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AccentTeal, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.activeAgent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Chat text bubble
            Surface(
                color = if (isUser) ThinBorderGrey.copy(alpha = 0.8f) else SpaceCard,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 14.dp else 2.dp,
                    topEnd = if (isUser) 2.dp else 14.dp,
                    bottomEnd = 14.dp,
                    bottomStart = 14.dp
                ),
                border = BorderStroke(1.dp, if (isUser) ThinBorderGrey else ThinBorderGrey.copy(alpha = 0.7f)),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .testTag(if (isUser) "user_msg_bubble" else "iagi_msg_bubble")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.cleanText,
                        fontSize = 13.sp,
                        color = HighContrastWhite,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    )

                    // Expandable Step-by-Step Cognitive Track Panel
                    if (message.reasoning != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .border(1.dp, GoldGlow.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .clickable { isThinkingExpanded = !isThinkingExpanded }
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Hub,
                                            contentDescription = null,
                                            tint = GoldGlow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "IAGI Cognitive Trace Panel",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldGlow,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isThinkingExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = SubtitleGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isThinkingExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(color = ThinBorderGrey, thickness = 1.dp, modifier = Modifier.padding(bottom = 6.dp))
                                        Text(
                                            text = message.reasoning,
                                            fontSize = 10.sp,
                                            color = SubtitleGrey,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(ThinBorderGrey, CircleShape)
                    .border(1.dp, SubtitleGrey, CircleShape)
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = SubtitleGrey,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GeneratingIndicatorBox(
    reasoningSteps: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(DeepSaffron.copy(alpha = 0.1f), CircleShape)
                .border(1.5.dp, DeepSaffron, CircleShape)
                .align(Alignment.Top),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = DeepSaffron,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "IAGI Synthesizer Engine",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepSaffron,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(coordinating cognitive nodes...)",
                    fontSize = 9.sp,
                    color = SubtitleGrey
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = SpaceCard,
                shape = RoundedCornerShape(2.dp, 14.dp, 14.dp, 14.dp),
                border = BorderStroke(1.dp, ThinBorderGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Formulating response...",
                        fontSize = 13.sp,
                        color = HighContrastWhite,
                        fontWeight = FontWeight.Medium
                    )

                    if (reasoningSteps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicDark, RoundedCornerShape(6.dp))
                                .border(1.dp, ThinBorderGrey, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Autonomous Logic Logs:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal,
                                    letterSpacing = 1.sp
                                )
                                reasoningSteps.forEach { step ->
                                    Text(
                                        text = step,
                                        fontSize = 10.sp,
                                        color = SubtitleGrey,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeVaultSection(
    onChipClicked: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = GoldGlow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "IAGI Knowledge Archives",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldGlow,
                letterSpacing = 0.5.sp
            )
        }

        val archives = listOf(
            Pair("🛰️ ISRO Vikram soft-landing parameters", "Explain Chandrayaan-3's path, soft-landing coordinates, and Vikram lander specifications."),
            Pair("🌿 Tridoshas & Adaptogens of Ayurveda", "Explain the physiological dynamic balance of Vata, Pitta, and Kapha and botanical herbs."),
            Pair("🧮 Brahmagupta Algebra of Shunya (Zero)", "Details on Brahmagupta rules of zero multiplication and division, plus ancient astronomy."),
            Pair("🕉️ Panini Grammatical Machine Metalanguage", "Explain Panini's linguistics machine rules and how they align with modern AI compiler theory.")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            archives.forEach { (label, searchPrompt) ->
                Surface(
                    onClick = { onChipClicked(searchPrompt) },
                    color = SpaceCard,
                    border = BorderStroke(1.dp, ThinBorderGrey),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HighContrastWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Select Preset Archive",
                            tint = DeepSaffron,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IagiInputArea(
    textValue: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Surface(
        color = SpaceCard,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, ThinBorderGrey),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Search archives or write complex prompts...",
                        fontSize = 12.sp,
                        color = SubtitleGrey
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .testTag("chat_input_textfield"),
                textStyle = LocalTextStyle.current.copy(color = HighContrastWhite, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textValue.isNotBlank() && !isGenerating) {
                            onSend()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepSaffron,
                    unfocusedBorderColor = ThinBorderGrey,
                    focusedLabelColor = DeepSaffron,
                    cursorColor = DeepSaffron,
                    focusedContainerColor = CosmicDark.copy(alpha = 0.4f),
                    unfocusedContainerColor = CosmicDark.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (textValue.isNotBlank() && !isGenerating) DeepSaffron else SpaceCard
                    )
                    .border(
                        1.dp,
                        if (textValue.isNotBlank() && !isGenerating) Color.Transparent else ThinBorderGrey,
                        CircleShape
                    )
                    .clickable(
                        enabled = textValue.isNotBlank() && !isGenerating,
                        onClick = {
                            onSend()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = DeepSaffron,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (textValue.isNotBlank()) CosmicDark else SubtitleGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentMindVisualizer(
    agents: List<CognitiveAgent>,
    selectedAgent: CognitiveAgent?,
    onAgentSelect: (CognitiveAgent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // Visual Header Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(SpaceCard, RoundedCornerShape(16.dp))
                .border(2.dp, Brush.linearGradient(listOf(DeepSaffron, AccentTeal)), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Hub,
                    contentDescription = null,
                    tint = DeepSaffron,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "CONGRUENT MULTI-AGENT SYNCHRONIZER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighContrastWhite,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Real-time distributed Indian cognitive neural processors matching ISRO telemetry, Sanskrit, Ayurveda, and logical math vectors.",
                    fontSize = 10.sp,
                    color = SubtitleGrey,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "CORE COGNITIVE PIPELINE NODES",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AccentTeal,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Grid List of Agents
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(agents) { agent ->
                val isSelected = selectedAgent?.name == agent.name
                val stateColor = when (agent.status) {
                    "READY" -> AccentTeal
                    "IDLE" -> SubtitleGrey
                    else -> DeepSaffron // SCANNING RAG, COMPILING, SYNTHESIS
                }

                Surface(
                    onClick = { onAgentSelect(agent) },
                    color = if (isSelected) stateColor.copy(alpha = 0.05f) else SpaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) stateColor else ThinBorderGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("agent_card_${agent.name.replace(" ", "_")}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(stateColor.copy(alpha = 0.1f), CircleShape)
                                        .border(1.dp, stateColor.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            agent.name.contains("RAG") -> Icons.Filled.Layers
                                            agent.name.contains("Saraswati") -> Icons.Filled.Psychology
                                            agent.name.contains("ISRO") -> Icons.Filled.Science
                                            agent.name.contains("Charaka") -> Icons.Filled.Eco
                                            else -> Icons.Filled.Calculate
                                        },
                                        contentDescription = null,
                                        tint = stateColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = agent.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighContrastWhite
                                    )
                                    Text(
                                        text = agent.role,
                                        fontSize = 10.sp,
                                        color = SubtitleGrey
                                    )
                                }
                            }

                            // Pulsing/status chip
                            Box(
                                modifier = Modifier
                                    .background(stateColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, stateColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(stateColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = agent.status,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = stateColor
                                    )
                                }
                            }
                        }

                        // Detail dropdown animation
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HorizontalDivider(color = ThinBorderGrey, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "CONGRUENT CORE CAPABILITY:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = agent.capability,
                                    fontSize = 11.sp,
                                    color = HighContrastWhite,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
