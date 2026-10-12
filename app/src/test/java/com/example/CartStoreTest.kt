package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.customer.CartStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CartStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun reset() { CartStore(context).clear() }

    @Test fun `persists only positive product quantities`() {
        CartStore(context).saveQuantities(mapOf("11" to 2, "12" to 0, "13" to -1))
        assertEquals(mapOf("11" to 2), CartStore(context).loadQuantities())
    }

    @Test fun `clear removes the guest cart`() {
        CartStore(context).saveQuantities(mapOf("11" to 2))
        CartStore(context).clear()
        assertEquals(emptyMap<String, Int>(), CartStore(context).loadQuantities())
    }
}
