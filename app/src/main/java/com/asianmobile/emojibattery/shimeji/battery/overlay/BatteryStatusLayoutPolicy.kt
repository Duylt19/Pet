package com.asianmobile.emojibattery.shimeji.battery.overlay

/**
 * Pure layout policy shared by the overlay and editor preview.
 *
 * Widths can be expressed in either px or dp as long as [availableWidth] uses the same unit.
 * Lower-priority optional components are removed until the remaining content cannot overlap.
 */
class BatteryStatusLayoutPolicy {
    fun resolve(
        availableWidth: Float,
        items: List<BatteryStatusLayoutItem>
    ): BatteryStatusLayoutResult {
        if (!availableWidth.isFinite() || availableWidth <= 0f) {
            return BatteryStatusLayoutResult(emptySet(), items.map { it.component }.toSet())
        }

        val visible = items
            .filter { it.width.isFinite() && it.width > 0f }
            .toMutableList()
        val hidden = items
            .filterNot { it.width.isFinite() && it.width > 0f }
            .mapTo(mutableSetOf()) { it.component }

        while (visible.sumOf { it.width.toDouble() } > availableWidth && visible.isNotEmpty()) {
            val removable = visible
                .withIndex()
                .filterNot { it.value.required }
                .minWithOrNull(
                    compareBy<IndexedValue<BatteryStatusLayoutItem>>(
                        { it.value.priority },
                        { -it.index }
                    )
                )
                ?: break
            hidden += removable.value.component
            visible.removeAt(removable.index)
        }

        return BatteryStatusLayoutResult(
            visibleComponents = visible.mapTo(linkedSetOf()) { it.component },
            hiddenComponents = hidden
        )
    }
}

data class BatteryStatusLayoutItem(
    val component: BatteryStatusComponent,
    val width: Float,
    val priority: Int,
    val required: Boolean = false
)

data class BatteryStatusLayoutResult(
    val visibleComponents: Set<BatteryStatusComponent>,
    val hiddenComponents: Set<BatteryStatusComponent>
) {
    fun shows(component: BatteryStatusComponent): Boolean = component in visibleComponents
}

enum class BatteryStatusComponent {
    TIME,
    DATE,
    AIRPLANE,
    RINGER,
    ANIMATION,
    THEME_EMOJI,
    EMOTION,
    HOTSPOT,
    CELLULAR,
    WIFI,
    PERCENTAGE,
    BATTERY,
    CHARGE
}

data class BatteryStatusPhysicalSides(
    val leadingFromLeft: Boolean,
    val trailingFromLeft: Boolean
) {
    companion object {
        fun resolve(isRtl: Boolean): BatteryStatusPhysicalSides =
            BatteryStatusPhysicalSides(
                leadingFromLeft = !isRtl,
                trailingFromLeft = isRtl
            )
    }
}
