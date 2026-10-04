package dj.nomade.velox_restaurant.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderItemTest {

    @Test
    fun `les options sont regroupees par groupe du menu`() {
        val item = OrderItem(
            name = "Tacos L",
            options = listOf(
                OrderItemOption("Algérienne", group = "Sauces", isSauce = true),
                OrderItemOption("Spicy fait maison", group = "Sauces", isSauce = true),
                OrderItemOption("Tenders", group = "Viande"),
                OrderItemOption("Cheddar", group = "Suppléments"),
                OrderItemOption("Fanta orange et grenadine", group = "Boisson"),
            ),
        )

        assertEquals(
            listOf(
                OptionTag("Sauces", OptionTagKind.SAUCE, listOf("Algérienne", "Spicy fait maison")),
                OptionTag("Viande", OptionTagKind.CHOICE, listOf("Tenders")),
                OptionTag("Suppléments", OptionTagKind.EXTRA, listOf("Cheddar")),
                OptionTag("Boisson", OptionTagKind.CHOICE, listOf("Fanta orange et grenadine")),
            ),
            item.optionTags(),
        )
    }

    @Test
    fun `sans nom de groupe on garde SAUCE et SUPPLEMENT`() {
        val item = OrderItem(
            name = "Paninis",
            options = listOf(
                OrderItemOption("Algérienne", isSauce = true),
                OrderItemOption("Poulet"),
            ),
        )

        assertEquals(
            listOf(
                OptionTag(null, OptionTagKind.SAUCE, listOf("Algérienne")),
                OptionTag(null, OptionTagKind.EXTRA, listOf("Poulet")),
            ),
            item.optionTags(),
        )
    }

    @Test
    fun `une ligne en cache sans options retombe sur sauces et extras`() {
        val item = OrderItem(name = "Paninis", sauces = listOf("Samourai"), extras = listOf("Poulet"))

        assertEquals(
            listOf(
                OptionTag(null, OptionTagKind.SAUCE, listOf("Samourai")),
                OptionTag(null, OptionTagKind.EXTRA, listOf("Poulet")),
            ),
            item.optionTags(),
        )
    }
}
