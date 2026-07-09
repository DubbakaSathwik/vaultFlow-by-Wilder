package com.example.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "LogoAlpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200) // Delay to let animation run beautifully
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            val logoColor = MaterialTheme.colorScheme.primary
            val logoAccent = MaterialTheme.colorScheme.tertiary
            
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleAnim)
                    .alpha(alphaAnim)
            ) {
                // Draw elegant outer safe/shield circle
                drawCircle(
                    color = logoColor,
                    radius = size.width / 2.2f,
                    style = Stroke(width = 5.dp.toPx())
                )
                
                // Draw inner safe lock node
                drawCircle(
                    color = logoAccent,
                    radius = 18.dp.toPx()
                )
                
                // Draw middle concentric indicator safe ring
                drawCircle(
                    color = logoColor.copy(alpha = 0.4f),
                    radius = 32.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "VaultFlow",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .scale(scaleAnim)
                    .alpha(alphaAnim)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Track • Save • Plan • Grow",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alphaAnim)
            )
        }
    }
}
