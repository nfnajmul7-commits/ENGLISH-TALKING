package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.LipSyncFrame
import com.example.model.AvatarState
import com.example.model.TutorMode
import com.example.ui.theme.StageAmberGlow
import com.example.ui.theme.StageCoralPulse
import com.example.ui.theme.StageCyanGlow
import com.example.ui.theme.StageDeepBottom
import com.example.ui.theme.StageDeepMid
import com.example.ui.theme.StageDeepTop
import com.example.ui.theme.StageHairDark
import com.example.ui.theme.StageHairHighlight
import com.example.ui.theme.StageIndigoGlow
import com.example.ui.theme.StageSkinLight
import com.example.ui.theme.StageSkinMid
import com.example.ui.theme.StageSkinShadow
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun TalkingAvatarStage(
    avatarState: AvatarState,
    lipSyncFrame: LipSyncFrame,
    micAmplitude: Float,
    tutorMode: TutorMode,
    speechRate: Float,
    sessionSeconds: Int,
    languageBadge: String,
    isExpanded: Boolean,
    isCollapsed: Boolean,
    liveCaption: String?,
    onModeChange: (TutorMode) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleCollapse: () -> Unit,
    onToggleSpeak: () -> Unit,
    onStopAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateAccentColor by animateColorAsState(
        targetValue = when (avatarState) {
            AvatarState.SPEAKING -> StageCyanGlow
            AvatarState.LISTENING -> StageCoralPulse
            AvatarState.THINKING -> StageIndigoGlow
            AvatarState.IDLE -> Color(0xFF7C8CFF)
        },
        animationSpec = tween(320),
        label = "state_accent"
    )

    val stageGradient = Brush.verticalGradient(
        colors = listOf(StageDeepTop, StageDeepMid, StageDeepBottom)
    )

    if (isCollapsed && !isExpanded) {
        // Compact sticky mini-header
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onToggleCollapse() }
                .testTag("avatar_stage_collapsed_bar"),
            color = StageDeepMid,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .background(stageGradient)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(2.dp, stateAccentColor, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(StageDeepTop),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = stateAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.avatar_name),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${avatarStateLabel(avatarState)} · $languageBadge",
                        style = MaterialTheme.typography.bodySmall,
                        color = stateAccentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TimerBadge(seconds = sessionSeconds)
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.testTag("show_avatar_stage_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(R.string.cd_show_stage),
                        tint = Color.White
                    )
                }
            }
        }
        return
    }

    val stageHeightModifier = if (isExpanded) {
        modifier.fillMaxSize()
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(232.dp)
            .clip(RoundedCornerShape(26.dp))
    }

    Box(
        modifier = stageHeightModifier
            .background(stageGradient)
            .border(
                width = 1.dp,
                color = stateAccentColor.copy(alpha = 0.28f),
                shape = if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(26.dp)
            )
            .testTag("talking_avatar_stage")
    ) {
        // Animated AI Humanoid Tutor Canvas + Speaking Ring + Waveform
        TalkingAiAvatarCanvas(
            avatarState = avatarState,
            lipSyncFrame = lipSyncFrame,
            micAmplitude = micAmplitude,
            accentColor = stateAccentColor,
            isExpanded = isExpanded,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (isExpanded) 56.dp else 18.dp,
                    bottom = if (isExpanded) 136.dp else 20.dp,
                    end = 44.dp
                )
        )

        // Top HUD Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Live Status Pill + Language Pair
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StateStatusPill(avatarState = avatarState, accentColor = stateAccentColor)
                Surface(
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = languageBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Right: Timer + Speed + Expand + Collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimerBadge(seconds = sessionSeconds)
                SpeedChip(
                    speechRate = speechRate,
                    onClick = onCycleSpeed
                )
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("expand_avatar_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull,
                        contentDescription = stringResource(
                            if (isExpanded) R.string.cd_collapse_stage else R.string.cd_expand_stage
                        ),
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (!isExpanded) {
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("minimize_avatar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            contentDescription = stringResource(R.string.cd_minimize_stage),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Vertical Lecture <-> Practice Mode Slider Switch on Right Edge
        VerticalModeSwitch(
            mode = tutorMode,
            onModeChange = onModeChange,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        )

        // Bottom Audio Waveform Strip (always visible in normal stage)
        AudioWaveformStrip(
            avatarState = avatarState,
            energy = when (avatarState) {
                AvatarState.SPEAKING -> lipSyncFrame.speechEnergy
                AvatarState.LISTENING -> micAmplitude.coerceAtLeast(0.28f)
                AvatarState.THINKING -> 0.35f
                AvatarState.IDLE -> 0.05f
            },
            accentColor = stateAccentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isExpanded) 110.dp else 10.dp, end = 36.dp)
        )

        // Fullscreen bottom caption + quick voice bar
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, StageDeepTop.copy(alpha = 0.94f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!liveCaption.isNullOrBlank()) {
                    Text(
                        text = liveCaption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (avatarState == AvatarState.SPEAKING) {
                        Surface(
                            onClick = onStopAudio,
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.testTag("fullscreen_stop_audio_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.action_stop_audio), color = Color.White)
                            }
                        }
                    }
                    Surface(
                        onClick = onToggleSpeak,
                        shape = RoundedCornerShape(50),
                        color = if (avatarState == AvatarState.LISTENING) StageCoralPulse else StageCyanGlow,
                        modifier = Modifier.testTag("fullscreen_speak_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (avatarState == AvatarState.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = StageDeepTop
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (avatarState == AvatarState.LISTENING) R.string.action_stop else R.string.action_speak
                                ),
                                color = StageDeepTop,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TalkingAiAvatarCanvas(
    avatarState: AvatarState,
    lipSyncFrame: LipSyncFrame,
    micAmplitude: Float,
    accentColor: Color,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Organic randomized eye blinking every 3–5 seconds
    val blinkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val waitMs = Random.nextLong(3000L, 5000L)
            delay(waitMs)
            blinkProgress.animateTo(1f, tween(70, easing = FastOutSlowInEasing))
            blinkProgress.animateTo(0f, tween(105, easing = FastOutSlowInEasing))
            // Occasional natural double blink
            if (Random.nextFloat() < 0.18f) {
                delay(120L)
                blinkProgress.animateTo(1f, tween(65))
                blinkProgress.animateTo(0f, tween(95))
            }
        }
    }

    // 2. Continuous subtle clock for head micro-movements, breathing, and orbital rings
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_motion")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "breath_phase"
    )
    val microPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "micro_phase"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit_angle"
    )

    // 3. Smoothly animated facial expression parameters
    val targetMouthOpen = if (avatarState == AvatarState.SPEAKING) lipSyncFrame.mouthOpenness else 0f
    val mouthOpen by animateFloatAsState(
        targetValue = targetMouthOpen,
        animationSpec = tween(durationMillis = 48, easing = FastOutSlowInEasing),
        label = "mouth_open"
    )
    val mouthWidth by animateFloatAsState(
        targetValue = if (avatarState == AvatarState.SPEAKING) lipSyncFrame.mouthWidth else 1f,
        animationSpec = tween(durationMillis = 70),
        label = "mouth_width"
    )

    val targetBrowLift = when {
        avatarState == AvatarState.SPEAKING && lipSyncFrame.isQuestionTone -> 1.0f
        avatarState == AvatarState.SPEAKING && lipSyncFrame.isEmphasis -> 0.65f
        avatarState == AvatarState.SPEAKING -> 0.25f + lipSyncFrame.speechEnergy * 0.25f
        avatarState == AvatarState.LISTENING -> 0.55f
        avatarState == AvatarState.THINKING -> -0.25f
        else -> 0.08f
    }
    val browLift by animateFloatAsState(
        targetValue = targetBrowLift,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 260f),
        label = "brow_lift"
    )

    val ringPulseTarget = when (avatarState) {
        AvatarState.SPEAKING -> 0.25f + lipSyncFrame.speechEnergy * 0.75f
        AvatarState.LISTENING -> 0.30f + micAmplitude * 0.70f
        AvatarState.THINKING -> 0.45f
        AvatarState.IDLE -> 0.12f + 0.06f * sin(breathPhase)
    }
    val ringPulse by animateFloatAsState(
        targetValue = ringPulseTarget,
        animationSpec = tween(durationMillis = 90),
        label = "ring_pulse"
    )

    // Compute gentle head tilt & micro-nod
    val headTiltDeg = when (avatarState) {
        AvatarState.SPEAKING -> sin(microPhase) * 2.8f + (if (lipSyncFrame.isQuestionTone) 3.4f else 0f)
        AvatarState.LISTENING -> 3.6f + sin(breathPhase) * 1.1f
        AvatarState.THINKING -> -3.0f + sin(breathPhase) * 1.4f
        AvatarState.IDLE -> sin(breathPhase) * 1.2f
    }
    val headNodOffset = when (avatarState) {
        AvatarState.SPEAKING -> sin(microPhase * 2f) * 2.5f + lipSyncFrame.speechEnergy * 2.8f
        AvatarState.IDLE -> sin(breathPhase) * 1.8f
        AvatarState.LISTENING -> sin(breathPhase) * 1.2f
        AvatarState.THINKING -> sin(breathPhase) * 1.0f
    }

    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * (if (isExpanded) 0.47f else 0.50f)
        val unit = minOf(size.width, size.height) * (if (isExpanded) 0.42f else 0.46f)

        // Ambient studio aura glow behind character
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.22f + ringPulse * 0.18f),
                    accentColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = unit * (1.55f + ringPulse * 0.24f)
            ),
            radius = unit * (1.55f + ringPulse * 0.24f),
            center = Offset(cx, cy)
        )

        // Speaking / Listening / Thinking Luminous Ring Indicator
        drawSpeakingRing(
            center = Offset(cx, cy),
            baseRadius = unit * 1.04f,
            accentColor = accentColor,
            pulse = ringPulse,
            avatarState = avatarState,
            orbitAngle = orbitAngle
        )

        // Shoulders & Futuristic Holographic Collar (subtle breathing)
        val breathY = sin(breathPhase) * (unit * 0.018f)
        drawShouldersAndCollar(
            cx = cx,
            cy = cy + breathY,
            unit = unit,
            accentColor = accentColor
        )

        // Head & Facial Expression Group with gentle tilt and micro-nod
        withTransform({
            translate(left = 0f, top = headNodOffset)
            rotate(degrees = headTiltDeg, pivot = Offset(cx, cy + unit * 0.35f))
        }) {
            drawHeadAndFace(
                cx = cx,
                cy = cy - unit * 0.04f,
                unit = unit,
                accentColor = accentColor,
                blink = blinkProgress.value,
                browLift = browLift,
                mouthOpen = mouthOpen,
                mouthWidth = mouthWidth,
                isThinking = avatarState == AvatarState.THINKING,
                isListening = avatarState == AvatarState.LISTENING
            )
        }
    }
}

private fun DrawScope.drawSpeakingRing(
    center: Offset,
    baseRadius: Float,
    accentColor: Color,
    pulse: Float,
    avatarState: AvatarState,
    orbitAngle: Float
) {
    // Outer soft reactive ripple
    if (avatarState == AvatarState.SPEAKING || avatarState == AvatarState.LISTENING) {
        drawCircle(
            color = accentColor.copy(alpha = (0.14f + pulse * 0.22f).coerceIn(0f, 0.5f)),
            radius = baseRadius * (1.06f + pulse * 0.16f),
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }

    // Core crisp studio ring
    drawCircle(
        color = accentColor.copy(alpha = 0.28f + pulse * 0.45f),
        radius = baseRadius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Thinking orbital sweep arc OR Speaking active energy arc
    val sweep = when (avatarState) {
        AvatarState.THINKING -> 110f
        AvatarState.SPEAKING -> 70f + pulse * 140f
        AvatarState.LISTENING -> 90f + pulse * 130f
        AvatarState.IDLE -> 45f
    }
    drawArc(
        color = accentColor.copy(alpha = 0.92f),
        startAngle = orbitAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
        size = Size(baseRadius * 2f, baseRadius * 2f),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawShouldersAndCollar(
    cx: Float,
    cy: Float,
    unit: Float,
    accentColor: Color
) {
    // Neck with subtle shading
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(StageSkinShadow, StageSkinMid),
            startY = cy + unit * 0.44f,
            endY = cy + unit * 0.78f
        ),
        topLeft = Offset(cx - unit * 0.18f, cy + unit * 0.44f),
        size = Size(unit * 0.36f, unit * 0.34f),
        cornerRadius = CornerRadius(unit * 0.12f, unit * 0.12f)
    )

    // Sleek futuristic jacket bust
    val bustPath = Path().apply {
        moveTo(cx - unit * 0.82f, cy + unit * 1.04f)
        quadraticTo(cx - unit * 0.68f, cy + unit * 0.66f, cx - unit * 0.24f, cy + unit * 0.66f)
        lineTo(cx + unit * 0.24f, cy + unit * 0.66f)
        quadraticTo(cx + unit * 0.68f, cy + unit * 0.66f, cx + unit * 0.82f, cy + unit * 1.04f)
        close()
    }
    drawPath(
        path = bustPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF283566), Color(0xFF1A2347)),
            start = Offset(cx - unit * 0.6f, cy + unit * 0.66f),
            end = Offset(cx + unit * 0.6f, cy + unit * 1.04f)
        )
    )

    // Luminous cyan/accent V-collar accent trim
    val collarPath = Path().apply {
        moveTo(cx - unit * 0.26f, cy + unit * 0.68f)
        lineTo(cx, cy + unit * 0.90f)
        lineTo(cx + unit * 0.26f, cy + unit * 0.68f)
    }
    drawPath(
        path = collarPath,
        color = accentColor.copy(alpha = 0.85f),
        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHeadAndFace(
    cx: Float,
    cy: Float,
    unit: Float,
    accentColor: Color,
    blink: Float,
    browLift: Float,
    mouthOpen: Float,
    mouthWidth: Float,
    isThinking: Boolean,
    isListening: Boolean
) {
    // Back hair silhouette
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(StageHairHighlight, StageHairDark),
            startY = cy - unit * 0.72f,
            endY = cy + unit * 0.45f
        ),
        topLeft = Offset(cx - unit * 0.64f, cy - unit * 0.68f),
        size = Size(unit * 1.28f, unit * 1.18f)
    )

    // Sleek AI Ear-Cuff / Holo Headset accents
    val earY = cy + unit * 0.04f
    listOf(-1f, 1f).forEach { dir ->
        val earX = cx + dir * unit * 0.54f
        drawOval(
            color = StageSkinMid,
            topLeft = Offset(earX - unit * 0.08f, earY - unit * 0.12f),
            size = Size(unit * 0.16f, unit * 0.24f)
        )
        // Glowing AI ear-piece dot
        drawCircle(
            color = accentColor.copy(alpha = if (isListening) 1f else 0.78f),
            radius = unit * 0.055f,
            center = Offset(earX + dir * unit * 0.02f, earY + unit * 0.02f)
        )
    }

    // Face contour with 3D-style soft radial shading + slight jaw drop when speaking wide vowels
    val jawDrop = mouthOpen * unit * 0.055f
    val facePath = Path().apply {
        moveTo(cx - unit * 0.50f, cy - unit * 0.12f)
        cubicTo(
            cx - unit * 0.52f, cy - unit * 0.60f,
            cx + unit * 0.52f, cy - unit * 0.60f,
            cx + unit * 0.50f, cy - unit * 0.12f
        )
        cubicTo(
            cx + unit * 0.48f, cy + unit * 0.34f,
            cx + unit * 0.26f, cy + unit * 0.56f + jawDrop,
            cx, cy + unit * 0.58f + jawDrop
        )
        cubicTo(
            cx - unit * 0.26f, cy + unit * 0.56f + jawDrop,
            cx - unit * 0.48f, cy + unit * 0.34f,
            cx - unit * 0.50f, cy - unit * 0.12f
        )
        close()
    }
    drawPath(
        path = facePath,
        brush = Brush.radialGradient(
            colors = listOf(StageSkinLight, StageSkinMid, StageSkinShadow),
            center = Offset(cx - unit * 0.10f, cy - unit * 0.12f),
            radius = unit * 0.76f
        )
    )

    // Subtle soft cheek warmth
    listOf(-1f, 1f).forEach { dir ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(StageCoralPulse.copy(alpha = 0.17f), Color.Transparent),
                center = Offset(cx + dir * unit * 0.26f, cy + unit * 0.14f),
                radius = unit * 0.16f
            ),
            radius = unit * 0.16f,
            center = Offset(cx + dir * unit * 0.26f, cy + unit * 0.14f)
        )
    }

    // Modern swept front bangs
    val bangsPath = Path().apply {
        moveTo(cx - unit * 0.54f, cy - unit * 0.14f)
        quadraticTo(cx - unit * 0.54f, cy - unit * 0.68f, cx, cy - unit * 0.66f)
        quadraticTo(cx + unit * 0.54f, cy - unit * 0.68f, cx + unit * 0.54f, cy - unit * 0.14f)
        quadraticTo(cx + unit * 0.32f, cy - unit * 0.38f, cx + unit * 0.08f, cy - unit * 0.34f)
        quadraticTo(cx - unit * 0.20f, cy - unit * 0.44f, cx - unit * 0.54f, cy - unit * 0.14f)
        close()
    }
    drawPath(
        path = bangsPath,
        brush = Brush.linearGradient(
            colors = listOf(StageHairHighlight, StageHairDark),
            start = Offset(cx - unit * 0.3f, cy - unit * 0.68f),
            end = Offset(cx + unit * 0.3f, cy - unit * 0.2f)
        )
    )

    // Expressive Eyebrows (subtle lift on questions/emphasis, thoughtful pinch when thinking)
    val eyeSpacing = unit * 0.22f
    val eyeY = cy - unit * 0.04f
    val browBaseY = eyeY - unit * 0.17f - (browLift * unit * 0.045f)
    listOf(-1f, 1f).forEach { dir ->
        val innerX = cx + dir * (eyeSpacing - unit * 0.09f)
        val outerX = cx + dir * (eyeSpacing + unit * 0.11f)
        val archLift = if (dir > 0 && browLift > 0.6f) unit * 0.055f else unit * 0.04f
        val innerOffset = if (isThinking) unit * 0.02f else 0f
        val browPath = Path().apply {
            moveTo(innerX, browBaseY + innerOffset)
            quadraticTo(cx + dir * eyeSpacing, browBaseY - archLift, outerX, browBaseY + unit * 0.015f)
        }
        drawPath(
            path = browPath,
            color = StageHairDark,
            style = Stroke(width = unit * 0.042f, cap = StrokeCap.Round)
        )
    }

    // Expressive Eyes with natural blinking, irises & specular catchlights
    val eyeOpenFactor = (1f - blink).coerceIn(0.06f, 1f)
    val eyeW = unit * 0.17f
    val eyeH = unit * 0.14f * eyeOpenFactor
    val gazeShiftX = if (isThinking) unit * 0.025f else 0f
    val gazeShiftY = if (isThinking) -unit * 0.015f else 0f

    listOf(-1f, 1f).forEach { dir ->
        val ex = cx + dir * eyeSpacing
        if (eyeOpenFactor > 0.18f) {
            // Sclera
            drawOval(
                color = Color.White,
                topLeft = Offset(ex - eyeW / 2f, eyeY - eyeH / 2f),
                size = Size(eyeW, eyeH)
            )
            // Iris with luminous indigo/cyan ring
            val irisR = minOf(eyeW * 0.34f, eyeH * 0.48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1F2A56), Color(0xFF394B94)),
                    center = Offset(ex + gazeShiftX, eyeY + gazeShiftY),
                    radius = irisR
                ),
                radius = irisR,
                center = Offset(ex + gazeShiftX, eyeY + gazeShiftY)
            )
            // Pupil
            drawCircle(
                color = Color(0xFF0E132B),
                radius = irisR * 0.52f,
                center = Offset(ex + gazeShiftX, eyeY + gazeShiftY)
            )
            // Specular highlight sparkle
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = irisR * 0.24f,
                center = Offset(ex + gazeShiftX - irisR * 0.28f, eyeY + gazeShiftY - irisR * 0.28f)
            )
            // Upper lash arc
            val lashPath = Path().apply {
                moveTo(ex - eyeW * 0.54f, eyeY)
                quadraticTo(ex, eyeY - eyeH * 0.72f, ex + eyeW * 0.54f, eyeY)
            }
            drawPath(
                path = lashPath,
                color = StageHairDark,
                style = Stroke(width = unit * 0.026f, cap = StrokeCap.Round)
            )
        } else {
            // Smooth closed blinking lash curve
            val closedPath = Path().apply {
                moveTo(ex - eyeW * 0.52f, eyeY)
                quadraticTo(ex, eyeY + unit * 0.045f, ex + eyeW * 0.52f, eyeY)
            }
            drawPath(
                path = closedPath,
                color = StageHairDark,
                style = Stroke(width = unit * 0.028f, cap = StrokeCap.Round)
            )
        }
    }

    // Subtle sculpted nose shadow
    val nosePath = Path().apply {
        moveTo(cx - unit * 0.03f, cy + unit * 0.14f)
        quadraticTo(cx, cy + unit * 0.17f, cx + unit * 0.035f, cy + unit * 0.14f)
    }
    drawPath(
        path = nosePath,
        color = StageSkinShadow.copy(alpha = 0.85f),
        style = Stroke(width = unit * 0.022f, cap = StrokeCap.Round)
    )

    // Articulated Lip-Sync Mouth & Jaw
    val mouthCenterY = cy + unit * 0.32f + jawDrop * 0.45f
    val halfW = unit * 0.175f * mouthWidth
    val openH = unit * 0.19f * mouthOpen

    if (mouthOpen < 0.06f) {
        // Warm natural closed smile when idle or paused cleanly
        val smilePath = Path().apply {
            moveTo(cx - halfW, mouthCenterY - unit * 0.015f)
            quadraticTo(cx, mouthCenterY + unit * 0.065f, cx + halfW, mouthCenterY - unit * 0.015f)
        }
        drawPath(
            path = smilePath,
            color = Color(0xFFB55864),
            style = Stroke(width = unit * 0.036f, cap = StrokeCap.Round)
        )
    } else {
        // Open mouth cavity (scales smoothly with vowel openness & roundedness)
        val cavityPath = Path().apply {
            moveTo(cx - halfW, mouthCenterY)
            quadraticTo(cx, mouthCenterY - openH * 0.42f, cx + halfW, mouthCenterY)
            quadraticTo(cx, mouthCenterY + openH * 1.15f, cx - halfW, mouthCenterY)
            close()
        }
        drawPath(
            path = cavityPath,
            color = Color(0xFF4A1926)
        )

        // Subtle tongue hint on wide open vowels
        if (mouthOpen > 0.42f) {
            drawOval(
                color = Color(0xFFD66B7A),
                topLeft = Offset(cx - halfW * 0.48f, mouthCenterY + openH * 0.25f),
                size = Size(halfW * 0.96f, openH * 0.42f)
            )
        }

        // Crisp upper teeth crescent on open speech sounds
        if (mouthOpen > 0.22f) {
            val teethH = (openH * 0.26f).coerceAtMost(unit * 0.048f)
            drawRoundRect(
                color = Color(0xFFF9FAFF),
                topLeft = Offset(cx - halfW * 0.62f, mouthCenterY - openH * 0.16f),
                size = Size(halfW * 1.24f, teethH),
                cornerRadius = CornerRadius(unit * 0.02f, unit * 0.02f)
            )
        }

        // Sculpted lips outline
        drawPath(
            path = cavityPath,
            color = Color(0xFFC45B6A),
            style = Stroke(width = unit * 0.028f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AudioWaveformStrip(
    avatarState: AvatarState,
    energy: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "waveform")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )
    val active = avatarState == AvatarState.SPEAKING || avatarState == AvatarState.LISTENING || avatarState == AvatarState.THINKING

    Canvas(
        modifier = modifier
            .width(148.dp)
            .height(22.dp)
    ) {
        val barCount = 15
        val spacing = size.width / barCount
        val barW = spacing * 0.46f
        val centerIdx = (barCount - 1) / 2f

        for (i in 0 until barCount) {
            val dist = kotlin.math.abs(i - centerIdx) / centerIdx
            val bell = 1f - (dist * dist * 0.72f)
            val wave = if (active) {
                0.35f + 0.65f * kotlin.math.abs(sin(phase + i * 0.65f))
            } else {
                0.15f
            }
            val barH = (size.height * (0.18f + energy * bell * wave * 0.82f))
                .coerceIn(3.dp.toPx(), size.height)
            val x = i * spacing + (spacing - barW) / 2f
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = accentColor.copy(alpha = if (active) 0.88f else 0.30f),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

@Composable
private fun VerticalModeSwitch(
    mode: TutorMode,
    onModeChange: (TutorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(46.dp)
            .testTag("vertical_mode_switch"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.11f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeSegmentItem(
                label = "Lec",
                icon = Icons.Rounded.School,
                selected = mode == TutorMode.LECTURE,
                activeColor = StageIndigoGlow,
                onClick = { onModeChange(TutorMode.LECTURE) },
                tag = "mode_lecture_button"
            )
            ModeSegmentItem(
                label = "Prac",
                icon = Icons.Rounded.RecordVoiceOver,
                selected = mode == TutorMode.PRACTICE,
                activeColor = StageCyanGlow,
                onClick = { onModeChange(TutorMode.PRACTICE) },
                tag = "mode_practice_button"
            )
        }
    }
}

@Composable
private fun ModeSegmentItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    val bg by animateColorAsState(
        targetValue = if (selected) activeColor else Color.Transparent,
        animationSpec = tween(220),
        label = "mode_bg"
    )
    val contentColor = if (selected) StageDeepTop else Color.White.copy(alpha = 0.72f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
private fun StateStatusPill(
    avatarState: AvatarState,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(Modifier.width(6.dp))
            AnimatedContent(targetState = avatarStateLabel(avatarState), label = "state_label") { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TimerBadge(seconds: Int) {
    val mins = seconds / 60
    val secs = seconds % 60
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("session_timer_text")
        )
    }
}

@Composable
private fun SpeedChip(
    speechRate: Float,
    onClick: () -> Unit
) {
    val label = when {
        speechRate < 0.9f -> "0.8x"
        speechRate > 1.1f -> "1.2x"
        else -> "1x"
    }
    Surface(
        onClick = onClick,
        color = StageAmberGlow.copy(alpha = 0.18f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("speed_toggle_button")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = StageAmberGlow,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun avatarStateLabel(state: AvatarState): String = when (state) {
    AvatarState.IDLE -> stringResource(R.string.avatar_state_idle)
    AvatarState.LISTENING -> stringResource(R.string.avatar_state_listening)
    AvatarState.THINKING -> stringResource(R.string.avatar_state_thinking)
    AvatarState.SPEAKING -> stringResource(R.string.avatar_state_speaking)
}
