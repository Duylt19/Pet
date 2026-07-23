package com.asianmobile.privatebrower.pet.engine

data class PetBehaviorRule(
    val action: PetAction,
    val weight: Int
) {
    init {
        require(weight > 0) { "behavior weight must be positive" }
    }
}

data class PetBehaviorProfile(
    val groundDelayMillis: LongRange = 1_800L..6_500L,
    val idleDurationMillis: LongRange = 900L..2_800L,
    val runDurationMillis: LongRange = 700L..2_200L,
    val creepDurationMillis: LongRange = 1_200L..3_500L,
    val wallDurationMillis: LongRange = 1_600L..6_500L,
    val ceilingDurationMillis: LongRange = 1_200L..5_000L,
    val continueWalkWeight: Int = 25,
    val turnAroundWeight: Int = 15,
    val wallJumpChancePercent: Int = 55,
    val wallDescendChancePercent: Int = 30,
    val recentActionMemory: Int = 2,
    val autonomousRules: List<PetBehaviorRule> = listOf(
        PetBehaviorRule(PetAction.IDLE, 24),
        PetBehaviorRule(PetAction.RUN, 13),
        PetBehaviorRule(PetAction.SIT, 16),
        PetBehaviorRule(PetAction.WINK, 14),
        PetBehaviorRule(PetAction.LOOK_UP, 12),
        PetBehaviorRule(PetAction.DANGLE, 8),
        PetBehaviorRule(PetAction.CREEP, 10),
        PetBehaviorRule(PetAction.TRIP, 7),
        PetBehaviorRule(PetAction.SPECIAL, 5),
        PetBehaviorRule(PetAction.SPECIAL_2, 4)
    )
) {
    init {
        require(groundDelayMillis.isValid()) { "ground delay range must be positive" }
        require(idleDurationMillis.isValid()) { "idle duration range must be positive" }
        require(runDurationMillis.isValid()) { "run duration range must be positive" }
        require(creepDurationMillis.isValid()) { "creep duration range must be positive" }
        require(wallDurationMillis.isValid()) { "wall duration range must be positive" }
        require(ceilingDurationMillis.isValid()) { "ceiling duration range must be positive" }
        require(continueWalkWeight >= 0) { "continue walk weight must not be negative" }
        require(turnAroundWeight >= 0) { "turn around weight must not be negative" }
        require(continueWalkWeight + turnAroundWeight > 0 || autonomousRules.isNotEmpty()) {
            "behavior profile must contain a possible decision"
        }
        require(wallJumpChancePercent in 0..100) {
            "wall jump chance must be a percentage"
        }
        require(wallDescendChancePercent in 0..100) {
            "wall descend chance must be a percentage"
        }
        require(wallJumpChancePercent + wallDescendChancePercent <= 100) {
            "wall exit chances must not exceed 100 percent"
        }
        require(recentActionMemory >= 0) { "recent action memory must not be negative" }
    }
}

private fun LongRange.isValid(): Boolean = first > 0 && last >= first
