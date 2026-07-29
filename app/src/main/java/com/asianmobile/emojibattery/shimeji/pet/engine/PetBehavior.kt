package com.asianmobile.emojibattery.shimeji.pet.engine

data class PetBehaviorProfile(
    val groundDelayMillis: LongRange = 4_000L..8_000L,
    val idleDurationMillis: LongRange = 3_000L..7_000L,
    val runDurationMillis: LongRange = 1_800L..4_000L,
    val creepDurationMillis: LongRange = 3_500L..7_500L,
    val wallDurationMillis: LongRange = 1_600L..6_500L,
    val ceilingDurationMillis: LongRange = 1_200L..5_000L,
    val wallJumpChancePercent: Int = 55,
    val wallDescendChancePercent: Int = 30,
    val recentComboMemory: Int = 3,
    val maxNonClimbCombosBeforeClimb: Int = 3,
    val blockedActions: Set<PetAction> = emptySet(),
    val autonomousComboRules: List<PetComboRule> = listOf(
        PetComboRule(PetComboId.CURIOUS_SCOUT, 10),
        PetComboRule(PetComboId.COZY_BREAK, 6),
        PetComboRule(PetComboId.HAPPY_ZOOMIES, 8),
        PetComboRule(PetComboId.CLUMSY_RECOVERY, 6),
        PetComboRule(PetComboId.TINY_PERFORMANCE, 5),
        PetComboRule(PetComboId.DAYDREAM, 5),
        PetComboRule(PetComboId.CHATTER, 5),
        PetComboRule(PetComboId.WALL_PARKOUR, 10),
        PetComboRule(PetComboId.CEILING_EXPEDITION, 8),
        PetComboRule(PetComboId.WALL_DIVE, 8),
        PetComboRule(PetComboId.WALL_TO_WALL_LEAP, 8),
        PetComboRule(PetComboId.WALL_TO_WALL_RISE, 8),
        PetComboRule(PetComboId.SKY_DIVER, 7),
        PetComboRule(PetComboId.NINJA_SKILL, 8),
        PetComboRule(PetComboId.BATTLE_DANCE, 6),
        PetComboRule(PetComboId.MAGIC_RITUAL, 6),
        PetComboRule(PetComboId.ACROBATIC_FINALE, 8)
    )
) {
    init {
        require(groundDelayMillis.isValid()) { "ground delay range must be positive" }
        require(idleDurationMillis.isValid()) { "idle duration range must be positive" }
        require(runDurationMillis.isValid()) { "run duration range must be positive" }
        require(creepDurationMillis.isValid()) { "creep duration range must be positive" }
        require(wallDurationMillis.isValid()) { "wall duration range must be positive" }
        require(ceilingDurationMillis.isValid()) { "ceiling duration range must be positive" }
        require(autonomousComboRules.isNotEmpty()) { "behavior profile must contain a combo" }
        require(wallJumpChancePercent in 0..100) {
            "wall jump chance must be a percentage"
        }
        require(wallDescendChancePercent in 0..100) {
            "wall descend chance must be a percentage"
        }
        require(wallJumpChancePercent + wallDescendChancePercent <= 100) {
            "wall exit chances must not exceed 100 percent"
        }
        require(recentComboMemory >= 0) { "recent combo memory must not be negative" }
        require(maxNonClimbCombosBeforeClimb > 0) {
            "non-climb combo limit must be positive"
        }
    }
}

object PetBehaviorProfiles {
    val SWARM = PetBehaviorProfile(
        groundDelayMillis = 900L..2_400L,
        idleDurationMillis = 800L..1_800L,
        runDurationMillis = 2_200L..4_500L,
        creepDurationMillis = 1_200L..2_800L,
        wallDurationMillis = 900L..2_800L,
        ceilingDurationMillis = 800L..2_400L,
        wallJumpChancePercent = 90,
        wallDescendChancePercent = 5,
        recentComboMemory = 2,
        maxNonClimbCombosBeforeClimb = 1,
        blockedActions = setOf(PetAction.TALK, PetAction.TALK_WALK),
        autonomousComboRules = listOf(
            PetComboRule(PetComboId.HAPPY_ZOOMIES, 18),
            PetComboRule(PetComboId.BUSY_PATROL, 10),
            PetComboRule(PetComboId.PEEK_AND_DASH, 12),
            PetComboRule(PetComboId.BRAVE_EXPLORER, 10),
            PetComboRule(PetComboId.WALL_PARKOUR, 22),
            PetComboRule(PetComboId.CEILING_EXPEDITION, 14),
            PetComboRule(PetComboId.WALL_DIVE, 20),
            PetComboRule(PetComboId.WALL_TO_WALL_LEAP, 36),
            PetComboRule(PetComboId.WALL_TO_WALL_RISE, 36),
            PetComboRule(PetComboId.SKY_DIVER, 28),
            PetComboRule(PetComboId.NINJA_SKILL, 22),
            PetComboRule(PetComboId.BATTLE_DANCE, 8),
            PetComboRule(PetComboId.ACROBATIC_FINALE, 24)
        )
    )
}

private fun LongRange.isValid(): Boolean = first > 0 && last >= first
