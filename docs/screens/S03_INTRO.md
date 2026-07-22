# S03 — Intro / Onboarding

## Visual Reference

- Screenshot: không trong demo set (kế thừa base)
- Figma: TODO

## Mục Đích

Onboarding 3-4 page giới thiệu USP của app: Private browsing, Fast download, Multi-tab, Search anywhere.

## Vị Trí Trong Navigation

- Route: `Routes.INTRO`
- Vào từ: LANGUAGE
- Ra đến: PERMISSION (sau page cuối tap "Get Started")
- Back behavior: chuyển page trước; ở page 1 → tắt app

## Layout Breakdown

```
┌─────────────────────────────┐
│                  [Skip]     │   <- text button top-right
├─────────────────────────────┤
│                             │
│     [Hero illustration]     │   <- 240sdp x 240sdp, image/Lottie per page
│                             │
│     Page title              │   <- Display 22ssp bold
│     Page description        │   <- Body L colors_808080
│                             │
├─────────────────────────────┤
│   ● ● ○ ○                   │   <- dot indicator
├─────────────────────────────┤
│  [PrimaryGradientButton]    │   <- "Next" / "Get Started" (page cuối)
├─────────────────────────────┤
│  [Native Ad inline]         │
└─────────────────────────────┘
```

## Pages (4 đề xuất)

| # | Title | Description | Illustration |
|---|-------|-------------|--------------|
| 1 | Browse privately | No tracking, no history. Stay invisible online. | mask/incognito |
| 2 | Download fast | Save videos and files directly from any site. | download arrow |
| 3 | Multi-tab freedom | Open many sites at once — Normal or Incognito. | tabs grid |
| 4 | Search your way | Choose from 6 search engines including DuckDuckGo. | search engines logos |

## States

| State | Display |
|-------|---------|
| Page 1-3 | Button label "Next" |
| Page 4 (cuối) | Button label "Get Started" |
| Tap Skip | Bỏ qua → PERMISSION |
| Tap Next | Animate sang page kế |
| Swipe | HorizontalPager handle swipe |

## ViewModel Contract

```kotlin
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    data class UiState(val currentPage: Int = 0, val pages: List<IntroPage> = IntroPage.ALL)

    val uiState: StateFlow<UiState>
    fun onPageChanged(index: Int)
    fun onNext()       // nếu cuối → completeIntro + nav PERMISSION
    fun onSkip()       // completeIntro + nav PERMISSION
}
```

## Resources

```xml
<string name="intro_page1_title">Browse privately</string>
<string name="intro_page1_desc">No tracking, no history. Stay invisible online.</string>
<string name="intro_page2_title">Download fast</string>
<string name="intro_page2_desc">Save videos and files directly from any site.</string>
<string name="intro_page3_title">Multi-tab freedom</string>
<string name="intro_page3_desc">Open many sites at once — Normal or Incognito.</string>
<string name="intro_page4_title">Search your way</string>
<string name="intro_page4_desc">Choose from 6 search engines including DuckDuckGo.</string>
<string name="intro_skip_button">Skip</string>
<string name="intro_next_button">Next</string>
<string name="intro_get_started_button">Get Started</string>
```

Drawables: `intro_illustration_1`...`_4` (PNG/Lottie raw).

## Ads

- Native inline mỗi page (`native_intro_full` từ ads module). Có thể skip page cuối để tăng tỉ lệ tap "Get Started".

## Edge Cases & Accessibility

- Swipe nhanh → debounce
- contentDescription cho illustration: "Page X of 4 — title"
- Skip = same effect là cuối page (mark intro done)
- Pager `userScrollEnabled = true` cho phép swipe back/forward

## Acceptance Criteria

- [ ] 4 page hiển thị đúng theo HorizontalPager
- [ ] Dot indicator đồng bộ với page
- [ ] Button text đổi page cuối
- [ ] Skip / Get Started → nav PERMISSION
- [ ] Restart app sau intro → không quay lại screen này
