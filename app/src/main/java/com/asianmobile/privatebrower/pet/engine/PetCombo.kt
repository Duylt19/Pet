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

data class PetComboDefinition(
    val id: PetComboId,
    val actions: List<PetAction>,
    val turnAtStart: Boolean = false
) {
    init {
        require(actions.isNotEmpty()) { "combo must contain at least one action" }
    }

    fun supportedActions(supported: Set<PetAction>): List<PetAction> =
        actions.filter(supported::contains)
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
            PetAction.WALK,
            PetAction.LOOK_UP,
            PetAction.CREEP,
            PetAction.WINK
        ),
        combo(
            PetComboId.COZY_BREAK,
            PetAction.IDLE,
            PetAction.SIT,
            PetAction.LOOK_UP,
            PetAction.WINK
        ),
        combo(
            PetComboId.HAPPY_ZOOMIES,
            PetAction.WINK,
            PetAction.RUN,
            PetAction.IDLE,
            PetAction.RUN,
            PetAction.WINK
        ),
        combo(
            PetComboId.SHY_SNEAK,
            PetAction.IDLE,
            PetAction.CREEP,
            PetAction.LOOK_UP,
            PetAction.CREEP,
            PetAction.WINK
        ),
        combo(
            PetComboId.CLUMSY_RECOVERY,
            PetAction.RUN,
            PetAction.TRIP,
            PetAction.SIT,
            PetAction.WINK
        ),
        combo(
            PetComboId.TINY_PERFORMANCE,
            PetAction.SIT,
            PetAction.SPECIAL,
            PetAction.SPECIAL_2,
            PetAction.WINK,
            PetAction.LOOK_UP
        ),
        combo(
            PetComboId.DAYDREAM,
            PetAction.IDLE,
            PetAction.LOOK_UP,
            PetAction.DANGLE,
            PetAction.LOOK_UP,
            PetAction.WINK
        ),
        combo(
            PetComboId.BUSY_PATROL,
            PetAction.WALK,
            PetAction.RUN,
            PetAction.WALK,
            PetAction.LOOK_UP
        ),
        combo(
            PetComboId.PEEK_AND_DASH,
            PetAction.CREEP,
            PetAction.LOOK_UP,
            PetAction.WINK,
            PetAction.RUN
        ),
        combo(
            PetComboId.SLOW_MORNING,
            PetAction.IDLE,
            PetAction.SIT,
            PetAction.WINK,
            PetAction.WALK
        ),
        PetComboDefinition(
            id = PetComboId.BRAVE_EXPLORER,
            actions = listOf(
                PetAction.LOOK_UP,
                PetAction.RUN,
                PetAction.CREEP,
                PetAction.RUN
            ),
            turnAtStart = true
        ),
        PetComboDefinition(
            id = PetComboId.CHEERFUL_ENCORE,
            actions = listOf(
                PetAction.SPECIAL_2,
                PetAction.WINK,
                PetAction.SPECIAL,
                PetAction.WINK
            ),
            turnAtStart = true
        ),
        combo(
            PetComboId.USER_AFFECTION,
            PetAction.TAPPED,
            PetAction.WINK,
            PetAction.LOOK_UP
        ),
        combo(
            PetComboId.USER_SHOWCASE,
            PetAction.SPECIAL,
            PetAction.SPECIAL_2,
            PetAction.WINK,
            PetAction.LOOK_UP
        ),
        combo(
            PetComboId.SOCIAL_APPROACH,
            PetAction.RUN,
            PetAction.WALK,
            PetAction.RUN
        ),
        combo(
            PetComboId.SOCIAL_HELLO,
            PetAction.IDLE,
            PetAction.WINK,
            PetAction.LOOK_UP,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_HELLO_REPLY,
            PetAction.LOOK_UP,
            PetAction.WINK,
            PetAction.SIT,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_CHASE_LEADER,
            PetAction.WINK,
            PetAction.RUN,
            PetAction.WALK,
            PetAction.RUN,
            PetAction.LOOK_UP
        ),
        combo(
            PetComboId.SOCIAL_CHASE_FOLLOWER,
            PetAction.LOOK_UP,
            PetAction.RUN,
            PetAction.RUN,
            PetAction.TRIP,
            PetAction.SIT,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_SHOW_OFF,
            PetAction.SIT,
            PetAction.SPECIAL,
            PetAction.SPECIAL_2,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_ADMIRE,
            PetAction.LOOK_UP,
            PetAction.WINK,
            PetAction.LOOK_UP,
            PetAction.SIT
        ),
        combo(
            PetComboId.SOCIAL_REST_A,
            PetAction.SIT,
            PetAction.IDLE,
            PetAction.LOOK_UP,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_REST_B,
            PetAction.LOOK_UP,
            PetAction.SIT,
            PetAction.IDLE,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_A,
            PetAction.WINK,
            PetAction.LOOK_UP,
            PetAction.SIT,
            PetAction.WINK
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_B,
            PetAction.LOOK_UP,
            PetAction.WINK,
            PetAction.SIT,
            PetAction.WINK
        )
    ).associateBy(PetComboDefinition::id)

    fun definition(id: PetComboId): PetComboDefinition? = definitions[id]

    fun supportedDefinition(
        id: PetComboId,
        supportedActions: Set<PetAction>
    ): PetComboDefinition? {
        val definition = definition(id) ?: return null
        val actions = definition.supportedActions(supportedActions)
        if (actions.size < MIN_COMBO_ACTIONS || actions.distinct().size < MIN_DISTINCT_ACTIONS) {
            return null
        }
        return definition.copy(actions = actions)
    }

    private fun combo(id: PetComboId, vararg actions: PetAction) =
        PetComboDefinition(id = id, actions = actions.toList())

    private const val MIN_COMBO_ACTIONS = 2
    private const val MIN_DISTINCT_ACTIONS = 2
}
