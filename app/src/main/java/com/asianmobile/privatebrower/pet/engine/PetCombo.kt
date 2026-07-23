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

enum class PetBeatPlayback {
    REPEAT,
    PLAY_ONCE,
    HOLD_LAST_FRAME
}

enum class PetComboHabitat {
    GROUND,
    AERIAL,
    WALL,
    CEILING;

    val isClimb: Boolean
        get() = this == WALL || this == CEILING
}

enum class PetComboEnergy {
    CALM,
    CURIOUS,
    ACTIVE,
    STUNT,
    PERFORMANCE
}

data class PetComboBeat(
    val action: PetAction,
    val durationMillis: LongRange? = null,
    val directionChange: PetBeatDirectionChange = PetBeatDirectionChange.KEEP,
    val completion: PetBeatCompletion = PetBeatCompletion.CLIP_OR_DURATION,
    val playback: PetBeatPlayback = PetBeatPlayback.REPEAT,
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
        require(
            playback != PetBeatPlayback.HOLD_LAST_FRAME ||
                (durationMillis != null && completion == PetBeatCompletion.CLIP_OR_DURATION)
        ) {
            "hold-last playback requires a duration-driven beat"
        }
        require(
            playback != PetBeatPlayback.PLAY_ONCE ||
                (durationMillis == null && completion == PetBeatCompletion.CLIP_OR_DURATION)
        ) {
            "play-once playback requires clip completion"
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

    val energy: PetComboEnergy
        get() = when (id) {
            PetComboId.COZY_BREAK,
            PetComboId.DAYDREAM,
            PetComboId.SLOW_MORNING,
            PetComboId.CHATTER,
            PetComboId.USER_AFFECTION,
            PetComboId.SOCIAL_HELLO,
            PetComboId.SOCIAL_HELLO_REPLY,
            PetComboId.SOCIAL_ADMIRE,
            PetComboId.SOCIAL_REST_A,
            PetComboId.SOCIAL_REST_B -> PetComboEnergy.CALM

            PetComboId.CURIOUS_SCOUT,
            PetComboId.SHY_SNEAK,
            PetComboId.BUSY_PATROL,
            PetComboId.PEEK_AND_DASH,
            PetComboId.BRAVE_EXPLORER,
            PetComboId.SOCIAL_APPROACH,
            PetComboId.SOCIAL_COPYCAT_A,
            PetComboId.SOCIAL_COPYCAT_B -> PetComboEnergy.CURIOUS

            PetComboId.HAPPY_ZOOMIES,
            PetComboId.CLUMSY_RECOVERY,
            PetComboId.SOCIAL_CHASE_LEADER,
            PetComboId.SOCIAL_CHASE_FOLLOWER -> PetComboEnergy.ACTIVE

            PetComboId.WALL_PARKOUR,
            PetComboId.CEILING_EXPEDITION,
            PetComboId.WALL_DIVE,
            PetComboId.WALL_TO_WALL_LEAP,
            PetComboId.WALL_TO_WALL_RISE,
            PetComboId.SKY_DIVER,
            PetComboId.NINJA_SKILL,
            PetComboId.ACROBATIC_FINALE -> PetComboEnergy.STUNT

            PetComboId.TINY_PERFORMANCE,
            PetComboId.CHEERFUL_ENCORE,
            PetComboId.BATTLE_DANCE,
            PetComboId.MAGIC_RITUAL,
            PetComboId.USER_SHOWCASE,
            PetComboId.SOCIAL_SHOW_OFF,
            PetComboId.SOCIAL_DUET_A,
            PetComboId.SOCIAL_DUET_B -> PetComboEnergy.PERFORMANCE
        }

    val hasSpeech: Boolean
        get() = actions.any(PetAction::isSpeechAction)

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
            speakWhileWalking(),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            once(PetAction.EMOTE)
        ),
        combo(
            PetComboId.COZY_BREAK,
            sustain(PetAction.IDLE, 3_000L..5_000L),
            sustain(PetAction.SIT, 7_000L..11_000L),
            sustain(PetAction.FLOOR_PLAY, 5_000L..8_000L),
            sustain(PetAction.IDLE, 2_000L..3_500L),
            speak(),
            sustain(PetAction.SPRAWL, 6_000L..10_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.HAPPY_ZOOMIES,
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            once(PetAction.EMOTE),
            sustain(PetAction.RUN, 3_500L..6_000L),
            sustain(PetAction.WALK, 4_000L..6_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SHY_SNEAK,
            sustain(PetAction.IDLE, 3_000L..5_000L),
            sustain(PetAction.CREEP, 5_000L..8_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 2_500L..4_500L)
        ),
        combo(
            PetComboId.CLUMSY_RECOVERY,
            sustain(PetAction.RUN, 2_500L..4_000L),
            once(PetAction.TRIP),
            sustain(PetAction.SPRAWL, 4_000L..6_000L),
            sustain(PetAction.SIT, 5_000L..8_000L),
            speak(),
            once(PetAction.EMOTE)
        ),
        requiredCombo(
            PetComboId.TINY_PERFORMANCE,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL_2),
            once(PetAction.EMOTE),
            speak(),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.DAYDREAM,
            sustain(PetAction.SIT, 6_000L..10_000L),
            sustain(PetAction.LOOK_UP, 4_000L..7_000L),
            sustain(PetAction.SPRAWL, 8_000L..14_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
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
        requiredReversedCombo(
            PetComboId.CHEERFUL_ENCORE,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            once(PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 2_500L..4_000L),
            once(PetAction.EMOTE),
            speak(),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        requiredCombo(
            PetComboId.CHATTER,
            requiredActions = setOf(PetAction.TALK),
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.LOOK_UP, 2_000L..4_000L),
            speak(),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        spatialCombo(
            PetComboId.WALL_PARKOUR,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.HOLD_WALL,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE
            ),
            habitat = PetComboHabitat.WALL,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            sustain(PetAction.CLIMB_WALL, 8_000L..13_000L, motionMultiplier = 1.8f),
            sustain(PetAction.HOLD_WALL, 2_500L..4_000L),
            sustain(PetAction.CLIMB_WALL, 3_000L..6_000L, motionMultiplier = 1.5f),
            sustain(PetAction.HOLD_WALL, 1_500L..2_500L),
            once(
                PetAction.JUMP,
                directionChange = PetBeatDirectionChange.REVERSE,
                motionMultiplier = 2.2f
            ),
            sustain(PetAction.FALL, 12_000L..18_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.WALK, 3_000L..5_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 2_500L..4_000L)
        ),
        spatialCombo(
            PetComboId.CEILING_EXPEDITION,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.HOLD_WALL,
                PetAction.CLIMB_CEILING,
                PetAction.HOLD_CEILING,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE
            ),
            habitat = PetComboHabitat.CEILING,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            untilCollision(PetAction.CLIMB_WALL, motionMultiplier = 2.4f),
            sustain(PetAction.HOLD_WALL, 2_000L..3_500L),
            sustain(
                PetAction.CLIMB_CEILING,
                10_000L..16_000L,
                directionChange = PetBeatDirectionChange.REVERSE
            ),
            sustain(PetAction.HOLD_CEILING, 3_000L..5_000L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 15_000L..22_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        spatialCombo(
            PetComboId.WALL_DIVE,
            requiredActions = setOf(
                PetAction.RUN,
                PetAction.CLIMB_WALL,
                PetAction.HOLD_WALL,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE,
                PetAction.SPECIAL
            ),
            habitat = PetComboHabitat.WALL,
            untilCollision(PetAction.RUN, motionMultiplier = 1.15f),
            sustain(PetAction.CLIMB_WALL, 8_000L..13_000L, motionMultiplier = 1.8f),
            sustain(PetAction.HOLD_WALL, 3_000L..5_000L),
            once(
                PetAction.JUMP,
                directionChange = PetBeatDirectionChange.REVERSE,
                motionMultiplier = 2.2f
            ),
            sustain(PetAction.FALL, 12_000L..18_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 4_000L..6_000L),
            once(PetAction.EMOTE)
        ),
        wallToWallCombo(PetComboId.WALL_TO_WALL_LEAP),
        wallToWallCombo(
            PetComboId.WALL_TO_WALL_RISE,
            crossScreenLaunchVelocityY = WALL_TO_WALL_RISE_VELOCITY_Y
        ),
        aerialCombo(
            PetComboId.SKY_DIVER,
            requiredActions = setOf(PetAction.JUMP, PetAction.FALL, PetAction.BOUNCE),
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 10_000L..16_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.SPRAWL, 4_000L..7_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.EMOTE)
        ),
        aerialCombo(
            PetComboId.NINJA_SKILL,
            requiredActions = setOf(
                PetAction.CREEP,
                PetAction.RUN,
                PetAction.JUMP,
                PetAction.FALL,
                PetAction.BOUNCE,
                PetAction.SPECIAL
            ),
            sustain(PetAction.CREEP, 3_500L..6_000L),
            sustain(PetAction.LOOK_UP, 1_500L..2_500L),
            sustain(PetAction.RUN, 2_000L..3_500L),
            once(PetAction.JUMP, motionMultiplier = 2.2f),
            sustain(PetAction.FALL, 10_000L..16_000L),
            once(PetAction.BOUNCE),
            sustain(PetAction.LOOK_UP, 1_500L..2_500L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 4_000L..6_000L),
            once(PetAction.EMOTE)
        ),
        requiredCombo(
            PetComboId.BATTLE_DANCE,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.LOOK_UP, 2_500L..4_000L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL_2),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        requiredCombo(
            PetComboId.MAGIC_RITUAL,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.SIT, 5_000L..8_000L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            once(PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            speak()
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
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            once(PetAction.SPECIAL),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.USER_AFFECTION,
            once(PetAction.TAPPED),
            sustain(PetAction.IDLE, 1_500L..2_500L),
            speak(),
            once(PetAction.EMOTE)
        ),
        requiredCombo(
            PetComboId.USER_SHOWCASE,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.LOOK_UP, 2_500L..4_000L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL_2),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_500L..6_000L),
            speak()
        ),
        combo(
            PetComboId.SOCIAL_APPROACH,
            sustain(PetAction.RUN, 3_500L..5_500L),
            sustain(PetAction.WALK, 2_500L..4_000L)
        ),
        combo(
            PetComboId.SOCIAL_HELLO,
            speak(),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SOCIAL_HELLO_REPLY,
            sustain(PetAction.IDLE, 9_000L..11_000L),
            speak(),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SOCIAL_CHASE_LEADER,
            once(PetAction.EMOTE),
            sustain(PetAction.RUN, 5_000L..8_000L),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SOCIAL_CHASE_FOLLOWER,
            sustain(PetAction.LOOK_UP, 2_000L..3_500L),
            sustain(PetAction.RUN, 5_500L..8_500L),
            once(PetAction.TRIP),
            sustain(PetAction.SPRAWL, 4_000L..6_000L),
            sustain(PetAction.SIT, 5_000L..8_000L)
        ),
        requiredCombo(
            PetComboId.SOCIAL_SHOW_OFF,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.LOOK_UP, 2_500L..4_000L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            once(PetAction.SPECIAL_2),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L),
            speak()
        ),
        combo(
            PetComboId.SOCIAL_ADMIRE,
            sustain(PetAction.LOOK_UP, 4_000L..7_000L),
            sustain(PetAction.IDLE, 5_000L..8_000L),
            speak(),
            once(PetAction.EMOTE)
        ),
        combo(
            PetComboId.SOCIAL_REST_A,
            sustain(PetAction.SIT, 8_000L..12_000L),
            sustain(PetAction.FLOOR_PLAY, 5_000L..8_000L),
            sustain(PetAction.IDLE, 4_000L..6_000L)
        ),
        combo(
            PetComboId.SOCIAL_REST_B,
            sustain(PetAction.IDLE, 2_000L..4_000L),
            sustain(PetAction.SPRAWL, 8_000L..12_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.IDLE, 3_000L..5_000L)
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_A,
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.FLOOR_PLAY, 5_000L..7_000L)
        ),
        combo(
            PetComboId.SOCIAL_COPYCAT_B,
            sustain(PetAction.IDLE, 1_500L..2_500L),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L),
            once(PetAction.EMOTE),
            sustain(PetAction.FLOOR_PLAY, 5_000L..7_000L)
        ),
        requiredCombo(
            PetComboId.SOCIAL_DUET_A,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 2_000L..2_500L),
            once(PetAction.SPECIAL),
            sustain(PetAction.IDLE, 5_000L..6_000L),
            once(PetAction.SPECIAL_2),
            once(PetAction.EMOTE),
            sustain(PetAction.LOOK_UP, 3_000L..5_000L)
        ),
        requiredCombo(
            PetComboId.SOCIAL_DUET_B,
            requiredActions = setOf(PetAction.SPECIAL, PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 6_000L..7_000L),
            once(PetAction.SPECIAL_2),
            sustain(PetAction.IDLE, 5_000L..6_000L),
            once(PetAction.SPECIAL),
            once(PetAction.EMOTE)
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

    internal fun transitionWeight(
        baseWeight: Int,
        candidate: PetComboDefinition,
        previousComboId: PetComboId?
    ): Int {
        val previous = definition(previousComboId ?: return baseWeight) ?: return baseWeight
        val energyPercent = when (previous.energy) {
            PetComboEnergy.CALM -> when (candidate.energy) {
                PetComboEnergy.CALM -> 70
                PetComboEnergy.CURIOUS -> 125
                PetComboEnergy.ACTIVE -> 140
                PetComboEnergy.STUNT,
                PetComboEnergy.PERFORMANCE -> 110
            }

            PetComboEnergy.CURIOUS -> when (candidate.energy) {
                PetComboEnergy.CALM -> 110
                PetComboEnergy.CURIOUS -> 90
                PetComboEnergy.ACTIVE -> 120
                PetComboEnergy.STUNT,
                PetComboEnergy.PERFORMANCE -> 100
            }

            PetComboEnergy.ACTIVE -> when (candidate.energy) {
                PetComboEnergy.CALM -> 150
                PetComboEnergy.CURIOUS -> 125
                PetComboEnergy.ACTIVE -> 65
                PetComboEnergy.STUNT,
                PetComboEnergy.PERFORMANCE -> 70
            }

            PetComboEnergy.STUNT,
            PetComboEnergy.PERFORMANCE -> when (candidate.energy) {
                PetComboEnergy.CALM -> 170
                PetComboEnergy.CURIOUS -> 135
                PetComboEnergy.ACTIVE -> 70
                PetComboEnergy.STUNT,
                PetComboEnergy.PERFORMANCE -> 45
            }
        }
        val speechPercent = if (previous.hasSpeech && candidate.hasSpeech) 45 else 100
        return (baseWeight.toLong() * energyPercent * speechPercent / 10_000L)
            .toInt()
            .coerceAtLeast(1)
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

    private fun requiredReversedCombo(
        id: PetComboId,
        requiredActions: Set<PetAction>,
        vararg beats: PetComboBeat
    ) = PetComboDefinition(
        id = id,
        beats = beats.toList(),
        requiredActions = requiredActions,
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
            PetAction.HOLD_WALL,
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
        sustain(PetAction.HOLD_WALL, 3_000L..5_000L),
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
        sustain(PetAction.HOLD_WALL, 4_000L..6_000L),
        once(
            PetAction.JUMP,
            directionChange = PetBeatDirectionChange.REVERSE,
            motionMultiplier = 2.2f
        ),
        sustain(PetAction.FALL, 12_000L..18_000L),
        once(PetAction.BOUNCE),
        sustain(PetAction.WALK, 3_000L..5_000L),
        once(PetAction.EMOTE),
        sustain(PetAction.IDLE, 3_000L..5_000L)
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
        playback = PetBeatPlayback.PLAY_ONCE,
        motionMultiplier = motionMultiplier
    )

    private fun sustain(
        action: PetAction,
        durationMillis: LongRange,
        motionMultiplier: Float = 1f,
        directionChange: PetBeatDirectionChange = PetBeatDirectionChange.KEEP
    ) = PetComboBeat(
        action = action,
        durationMillis = durationMillis,
        directionChange = directionChange,
        motionMultiplier = motionMultiplier
    )

    private fun speak() = sustain(
        action = PetAction.TALK,
        durationMillis = PET_TALK_BEAT_DURATION_MILLIS
    )

    private fun speakWhileWalking() = sustain(
        action = PetAction.TALK_WALK,
        durationMillis = PET_TALK_BEAT_DURATION_MILLIS
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

internal val PET_TALK_BEAT_DURATION_MILLIS = 9_000L..11_000L
