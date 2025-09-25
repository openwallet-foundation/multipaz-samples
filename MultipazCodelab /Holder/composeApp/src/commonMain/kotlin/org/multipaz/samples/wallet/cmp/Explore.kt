package org.multipaz.samples.wallet.cmp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import utopiasample.composeapp.generated.resources.Res
import utopiasample.composeapp.generated.resources.vegetable_oil
import utopiasample.composeapp.generated.resources.cereal
import utopiasample.composeapp.generated.resources.paper_towels
import utopiasample.composeapp.generated.resources.whole_bean_coffee
import utopiasample.composeapp.generated.resources.laundry_detergent
import utopiasample.composeapp.generated.resources.mixed_nuts
import utopiasample.composeapp.generated.resources.organic_bananas
import utopiasample.composeapp.generated.resources.whole_grain_bread
import utopiasample.composeapp.generated.resources.fresh_milk
import utopiasample.composeapp.generated.resources.free_range_ggs
import utopiasample.composeapp.generated.resources.organic_spinach

// Data class for the product items
data class Product(
    val name: String,
    val price: String,
    val imageResource: DrawableResource
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    val products = listOf(
        Product("Vegetable Oil", "$9,99", Res.drawable.vegetable_oil),
        Product("Cereal", "$3,49", Res.drawable.cereal),
        Product("Paper Towels", "$16,99", Res.drawable.paper_towels),
        Product("Whole Bean Coffee", "$15,99", Res.drawable.whole_bean_coffee),
        Product("Laundry Detergent", "$4,49", Res.drawable.laundry_detergent),
        Product("Mixed Nuts", "$5,99", Res.drawable.mixed_nuts),
        Product("Organic Bananas", "$2,99", Res.drawable.organic_bananas),
        Product("Greek Yogurt", "$4,79", Res.drawable.cereal), // Using cereal as placeholder for yogurt
        Product("Whole Grain Bread", "$3,29", Res.drawable.whole_grain_bread),
        Product("Fresh Milk", "$3,89", Res.drawable.fresh_milk),
        Product("Free Range Eggs", "$5,49", Res.drawable.free_range_ggs),
        Product("Organic Spinach", "$2,79", Res.drawable.organic_spinach)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .padding(16.dp)
        ) {
            Text(
                text = "Explore",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Product Grid - Scrollable with 2 columns
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(products) { product ->
                ProductCard(product = product)
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Product image
            Image(
                painter = painterResource(product.imageResource),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = product.price,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        }
    }
} 