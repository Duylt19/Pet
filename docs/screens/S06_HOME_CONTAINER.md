# S06 — Home Container

## Visual Reference

- Screenshots: trong tất cả screenshot #2-#5 (Home/Tabs/Files/Progress đều cùng container)
- Cụ thể: [docs/assets/screenshots/Screenshot_20260608-094940.png](../assets/screenshots/Screenshot_20260608-094940.png)

## Mục Đích

Container chính của app sau onboarding: scaffold chứa top bar (hamburger + title), 4 tab nội dung swappable, bottom navigation, sticky banner ad.

## Vị Trí Trong Navigation

- Route: `Routes.HOME`
- Vào từ: SET_DEFAULT_BROWSER (session 1), SPLASH (session 2+), back từ BrowserWebView/Settings/Bookmarks
- Ra đến: BROWSER_WEBVIEW (qua tab Home action), BOOKMARKS_HISTORY (drawer), SETTINGS (drawer), HOW_TO_DOWNLOAD modal
- Back behavior:
  - Nếu drawer mở → đóng drawer
  - Nếu tab ≠ Home tab → switch về Home tab
  - Nếu tab = Home → show ExitDialog "Exit Private Browser?"

## Layout Breakdown

```
┌─────────────────────────────────────┐
│  ☰  Private Browser                 │  <- AppHeaderBar (hamburger left + center title)
├─────────────────────────────────────┤
│                                     │
│        [Active tab content]         │  <- swap theo selectedTab: 0/1/2/3
│                                     │
│                                     │
├─────────────────────────────────────┤
│  Home   Tabs(2)  Files   Progress   │  <- BottomNavBar 4 items
│   ●       ○        ○        ○        │     selected has indicator line top
├─────────────────────────────────────┤
│  [Sticky Banner Ad - 50dp height]   │
└─────────────────────────────────────┘
```

**Top bar:**
- Height `_36sdp` (~48dp)
- Hamburger icon 24dp left, padding `_12sdp`
- Title "Private Browser" center, Title L Bold
- Background `colors_FFFFFF`

**Bottom nav:**
- Height `_42sdp` (~56dp)
- 4 items equal weight
- Selected: icon tint primary, label primary, line indicator top 4dp width 24dp
- Tab "Tabs" có badge: small purple bg `colors_7C5BFB` với số count tab Normal mở

**Drawer (left):**
- Modal navigation drawer 80% width
- Header: app logo + name + "Settings"
- Items:
  - Bookmarks
  - History (same screen, default tab History)
  - Settings
  - Share app
  - Rate us (optional)
  - Privacy Policy
  - Premium upgrade (nếu chưa premium)

## States

| State | Behavior |
|-------|----------|
| selectedTab = 0 (Home) | Render `BrowserHomeTabScreen` |
| selectedTab = 1 (Tabs) | Render `TabsTabScreen` |
| selectedTab = 2 (Files) | Render `FilesTabScreen` |
| selectedTab = 3 (Progress) | Render `ProgressTabScreen` |
| Drawer mở | Modal navigation drawer slide in |
| Tab badge | `tabsCount > 0` hiển thị; > 99 hiển thị "99+" |

## ViewModel Contract

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val preferencesRepository: PreferencesRepository,
    private val billingHelper: BillingHelper,
) : ViewModel() {

    data class UiState(
        val selectedTab: Int = 0,
        val tabsCount: Int = 0,
        val isPremium: Boolean = false,
        val isDrawerOpen: Boolean = false,
    )

    val uiState: StateFlow<UiState>

    fun onTabSelected(index: Int)
    fun onOpenDrawer()
    fun onCloseDrawer()
}
```

Composable:
```kotlin
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(if (state.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { HomeDrawerContent(navController, ...) },
    ) {
        Scaffold(
            topBar = { AppHeaderBar(title = "Private Browser", leadingIcon = Hamburger, onLeadingClick = viewModel::onOpenDrawer) },
            bottomBar = {
                Column {
                    BottomNavBar(selected = state.selectedTab, tabsCount = state.tabsCount, onClick = viewModel::onTabSelected)
                    if (!state.isPremium) BannerAd(bannerAdsId = R.string.banner_id_home)
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (state.selectedTab) {
                    0 -> BrowserHomeTabScreen(navController)
                    1 -> TabsTabScreen(navController)
                    2 -> FilesTabScreen(navController)
                    3 -> ProgressTabScreen(navController)
                }
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            state.isDrawerOpen -> viewModel.onCloseDrawer()
            state.selectedTab != 0 -> viewModel.onTabSelected(0)
            else -> showExitDialog = true
        }
    }
}
```

## Resources

```xml
<string name="home_title">Private Browser</string>
<string name="home_tab_home_label">Home</string>
<string name="home_tab_tabs_label">Tabs</string>
<string name="home_tab_files_label">Files</string>
<string name="home_tab_progress_label">Progress</string>
<string name="home_exit_dialog_title">Exit Private Browser?</string>
<string name="home_exit_dialog_message">Are you sure you want to exit the app?</string>
<string name="home_drawer_bookmarks">Bookmarks</string>
<string name="home_drawer_history">History</string>
<string name="home_drawer_settings">Settings</string>
<string name="home_drawer_share">Share app</string>
<string name="home_drawer_premium">Upgrade Premium</string>

<!-- ad unit -->
<string name="banner_id_home" translatable="false">ca-app-pub-...</string>
```

Drawables:
- `ic_hamburger.xml`
- `ic_tab_home.xml`, `ic_tab_home_selected.xml`
- `ic_tab_tabs.xml`, `ic_tab_tabs_selected.xml`
- `ic_tab_files.xml`, `ic_tab_files_selected.xml`
- `ic_tab_progress.xml`, `ic_tab_progress_selected.xml`

## Ads

- Banner sticky bottom (`banner_id_home`)
- Interstitial khi enter Home từ onboarding
- Ad ẩn khi `isPremium`

## Edge Cases & Accessibility

- Tab badge > 99: hiển thị "99+"
- Switch tab nhanh: debounce 300ms
- Drawer mở + tap content area → đóng drawer
- contentDescription bottom nav item: "Home tab, selected/unselected, X of 4"
- Min touch target 48dp cho bottom nav items
- RTL: drawer mở từ right thay vì left

## Acceptance Criteria

- [ ] 4 tab swap mượt
- [ ] Bottom nav indicator chính xác
- [ ] Tab badge cập nhật realtime khi mở/đóng tab
- [ ] Drawer mở/đóng mượt
- [ ] Back press 2 lần thoát app (Tab khác → Home → Exit)
- [ ] Banner ad load + premium ẩn
- [ ] Restart từ background giữ tab đã chọn

## Liên Quan

- [S06a_HOME_BROWSER_TAB.md](S06a_HOME_BROWSER_TAB.md)
- [S06b_TABS_TAB.md](S06b_TABS_TAB.md)
- [S06c_FILES_TAB.md](S06c_FILES_TAB.md)
- [S06d_PROGRESS_TAB.md](S06d_PROGRESS_TAB.md)
- [F03_TABS_MANAGER.md](../features/F03_TABS_MANAGER.md) — tabsCount source
