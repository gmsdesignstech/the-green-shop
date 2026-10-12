package com.example

import com.example.data.SampleData
import com.example.viewmodel.GreenShopViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testGreenShopCart() {
        val viewModel = GreenShopViewModel()
        // Test Cart operations directly on state with fresh tomatoes
        val tomato = SampleData.products.first { it.id == "p_tomato" }
        viewModel.clearCart()
        assertEquals(0, viewModel.cartItems.value.size)

        viewModel.addToCart(tomato)
        assertEquals(1, viewModel.cartItems.value.size)
        assertEquals(1, viewModel.getProductQuantity(tomato.id))

        // Checkout is deliberately excluded: totals and order IDs must come from the backend.
    }

    @Test
    fun testFruitsAndVegetablesOnlyCatalog() {
        val allProducts = SampleData.products
        assertTrue("Products catalog should not be empty", allProducts.isNotEmpty())

        // Verify all products belong to produce categories (veg, fruits, combos)
        val validCategoryIds = setOf("cat_veg", "cat_fruits", "cat_combos", "cat_organic", "cat_herbs")
        allProducts.forEach { product ->
            assertTrue(
                "Product ${product.name} should belong to fresh fruits or vegetables categories",
                product.category in validCategoryIds
            )
        }

        // Search for "spinach" / "palak"
        val spinachMatches = allProducts.filter {
            it.name.contains("Spinach", ignoreCase = true) || it.name.contains("Palak", ignoreCase = true)
        }
        assertTrue("Expected spinach/palak in vegetables", spinachMatches.isNotEmpty())

        // Search for "tomato"
        val tomatoMatches = allProducts.filter {
            it.name.contains("Tomato", ignoreCase = true)
        }
        assertTrue("Expected tomatoes in vegetables", tomatoMatches.isNotEmpty())

        // Search for "apple"
        val appleMatches = allProducts.filter {
            it.name.contains("Apple", ignoreCase = true)
        }
        assertTrue("Expected apples in fruits", appleMatches.isNotEmpty())

        // Search for "banana"
        val bananaMatches = allProducts.filter {
            it.name.contains("Banana", ignoreCase = true)
        }
        assertTrue("Expected bananas in fruits", bananaMatches.isNotEmpty())
    }
}
