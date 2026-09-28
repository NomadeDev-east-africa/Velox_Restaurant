package dj.nomade.velox_restaurant.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class OptionGroupTest {

    private fun rawGroups(choice: Map<String, Any?>): List<Map<String, Any?>> = listOf(
        mapOf(
            "name" to "Suppléments",
            "type" to "multiple",
            "required" to false,
            "choices" to listOf(choice),
        ),
    )

    @Test
    fun `l'id d'un choix survit a une mise en rupture`() {
        val groups = parseOptionGroups(
            rawGroups(mapOf("id" to "ch_k3f9x2ab", "name" to "Cheddar", "price" to 100)),
        )

        val updated = groups.map { g ->
            g.copy(choices = g.choices.map { it.copy(available = false) })
        }
        val choice = (updated.toFirestoreList().single()["choices"] as List<*>).single() as Map<*, *>

        assertEquals("ch_k3f9x2ab", choice["id"])
        assertEquals(false, choice["available"])
        assertEquals("Cheddar", choice["name"])
    }

    @Test
    fun `un choix sans id reste sans id`() {
        val groups = parseOptionGroups(rawGroups(mapOf("name" to "Cheddar", "price" to 100)))

        assertNull(groups.single().choices.single().id)
        val choice = (groups.toFirestoreList().single()["choices"] as List<*>).single() as Map<*, *>
        assertFalse(choice.containsKey("id"))
    }

    @Test
    fun `un id vide est ignore`() {
        val groups = parseOptionGroups(rawGroups(mapOf("id" to " ", "name" to "Cheddar")))

        assertNull(groups.single().choices.single().id)
    }
}
