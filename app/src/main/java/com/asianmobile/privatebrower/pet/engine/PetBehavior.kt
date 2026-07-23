package com.asianmobile.privatebrower.pet.engine

data class PetBehaviorProfile(
    val groundDelayMillis: LongRange = 1_800L..6_500L,
    val idleDurationMillis: LongRange = 900L..2_800L,
    val runDurationMillis: LongRange = 700L..2_200L,
    val creepDurationMillis: LongRange = 1_200L..3_500L,
    val wallDurationMillis: LongRange = 1_600L..6_500L,
    val ceilingDurationMillis: LongRange = 1_200L..5_000L,
    val wallJumpChancePercent: Int = 55,
    val wallDescendChancePercent: Int = 30,
    val recentComboMemory: Int = 3,
    val autonomousComboRules: List<PetComboRule> = listOf(
        PetComboRule(PetComboId.CURIOUS_SCOUT, 15),
        PetComboRule(PetComboId.COZY_BREAK, 13),
        PetComboRule(PetComboId.HAPPY_ZOOMIES, 11),
        PetComboRule(PetComboId.SHY_SNEAK, 10),
        PetComboRule(PetComboId.CLUMSY_RECOVERY, 8),
        PetComboRule(PetComboId.TINY_PERFORMANCE, 7),
        PetComboRule(PetComboId.DAYDREAM, 9),
        PetComboRule(PetComboId.BUSY_PATROL, 12),
        PetComboRule(PetComboId.PEEK_AND_DASH, 10),
        PetComboRule(PetComboId.SLOW_MORNING, 12),
        PetComboRule(PetComboId.BRAVE_EXPLORER, 9),
        PetComboRule(PetComboId.CHEERFUL_ENCORE, 6)
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
    }
}

private fun LongRange.isValid(): Boolean = first > 0 && last >= first
