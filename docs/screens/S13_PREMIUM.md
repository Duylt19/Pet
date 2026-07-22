# S13 — Premium / Subscription

## Visual Reference

- Screenshot: kế thừa từ base FileRecovery
- Figma: TODO

## Mục Đích

Trang chào bán subscription Premium: hiển thị benefits, plans, BillingClient flow, confirm purchase.

## Vị Trí Trong Navigation

- Route: `Routes.PREMIUM`
- Vào từ:
  - Splash session 2+ (Premium Splash Return — nếu RC bật)
  - Get Started onboarding (Premium Onboarding First — nếu RC bật)
  - Home drawer (Upgrade Premium)
  - Settings (nếu có row, hiện không có theo screenshot — optional)
- Ra: back về screen trigger; sau Purchase Success → back

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  [Close X]              [Restore]   │
├─────────────────────────────────────┤
│         [Crown illustration]        │  <- hero image
│                                     │
│        Premium                      │  <- Display Bold
│   Unlock all features               │  <- Body L secondary
│                                     │
│   ┌─────────────────────────────┐  │
│   │ ✓ No ads                    │  │
│   │ ✓ Unlimited tabs            │  │
│   │ ✓ Faster downloads          │  │
│   │ ✓ Priority support          │  │
│   └─────────────────────────────┘  │
│                                     │
│   ┌─────────────────────────────┐  │
│   │ Monthly        $X.XX       │  │  <- plan option
│   │ Yearly         $XX.XX  ●   │  │  <- selected
│   │ Lifetime       $XXX.XX     │  │
│   └─────────────────────────────┘  │
│                                     │
│  [Subscribe - PrimaryGradient]      │
│                                     │
│  Terms / Privacy / Restore links    │
└─────────────────────────────────────┘
```

**Specs:**
- Hero illustration: Crown icon hoặc Lottie
- Title "Premium" Display Bold center
- Benefits card: 4 row check icons + text
- Plans: 3 option (Monthly/Yearly/Lifetime) tuỳ Play Console config
- Subscribe button: full width
- Footer links Caption

## States

| State | Display |
|-------|---------|
| Loading plans | Skeleton cards |
| Plans loaded | Render options |
| Plan selected | Highlight border |
| Purchase in progress | Button loading |
| Purchase success | Toast "Welcome to Premium!" + dismiss |
| Purchase failed | Error snackbar |
| Already premium | Hiển thị "You are already Premium" + Close |

## ViewModel Contract

```kotlin
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingHelper: BillingHelper,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val plans: List<PremiumPlan> = emptyList(),
        val selectedPlanId: String? = null,
        val isPurchasing: Boolean = false,
        val isPremium: Boolean = false,
        val errorMessage: String? = null,
    )

    val uiState: StateFlow<UiState>

    fun loadPlans()
    fun onPlanSelected(id: String)
    fun onSubscribe(activity: Activity)
    fun onRestore()
}
```

Tích hợp với `BillingHelper` đã có sẵn từ base.

## Resources

```xml
<string name="premium_title">Premium</string>
<string name="premium_subtitle">Unlock all features</string>
<string name="premium_benefit_no_ads">No ads</string>
<string name="premium_benefit_unlimited_tabs">Unlimited tabs</string>
<string name="premium_benefit_faster_downloads">Faster downloads</string>
<string name="premium_benefit_priority_support">Priority support</string>
<string name="premium_plan_monthly">Monthly</string>
<string name="premium_plan_yearly">Yearly</string>
<string name="premium_plan_lifetime">Lifetime</string>
<string name="premium_subscribe_button">Subscribe</string>
<string name="premium_restore_button">Restore Purchase</string>
<string name="premium_already_message">You are already Premium</string>
```

## Ads

- KHÔNG ads trên Premium screen (Premium chính là remove ads)
- OpenAd OFF

## Edge Cases & Accessibility

- Network fail load plans → retry button
- BillingClient không available → "Play Services required" message
- Purchase cancelled by user → dismiss flow gracefully
- Restore: query existing entitlements + apply
- contentDescription cho close X, plans
- Min touch target 48dp

## Acceptance Criteria

- [ ] Load plans từ Play Console
- [ ] Selected plan visual highlight
- [ ] Subscribe trigger BillingClient flow
- [ ] Purchase success → isPremium = true → ads ẩn ngay
- [ ] Restore hoạt động
- [ ] Already premium screen render đúng

## Liên Quan

- BillingHelper trong base code
- [07_ADS_INTEGRATION.md](../07_ADS_INTEGRATION.md) — premium hides ads
