package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.GalleryImage
import com.example.viewmodel.BackgroundSettings
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import java.io.File

// Defined presets for theme customization
object GalleryPresets {
    val SOLIDS = listOf(
        "Off-Black" to "#0A0A0C",
        "Slate Shadow" to "#0F172A",
        "Royal Violet" to "#1E1B4B",
        "Hunter Forest" to "#022C22",
        "Burgundy Velvet" to "#2D0B0B",
        "Sweet Lavender" to "#4C1D95",
        "Cream Linen" to "#FAF7F2",
        "Soft Rose" to "#FFF1F2"
    )

    val GRADIENTS = listOf(
        "Cosmic Dusk" to listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        "Northern Lights" to listOf(Color(0xFF051937), Color(0xFF004D7A), Color(0xFF008793), Color(0xFF00BF72)),
        "Sunset Flare" to listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
        "Neon Purple" to listOf(Color(0xFFF72585), Color(0xFF7209B7), Color(0xFF3F37C9)),
        "Ocean Wave" to listOf(Color(0xFF2193B0), Color(0xFF6DDCF4)),
        "Irish Mint" to listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        "Dark Obsidian" to listOf(Color(0xFF121212), Color(0xFF2A2A2A))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryHomeScreen(
    viewModel: GalleryViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val images by viewModel.imagesState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var selectedImgForViewer by remember { mutableStateOf<GalleryImage?>(null) }

    // Custom background rendering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .renderGalleryBackground(settings)
    ) {
        // Dot texture overlay if enabled
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawDotGrid(settings, isLightMode(settings))
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Scaffold(
                containerColor = Color.Transparent, // Transparent scaffold to show our gorgeous custom background!
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "PERSONAL GALLERY",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = if (isLightMode(settings)) Color(0xFF1F2937) else Color.White
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            IconButton(
                                onClick = { showCustomizeSheet = true },
                                modifier = Modifier.testTag("customize_theme_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Customize Background",
                                    tint = if (isLightMode(settings)) Color(0xFF1F2937) else Color.White
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showAddImageDialog = true },
                        icon = { Icon(Icons.Filled.Add, "Add Image Action") },
                        text = { Text("Upload Title", fontWeight = FontWeight.SemiBold) },
                        containerColor = if (isLightMode(settings)) MaterialTheme.colorScheme.primary else Color.White,
                        contentColor = if (isLightMode(settings)) MaterialTheme.colorScheme.onPrimary else Color(0xFF0F172A),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("floating_upload_button")
                            .padding(bottom = 24.dp, end = 8.dp)
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (images.isEmpty()) {
                        EmptyStateView(
                            settings = settings,
                            onUploadClicked = { showAddImageDialog = true }
                        )
                    } else {
                        ImageLibraryGrid(
                            images = images,
                            settings = settings,
                            onImageClicked = { selectedImgForViewer = it },
                            onDeleteClicked = { viewModel.deleteImage(it) },
                            onTitleUpdated = { img, title -> viewModel.updateImageTitle(img, title) },
                            onSetAsBackground = { img ->
                                // Set this image's copy file as the gallery background
                                viewModel.saveBackgroundType("image")
                                coroutineScope.launch {
                                    viewModel.saveCustomImageBackground(context, Uri.fromFile(File(img.localPath))) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Background updated successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

// Modal Background Customizer Sheet
    if (showCustomizeSheet) {
        CustomizeThemeSheet(
            settings = settings,
            onDismiss = { showCustomizeSheet = false },
            onThemeChanged = { mode -> viewModel.saveThemeMode(mode) },
            onSolidSelected = { hexString -> viewModel.saveBackgroundValueColor(hexString) },
            onGradientSelected = { name -> viewModel.saveBackgroundValueGradient(name) },
            onCustomBackgroundUploaded = { uri ->
                viewModel.saveCustomImageBackground(context, uri) { success ->
                    if (success) {
                        Toast.makeText(context, "Custom background saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to load custom background image", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBlurChanged = { viewModel.updateLiveBlur(it) },
            onDimChanged = { viewModel.updateLiveDim(it) },
            onColumnsChanged = { viewModel.saveGridColumns(it) },
            onToggleDots = { viewModel.saveDotOverlay(it) },
            onBlurFinished = { viewModel.saveSliderBlur(settings.bgBlur) },
            onDimFinished = { viewModel.saveSliderDim(settings.bgDim) }
        )
    }

    // Bluetooth share dialogue removed

    // Bluetooth P2P sync dialogue removed

    // Upload & Title Image Modal Dialog
    if (showAddImageDialog) {
        AddImageDialog(
            onDismiss = { showAddImageDialog = false },
            onAddImage = { uri, title, isVideo ->
                viewModel.importMedia(context, uri, title, isVideo) { success ->
                    if (success) {
                        showAddImageDialog = false
                        Toast.makeText(context, "Media uploaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to process media file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

    // Placeholder for removed Bluetooth dialog functions

// Check if active settings lead to a light mode look-and-feel (for label color contrasts)
fun isLightMode(settings: BackgroundSettings): Boolean {
    if (settings.type == "solid") {
        return settings.valColor.equals("#FAF7F2", ignoreCase = true) || 
               settings.valColor.equals("#FFF1F2", ignoreCase = true)
    }
    return false
}

// Background utility mod
fun Modifier.renderGalleryBackground(settings: BackgroundSettings): Modifier = this.then(
    drawBehind {
        when (settings.type) {
            "solid" -> {
                val hexStr = settings.valColor.replace("#", "")
                val colorHex = hexStr.toLongOrNull(16) ?: 0xFF0F172A
                val finalColor = if (hexStr.length == 6) Color(colorHex or 0xFF000000) else Color(colorHex)
                drawRect(finalColor)
            }
            "gradient" -> {
                val colorList = GalleryPresets.GRADIENTS.firstOrNull { it.first == settings.valGradient }?.second
                    ?: listOf(Color(0xFF0F2027), Color(0xFF2C5364))
                val brush = Brush.linearGradient(
                    colors = colorList,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawRect(brush)
            }
            "image" -> {
                // If it is custom image background, it will render under the image rendering modifier.
                // We'll draw solid charcoal here as fallback
                drawRect(Color(0xFF0F172A))
            }
            else -> drawRect(Color(0xFF0F172A))
        }
    }
).then(
    if (settings.type == "image" && settings.bgImagePath.isNotEmpty()) {
        Modifier.drawBehind {
            // Drawn back layer
        }
    } else {
        Modifier
    }
)

@Composable
fun BoxScope.renderCustomImageBackground(settings: BackgroundSettings) {
    if (settings.type == "image" && settings.bgImagePath.isNotEmpty()) {
        val file = File(settings.bgImagePath)
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "Wallpaper background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (settings.bgBlur > 0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            it.blur(radius = settings.bgBlur.dp)
                        } else {
                            it
                        }
                    }
                    .graphicsLayer { alpha = 1f }
            )
            // Mask layer to control dim ratio and maintain excellent contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.bgDim))
            )
        }
    }
}

// Custom Extension to paint Dot Grid texture
fun Modifier.drawDotGrid(settings: BackgroundSettings, isLight: Boolean): Modifier = this.then(
    Modifier.drawBehind {
        if (settings.showDotOverlay) {
            val dotRadius = 1.1.dp.toPx()
            val spacing = 22.dp.toPx()
            val color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
            if (spacing > 1f) {
                var x = 11.dp.toPx()
                while (x < size.width) {
                    var y = 11.dp.toPx()
                    while (y < size.height) {
                        drawCircle(color, dotRadius, Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }
        }
    }
)

@Composable
fun Modifier.renderBackgroundLayer(settings: BackgroundSettings) = this.then(
    if (settings.type == "image" && settings.bgImagePath.isNotEmpty()) {
        Modifier.drawBehind {
            // Draw dummy black rect as backup
        }
    } else Modifier
)

// Empty State layout
@Composable
fun EmptyStateView(settings: BackgroundSettings, onUploadClicked: () -> Unit) {
    val contentColor = if (isLightMode(settings)) Color(0xFF374151) else Color.White.copy(alpha = 0.85f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    if (isLightMode(settings)) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isLightMode(settings)) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "Empty Gallery Library Icon",
                modifier = Modifier.size(56.dp),
                tint = if (isLightMode(settings)) Color(0xFF4B5563) else Color.White.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your Gallery is Empty",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Upload high-quality custom photos from your device's photo gallery and specify titles to populate your artistic library.",
            fontSize = 14.sp,
            color = if (isLightMode(settings)) Color(0xFF6B7280) else Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onUploadClicked,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLightMode(settings)) MaterialTheme.colorScheme.primary else Color.White,
                contentColor = if (isLightMode(settings)) MaterialTheme.colorScheme.onPrimary else Color(0xFF0F172A)
            ),
            modifier = Modifier.testTag("empty_state_action_button")
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Photo", fontWeight = FontWeight.SemiBold)
        }
    }
}

// Master grid system showing images with customized backgrounds
@Composable
fun BoxScope.ImageLibraryGrid(
    images: List<GalleryImage>,
    settings: BackgroundSettings,
    onImageClicked: (GalleryImage) -> Unit,
    onDeleteClicked: (GalleryImage) -> Unit,
    onTitleUpdated: (GalleryImage, String) -> Unit,
    onSetAsBackground: (GalleryImage) -> Unit
) {
    // Render custom wallpaper background image first, underneath everything
    renderCustomImageBackground(settings = settings)

    LazyVerticalGrid(
        columns = GridCells.Fixed(settings.gridColumns),
        modifier = Modifier
            .fillMaxSize()
            .testTag("gallery_photo_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(images, key = { it.id }) { image ->
            GalleryGridCard(
                image = image,
                settings = settings,
                onCardClick = { onImageClicked(image) },
                onDelete = { onDeleteClicked(image) },
                onEditTitle = { newTitle -> onTitleUpdated(image, newTitle) },
                onSetAsBg = { onSetAsBackground(image) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

// Individual Polaroid / Sleek modern grid card
@Composable
fun GalleryGridCard(
    image: GalleryImage,
    settings: BackgroundSettings,
    onCardClick: () -> Unit,
    onDelete: () -> Unit,
    onEditTitle: (String) -> Unit,
    onSetAsBg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = File(image.localPath)
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = {
                if (image.isVideo) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        intent.setDataAndType(uri, "video/*")
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Could not open video player: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    onCardClick()
                }
            })
            .testTag("image_card_${image.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightMode(settings)) Color.White else Color(0xFF1E1E26)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLightMode(settings)) Color(0xFFF3F4F6) else Color(0xFF2D2D38)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
            ) {
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = image.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "File not found",
                            tint = Color.Red.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (image.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Post Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = image.title.ifBlank { "Untitled" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("menu_button_${image.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Image Menu Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = "Added: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(image.dateAdded))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Context menu for the card
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Fullscreen") },
                    onClick = { showMenu = false; onCardClick() },
                    leadingIcon = { Icon(Icons.Default.Fullscreen, null) }
                )
                DropdownMenuItem(
                    text = { Text("Background") },
                    onClick = { showMenu = false; onSetAsBg() },
                    leadingIcon = { Icon(Icons.Default.Panorama, null) }
                )
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { showMenu = false; showEditDialog = true },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }

    // Modal dialog to Edit Title
    if (showEditDialog) {
        var tempTitle by remember { mutableStateOf(image.title) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modify Image Title", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditTitle(tempTitle)
                        showEditDialog = false
                    },
                    modifier = Modifier.testTag("confirm_edit_title")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Customize Background & Style bottom sheet dialog (Manual implement with beautiful dialog overlay / slides)
@Composable
fun CustomizeThemeSheet(
    settings: BackgroundSettings,
    onDismiss: () -> Unit,
    onThemeChanged: (String) -> Unit,
    onSolidSelected: (String) -> Unit,
    onGradientSelected: (String) -> Unit,
    onCustomBackgroundUploaded: (Uri) -> Unit,
    onBlurChanged: (Float) -> Unit,
    onDimChanged: (Float) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onToggleDots: (Boolean) -> Unit,
    onBlurFinished: () -> Unit = {},
    onDimFinished: () -> Unit = {}
) {
    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCustomBackgroundUploaded(uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onDismiss,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Elegant Blurred Sheet Background Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {}) // Block tap propagation
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top drag indicator anchor
                    Box(
                        modifier = Modifier
                            .size(36.dp, 4.dp)
                            .background(Color.Gray.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Customize Gallery Backdrop",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

// SECTION 0: Theme Apperance
                    Text("App Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("system", "light", "dark").forEach { mode ->
                            val selected = settings.themeMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onThemeChanged(mode) },
                                label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Style Options
                    Text("Background Style", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 1: Solid Colors
                    Text("Solid Backdrops", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GalleryPresets.SOLIDS.forEach { (name, hex) ->
                            val hexStr = hex.replace("#", "")
                            val colorHex = hexStr.toLongOrNull(16) ?: 0xFF0F172A
                            val color = if (hexStr.length == 6) Color(colorHex or 0xFF000000) else Color(colorHex)
                            val selected = settings.type == "solid" && settings.valColor.equals(hex, true)

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                            alpha = 0.3f
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { onSolidSelected(hex) }
                                    .testTag("solid_btn_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = if (name.contains("Cream") || name.contains("Rose")) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: Gradients
                    Text("Flowing Gradients", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GalleryPresets.GRADIENTS.forEach { (name, colors) ->
                            val selected = settings.type == "gradient" && settings.valGradient == name

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Brush.linearGradient(colors), shape = CircleShape)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                            alpha = 0.3f
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { onGradientSelected(name) }
                                    .testTag("grad_btn_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // SECTION 3: Custom Backdrop photo uploaded by user
                    Text("Your Custom Photos Backdrop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                bgImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_bg_image_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Wallpaper Pic")
                        }

                        if (settings.type == "image" && settings.bgImagePath.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = File(settings.bgImagePath),
                                    contentDescription = "Current custom backdrop thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (settings.type == "image" && settings.bgImagePath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Custom Blur slider control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Coil Blur Focus:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                            Slider(
                                value = settings.bgBlur,
                                onValueChange = onBlurChanged,
                                onValueChangeFinished = onBlurFinished,
                                valueRange = 0f..25f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("blur_slider")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${settings.bgBlur.toInt()}dp", fontSize = 12.sp)
                        }

                        // Dim darkness slider control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Black Shading:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                            Slider(
                                value = settings.bgDim,
                                onValueChange = onDimChanged,
                                onValueChangeFinished = onDimFinished,
                                valueRange = 0.05f..0.85f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dim_slider")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${(settings.bgDim * 100).toInt()}%", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 4: Grid Adjuster & Mesh patterns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Layout Density", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text("Grid Columns: ${settings.gridColumns}", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3, 4).forEach { col ->
                                val active = settings.gridColumns == col
                                OutlinedButton(
                                    onClick = { onColumnsChanged(col) },
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("grid_col_$col"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "$col",
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Backdrop Grid Mesh Overlay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text("Subtle dot alignment grid lines", fontSize = 11.sp, color = Color.Gray)
                        }
                        
                        Switch(
                            checked = settings.showDotOverlay,
                            onCheckedChange = onToggleDots,
                            modifier = Modifier.testTag("mesh_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dismiss_customize_button")
                    ) {
                        Text("Apply Background Preferences", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// Dialog to Select Pic & Enter Image Title before saving to database
@Composable
fun AddImageDialog(
    onDismiss: () -> Unit,
    onAddImage: (Uri, String, Boolean) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val contentResolverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("add_image_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Upload Image with Title",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Photo Selection card frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(
                            width = 1.1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            contentResolverPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .testTag("picker_zone_clickable"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUri != null) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Preview selected picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Pick Gallery Image File",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Image Title Label") },
                    placeholder = { Text("Nature, Travel memory, etc.") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_title_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val uri = selectedPhotoUri
                            if (uri == null) {
                                // Add warning toasts/validation
                                return@Button
                            }
                            val mimeType = context.contentResolver.getType(uri)
                            val isVideo = mimeType?.startsWith("video/") == true
                            onAddImage(uri, titleText.ifBlank { "Untitled Memory" }, isVideo)
                        },
                        enabled = selectedPhotoUri != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_image_save_button")
                    ) {
                        Text("Add Picture")
                    }
                }
            }
        }
    }
}

// Gorgeous full screen Immersive Image viewer with horizontal swipe navigation and titles
@Composable
fun ImmersiveGalleryViewer(
    images: List<GalleryImage>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val activeImage = images.getOrNull(currentIndex) ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Photo displayed core
                val file = File(activeImage.localPath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = activeImage.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Image file loaded unsuccessfully", color = Color.White)
                    }
                }

                // Close Button Top-Left
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.TopStart)
                        .testTag("immersive_viewer_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Fullscreen View",
                        tint = Color.White
                    )
                }

                // Left Navigation indicator button if there's previous photo
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("immersive_viewer_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Image",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Right Navigation indicator button if there's next photo
                if (currentIndex < images.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("immersive_viewer_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Image",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Transparent Bottom Panel with visual title overlaid neatly
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeImage.title.ifBlank { "Untitled Picture" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Image ${currentIndex + 1} of ${images.size}",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// BluetoothShareDialog and associated UI removed

// Bluetooth share functionality and BluetoothP2PSyncDialog fully removed

// BluetoothP2PSyncDialog and related bluetooth functionality removed
