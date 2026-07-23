package com.asianmobile.privatebrower.pet.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSocialDirectorTest {
    private val bounds = PetBounds(0f, 0f, 500f, 100f)
    private val size = PetSize(20f, 20f)
    private val engine = PetEngine()

    @Test
    fun `two available pets approach each other with opposite facing directions`() {
        val director = director()
        val pets = listOf(
            snapshot(0, x = 20f),
            snapshot(1, x = 300f)
        )

        val directives = director.update(pets, elapsedMillis = 1)

        assertEquals(
            listOf(
                PetSocialDirective.StartCombo(
                    0,
                    PetComboId.SOCIAL_APPROACH,
                    PetDirection.RIGHT
                ),
                PetSocialDirective.StartCombo(
                    1,
                    PetComboId.SOCIAL_APPROACH,
                    PetDirection.LEFT
                )
            ),
            directives
        )
    }

    @Test
    fun `nearby pets perform complementary greeting roles`() {
        val director = director()
        val pets = listOf(
            snapshot(4, x = 20f),
            snapshot(7, x = 42f)
        )

        val performance = director.update(pets, elapsedMillis = 1)

        assertEquals(
            listOf(
                PetSocialDirective.StartCombo(
                    4,
                    PetComboId.SOCIAL_HELLO,
                    PetDirection.RIGHT
                ),
                PetSocialDirective.StartCombo(
                    7,
                    PetComboId.SOCIAL_HELLO_REPLY,
                    PetDirection.LEFT
                )
            ),
            performance
        )
    }

    @Test
    fun `nearby pets perform offset call and response dance roles`() {
        val duetOffset = PetSocialScene.entries.indexOf(PetSocialScene.DUET_DANCE)
        val director = director(sceneOffset = duetOffset)
        val pets = listOf(
            snapshot(4, x = 20f),
            snapshot(7, x = 42f)
        )

        val performance = director.update(pets, elapsedMillis = 1)

        assertEquals(
            listOf(
                PetSocialDirective.StartCombo(
                    4,
                    PetComboId.SOCIAL_DUET_A,
                    PetDirection.RIGHT
                ),
                PetSocialDirective.StartCombo(
                    7,
                    PetComboId.SOCIAL_DUET_B,
                    PetDirection.LEFT
                )
            ),
            performance
        )
    }

    @Test
    fun `director chooses the closest grounded pair and ignores a climbing pet`() {
        val director = director()
        val pets = listOf(
            snapshot(0, x = 0f),
            snapshot(1, x = 250f),
            snapshot(2, x = 280f),
            snapshot(3, x = 270f, action = PetAction.CLIMB_WALL)
        )

        val directives = director.update(pets, elapsedMillis = 1)

        assertEquals(setOf(1, 2), directives.map(PetSocialDirective::petId).toSet())
    }

    @Test
    fun `single pet never receives a fabricated social interaction`() {
        val director = director()

        val directives = director.update(listOf(snapshot(0, x = 20f)), elapsedMillis = 10_000)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `declined social invitation leaves both pets autonomous`() {
        val director = PetSocialDirector(
            config = PetSocialConfig(
                initialDelayMillis = 0,
                interactionChancePercent = 0
            )
        )
        val pets = listOf(snapshot(0, x = 20f), snapshot(1, x = 42f))

        val directives = director.update(pets, elapsedMillis = 1)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `distant pets keep their autonomous stories instead of forced approach`() {
        val director = PetSocialDirector(
            config = PetSocialConfig(
                initialDelayMillis = 0,
                interactionChancePercent = 100,
                maximumApproachDistanceInPetWidths = 2f
            )
        )
        val pets = listOf(snapshot(0, x = 20f), snapshot(1, x = 300f))

        val directives = director.update(pets, elapsedMillis = 1)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `approach releases ownership instead of overriding an interrupted pet`() {
        val director = director()
        val available = listOf(snapshot(0, x = 20f), snapshot(1, x = 90f))
        director.update(available, elapsedMillis = 1)
        val interrupted = listOf(
            snapshot(
                id = 0,
                x = 25f,
                action = PetAction.TAPPED,
                activeComboId = PetComboId.USER_AFFECTION
            ),
            snapshot(
                id = 1,
                x = 85f,
                action = PetAction.RUN,
                activeComboId = PetComboId.SOCIAL_APPROACH
            )
        )

        val directives = director.update(interrupted, elapsedMillis = 100)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `stable social roles are not forced to face again every frame`() {
        val director = director()
        val available = listOf(
            snapshot(0, x = 20f),
            snapshot(1, x = 42f)
        )
        director.update(available, elapsedMillis = 1)
        val performing = listOf(
            snapshot(
                id = 0,
                x = 20f,
                action = PetAction.IDLE,
                direction = PetDirection.RIGHT,
                activeComboId = PetComboId.SOCIAL_HELLO
            ),
            snapshot(
                id = 1,
                x = 42f,
                action = PetAction.LOOK_UP,
                direction = PetDirection.LEFT,
                activeComboId = PetComboId.SOCIAL_HELLO_REPLY
            )
        )

        val directives = director.update(performing, elapsedMillis = 100)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `overlapping social pets keep their facing inside the dead zone`() {
        val director = director()
        val available = listOf(
            snapshot(0, x = 40f),
            snapshot(1, x = 40f)
        )
        director.update(available, elapsedMillis = 1)
        val performing = listOf(
            snapshot(
                id = 0,
                x = 40f,
                action = PetAction.IDLE,
                direction = PetDirection.LEFT,
                activeComboId = PetComboId.SOCIAL_HELLO
            ),
            snapshot(
                id = 1,
                x = 40f,
                action = PetAction.LOOK_UP,
                direction = PetDirection.RIGHT,
                activeComboId = PetComboId.SOCIAL_HELLO_REPLY
            )
        )

        val directives = director.update(performing, elapsedMillis = 100)

        assertTrue(directives.isEmpty())
    }

    @Test
    fun `social session releases both pets when either role finishes`() {
        val director = director()
        val available = listOf(
            snapshot(0, x = 20f),
            snapshot(1, x = 42f)
        )
        director.update(available, elapsedMillis = 1)
        val oneRoleFinished = listOf(
            snapshot(
                id = 0,
                x = 20f,
                action = PetAction.RUN,
                direction = PetDirection.RIGHT,
                activeComboId = PetComboId.CURIOUS_SCOUT
            ),
            snapshot(
                id = 1,
                x = 42f,
                action = PetAction.SIT,
                direction = PetDirection.LEFT,
                activeComboId = PetComboId.SOCIAL_HELLO_REPLY
            )
        )

        val ending = director.update(oneRoleFinished, elapsedMillis = 500)
        val nextScene = director.update(available, elapsedMillis = 100)

        assertTrue(ending.isEmpty())
        assertTrue(nextScene.filterIsInstance<PetSocialDirective.StartCombo>().isNotEmpty())
    }

    private fun director(sceneOffset: Int = 0) = PetSocialDirector(
        config = PetSocialConfig(
            initialDelayMillis = 0,
            interactionCooldownMillis = 100,
            retryDelayMillis = 100,
            approachTimeoutMillis = 1_000,
            performanceTimeoutMillis = 1_000,
            interactionChancePercent = 100,
            maximumApproachDistanceInPetWidths = 20f
        ),
        sceneOffset = sceneOffset
    )

    private fun snapshot(
        id: Int,
        x: Float,
        action: PetAction = PetAction.WALK,
        direction: PetDirection = PetDirection.RIGHT,
        activeComboId: PetComboId? = null
    ) = PetSocialSnapshot(
        id = id,
        state = engine.initialState(
            bounds = bounds,
            size = size,
            position = PetVector(x, bounds.bottom - size.height),
            action = action,
            direction = direction
        ).copy(activeComboId = activeComboId)
    )
}
