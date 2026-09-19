package com.example.ui.dialogs

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GoalEntity
import com.example.util.CertificateGenerator

@Composable
fun CertificateDialog(
    goal: GoalEntity,
    totalLoggedSeconds: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var achieverName by remember { mutableStateOf("Goal Achiever") }
    var certificateBitmap by remember(achieverName, goal, totalLoggedSeconds) {
        mutableStateOf(
            CertificateGenerator.generateCertificateBitmap(
                goal = goal,
                totalLoggedSeconds = totalLoggedSeconds,
                achieverName = achieverName.ifBlank { "Goal Achiever" }
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Certificate of Completion",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Congratulations on completing your goal! Customize your official certificate of accomplishment below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = achieverName,
                    onValueChange = {
                        achieverName = it
                        certificateBitmap = CertificateGenerator.generateCertificateBitmap(
                            goal = goal,
                            totalLoggedSeconds = totalLoggedSeconds,
                            achieverName = it.ifBlank { "Goal Achiever" }
                        )
                    },
                    label = { Text("Presented To (Name)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achiever_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Certificate image preview
                Image(
                    bitmap = certificateBitmap.asImageBitmap(),
                    contentDescription = "Certificate of Completion Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1400f / 980f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Save & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val uri = CertificateGenerator.saveCertificateToDevice(
                                context = context,
                                bitmap = certificateBitmap,
                                goalName = goal.name
                            )
                            if (uri != null) {
                                Toast.makeText(
                                    context,
                                    "Certificate saved to Pictures/GoalTracker!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to save certificate.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_certificate_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Image", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val uri = CertificateGenerator.saveCertificateToDevice(
                                context = context,
                                bitmap = certificateBitmap,
                                goalName = goal.name
                            )
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Goal Completed: ${goal.name}")
                                    putExtra(Intent.EXTRA_TEXT, "I achieved my goal '${goal.name}' with Goal Tracker!")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Certificate")
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_certificate_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
