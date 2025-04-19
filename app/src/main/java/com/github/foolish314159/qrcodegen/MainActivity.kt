package com.github.foolish314159.qrcodegen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import com.github.foolish314159.qrcodegen.ui.theme.QRCodeGenTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { data ->
                        setContent {
                            App(data)
                        }
                    }
                }
            }

            else -> {
                setContent {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Use system Share feature to share text with this app")
                    }
                }
            }
        }
    }
}

@Composable
fun App(data: String) {
    Column(verticalArrangement = Arrangement.Center) {
        QRCode(data, 1600)
    }
}

@Composable
fun QRCode(data: String, qrSize: Int) {
    // Mutable state variables to hold scale and offset values
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val minScale = 1f
    val maxScale = 4f

    // Remember the initial offset
    var initialOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Coefficient for slowing down movement
    val slowMovement = 0.5f

    Surface(
        color = Color.White, modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Update scale with the zoom
                    val newScale = scale * zoom
                    scale = newScale.coerceIn(minScale, maxScale)

                    // Calculate new offsets based on zoom and pan
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val offsetXChange = (centerX - offsetX) * (newScale / scale - 1)
                    val offsetYChange = (centerY - offsetY) * (newScale / scale - 1)

                    // Calculate min and max offsets
                    val maxOffsetX = (size.width / 2) * (scale - 1)
                    val minOffsetX = -maxOffsetX
                    val maxOffsetY = (size.height / 2) * (scale - 1)
                    val minOffsetY = -maxOffsetY

                    // Update offsets while ensuring they stay within bounds
                    if (scale * zoom <= maxScale) {
                        offsetX = (offsetX + pan.x * scale * slowMovement + offsetXChange).coerceIn(
                            minOffsetX, maxOffsetX
                        )
                        offsetY = (offsetY + pan.y * scale * slowMovement + offsetYChange).coerceIn(
                            minOffsetY, maxOffsetY
                        )
                    }

                    // Store initial offset on pan
                    if (pan != Offset(0f, 0f) && initialOffset == Offset(0f, 0f)) {
                        initialOffset = Offset(offsetX, offsetY)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset scale and offset on double tap
                        if (scale != 1f) {
                            scale = 1f
                            offsetX = initialOffset.x
                            offsetY = initialOffset.y
                        } else {
                            scale = 2f
                        }
                    })
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }) {
        genQrCode(data, qrSize)?.let { qr ->
            Image(
                bitmap = qr.asImageBitmap(),
                contentDescription = "my site",
                contentScale = ContentScale.Fit,
            )
        }
    }
}

fun genQrCode(data: String, size: Int) = try {
    val writer = QRCodeWriter()
    val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
    val width: Int = bitMatrix.width
    val height: Int = bitMatrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    createBitmap(width, height).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
} catch (e: Exception) {
    null
}

@Preview(showBackground = true)
@Composable
fun QRCodePreview() {
    QRCodeGenTheme {
        App("hallo")
    }
}