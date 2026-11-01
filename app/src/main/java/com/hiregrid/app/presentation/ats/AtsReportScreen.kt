package com.hiregrid.app.presentation.ats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiregrid.app.core.ui.components.HireGridButton
import com.hiregrid.app.core.ui.theme.Shapes
import com.hiregrid.app.core.ui.theme.Success
import com.hiregrid.app.core.ui.theme.Warning
import com.hiregrid.app.data.model.AtsReportDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtsReportScreen(
    report: AtsReportDto,
    onClose: () -> Unit
) {
    val overallScore = report.overallScore.toInt()
    val scoreColor = when {
        overallScore >= 75 -> Success
        overallScore >= 50 -> Warning
        else -> MaterialTheme.colorScheme.error
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ATS Analysis Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Circular Score Dial
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(scoreColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$overallScore%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "Compatibility",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sub-Scores Breakdown (if Job Match)
            if (report.skillMatchScore != null || report.experienceScore != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubScoreCard(
                        title = "Skills Match",
                        score = "${report.skillMatchScore?.toInt() ?: 0}%",
                        color = Success,
                        modifier = Modifier.weight(1f)
                    )
                    SubScoreCard(
                        title = "Experience",
                        score = "${report.experienceScore?.toInt() ?: 0}%",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SubScoreCard(
                        title = "Keywords",
                        score = "${report.keywordScore?.toInt() ?: 0}%",
                        color = Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Matched Skills Section
            if (report.matchedSkills.isNotEmpty()) {
                FeedbackCard(
                    title = "Matching Skills Detected (${report.matchedSkills.size})",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Success
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        report.matchedSkills.forEach { skill ->
                            Surface(
                                shape = Shapes.small,
                                color = Success.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Success,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Missing Skills Section
            if (report.missingSkills.isNotEmpty()) {
                FeedbackCard(
                    title = "Missing Required Skills (${report.missingSkills.size})",
                    icon = Icons.Default.Warning,
                    iconColor = Warning
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        report.missingSkills.forEach { skill ->
                            Surface(
                                shape = Shapes.small,
                                color = Warning.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Warning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Strengths Section
            if (report.strengths.isNotEmpty()) {
                FeedbackCard(
                    title = "Resume Strengths",
                    icon = Icons.Default.ThumbUp,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    report.strengths.forEach { strength ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = strength, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Actionable Improvement Suggestions
            if (report.improvementSuggestions.isNotEmpty()) {
                FeedbackCard(
                    title = "Actionable Recommendations",
                    icon = Icons.Default.Lightbulb,
                    iconColor = Warning
                ) {
                    report.improvementSuggestions.forEach { suggestion ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("→ ", fontWeight = FontWeight.Bold, color = Warning)
                            Text(text = suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            HireGridButton(
                text = "Done",
                onClick = onClose
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SubScoreCard(
    title: String,
    score: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = score, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
