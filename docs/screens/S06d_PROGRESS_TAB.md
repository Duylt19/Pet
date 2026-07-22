# S06d — Progress Tab (Download Progress)

## Visual Reference

- Screenshot: [Screenshot_20260608-095040.png](../assets/screenshots/Screenshot_20260608-095040.png)

## Mục Đích

Hiển thị danh sách download đang chạy / paused / completed gần đây. Empty state với button "How to Download".

## Vị Trí Trong Navigation

- Render bên trong `HomeScreen` khi `selectedTab == 3`

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ☰  Progress                        │  <- top bar (override title)
├─────────────────────────────────────┤
│                                     │
│                                     │
│        ┌──────┐                     │
│        │      │  + + + +            │  <- Empty illustration
│        │  📁 │                      │     ic_folder_empty (mặt cười)
│        │      │                     │
│        └──────┘                     │
│                                     │
│      No file is downloading         │  <- Body L colors_808080
│                                     │
│  ┌────────────────────────────────┐ │
│  │       How to Download          │ │  <- PrimaryGradientButton
│  └────────────────────────────────┘ │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  [Sticky banner ad inherit]         │
└─────────────────────────────────────┘
```

**Specs:**

- Top bar title: "Progress"
- Empty state vertical centered, takes ~60% height:
  - Illustration `ic_folder_empty` size `_75sdp` (~100dp)
  - Spacer `_9sdp`
  - Text "No file is downloading" Body L `colors_808080`
  - Spacer `_18sdp`
  - "How to Download" button full width minus padding, `_42sdp` height
- Padding screen horizontal `_18sdp`

### Khi có downloads active:

```
┌─────────────────────────────────────┐
│  ☰  Progress                        │
├─────────────────────────────────────┤
│  Active Downloads                   │
│  ┌────────────────────────────────┐ │
│  │ 🎬 video1.mp4         [×]     │ │  <- Item with cancel
│  │ ▰▰▰▰▰▰▰▰▱▱  45%               │ │     LinearProgressIndicator
│  │ 12.4 MB / 27.5 MB - 320KB/s   │ │     Caption
│  └────────────────────────────────┘ │
│  ┌────────────────────────────────┐ │
│  │ 🎬 video2.mp4         [▶]     │ │  <- Paused, resume button
│  │ ▰▰▱▱▱▱▱▱▱▱  18%               │ │
│  │ 5.0 MB / 28.0 MB - PAUSED     │ │
│  └────────────────────────────────┘ │
│                                     │
│  Recently Completed                 │
│  ┌────────────────────────────────┐ │
│  │ ✓ image1.jpg                   │ │
│  │ 2.4 MB                         │ │
│  └────────────────────────────────┘ │
│                                     │
│  [How to Download button]           │
└─────────────────────────────────────┘
```

## States

| State | Display |
|-------|---------|
| No downloads at all | Empty state với illustration + button |
| Có active downloads | Section "Active" + items + "Recently Completed" |
| Tất cả completed | Section "Recently Completed" + button "How to Download" |
| Permission notification denied | Banner warning trên cùng (optional) |

## ViewModel Contract

```kotlin
@HiltViewModel
class ProgressTabViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    data class UiState(
        val activeItems: List<DownloadItem> = emptyList(),
        val completedItems: List<DownloadItem> = emptyList(),
    )

    val uiState: StateFlow<UiState>

    fun onCancelDownload(id: Long)
    fun onPauseDownload(id: Long)
    fun onResumeDownload(id: Long)
    fun onCompletedItemClicked(item: DownloadItem)   // open file via intent
    fun onHowToDownloadClicked()
}
```

## Resources

```xml
<string name="progress_title">Progress</string>
<string name="progress_no_downloading">No file is downloading</string>
<string name="progress_how_to_download_button">How to Download</string>
<string name="progress_section_active">Active Downloads</string>
<string name="progress_section_completed">Recently Completed</string>
<string name="progress_status_pending">Pending</string>
<string name="progress_status_running">%1$s / %2$s - %3$s</string>  <!-- 12.4 MB / 27.5 MB - 320KB/s -->
<string name="progress_status_paused">PAUSED</string>
<string name="progress_status_failed">FAILED: %1$s</string>
<string name="progress_status_completed">Completed</string>
<string name="progress_cancel_label">Cancel</string>
<string name="progress_pause_label">Pause</string>
<string name="progress_resume_label">Resume</string>
```

Drawables:
- `ic_folder_empty.xml` (folder face đối chiếu screenshot)
- `ic_pause.xml`, `ic_resume.xml`, `ic_cancel.xml`
- `ic_file_video.xml`, `ic_file_image.xml`, etc. by mime type

## Ads

- Banner sticky inherit
- KHÔNG show OpenAd khi đang có active downloads

## Edge Cases & Accessibility

- Download completed → tap → open file qua Intent.VIEW
- Cancel download → confirm dialog
- File completed nhưng bị xoá ngoài app → mark as "File missing"
- Long running download > 1h → vẫn render bình thường, refresh mỗi giây
- contentDescription cho action buttons
- Item swipe to dismiss = cancel/delete (v2)

## Acceptance Criteria

- [ ] Layout match screenshot #5 (empty state)
- [ ] Illustration + text + button đúng vị trí
- [ ] Khi có download active: list realtime update progress mỗi giây
- [ ] Tap "How to Download" → modal mở
- [ ] Cancel/Pause/Resume action functional
- [ ] Empty state hiện khi không có download

## Liên Quan

- [F05_DOWNLOAD_MANAGER.md](../features/F05_DOWNLOAD_MANAGER.md)
- [S10_HOW_TO_DOWNLOAD.md](S10_HOW_TO_DOWNLOAD.md)
