package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

// STEP 3: Pre-hoisted data structures to keep the item content scope completely clean.
// This prevents object allocations inside the Pager's inner loop during fast scrolling.
data class SliderItem(
    val id: Int,
    val title: String,
    val description: String,
    val backgroundColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlinketSliderScreen(modifier: Modifier = Modifier) {
    
    // Pre-allocate the data once using remember to avoid reallocation on recomposition.
    val sliderItems = remember {
        listOf(
            SliderItem(1, "Lightning Fast", "Zero dropped frames during swipe.", Color(0xFF4CAF50)),
            SliderItem(2, "Hardware Accelerated", "Transitions processed on the GPU.", Color(0xFF2196F3)),
            SliderItem(3, "Optimized Scope", "No logic inside the compose loop.", Color(0xFF9C27B0)),
            SliderItem(4, "Responsive Snapping", "Light drag threshold applied.", Color(0xFFFF9800)),
            SliderItem(5, "Premium Feel", "Smooth scaling and opacity fading.", Color(0xFFE91E63))
        )
    }

    val pagerState = rememberPagerState(pageCount = { sliderItems.size })

    // STEP 1: Custom Fling Behavior for a light, highly sensitive swipe.
    // snapPositionalThreshold = 0.3f means you only need to drag 30% of the screen to snap to the next page,
    // rather than the standard 50%.
    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapPositionalThreshold = 0.3f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)), // Dark background to make the cards pop
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            flingBehavior = fling,
            contentPadding = PaddingValues(horizontal = 48.dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            
            // Get the specific item without running map/filter functions
            val item = sliderItems[page]

            // STEP 2: graphicsLayer lambda-based transitions.
            // This entire block runs strictly in the Draw phase. It reads the scroll state directly
            // and applies visual changes (scale and alpha) without triggering Recomposition.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .graphicsLayer {
                        // Calculate how far off-center this specific page is
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                        // Gentle scaling: Center item is 1.0f, shrinks down to 0.85f as it moves away
                        val scale = 1f - (0.15f * pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale

                        // Fading: Center item is fully opaque, fades to 40% as it moves away
                        val alphaValue = 1f - (0.6f * pageOffset.coerceIn(0f, 1f))
                        alpha = alphaValue
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Minimal UI rendering, relying completely on the hoisted data object
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = item.description,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
