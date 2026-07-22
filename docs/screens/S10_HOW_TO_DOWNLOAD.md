# S10 — How To Download (Tutorial Modal)

## Visual Reference

- Screenshot: [Screenshot_20260608-095113.png](../assets/screenshots/Screenshot_20260608-095113.png)

## Mục Đích

Modal tutorial 3 bước hướng dẫn user download video từ web. Disclaimer ở dưới về copyright.

## Vị Trí Trong Navigation

- KHÔNG phải route (modal). Render với `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))`
- Vào từ: Files tab "How to Download" button, Progress tab "How to Download" button
- Ra: tap X hoặc "Got it"

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ×       How to Download            │  <- top bar: close X + title
├─────────────────────────────────────┤
│                                     │
│  1. Search or type URL              │  <- step heading Title M
│  ┌────────────────────────────────┐ │
│  │ G  http://www.example          │ │  <- mock URL bar
│  └────────────────────────────────┘ │
│                                     │
│  2. Select video to Download        │
│  ┌────────────────────────────────┐ │
│  │ 🔍 https://www.example  + [3] ⋮│ │
│  │ ┌──────────┐ ▰▰▰▰▰▰▰          │ │  <- mock browser with video items
│  │ │ 00:15    │ ▰▰▰▰▰▰▰          │ │
│  │ └──────────┘                   │ │
│  │ ┌──────────┐ ▰▰▰▰▰▰▰    [●]   │ │
│  │ │ 00:15    │ ▰▰▰▰▰▰▰          │ │
│  │ └──────────┘                   │ │
│  └────────────────────────────────┘ │
│                                     │
│  3. Play the video                  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │       Got it          →      │  │  <- PrimaryGradientButton
│  └──────────────────────────────┘  │
│                                     │
│  Please get the permission from     │  <- Disclaimer Body M secondary
│  the owner before you repost...     │     full text below
│                                     │
├─────────────────────────────────────┤
│  [Sticky banner ad with sign up]    │  <- banner inherit hoặc dedicated
└─────────────────────────────────────┘
```

**Specs:**

- Modal fullscreen với background white
- Top bar: X close icon left + "How to Download" center
- Steps:
  - Step heading: Title M Bold black, padding top `_12sdp`
  - Step illustration card: bg `colors_F2F2F7`, radius `_9sdp`, padding `_9sdp`
  - Mock URL/browser representation, dùng images sẵn
- Got it button: full width pill, gradient
- Disclaimer text: padding horizontal `_18sdp`, Body M `colors_808080`, max width 320dp, line height 1.5
- Banner ad sticky bottom

### Disclaimer Text Đầy Đủ

> "Please get the permission from the owner before you repost videos. Private Browser: Safe & Secure is just a tool. We have no control over the user and what they use it for. Any unauthorized actions (e.g. re-posting or downloading of contents) and/or violations of intellectual property rights is the sole responsibility of the user."

## States

| State | Display |
|-------|---------|
| Default | Render all sections |
| Got it tap | Dismiss modal |
| X tap | Dismiss modal |

## ViewModel Contract

Không cần ViewModel riêng — pure UI composable với callback:

```kotlin
@Composable
fun HowToDownloadDialog(
    onDismiss: () -> Unit,
    onGotIt: () -> Unit = onDismiss,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colorResource(R.color.colors_FFFFFF)) {
            Column {
                AppHeaderBar(title = stringResource(R.string.how_to_download_title), leadingIcon = Close, onLeadingClick = onDismiss)
                // ... steps
                PrimaryGradientButton(stringResource(R.string.how_to_download_got_it), onClick = onGotIt)
                Text(stringResource(R.string.how_to_download_disclaimer))
                Spacer(Modifier.weight(1f))
                BannerAd(bannerAdsId = R.string.banner_id_how_to_download)
            }
        }
    }
}
```

## Resources

```xml
<string name="how_to_download_title">How to Download</string>
<string name="how_to_download_step1_title">1. Search or type URL</string>
<string name="how_to_download_step2_title">2. Select video to Download</string>
<string name="how_to_download_step3_title">3. Play the video</string>
<string name="how_to_download_step1_url">http://www.example</string>
<string name="how_to_download_step2_url">https://www.example</string>
<string name="how_to_download_got_it">Got it</string>
<string name="how_to_download_disclaimer">Please get the permission from the owner before you repost videos. Private Browser: Safe &amp; Secure is just a tool. We have no control over the user and what they use it for. Any unauthorized actions (e.g. re-posting or downloading of contents) and/or violations of intellectual property rights is the sole responsibility of the user.</string>
```

Drawables:
- `ic_close_x.xml`
- `how_to_download_step1.xml` (mock URL bar)
- `how_to_download_step2.xml` (mock browser with video) — có thể là layout Compose dựng thay vì drawable
- `how_to_download_step3.xml` (placeholder hoặc Lottie)

## Ads

- Banner sticky bottom (theo screenshot có "Sign up" CTA — ad mediation)

## Edge Cases & Accessibility

- Back button hardware → dismiss
- Tap outside modal → KHÔNG dismiss (force read content)
- Long disclaimer text scrollable nếu màn hình nhỏ
- contentDescription cho close X
- Banner ad fail → collapse, không reserve space trống

## Acceptance Criteria

- [ ] Layout match screenshot #6
- [ ] 3 step illustrations + heading
- [ ] Got it button → dismiss
- [ ] Close X → dismiss
- [ ] Disclaimer text đầy đủ
- [ ] Banner ad load
- [ ] Dialog không cancel khi tap outside (modal force)

## Liên Quan

- [F05_DOWNLOAD_MANAGER.md](../features/F05_DOWNLOAD_MANAGER.md)
- [S06c_FILES_TAB.md](S06c_FILES_TAB.md)
- [S06d_PROGRESS_TAB.md](S06d_PROGRESS_TAB.md)
