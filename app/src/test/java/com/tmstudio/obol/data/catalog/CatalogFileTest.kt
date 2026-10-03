package com.tmstudio.obol.data.catalog

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Čuva ispravnost stvarne `assets/services.json` datoteke koja ide u aplikaciju. */
class CatalogFileTest {

    // Unit testovi se pokreću iz direktorija modula `app/`.
    private val catalog = parseCatalog(File("src/main/assets/services.json").readText())

    @Test
    fun header_isValid() {
        assertTrue(catalog.version >= 1)
        LocalDate.parse(catalog.updatedOn)
    }

    @Test
    fun containsServicesRequiredBySpec() {
        val ids = catalog.services.map { it.id }.toSet()
        val required = listOf(
            "netflix", "spotify", "hbo-max", "disney-plus", "youtube-premium",
            "playstation-plus", "xbox-game-pass", "nintendo-switch-online", "icloud-plus",
            "google-one", "microsoft-365", "duolingo", "strava", "canva", "adobe-creative-cloud",
        )
        assertEquals(emptyList<String>(), required.filterNot { it in ids })
    }

    @Test
    fun serviceIds_areUniqueSlugs() {
        val ids = catalog.services.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("Neispravan slug: $it", it.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) }
    }

    @Test
    fun services_haveValidDisplayFields() {
        catalog.services.forEach { s ->
            assertTrue("${s.id}: prazno ime", s.name.isNotBlank())
            assertTrue("${s.id}: monogram '${s.monogram}'", s.monogram.length in 1..2)
            assertTrue("${s.id}: boja '${s.colorHex}'", s.colorHex.matches(Regex("#[0-9A-F]{6}")))
        }
    }

    @Test
    fun plans_areValid() {
        catalog.services.forEach { s ->
            assertTrue("${s.id}: nema planova", s.plans.isNotEmpty())
            s.plans.forEach { p ->
                assertTrue("${s.id}/${p.name}: prazno ime", p.name.isNotBlank())
                assertTrue("${s.id}/${p.name}: cijena ${p.priceCents}", p.priceCents > 0)
            }
            val keys = s.plans.map { it.name to it.cycle }
            assertEquals("${s.id}: dupli plan (ime + ciklus)", keys.size, keys.toSet().size)
        }
    }

    @Test(expected = SerializationException::class)
    fun unknownKey_isRejected() {
        parseCatalog(
            """
            {"version":1,"updatedOn":"2026-10-01","services":[
              {"id":"x","name":"X","category":"OTHER","monogram":"X","colorHex":"#FFFFFF",
               "plans":[{"name":"P","priceCent":100}]}
            ]}
            """.trimIndent()
        )
    }
}
