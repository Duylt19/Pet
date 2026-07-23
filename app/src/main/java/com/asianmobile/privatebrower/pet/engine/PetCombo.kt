package com.asianmobile.privatebrower.pet.engine

enum class PetComboId {
    CURIOUS_SCOUT,
    COZY_BREAK,
    HAPPY_ZOOMIES,
    SHY_SNEAK,
    CLUMSY_RECOVERY,
    TINY_PERFORMANCE,
    DAYDREAM,
    BUSY_PATROL,
    PEEK_AND_DASH,
    SLOW_MORNING,
    BRAVE_EXPLORER,
    CHEERFUL_ENCORE,
    USER_AFFECTION,
    USER_SHOWCASE,
    SOCIAL_APPROACH,
    SOCIAL_HELLO,
    SOCIAL_HELLO_REPLY,
    SOCIAL_CHASE_LEADER,
    SOCIAL_CHASE_FOLLOWER,
    SOCIAL_SHOW_OFF,
    SOCIAL_ADMIRE,
    SOCIAL_REST_A,
    SOCIAL_REST_B,
    SOCIAL_COPYCAT_A,
    SOCIAL_COPYCAT_B
}

data class PetComboBeat(
    val action: PetAction,
    val durationMillis: LongRange? = null
) {
    init {
        require(durationMillis == null ||
            (durationMillis.first > 0 && durationMillis.last >= durationMillis.first)
        ) {
            "combo beat duration must be positive"
        }
    }

    val isSustained: Boolean
        get() = durationMillis != null
}

data class PetComboDefinition(
    val id: PetComboId,
    val beats: List<PetComboBeat>,
    val turnAtStart: Boolean = false
) {
    init {
        require(beats.isNotEmpty()) { "combo must contain at least one beat" }
    }

    val actions: List<PetAction>
        get() = beats.map(PetComboBeat::action)

    fun supportedBeats(supported: Set<PetAction>): List<PetComboBeat> =
        beats.filter { it.action in supported }
}

data class PetComboRule(
    val comboId: PetComboId,
    val weight: Int
) {
    init {
        require(weight > 0) { "combo weight must be positive" }
    }
}

object PetComboCatalog {
    private val definitions = listOf(
        combo(
            PetComboId.CURIOUS_SCOUT,
            sustain(PetAction.WALK, 4_000L..7_000L),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.CREEP, 4_000L..7_000L)
        ),
        combo(
            PetComboId.COZY_BREAK,
            sustain(PetAction.IDLE, 3_000L..5_000L),
            sustain(PetAction.SIT, 7_000L..12_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        combo(
            PetComboId.HAPPY_ZOOMIES,
            sustain(PetAction.IDLE, 2_000L..3_500L),
            once(PetAction.WINK),
            sustain(PetAction.RUN, 3_500L..6_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SHY_SNEAK,
            sustain(PetAction.IDLE, 3_000L..5_000L),
            sustain(PetAction.CREEP, 5_000L..8_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.IDLE, 2_500L..4_500L)
        ),
        combo(
            PetComboId.CLUMSY_RECOVERY,
            sustain(PetAction.RUN, 2_500L..4_000L),
            once(PetAction.TRIP),
            sustain(PetAction.SIT, 7_000L..11_000L),
            once(PetAction.WINK)
        ),
        combo(
            PetComboId.TINY_PERFORMANCE,
            sustain(PetAction.SIT, 3_000L..5_000L),
            sustain(PetAction.SPECIAL, 4_500L..7_000L),
            sustain(PetAction.IDLE, 2_000L..3_500L),
            sustain(PetAction.SPECIAL_2, 4_500L..7_000L),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        combo(
            PetComboId.DAYDREAM,
            sustain(PetAction.SIT, 6_000L..10_000L),
            sustain(PetAction.LOOK_UP, 4_000L..7_000L),
            sustain(PetAction.DANGLE, 4_000L..7_000L),
            sustain(PetAction.SIT, 5_000L..9_000L)
        ),
        combo(
            PetComboId.BUSY_PATROL,
            sustain(PetAction.WALK, 6_000L..10_000L),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.RUN, 2_500L..4_500L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L)
        ),
        combo(
            PetComboId.PEEK_AND_DASH,
            sustain(PetAction.CREEP, 5_000L..8_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.IDLE, 1_500L..3_000L),
            sustain(PetAction.RUN, 2_500L..4_500L)
        ),
        combo(
            PetComboId.SLOW_MORNING,
            sustain(PetAction.IDLE, 5_000L..9_000L),
            sustain(PetAction.SIT, 8_000L..14_000L),
            sustain(PetAction.LOOK_UP, 3_000L..6_000L),
            sustain(PetAction.WALK, 4_000L..7_000L)
        ),
        turnedCombo(
            PetComboId.BRAVE_EXPLORER,
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.RUN, 3_000L..5_000L),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.CREEP, 4_000L..7_000L)
        ),
        turnedCombo(
            PetComboId.CHEERFUL_ENCORE,
            sustain(PetAction.SPECIAL_2, 4_000L..6_500L),
            sustain(PetAction.SIT, 3_000L..5_000L),
            sustain(PetAction.SPECIAL, 4_000L..6_500L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.USER_AFFECTION,
            once(PetAction.TAPPED),
            sustain(PetAction.IDLE, 1_500L..2_500L),
            once(PetAction.WINK)
        ),
        combo(
            PetComboId.USER_SHOWCASE,
            sustain(PetAction.SIT, 2_500L..4_000L),
            sustain(PetAction.SPECIAL, 4_500L..7_000L),
            sustain(PetAction.IDLE, 2_000L..3_500L),
            sustain(PetAction.SPECIAL_2, 4_500L..7_000L),
            sustain(PetAction.SIT, 3_500L..6_000L)
        ),
        combo(
            PetComboId.SOCIAL_APPROACH,
            sustain(PetAction.RUN, 3_500L..5_500L),
            sustain(PetAction.WALK, 2_500L..4_000L)
        ),
        combo(
            PetComboId.SOCIAL_HELLO,
            sustain(PetAction.IDLE, 2_000L..3_500L),
            once(PetAction.WINK),
            sustain(PetAction.SIT, 5_000L..8_000L)
        ),
        combo(
            PetComboId.SOCIAL_HELLO_REPLY,
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.IDLE, 1_500L..3_000L),
            once(PetAction.WINK),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        combo(
            PetComboId.SOCIAL_CHASE_LEADER,
            once(PetAction.WINK),
            sustain(PetAction.RUN, 5_000L..8_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SOCIAL_CHASE_FOLLOWER,
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            sustain(PetAction.RUN, 5_500L..8_500L),
            once(PetAction.TRIP),
            sustain(PetAction.SIT, 5_000L..8_000L)
        ),
        combo(
            PetComboId.SOCIAL_SHOW_OFF,
            sustain(PetAction.SIT, 2_500L..4_000L),
            sustain(PetAction.SPECIAL, 5_000L..8_000L),
            sustain(PetAction.IDLE, 2_000L..3_500L),
            sustain(PetAction.SPECIAL_2, 5_000L..8_000L)
        ),
        combo(
            PetComboId.SOCIAL_ADMIRE,
            sustain(PetAction.LOOK_UP, 4_000L..7_000L),
            sustain(PetAction.SIT, 7_000L..11_000L),
            once(PetAction.WINK)
        ),
        combo(
            PetComboId.SOCIAL_REST_A,
            sustain(PetAction.SIT, 10_000L..16_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.SIT, 6_000L..10_000L)
        ),
        combo(
            PetComboId.SOCIAL_REST_B,
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.SIT, 11_000L..17_000L),
            once(PetAction.WINK),
            sustain(PetAction.SIT, 5_000L..9_000L)
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_A,
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.SIT, 6_000L..9_000L),
            once(PetAction.WINK)
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_B,
            sustain(PetAction.IDLE, 1_500L..2_500L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.SIT, 6_000L..9_000L),
            once(PetAction.WINK)
        )
    ).associateBy(PetComboDefinition::id)

    fun definition(id: PetComboId): PetComboDefinition? = definitions[id]

    fun supportedDefinition(
        id: PetComboId,
        supportedActions: Set<PetAction>
    ): PetComboDefinition? {
        val definition = definition(id) ?: return null
        val beats = definition.supportedBeats(supportedActions)
        if (beats.size < MIN_COMBO_BEATS || beats.map(PetComboBeat::action).distinct().size <
            MIN_DISTINCT_ACTIONS
        ) {
            return null
        }
        return definition.copy(beats = beats)
    }

    private fun combo(
        id: PetComboId,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(id = id, beats = beats.toList())

    private fun turnedCombo(
        id: PetComboId,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(id = id, beats = beats.toList(), turnAtStart = true)

    private fun once(action: PetAction) = PetComboBeat(action)

    private fun sustain(action: PetAction, durationMillis: LongRange) =
        PetComboBeat(action, durationMillis)

    private const val MIN_COMBO_BEATS = 2
    private const val MIN_DISTINCT_ACTIONS = 2
}
