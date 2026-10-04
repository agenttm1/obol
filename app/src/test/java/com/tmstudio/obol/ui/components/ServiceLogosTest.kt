package com.tmstudio.obol.ui.components

import com.tmstudio.obol.data.catalog.parseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ServiceLogosTest {

    private val catalogIds = parseCatalog(File("src/main/assets/services.json").readText())
        .services.map { it.id }.toSet()

    @Test
    fun everyLogo_belongsToACatalogService() {
        // Tipfeler u id-ju bi tiho vratio monogram umjesto logotipa.
        assertEquals(emptySet<String>(), ServiceLogos.serviceIds - catalogIds)
    }

    @Test
    fun manualSubscriptions_andRemovedBrands_haveNoLogo() {
        assertNull(ServiceLogos.forService(null))
        listOf("disney-plus", "xbox-game-pass", "nintendo-switch-online", "microsoft-365", "canva", "adobe-creative-cloud")
            .forEach { assertNull(it, ServiceLogos.forService(it)) }
    }
}
