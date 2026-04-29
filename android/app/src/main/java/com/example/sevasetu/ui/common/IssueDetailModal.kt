package com.example.sevasetu.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.sevasetu.data.remote.dto.IssueDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssueDetailModal(
    issue: IssueDto,
    onDismiss: () -> Unit,
    onLocationClick: ((lat: Double, lng: Double) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                IssueDetailHeader(
                    title = issue.title.ifBlank { "Untitled issue" },
                    onDismiss = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(issue.status)
                        PriorityBadge(issue.priority)
                        issue.category?.name?.takeIf { it.isNotBlank() }?.let { CategoryBadge(it) }
                    }

                    IssueImages(imageUrls = issue.resolveImageUrls())

                    DetailSection(title = "Description") {
                        Text(
                            text = issue.description?.takeIf { it.isNotBlank() }
                                ?: "No detailed description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF34423A),
                            lineHeight = 20.sp
                        )
                    }

                    if (!issue.addressText.isNullOrBlank() || issue.hasCoordinates()) {
                        DetailSection(title = "Location") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF00875A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = issue.addressText?.takeIf { it.isNotBlank() }
                                            ?: "Location unavailable",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF2D2D2D)
                                    )
                                    if (issue.hasCoordinates()) {
                                        Text(
                                            text = "Coordinates: ${formatCoordinate(issue.lat)}, ${formatCoordinate(issue.lng)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6E7C73)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DetailSection(title = "Details") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DetailRow(label = "Issue ID: ", value = issue.id)
                            DetailRow(label = "Created: ", value = formatDate(issue.createdAt))
                            DetailRow(label = "Updated: ", value = formatDate(issue.updatedAt))
                            DetailRow(label = "Status: ", value = issue.status.toDisplayLabel("Unknown"))
                            DetailRow(label = "Priority: ", value = issue.priority.toDisplayLabel("Medium"))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE7EEE9))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Close")
                    }

                    if (issue.hasCoordinates() && onLocationClick != null) {
                        Button(
                            onClick = { onLocationClick(issue.lat!!, issue.lng!!) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A))
                        ) {
                            Text("View Map")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueDetailHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7FBF8))
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Issue Details",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF00875A),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF17231D),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun IssueImages(imageUrls: List<String>) {
    if (imageUrls.isEmpty()) {
        DetailSection(title = "Images") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F4F2)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF9AA79F))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No images attached", color = Color(0xFF6E7C73), fontSize = 13.sp)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Images")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(imageUrls.size) { index ->
                AsyncImage(
                    model = imageUrls[index],
                    contentDescription = "Issue image ${index + 1}",
                    modifier = Modifier
                        .size(width = 188.dp, height = 136.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F4F2)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FBF9),
            border = BorderStroke(1.dp, Color(0xFFE1EBE5))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFF17231D),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatusBadge(status: String?) {
    val (backgroundColor, textColor) = when (status?.uppercase(Locale.ROOT)) {
        "OPEN" -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        "IN_PROGRESS" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "RESOLVED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        else -> Color(0xFFF1F4F2) to Color(0xFF5D6B63)
    }

    InfoBadge(
        text = status.toDisplayLabel("Unknown"),
        backgroundColor = backgroundColor,
        textColor = textColor
    )
}

@Composable
private fun PriorityBadge(priority: String?) {
    val (backgroundColor, textColor) = when (priority?.uppercase(Locale.ROOT)) {
        "HIGH" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "LOW" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> Color(0xFFFFF8E1) to Color(0xFFEF6C00)
    }

    InfoBadge(
        text = "Priority: ${priority.toDisplayLabel("Medium")}",
        backgroundColor = backgroundColor,
        textColor = textColor
    )
}

@Composable
private fun CategoryBadge(category: String) {
    InfoBadge(
        text = category,
        backgroundColor = Color(0xFFE8F5E9),
        textColor = Color(0xFF00875A)
    )
}

@Composable
private fun InfoBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6E7C73),
            modifier = Modifier.widthIn(max = 112.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D2D2D),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun IssueDto.resolveImageUrls(): List<String> {
    return buildList {
        images.forEach { image ->
            image.imageUrl?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        }
        imageUrls?.forEach { imageUrl ->
            imageUrl.trim().takeIf(String::isNotEmpty)?.let(::add)
        }
        imageUrl?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    }.distinct()
}

private fun IssueDto.hasCoordinates(): Boolean = lat != null && lng != null

private fun String?.toDisplayLabel(fallback: String): String {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return fallback
    return value
        .lowercase(Locale.ROOT)
        .split('_', ' ')
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.ROOT) } }
}

private fun formatCoordinate(value: Double?): String {
    return if (value == null) "N/A" else String.format(Locale.US, "%.5f", value)
}

private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "N/A"

    val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
        .withZone(ZoneId.systemDefault())

    return try {
        outputFormatter.format(Instant.parse(dateString))
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(dateString)
                .atZone(ZoneId.systemDefault())
                .format(outputFormatter)
        } catch (_: DateTimeParseException) {
            dateString
        }
    }
}
