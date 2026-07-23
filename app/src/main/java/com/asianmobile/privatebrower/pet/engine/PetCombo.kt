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
    CHATTER,
    WALL_PARKOUR,
    CEILING_EXPEDITION,
    WALL_DIVE,
    WALL_TO_WALL_LEAP,
    WALL_TO_WALL_RISE,
    SKY_DIVER,
    NINJA_SKILL,
    BATTLE_DANCE,
    MAGIC_RITUAL,
    ACROBATIC_FINALE,
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
    SOCIAL_COPYCAT_B,
    SOCIAL_DUET_A,
    SOCIAL_DUET_B
}

enum class PetComboStartDirection {
    KEEP,
    REVERSE,
    NEAREST_WALL
}

enum class PetBeatDirectionChange {
    KEEP,
    REVERSE
}

enum class PetBeatCompletion {
    CLIP_OR_DURATION,
    COLLISION
}

enum class PetComboHabitat {
    GROUND,
    AERIAL,
    WALL,
    CEILING;

    val isClimb: Boolean
        get() = this == WALL || this == CEILING
}

data class PetComboBeat(
    val action: PetAction,
    val durationMillis: LongRange? = null,
    val directionChange: PetBeatDirectionChange = PetBeatDirectionChange.KEEP,
    val completion: PetBeatCompletion = PetBeatCompletion.CLIP_OR_DURATION,
    val motionMultiplier: Float = 1f,
    val crossScreenDurationMillis: Long? = null,
    val crossScreenLaunchVelocityY: Float? = null
) {
    init {
        require(durationMillis == null ||
            (durationMillis.first > 0 && durationMillis.last >= durationMillis.first)
        ) {
            "combo beat duration must be positive"
        }
        require(motionMultiplier > 0f) { "combo beat motion multiplier must be positive" }
        require(crossScreenDurationMillis == null || crossScreenDurationMillis > 0) {
            "cross-screen duration must be positive"
        }
        require(crossScreenDurationMillis == null || completion == PetBeatCompletion.COLLISION) {
            "cross-screen beat must complete on collision"
        }
        require(
            crossScreenLaunchVelocityY == null ||
                (crossScreenLaunchVelocityY.isFinite() && crossScreenLaunchVelocityY < 0f)
        ) {
            "cross-screen launch velocity must be finite and negative"
        }
        require(crossScreenLaunchVelocityY == null || crossScreenDurationMillis != null) {
            "cross-screen launch velocity requires cross-screen travel"
        }
    }

    val isSustained: Boolean
        get() = durationMillis != null || completion == PetBeatCompletion.COLLISION
}

data class PetComboDefinition(
    val id: PetComboId,
    val beats: List<PetComboBeat>,
    val startDirection: PetComboStartDirection = PetComboStartDirection.KEEP,
    val requiredActions: Set<PetAction> = emptySet(),
    val habitat: PetComboHabitat = PetComboHabitat.GROUND
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
        reversedCombo(
            PetComboId.BRAVE_EXPLORER,
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.RUN, 3_000L..5_000L),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.CREEP, 4_000L..7_000L)
        ),
        reversedCombo(
            PetComboId.CHEERFUL_ENCORE,
            sustain(PetAction.SPECIAL_2, 4_000L..6_500L),
            sustain(PetAction.SIT, 3_000L..5_000L),
            sustain(PetAction.SPECIAL, 4_000L..6_500L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        requiredCombo(
            PetComboId.CHATTER,
            requiredActions = setOf(PetAction.TALK),
            sustain(PetAction.IDLE, 1_500L..2_500L),
            sustain(PetAction.TALK, 6_500L..10_000L),
            once(PetAction.WINK),
            sustain(PetAction.SIT, 3_000L..5_000L)
        ),
        spatialCombo(
            PetComboId.WALL_PARKOUR,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.DANGLE,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE
            ),
            habitat = PetComboHabitat.WALL,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            sustain(PetAction.CLIMB_WALL, 10_000L..16_000L, motionMultiplier = 1.8f),
            sustain(PetAction.DANGLE, 6_000L..10_000L),
            once(
                PetAction.JUMP,
                directionChange = PetBeatDirectionChange.REVERSE,
                motionMultiplier = 2.2f
            ),
            sustain(PetAction.FALL, 12_000L..18_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        spatialCombo(
            PetComboId.CEILING_EXPEDITION,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.CLIMB_CEILING,
                PetAction.DANGLE,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE
            ),
            habitat = PetComboHabitat.CEILING,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            untilCollision(PetAction.CLIMB_WALL, motionMultiplier = 2.4f),
            sustain(PetAction.CLIMB_CEILING, 12_000L..20_000L),
            sustain(PetAction.DANGLE, 8_000L..14_000L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 15_000L..22_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L)
        ),
        spatialCombo(
            PetComboId.WALL_DIVE,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE
            ),
            habitat = PetComboHabitat.WALL,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            sustain(PetAction.CLIMB_WALL, 8_000L..13_000L, motionMultiplier = 1.8f),
            once(
                PetAction.JUMP,
                directionChange = PetBeatDirectionChange.REVERSE,
                motionMultiplier = 2.2f
            ),
            sustain(PetAction.FALL, 12_000L..18_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.SPECIAL, 3_500L..6_000L)
        ),
        wallToWallCombo(PetComboId.WALL_TO_WALL_LEAP),
        wallToWallCombo(
            PetComboId.WALL_TO_WALL_RISE,
            crossScreenLaunchVelocityY = WALL_TO_WALL_RISE_VELOCITY_Y
        ),
        aerialCombo(
            PetComboId.SKY_DIVER,
            requiredActions = setOf(PetAction.JUMP, PetAction.FALL, PetAction.BOUNCE),
            sustain(PetAction.IDLE, 2_000L..3_500L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 10_000L..16_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.SIT, 4_000L..7_000L),
            once(PetAction.WINK)
        ),
        aerialCombo(
            PetComboId.NINJA_SKILL,
            requiredActions = setOf(
                PetAction.CREEP,
                PetAction.RUN,
                PetAction.JUMP,
                PetAction.FALL
            ),
            sustain(PetAction.CREEP, 3_500L..6_000L),
            sustain(PetAction.RUN, 2_500L..4_000L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 10_000L..16_000L),
            sustain(PetAction.SPECIAL, 4_000L..6_500L),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        requiredCombo(
            PetComboId.BATTLE_DANCE,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.LOOK_UP, 2_500L..4_000L),
            sustain(PetAction.SPECIAL, 4_000L..6_000L),
            sustain(PetAction.IDLE, 1_500L..2_500L),
            sustain(PetAction.SPECIAL_2, 4_000L..6_500L),
            sustain(PetAction.DANGLE, 3_500L..5_500L),
            sustain(PetAction.SIT, 4_000L..7_000L)
        ),
        requiredCombo(
            PetComboId.MAGIC_RITUAL,
            requiredActions = setOf(PetAction.SPECIAL_2),
            sustain(PetAction.SIT, 5_000L..8_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.SPECIAL_2, 6_000L..9_000L),
            sustain(PetAction.DANGLE, 4_000L..6_000L),
            sustain(PetAction.SPECIAL, 4_000L..7_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        aerialCombo(
            PetComboId.ACROBATIC_FINALE,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE,
                PetAction.SPECIAL
            ),
            sustain(PetAction.RUN, 3_000L..5_000L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 10_000L..16_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.SPECIAL, 5_000L..8_000L),
            sustain(PetAction.SIT, 5_000L..8_000L)
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
        ),
        requiredCombo(
            PetComboId.SOCIAL_DUET_A,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.SIT, 2_000L..3_500L),
            sustain(PetAction.SPECIAL, 5_000L..8_000L),
            sustain(PetAction.IDLE, 2_000L..3_000L),
            sustain(PetAction.SPECIAL_2, 5_000L..8_000L),
            sustain(PetAction.SIT, 4_000L..6_000L)
        ),
        requiredCombo(
            PetComboId.SOCIAL_DUET_B,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 1_500L..2_500L),
            sustain(PetAction.SPECIAL_2, 5_000L..8_000L),
            sustain(PetAction.SIT, 2_000L..3_500L),
            sustain(PetAction.SPECIAL, 5_000L..8_000L),
            once(PetAction.WINK)
        )
    ).associateBy(PetComboDefinition::id)

    fun definition(id: PetComboId): PetComboDefinition? = definitions[id]

    fun supportedDefinition(
        id: PetComboId,
        supportedActions: Set<PetAction>
    ): PetComboDefinition? {
        val definition = definition(id) ?: return null
        if (!supportedActions.containsAll(definition.requiredActions)) return null
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

    private fun reversedCombo(
        id: PetComboId,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(
        id = id,
        beats = beats.toList(),
        startDirection = PetComboStartDirection.REVERSE
    )

    private fun spatialCombo(
        id: PetComboId,
        requiredActions: Set<PetAction>,
        habitat: PetComboHabitat,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(
        id = id,
        beats = beats.toList(),
        startDirection = PetComboStartDirection.NEAREST_WALL,
        requiredActions = requiredActions,
        habitat = habitat
    )

    private fun wallToWallCombo(
        id: PetComboId,
        crossScreenLaunchVelocityY: Float? = null
    ) = spatialCombo(
        id = id,
        requiredActions = setOf(
            PetAction.RUN,
            PetAction.CLIMB_WALL,
            PetAction.DANGLE,
            PetAction.JUMP,
            PetAction.FALL,
            PetAction.BOUNCE
        ) + if (crossScreenLaunchVelocityY != null) {
            setOf(PetAction.FLUNG)
        } else {
            emptySet()
        },
        habitat = PetComboHabitat.WALL,
        untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
        sustain(PetAction.CLIMB_WALL, 12_000L..18_000L, motionMultiplier = 1.8f),
        sustain(PetAction.DANGLE, 3_000L..5_000L),
        once(
            PetAction.JUMP,
            directionChange = PetBeatDirectionChange.REVERSE,
            motionMultiplier = 2.5f
        ),
        crossScreenFlight(
            durationMillis = WALL_TO_WALL_CROSS_DURATION_MILLIS,
            launchVelocityY = crossScreenLaunchVelocityY
        ),
        sustain(PetAction.CLIMB_WALL, 5_000L..8_000L, motionMultiplier = 1.8f),
        sustain(PetAction.DANGLE, 6_000L..10_000L),
        once(
            PetAction.JUMP,
            directionChange = PetBeatDirectionChange.REVERSE,
            motionMultiplier = 2.2f
        ),
        sustain(PetAction.FALL, 12_000L..18_000L),
        once(PetAction.BOUNCE),
        sustain(PetAction.SIT, 4_000L..7_000L)
    )

    private fun requiredCombo(
        id: PetComboId,
        requiredActions: Set<PetAction>,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(
        id = id,
        beats = beats.toList(),
        requiredActions = requiredActions
    )

    private fun aerialCombo(
        id: PetComboId,
        requiredActions: Set<PetAction>,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(
        id = id,
        beats = beats.toList(),
        requiredActions = requiredActions,
        habitat = PetComboHabitat.AERIAL
    )

    private fun once(
        action: PetAction,
        directionChange: PetBeatDirectionChange = PetBeatDirectionChange.KEEP,
        motionMultiplier: Float = 1f
    ) = PetComboBeat(
        action = action,
        directionChange = directionChange,
        motionMultiplier = motionMultiplier
    )

    private fun sustain(
        action: PetAction,
        durationMillis: LongRange,
        motionMultiplier: Float = 1f
    ) = PetComboBeat(
        action = action,
        durationMillis = durationMillis,
        motionMultiplier = motionMultiplier
    )

    private fun untilCollision(
        action: PetAction,
        motionMultiplier: Float = 1f
    ) = PetComboBeat(
        action = action,
        completion = PetBeatCompletion.COLLISION,
        motionMultiplier = motionMultiplier
    )

    private fun crossScreenFlight(
        durationMillis: Long,
        launchVelocityY: Float? = null
    ) = PetComboBeat(
        action = if (launchVelocityY == null) PetAction.FALL else PetAction.FLUNG,
        completion = PetBeatCompletion.COLLISION,
        crossScreenDurationMillis = durationMillis,
        crossScreenLaunchVelocityY = launchVelocityY
    )

    private const val MIN_COMBO_BEATS = 2
    private const val MIN_DISTINCT_ACTIONS = 2
    private const val WALL_TO_WALL_CROSS_DURATION_MILLIS = 1_100L
    private const val WALL_TO_WALL_RISE_VELOCITY_Y = -700f
}
