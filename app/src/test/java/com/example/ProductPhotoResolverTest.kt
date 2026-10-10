package com.example

import com.example.ui.components.productPhotoResource
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProductPhotoResolverTest {
    @Test
    fun photographedCatalogProductsResolveToDistinctAssets() {
        val ids = listOf("p_coriander", "p_spinach", "p_mint", "p_onion", "p_potato", "p_carrot", "p_tomato", "p_cucumber", "p_banana", "p_apple")
        val resources = ids.map(::productPhotoResource)
        resources.forEach(::assertNotNull)
        check(resources.filterNotNull().distinct().size == ids.size)
    }

    @Test
    fun productsWithoutApprovedPhotographyUseTheHonestPlaceholder() {
        assertNull(productPhotoResource("p_image_not_supplied"))
    }
}
