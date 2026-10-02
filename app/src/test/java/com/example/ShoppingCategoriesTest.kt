package com.example

import com.example.data.local.SeedData
import com.example.ui.customer.productMatchesCategory
import com.example.ui.locale.AppStrings
import org.junit.Assert.*
import org.junit.Test

class ShoppingCategoriesTest {

    private val requiredCategories = listOf(
        "Clothing",
        "Saree",
        "Chappal",
        "Shoes",
        "Cosmetics",
        "Fancy",
        "Bags",
        "Jewellery",
        "Kids Wear",
        "Men’s Wear",
        "Women’s Wear",
        "Household Items",
        "Grocery",
        "Vegetables",
        "Fruits",
        "Electronics",
        "Other"
    )

    @Test
    fun testAllRequiredShoppingCategoriesPresent() {
        assertEquals("Must contain 17 requested categories", 17, requiredCategories.size)
        for (cat in requiredCategories) {
            assertTrue("SeedData.shoppingCategories contains '$cat'", SeedData.shoppingCategories.contains(cat))
            assertTrue("SeedData.defaultCategoryNames contains '$cat'", SeedData.defaultCategoryNames.contains(cat))
            val entityExists = SeedData.categories.any { it.name.equals(cat, ignoreCase = true) }
            assertTrue("SeedData.categories contains CategoryEntity for '$cat'", entityExists)
        }
    }

    @Test
    fun testCategoryDisplayNameAndEmoji() {
        val englishStrings = AppStrings.English
        val odiaStrings = AppStrings.Odia

        for (cat in requiredCategories) {
            val engName = englishStrings.getCategoryDisplayName(cat)
            val odiaName = odiaStrings.getCategoryDisplayName(cat)
            val emoji = englishStrings.getCategoryEmoji(cat)

            assertEquals(cat, engName)
            assertFalse("Odia name should not be blank for '$cat'", odiaName.isBlank())
            assertFalse("Emoji should not be blank for '$cat'", emoji.isBlank())
            assertNotEquals("Should not be generic tag fallback", "🏷️", emoji)
        }
    }

    @Test
    fun testProductMatchesCategoryFiltering() {
        // Exact matches
        assertTrue(productMatchesCategory("Clothing", "Clothing"))
        assertTrue(productMatchesCategory("Saree", "Saree"))
        assertTrue(productMatchesCategory("Chappal", "Chappal"))
        assertTrue(productMatchesCategory("Shoes", "Shoes"))
        assertTrue(productMatchesCategory("Cosmetics", "Cosmetics"))
        assertTrue(productMatchesCategory("Fancy", "Fancy"))
        assertTrue(productMatchesCategory("Bags", "Bags"))
        assertTrue(productMatchesCategory("Jewellery", "Jewellery"))
        assertTrue(productMatchesCategory("Kids Wear", "Kids Wear"))
        assertTrue(productMatchesCategory("Men’s Wear", "Men’s Wear"))
        assertTrue(productMatchesCategory("Women’s Wear", "Women’s Wear"))
        assertTrue(productMatchesCategory("Household Items", "Household Items"))
        assertTrue(productMatchesCategory("Grocery", "Grocery"))
        assertTrue(productMatchesCategory("Vegetables", "Vegetables"))
        assertTrue(productMatchesCategory("Fruits", "Fruits"))
        assertTrue(productMatchesCategory("Electronics", "Electronics"))
        assertTrue(productMatchesCategory("Other", "Other"))

        // Smart keyword matches for existing seed items
        assertTrue(productMatchesCategory("Groceries & Spices", "Grocery"))
        assertTrue(productMatchesCategory("Fresh Vegetables", "Vegetables"))
        assertTrue(productMatchesCategory("Fresh Fruits", "Fruits"))
        assertTrue(productMatchesCategory("Electronics & Mobile", "Electronics"))
        assertTrue(productMatchesCategory("Handloom & Sambalpuri Clothes", "Clothing"))
        assertTrue(productMatchesCategory("Home & Kitchen Essentials", "Household Items"))

        // "All" matches everything
        assertTrue(productMatchesCategory("Grocery", "All"))
        assertTrue(productMatchesCategory("Clothing", "All"))
    }

    @Test
    fun testSampleProductsCoverAllCategories() {
        val products = SeedData.products
        assertTrue("Seed products is not empty", products.isNotEmpty())
        for (cat in requiredCategories) {
            val matching = products.filter { productMatchesCategory(it.category, cat) }
            assertTrue("Category '$cat' must have at least one matching product", matching.isNotEmpty())
        }
    }
}
