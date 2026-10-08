package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SavedProject
import com.example.ui.components.EclipseEyeLogo
import com.example.ui.theme.NoirAccentMuted
import com.example.ui.theme.NoirAccentSilver
import com.example.ui.theme.NoirAccentWhite
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirBorderLight
import com.example.ui.theme.NoirDark
import com.example.ui.theme.NoirPitchBlack
import com.example.ui.theme.NoirSurface
import com.example.ui.theme.NoirSurfaceElevated
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import com.example.ui.theme.NoirTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    savedProjects: List<SavedProject>,
    onSelectImageUri: (Uri) -> Unit,
    onSelectSample: () -> Unit,
    onOpenProject: (SavedProject) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var isInfoDialogVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onSelectImageUri(it) }
    }

    Scaffold(
        containerColor = NoirPitchBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Info icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { isInfoDialogVisible = true },
                    modifier = Modifier.testTag("info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About UMBRA",
                        tint = NoirTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // The Eclipse Eye Logo
            EclipseEyeLogo(size = 68.dp)

            Spacer(modifier = Modifier.height(18.dp))

            // App Name & Subtitle: UMBRA
            Text(
                text = "UMBRA",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    letterSpacing = 12.sp,
                    color = NoirAccentWhite,
                    fontWeight = FontWeight.ExtraLight
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SEE THE DARK DIFFERENTLY",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 4.sp,
                    fontSize = 11.sp,
                    color = NoirTextTertiary
                )
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Primary Import Action
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NoirAccentWhite,
                    contentColor = NoirPitchBlack
                ),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("import_image_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "IMPORT PHOTOGRAPH",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Sample Action
            OutlinedButton(
                onClick = onSelectSample,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NoirAccentSilver
                ),
                border = BorderStroke(1.dp, NoirBorder),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("sample_image_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NoirTextSecondary
                    )
                    Text(
                        text = "OPEN SAMPLE PHOTO (MONOCHROME STUDY)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            color = NoirTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Recent Projects Archive Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NoirTextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "DARKROOM ARCHIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontSize = 11.sp,
                            color = NoirTextSecondary
                        )
                    )
                }

                Text(
                    text = "${savedProjects.size} PROJECTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NoirTextTertiary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = NoirBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Projects List
            if (savedProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO PREVIOUS EDITS RECORDED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            color = NoirTextTertiary,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedProjects, key = { it.id }) { project ->
                        ProjectListItem(
                            project = project,
                            onClick = { onOpenProject(project) },
                            onDelete = { onDeleteProject(project.id) }
                        )
                    }
                }
            }
        }
    }

    if (isInfoDialogVisible) {
        UmbraAboutDialog(onDismiss = { isInfoDialogVisible = false })
    }
}

@Composable
private fun ProjectListItem(
    project: SavedProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.timestamp) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(project.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoirSurface, RoundedCornerShape(2.dp))
            .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.sp,
                        color = NoirAccentWhite,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${project.resolutionString()} (${project.megapixelsString()})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NoirAccentSilver,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(color = NoirBorderLight)
                    )
                    Text(
                        text = project.editState.presetId.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NoirTextTertiary,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NoirTextTertiary,
                        fontSize = 9.sp
                    )
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Project",
                    tint = NoirTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UmbraAboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = NoirSurface,
            border = BorderStroke(1.dp, NoirBorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("about_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Eclipse Eye Logo Emblem
                EclipseEyeLogo(size = 48.dp)

                Spacer(modifier = Modifier.height(14.dp))

                // Brand Title
                Text(
                    text = "UMBRA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Light,
                        color = NoirAccentWhite
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Creator Information
                Text(
                    text = "Created by Zayd",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoirAccentSilver
                    )
                )

                Text(
                    text = "Artist • Writer • Creator",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        color = NoirTextTertiary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Creator's Statement
                Text(
                    text = "UMBRA is an image editor built around my fascination with monochrome, contrast, atmosphere, and visual storytelling.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = NoirTextSecondary,
                        lineHeight = 17.sp,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = NoirBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Social Media: INSTAGRAM @obscurithmic
                Button(
                    onClick = {
                        openUrl(context, "https://www.instagram.com/obscurithmic/")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoirSurfaceElevated,
                        contentColor = NoirAccentWhite
                    ),
                    border = BorderStroke(1.dp, NoirBorderLight),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("instagram_link_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INSTAGRAM @obscurithmic",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NoirAccentWhite
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = NoirTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email Contact: obsurithmic@gmail.com
                OutlinedButton(
                    onClick = {
                        openEmail(context, "obsurithmic@gmail.com")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NoirTextSecondary
                    ),
                    border = BorderStroke(1.dp, NoirBorder),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("email_link_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = NoirTextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "obsurithmic@gmail.com",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NoirTextSecondary
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = NoirTextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy Policy Link
                OutlinedButton(
                    onClick = {
                        openUrl(context, "https://www.termsfeed.com/live/6f22a35b-02be-44f6-b211-46743c968ce3")
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NoirTextTertiary
                    ),
                    border = BorderStroke(1.dp, NoirBorder),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("privacy_policy_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = NoirTextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Privacy Policy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp,
                                    color = NoirTextTertiary
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = NoirTextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Version display
                Text(
                    text = "Version 1.2",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = NoirTextTertiary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NoirAccentWhite,
                        contentColor = NoirPitchBlack
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CLOSE",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp)
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openEmail(context: Context, emailAddress: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$emailAddress")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
