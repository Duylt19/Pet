package com.asianmobile.emojibattery.shimeji.ui.pet.room

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRoomRemoveDialogTest {

    @Test
    fun `pet name uses the delete dialog accent style`() {
        val petName = "Catty"
        val message = "Are you sure you want to remove ${markedPetName(petName)} from your pet list?"

        val annotatedMessage = buildPetRemovalMessage(
            markedMessage = message,
            petNameColor = Color(0xFFFB3675)
        )

        val expectedMessage = "Are you sure you want to remove $petName from your pet list?"
        assertEquals(expectedMessage, annotatedMessage.text)
        assertEquals(1, annotatedMessage.spanStyles.size)
        with(annotatedMessage.spanStyles.single()) {
            assertEquals(expectedMessage.indexOf(petName), start)
            assertEquals(expectedMessage.indexOf(petName) + petName.length, end)
            assertEquals(Color(0xFFFB3675), item.color)
            assertEquals(FontWeight.Medium, item.fontWeight)
        }
    }

    @Test
    fun `message remains unstyled when pet name is unavailable`() {
        val annotatedMessage = buildPetRemovalMessage(
            markedMessage = "Remove this pet?",
            petNameColor = Color(0xFFFB3675)
        )

        assertEquals("Remove this pet?", annotatedMessage.text)
        assertTrue(annotatedMessage.spanStyles.isEmpty())
    }
}
