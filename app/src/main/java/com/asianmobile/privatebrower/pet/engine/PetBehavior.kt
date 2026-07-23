package com.asianmobile.privatebrower.pet.engine

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
    val maxNonClimbCombosBeforeClimb: Int = 2,
    val autonomousComboRules: List<PetComboRule> = listOf(
        PetComboRule(PetComboId.CURIOUS_SCOUT, 8),
        PetComboRule(PetComboId.COZY_BREAK, 6),
        PetComboRule(PetComboId.HAPPY_ZOOMIES, 8),
        PetComboRule(PetComboId.CLUMSY_RECOVERY, 7),
        PetComboRule(PetComboId.TINY_PERFORMANCE, 6),
        PetComboRule(PetComboId.DAYDREAM, 5),
        PetComboRule(PetComboId.WALL_PARKOUR, 12),
        PetComboRule(PetComboId.CEILING_EXPEDITION, 10),
        PetComboRule(PetComboId.WALL_DIVE, 12),
        PetComboRule(PetComboId.SKY_DIVER, 10),
        PetComboRule(PetComboId.NINJA_SKILL, 12),
        PetComboRule(PetComboId.BATTLE_DANCE, 8),
        PetComboRule(PetComboId.MAGIC_RITUAL, 8),
        PetComboRule(PetComboId.ACROBATIC_FINALE, 12)
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

private fun LongRange.isValid(): Boolean = first > 0 && last >= first
