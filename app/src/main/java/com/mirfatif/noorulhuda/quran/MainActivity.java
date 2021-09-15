package com.mirfatif.noorulhuda.quran;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_JUZS;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_MANZILS;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_PAGES;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_SURAHS;
import static com.mirfatif.noorulhuda.dua.DuaActivity.EXTRA_AAYAH_NUM;
import static com.mirfatif.noorulhuda.dua.DuaActivity.EXTRA_SURAH_NUM;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.isLandscape;
import static com.mirfatif.noorulhuda.util.Utils.reduceDragSensitivity;
import static com.mirfatif.noorulhuda.util.Utils.setNightTheme;
import static com.mirfatif.noorulhuda.util.Utils.setTooltip;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import androidx.annotation.ArrayRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.util.Pair;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.ActivityMainBinding;
import com.mirfatif.noorulhuda.databinding.GotoPickerBinding;
import com.mirfatif.noorulhuda.databinding.SearchHelpBinding;
import com.mirfatif.noorulhuda.databinding.SliderBinding;
import com.mirfatif.noorulhuda.db.AayahEntity;
import com.mirfatif.noorulhuda.db.DbBuilder;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.SurahEntity;
import com.mirfatif.noorulhuda.dua.DuaActivity;
import com.mirfatif.noorulhuda.feedback.Feedback;
import com.mirfatif.noorulhuda.prayer.PrayerTimeActivity;
import com.mirfatif.noorulhuda.prayer.WidgetProvider;
import com.mirfatif.noorulhuda.prefs.AppUpdate;
import com.mirfatif.noorulhuda.prefs.MySettings;
import com.mirfatif.noorulhuda.svc.PrayerNotifySvc;
import com.mirfatif.noorulhuda.tags.TagsDialogFragment;
import com.mirfatif.noorulhuda.ui.AboutActivity;
import com.mirfatif.noorulhuda.ui.base.BaseActivity;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListCallback;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListItem;
import com.mirfatif.noorulhuda.util.FileDownload;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends BaseActivity {

  private static final String TAG = "MainActivity";

  private ActivityMainBinding mB;
  private BackupRestore mBackupRestore;

  private QuranPageAdapter mQuranPageAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Activity is recreated on switching to Dark Theme, so return here
    if (setNightTheme(this)) {
      return;
    }
    mB = ActivityMainBinding.inflate(getLayoutInflater());
    setBgColor();
    setContentView(mB.getRoot());

    ActionBar actionbar = getSupportActionBar();
    if (actionbar != null) {
      actionbar.hide();
    }

    setUpBottomBar();

    mQuranPageAdapter = new QuranPageAdapter(this);
    mB.pager.setAdapter(mQuranPageAdapter);
    mB.pager.registerOnPageChangeCallback(new ScrollListener());
    reduceDragSensitivity(mB.pager);

    setUpSearchView();

    mB.leftArrow.setOnClickListener(v -> arrowClicked(true));
    mB.rightArrow.setOnClickListener(v -> arrowClicked(false));

    if (SETTINGS.isDbBuilt(DbBuilder.MAIN_DB)) {
      refreshUi(false);
    } else {
      buildDbAndRefreshUi();
    }

    Window window = getWindow();
    if (window != null) {
      LayoutParams params = window.getAttributes();
      params.screenBrightness = SETTINGS.getBrightness();
      window.setAttributes(params);
    }

    PrayerNotifySvc.reset(false);
    WidgetProvider.reset();
    Utils.runBg(() -> new AppUpdate().check(true));

    goToAayah(getIntent());

    SETTINGS.getFontSizeChanged().observe(this, empty -> refreshUi(true));

    if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
      SETTINGS.plusAppLaunchCount();
    }

    mBackupRestore = new BackupRestore(this);
    mFeedbackTask = () -> new Feedback(this).askForFeedback();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    goToAayah(intent);
  }

  @Override
  protected void onStop() {
    super.onStop();
    SETTINGS.setScrollPosition(mCurrentPage, mCurrentAayah);
  }

  private boolean mControlsShown = false;

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus && !mControlsShown) {
      toggleFullScreen(false);
      mControlsShown = true;
      Window window = getWindow();
      View view;
      if (window != null && (view = window.getDecorView()) != null) {
        view.setOnSystemUiVisibilityChangeListener(
            flags -> mIsFullScreen = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0);
      }
    }
  }

  @Override
  public void onBackPressed() {
    QuranPageFragment page = getPageFrag(null);
    if (page != null && page.onBackPressed()) {
      return;
    }
    if (mB != null) {
      // SearchView does not but androidx.appcompat.widget.SearchView auto clears
      // focus when Soft Keyboard is closed.
      if (mB.bottomBar.searchV.hasFocus()) {
        mB.bottomBar.searchV.clearFocus();
        return;
      }
      if (!mB.bottomBar.searchV.isIconified()) {
        collapseSearchView();
        return;
      }
    }
    if (VERSION.SDK_INT == VERSION_CODES.Q) {
      // Bug: https://issuetracker.google.com/issues/139738913
      finishAfterTransition();
    } else {
      super.onBackPressed();
    }
  }

  private final ScheduledExecutorService FB_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
  private Runnable mFeedbackTask;

  @Override
  protected void onResume() {
    super.onResume();
    if (mFeedbackTask != null) {
      FB_EXECUTOR.schedule(mFeedbackTask, 2, TimeUnit.SECONDS);
    }
  }

  private static final String CLASS = MainActivity.class.getName();
  private static final String TAG_NAVIGATOR = CLASS + ".NAVIGATOR";
  private static final String TAG_BACKUP_RESTORE = CLASS + ".TAG_BACKUP_RESTORE";

  @Override
  public AlertDialog createDialog(String tag, AlertDialogFragment dialogFragment) {
    if (TAG_NAVIGATOR.equals(tag)) {
      return getGotoDialog();
    }
    if (TAG_BACKUP_RESTORE.equals(tag)) {
      return mBackupRestore.createDialog();
    }
    return super.createDialog(tag, dialogFragment);
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// GENERAL ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void setBgColor() {
    int bgColorRes = SETTINGS.getBgColor();
    if (bgColorRes > 0) {
      mB.getRoot().setBackgroundColor(getColor(bgColorRes));
    } else {
      mB.getRoot().setBackgroundColor(Color.TRANSPARENT);
    }
  }

  private void showBrightnessSlider() {
    Window window = getWindow();
    if (window == null) {
      return;
    }
    LayoutParams wParams = window.getAttributes();

    SliderBinding b = SliderBinding.inflate(getLayoutInflater());
    SeekBar seekBar = b.getRoot();
    final int MAX = 100;
    seekBar.setMax(MAX);
    if (wParams.screenBrightness != BRIGHTNESS_OVERRIDE_NONE) {
      seekBar.setProgress((int) (wParams.screenBrightness * MAX));
    }
    int width = App.getRes().getDisplayMetrics().widthPixels * (isLandscape() ? 5 : 9) / 10;
    PopupWindow popup = new PopupWindow(seekBar, width, LayoutParams.WRAP_CONTENT);
    popup.setElevation(500);
    popup.setOverlapAnchor(true);
    popup.setOutsideTouchable(true); // Dismiss on outside touch.
    Runnable hider = popup::dismiss;
    seekBar.setOnSeekBarChangeListener(
        new OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            wParams.screenBrightness = (float) progress / MAX;
            if (wParams.screenBrightness == 0) {
              wParams.screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
            }
            window.setAttributes(wParams);
            SETTINGS.saveBrightness(wParams.screenBrightness);
            seekBar.removeCallbacks(hider);
            seekBar.postDelayed(hider, 5000);
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    popup.showAtLocation(
        mB.bottomBar.getRoot(), Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 2 * Utils.toPx(48));
    seekBar.postDelayed(hider, 5000);
  }

  private void buildDbAndRefreshUi() {
    AlertDialogFragment dialog = showDbBuildDialog();
    Utils.showThirdPartyCredits(this, false);
    Utils.runBg(
        () -> {
          if (DbBuilder.buildDb(DbBuilder.MAIN_DB)) {
            refreshUi(false);
          }
          Utils.runUi(this, dialog::dismissIt);
        });
  }

  private AlertDialogFragment showDbBuildDialog() {
    Builder builder =
        new Builder(this).setTitle(R.string.creating_database).setView(R.layout.dialog_progress);
    AlertDialogFragment dialog = AlertDialogFragment.show(this, builder.create(), "BUILD_DATABASE");
    dialog.setCancelable(false);
    return dialog;
  }

  private void refreshUi(boolean saveScrollPos) {
    if (saveScrollPos) {
      SETTINGS.setScrollPosition(mCurrentPage, mCurrentAayah);
    }
    if (Utils.isMainThread()) {
      refreshUi();
    } else {
      Utils.runUi(this, this::refreshUi).waitForMe();
    }
  }

  private void refreshUi() {
    updateHeaderCosmetics();
    updateSearchSettingViews();

    if (SETTINGS.isPageModeAndNotInSearch()) {
      mQuranPageAdapter.setPageCount(TOTAL_PAGES);
      // Restore slide position.
      mB.pager.setCurrentItem(SETTINGS.getLastPage() - 1, false);
    } else {
      mQuranPageAdapter.setPageCount(1);
    }
  }

  private void arrowClicked(boolean nextPage) {
    int pos = mB.pager.getCurrentItem();
    mB.pager.setCurrentItem(nextPage ? pos + 1 : pos - 1, true);
    cancelAutoFullScreen();
  }

  void setProgBarVisibility(boolean show) {
    if (show) {
      mB.progressBar.show();
    } else {
      mB.progressBar.hide();
    }
  }

  private void showHelpDialog() {
    SearchHelpBinding b = SearchHelpBinding.inflate(getLayoutInflater());
    b.searchHelpV.setText(R.string.search_help);
    int[] charArray = getResources().getIntArray(R.array.search_chars);
    String[] descArray = getResources().getStringArray(R.array.search_chars_desc);
    List<Pair<Integer, String>> chars = new ArrayList<>();
    for (int i = 0; i < charArray.length; i++) {
      chars.add(new Pair<>(charArray[i], descArray[i]));
    }
    b.recyclerV.setAdapter(new SearchHelpAdapter(chars));
    b.recyclerV.setLayoutManager(new LinearLayoutManager(this));
    Builder builder =
        new Builder(this).setTitle(R.string.search_help_menu_item).setView(b.getRoot());
    AlertDialogFragment.show(this, builder.create(), "SEARCH_HELP");
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////////// FULL SCREEN //////////////////////////
  //////////////////////////////////////////////////////////////////

  private boolean mIsFullScreen = false;

  private final ScheduledExecutorService mAutoFullScreenExecutor =
      Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> mAutoFullScreenFuture;

  private void cancelAutoFullScreen() {
    if (mAutoFullScreenFuture != null) {
      mAutoFullScreenFuture.cancel(false);
    }
  }

  private void autoFullScreen() {
    if (mB == null || mB.bottomBar.searchV.isIconified()) {
      toggleFullScreen(true);
    }
  }

  public static final int PRE_FULL_SCREEN_FLAGS =
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
  public static final int FULL_SCREEN_FLAGS =
      View.SYSTEM_UI_FLAG_IMMERSIVE
          | View.SYSTEM_UI_FLAG_LOW_PROFILE
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_FULLSCREEN;

  void toggleFullScreen(Boolean hideControls) {
    /* Do not hide controls if soft keyboard is visible. SearchView does not but
    androidx.appcompat.widget.SearchView auto clears focus when Soft Keyboard is closed. So
    we need to manually check if focus is due to soft keyboard.
    */
    if (mB != null && mB.bottomBar.searchV.hasFocus() && mSoftKbVisible) {
      return;
    }

    Window window = getWindow();
    View view;
    if (window == null || (view = window.getDecorView()) == null) {
      return;
    }

    cancelAutoFullScreen();
    boolean setAutoFullScreen = false;

    int flags;

    if (hideControls == null) {
      flags = view.getSystemUiVisibility();
      hideControls = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
    } else if (!hideControls) {
      setAutoFullScreen = true;
    }

    if (hideControls && mIsFullScreen) {
      return;
    }

    flags = PRE_FULL_SCREEN_FLAGS;
    if (hideControls) {
      flags |= FULL_SCREEN_FLAGS;

      mB.bottomBar.getRoot().setVisibility(View.GONE);
    } else {
      mB.bottomBar.getRoot().setVisibility(View.VISIBLE);
    }

    if (setAutoFullScreen) {
      Runnable task =
          () ->
              Utils.runUi(
                  this,
                  () -> {
                    if (mB.bottomBar.feedbackCont.getVisibility() != View.VISIBLE) {
                      autoFullScreen();
                    }
                  });
      mAutoFullScreenFuture = mAutoFullScreenExecutor.schedule(task, 3, SECONDS);
    }

    view.setSystemUiVisibility(flags);
    mIsFullScreen = hideControls;

    toggleArrowsVisibility(hideControls);
  }

  private void toggleArrowsVisibility(boolean hide) {
    boolean showLeft, showRight;
    showLeft = showRight = !mIsFullScreen && SETTINGS.isPageModeAndNotInSearch() && !hide;
    showLeft = showLeft && mB.pager.getCurrentItem() < TOTAL_PAGES - 1;
    showRight = showRight && mB.pager.getCurrentItem() > 0;
    mB.leftArrow.setVisibility(showLeft ? View.VISIBLE : View.GONE);
    mB.rightArrow.setVisibility(showRight ? View.VISIBLE : View.GONE);
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// MENUS /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private boolean mSoftKbVisible = false;

  private void setUpBottomBar() {
    ImageView[] bottomBarItems =
        new ImageView[] {
          mB.bottomBar.actionSearchSettings,
          mB.bottomBar.actionSearchHelp,
          mB.bottomBar.actionBrightness,
          mB.bottomBar.actionBgColor,
          mB.bottomBar.actionTextColor,
          mB.bottomBar.actionFontSize,
          mB.bottomBar.actionFont,
          mB.bottomBar.actionPageView,
          mB.bottomBar.actionInfoHeader,
          mB.bottomBar.actionOverflow
        };

    for (ImageView view : bottomBarItems) {
      view.setOnClickListener(v -> handleMenuItemClick(v.getId()));
      setTooltip(view);
    }

    mB.bottomBar.searchSettingsTrans.setOnClickListener(
        v -> {
          SETTINGS.toggleSearchInTranslation();
          updateSearchSettingViews();
          if (mB != null && !mB.bottomBar.searchV.isIconified()) {
            mB.bottomBar.searchV.setQuery(null, true);
          }
        });

    mB.bottomBar.searchSettingsVowels.setOnClickListener(
        v -> {
          SETTINGS.toggleSearchWithVowels();
          updateSearchSettingViews();
          if (mB != null && !mB.bottomBar.searchV.isIconified()) {
            mB.bottomBar.searchV.setQuery(null, true);
          }
        });

    mB.bottomBar.searchSettingsTrans.setSelected(true);
    mB.bottomBar.searchSettingsVowels.setSelected(true);
    updateSearchSettingViews();

    /* Display#getSize() and Display#getRealSize() give total diff (status bar + nav bar).
    So using WindowInsetsListener instead.

    It also covers functionality of
    getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener()
    which is required to move bottom app bar above soft keyboard since
    windowSoftInputMode="adjustResize" does not work with FLAG_FULLSCREEN.
    A diff of display height with visible Activity area is calculated on each onGlobalLayout()
    call to figure out if soft kb is visible or not. Then setTranslationY() is called on
    bottom bar to move it up the keyboard.
    */
    mB.bottomBar
        .getRoot()
        .setOnApplyWindowInsetsListener(
            (v, insets) -> {
              int leftOff = insets.getSystemWindowInsetLeft();
              int bottomOff = insets.getSystemWindowInsetBottom();
              int rightOff = insets.getSystemWindowInsetRight();
              v.setPadding(leftOff, 0, rightOff, bottomOff);

              View windowView = getWindow().getDecorView();

              // Coordinates of visible Activity area.
              Rect activitySize = new Rect();
              windowView.getWindowVisibleDisplayFrame(activitySize);
              // Display height
              int height = windowView.getContext().getResources().getDisplayMetrics().heightPixels;
              mSoftKbVisible = bottomOff >= height / 4;

              return v.onApplyWindowInsets(insets);
            });
  }

  private void updateSearchSettingViews() {
    boolean searchingTrans = false;
    if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
      mB.bottomBar.searchSettingsTrans.setEnabled(true);
      if (SETTINGS.doSearchInTranslation()) {
        mB.bottomBar.searchSettingsTrans.setChecked(true);
        searchingTrans = true;
      }
    } else {
      mB.bottomBar.searchSettingsTrans.setEnabled(false);
      mB.bottomBar.searchSettingsTrans.setChecked(false);
    }

    if (searchingTrans) {
      mB.bottomBar.searchSettingsVowels.setChecked(false);
      mB.bottomBar.searchSettingsVowels.setEnabled(false);
    } else {
      mB.bottomBar.searchSettingsVowels.setChecked(SETTINGS.doSearchWithVowels());
      mB.bottomBar.searchSettingsVowels.setEnabled(true);
    }
  }

  private void handleMenuItemClick(int itemId) {
    cancelAutoFullScreen();

    if (itemId == R.id.action_search_settings) {
      if (mB.bottomBar.searchSettingsCont.getVisibility() == View.VISIBLE) {
        mB.bottomBar.searchSettingsCont.setVisibility(View.GONE);
      } else {
        mB.bottomBar.searchSettingsCont.setVisibility(View.VISIBLE);
      }
    } else if (itemId == R.id.action_search_help) {
      showHelpDialog();
    } else if (itemId == R.id.action_brightness) {
      showBrightnessSlider();
    } else if (itemId == R.id.action_bg_color) {
      SETTINGS.setNextBgColor();
      setBgColor();
    } else if (itemId == R.id.action_text_color) {
      SETTINGS.setNextFontColor();
      refreshUi(true);
    } else if (itemId == R.id.action_font_size) {
      SETTINGS.setNextFontSize();
    } else if (itemId == R.id.action_font) {
      PopupMenu popupMenu = new PopupMenu(this, mB.bottomBar.actionFont);
      popupMenu.inflate(R.menu.main_font);
      popupMenu.setOnMenuItemClickListener(this::setFont);
      Menu menu = popupMenu.getMenu();
      String font = SETTINGS.getFontName();
      if (font.equals(getString(R.string.font_hafs))) {
        menu.findItem(R.id.action_font_hafs).setChecked(true);
      } else if (font.equals(getString(R.string.font_warsh))) {
        menu.findItem(R.id.action_font_warsh).setChecked(true);
      } else if (font.equals(getString(R.string.font_saleem))) {
        menu.findItem(R.id.action_font_saleem).setChecked(true);
      } else if (font.equals(getString(R.string.font_scheherazade))) {
        menu.findItem(R.id.action_font_sch).setChecked(true);
      } else if (font.equals(getString(R.string.font_noor_e_huda))) {
        menu.findItem(R.id.action_font_noor).setChecked(true);
      } else if (font.equals(getString(R.string.font_me_quran))) {
        menu.findItem(R.id.action_font_me).setChecked(true);
      } else if (font.equals(getString(R.string.font_kitab))) {
        menu.findItem(R.id.action_font_kitab).setChecked(true);
      }
      popupMenu.show();
    } else if (itemId == R.id.action_page_view) {
      SETTINGS.togglePageMode();
      refreshUi(true);
      if (!mIsFullScreen) {
        toggleArrowsVisibility(false);
      }
    } else if (itemId == R.id.action_info_header) {
      SETTINGS.toggleShowHeader();
      refreshUi(true);
    } else if (itemId == R.id.action_overflow) {
      PopupMenu popupMenu = new PopupMenu(this, mB.bottomBar.actionOverflow);
      popupMenu.inflate(R.menu.main_overflow);
      Menu menu = popupMenu.getMenu();
      popupMenu.setOnMenuItemClickListener(this::handleMenuItemClick);
      MenuCompat.setGroupDividerEnabled(menu, true);
      setOptionalIconsVisible(menu);
      menu.findItem(R.id.action_dark_theme).setChecked(SETTINGS.getForceDarkMode());
      menu.findItem(R.id.action_trans_with_text).setChecked(SETTINGS.showTransWithText());

      String themeColor = SETTINGS.getThemeColor();
      if (themeColor.equals(getString(R.string.theme_color_green))) {
        menu.findItem(R.id.action_theme_color_green).setChecked(true);
      } else if (themeColor.equals(getString(R.string.theme_color_blue))) {
        menu.findItem(R.id.action_theme_color_blue).setChecked(true);
      } else if (themeColor.equals(getString(R.string.theme_color_red))) {
        menu.findItem(R.id.action_theme_color_red).setChecked(true);
      } else if (themeColor.equals(getString(R.string.theme_color_gray))) {
        menu.findItem(R.id.action_theme_color_gray).setChecked(true);
      }

      popupMenu.show();
    }
  }

  private boolean setFont(MenuItem item) {
    int itemId = item.getItemId();
    Integer fontFile = null, fontName = null;
    if (itemId == R.id.action_font_hafs) {
      fontName = R.string.font_hafs;
    } else if (itemId == R.id.action_font_warsh) {
      fontName = R.string.font_warsh;
      fontFile = R.string.font_file_warsh;
    } else if (itemId == R.id.action_font_saleem) {
      fontName = R.string.font_saleem;
      fontFile = R.string.font_file_saleem;
    } else if (itemId == R.id.action_font_sch) {
      fontName = R.string.font_scheherazade;
      fontFile = R.string.font_file_scheherazade;
    } else if (itemId == R.id.action_font_noor) {
      fontName = R.string.font_noor_e_huda;
      fontFile = R.string.font_file_noor_e_huda;
    } else if (itemId == R.id.action_font_me) {
      fontName = R.string.font_me_quran;
      fontFile = R.string.font_file_me_quran;
    } else if (itemId == R.id.action_font_kitab) {
      fontName = R.string.font_kitab;
      fontFile = R.string.font_file_kitab;
    }

    if (fontName != null) {
      if (fontFile == null || SETTINGS.getFontFile(getString(fontFile)).exists()) {
        if (!getString(fontName).equals(SETTINGS.getFontName())) {
          SETTINGS.setFont(getString(fontName));
          refreshUi(true);
        }
      } else {
        downloadFonts(QURAN_FONTS_ZIP, getString(fontName), true);
      }
      return true;
    }
    return false;
  }

  @SuppressLint("RestrictedApi")
  private void setOptionalIconsVisible(Menu menu) {
    if (menu instanceof MenuBuilder) {
      ((MenuBuilder) menu).setOptionalIconsVisible(true);
    }
  }

  private boolean handleMenuItemClick(MenuItem item) {
    int itemId = item.getItemId();

    if (itemId == R.id.action_dark_theme) {
      SETTINGS.setForceDarkMode(!item.isChecked());
      setNightTheme(this);
      return true;
    }

    Integer newThemeColor = null;
    if (itemId == R.id.action_theme_color_green) {
      newThemeColor = R.string.theme_color_green;
    } else if (itemId == R.id.action_theme_color_blue) {
      newThemeColor = R.string.theme_color_blue;
    } else if (itemId == R.id.action_theme_color_red) {
      newThemeColor = R.string.theme_color_red;
    } else if (itemId == R.id.action_theme_color_gray) {
      newThemeColor = R.string.theme_color_gray;
    }

    if (newThemeColor != null) {
      if (!getString(newThemeColor).equals(SETTINGS.getThemeColor())) {
        SETTINGS.setThemeColor(getString(newThemeColor));
        recreate();
      }
      return true;
    }

    if (itemId == R.id.action_bookmarks) {
      Utils.runBg(this::showBookmarks);
      return true;
    }

    if (itemId == R.id.action_tags) {
      new TagsDialogFragment()
          .showNow(getSupportFragmentManager(), TagsDialogFragment.PARENT_FRAG_TAG);
      return true;
    }

    if (itemId == R.id.action_search_fake) {
      setSearchViewVisibility(true);
      return true;
    }

    if (itemId == R.id.action_goto) {
      AlertDialogFragment.show(this, null, TAG_NAVIGATOR);
      return true;
    }

    if (itemId == R.id.action_texts) {
      showDbDialog(
          R.array.db_text_names,
          R.array.db_text_files,
          R.string.texts,
          SETTINGS.getQuranDbName(),
          false);
      return true;
    }

    if (itemId == R.id.action_select_trans) {
      showDbDialog(
          R.array.db_trans_names,
          R.array.db_trans_files,
          R.string.translations,
          SETTINGS.getTransDbName(),
          true);
      return true;
    }

    if (itemId == R.id.action_trans_with_text) {
      item.setChecked(!item.isChecked());
      SETTINGS.setShowTransWithText(item.isChecked());
      refreshUi(true);
      return true;
    }

    if (itemId == R.id.action_prayer_time) {
      startActivity(new Intent(this, PrayerTimeActivity.class));
      return true;
    }

    if (itemId == R.id.action_supplications) {
      Intent intent = new Intent(this, DuaActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      startActivity(intent);
      return true;
    }

    if (itemId == R.id.action_backup_restore) {
      AlertDialogFragment.show(this, null, TAG_BACKUP_RESTORE);
      return true;
    }

    if (itemId == R.id.action_about) {
      startActivity(new Intent(App.getCxt(), AboutActivity.class));
      return true;
    }

    return false;
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// BOOKMARKS ///////////////////////////
  //////////////////////////////////////////////////////////////////

  // Inspired from ListFragment
  private void showBookmarks() {
    List<DialogListItem> items = new ArrayList<>();
    List<AayahEntity> aayahs =
        QuranDao.getAayahEntities(SETTINGS.getQuranDb(), SETTINGS.getBookmarks());
    aayahs.sort(Comparator.comparingInt(a -> a.id));

    for (AayahEntity aayah : aayahs) {
      SurahEntity surah;
      if (aayah == null || (surah = SETTINGS.getMetaDb().getSurah(aayah.surahNum)) == null) {
        continue;
      }
      DialogListItem item = new DialogListItem();
      item.title = getString(R.string.surah_name, surah.name);
      item.subTitle = getArNum(aayah.aayahNum);
      item.text = aayah.text;
      items.add(item);
    }

    DialogListCallback callback =
        new DialogListCallback() {
          @Override
          public void onItemSelect(int pos) {
            goTo(aayahs.get(pos));
          }

          @Override
          public void onDelete(int pos) {
            int id = aayahs.get(pos).id;
            aayahs.remove(pos);
            Utils.runBg(() -> SETTINGS.removeBookmark(id));
          }
        };
    AlertDialogFragment.showListDialog(
        this, R.string.bookmarks_menu_item, R.string.long_press_to_bookmark, items, callback);
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// GOTO //////////////////////////////
  //////////////////////////////////////////////////////////////////

  private AlertDialog getGotoDialog() {
    GotoPickerBinding b = GotoPickerBinding.inflate(getLayoutInflater());
    b.surahNameV.setTypeface(SETTINGS.getTypeface());

    b.typePicker.setMinValue(1);
    b.typePicker.setMaxValue(PICKER_TYPES.length);
    b.typePicker.setDisplayedValues(PICKER_TYPES);
    b.typePicker.setValue(SETTINGS.getNavigatorType());
    b.typePicker.setOnValueChangedListener(
        (p, o, n) -> typePickerChanged(b.typePicker, b.valuePicker, b.surahNameV));

    b.valuePicker.setMinValue(1);
    b.valuePicker.setFormatter(Utils::getArNum);
    typePickerChanged(b.typePicker, b.valuePicker, b.surahNameV);
    b.valuePicker.setValue(SETTINGS.getNavigatorValue());
    Utils.runBg(() -> setSurahName(b.valuePicker.getValue(), b.surahNameV));
    b.valuePicker.setOnValueChangedListener(
        (picker, oldVal, newVal) -> {
          if (b.typePicker.getValue() == PICKER_TYPE_SURAH) {
            Utils.runBg(() -> setSurahName(newVal, b.surahNameV));
          }
        });

    Builder builder =
        new Builder(this)
            .setTitle(R.string.goto_menu_item)
            .setPositiveButton(
                R.string.go,
                (dialog, which) -> Utils.runBg(() -> goTo(b.typePicker, b.valuePicker)))
            .setNegativeButton(android.R.string.cancel, null)
            .setView(b.getRoot());
    return builder.create();
  }

  private static final int PICKER_TYPE_SURAH = 1;
  private static final int PICKER_TYPE_JUZ = 2;
  private static final int PICKER_TYPE_MANZIL = 3;
  private static final int PICKER_TYPE_PAGE = 4;

  private final String[] PICKER_TYPES =
      new String[] {
        Utils.getString(R.string.surah),
        Utils.getString(R.string.juz),
        Utils.getString(R.string.manzil),
        Utils.getString(R.string.page)
      };

  private final int[] PICKER_MAX_VALUES =
      new int[] {TOTAL_SURAHS, TOTAL_JUZS, TOTAL_MANZILS, TOTAL_PAGES};

  private void typePickerChanged(
      NumberPicker typePicker, NumberPicker valuePicker, TextView surahNameView) {
    valuePicker.setMaxValue(PICKER_MAX_VALUES[typePicker.getValue() - 1]);
    if (typePicker.getValue() == PICKER_TYPE_SURAH) {
      Utils.runBg(() -> setSurahName(valuePicker.getValue(), surahNameView));
      surahNameView.setVisibility(View.VISIBLE);
    } else {
      surahNameView.setVisibility(View.INVISIBLE);
    }
  }

  private void setSurahName(int surahNum, TextView surahNameView) {
    String name = getString(R.string.surah_name, SETTINGS.getMetaDb().getSurah(surahNum).name);
    Utils.runUi(this, () -> surahNameView.setText(name));
  }

  private void goTo(NumberPicker typePicker, NumberPicker valuePicker) {
    SETTINGS.setNavigatorState(typePicker.getValue(), valuePicker.getValue());

    int value = valuePicker.getValue();
    QuranDao db = SETTINGS.getQuranDb();

    int type = typePicker.getValue();
    if (type == PICKER_TYPE_SURAH) {
      goTo(db.getSurahStartEntity(value));
    } else if (type == PICKER_TYPE_JUZ) {
      goTo(db.getJuzStartEntity(value));
    } else if (type == PICKER_TYPE_MANZIL) {
      goTo(db.getManzilStartEntity(value));
    } else if (type == PICKER_TYPE_PAGE) {
      goTo(db.getAayahEntities(value).get(0));
    }
  }

  private final ScrollPos mScrollPos = new ScrollPos();

  public void goTo(AayahEntity aayah) {
    if (Utils.isMainThread()) {
      Utils.runBg(() -> goTo(aayah));
      return;
    }

    // Make sure we are not in search.
    if (!mB.bottomBar.searchV.isIconified()) {
      // On returning from search mode, new List is submitted to Adapter.
      // Let's save the required positions to move to.
      SETTINGS.setScrollPosition(aayah.page, aayah.id);

      Utils.runUi(this, this::collapseSearchView).waitForMe();

      // notifyDataSetChanged() is asynchronous. Let's give it a second to complete.
      SystemClock.sleep(1000);
    }

    int page = aayah.page;
    if (SETTINGS.isPageModeAndNotInSearch()) {
      synchronized (mScrollPos) {
        mScrollPos.page = page;
        mScrollPos.aayahId = aayah.id;
      }
      int pos = page - 1;
      Utils.runUi(
          this,
          () -> {
            if (mB.pager.getCurrentItem() == pos) {
              scrollRvToAayahId(page);
            } else {
              mB.pager.setCurrentItem(pos, true);
            }
          });
    } else {
      Utils.runUi(this, () -> scrollRvToPos(null, aayah.id, true));
    }
  }

  private void goToAayah(Intent intent) {
    int surahNum, aayahNum;
    if (intent != null
        && (surahNum = intent.getIntExtra(EXTRA_SURAH_NUM, 0)) > 0
        && (aayahNum = intent.getIntExtra(EXTRA_AAYAH_NUM, 0)) > 0) {
      Utils.runBg(() -> goTo(surahNum, aayahNum));
    }
  }

  private void goTo(int surahNum, int aayahNum) {
    AayahEntity aayah = SETTINGS.getQuranDb().getAayahEntity(surahNum, aayahNum);
    goTo(aayah);
  }

  private static class ScrollPos {

    Integer page, aayahId;

    private void reset() {
      page = null;
      aayahId = null;
    }
  }

  private QuranPageFragment getPageFrag(@Nullable Integer page) {
    if (page == null) {
      page = mB.pager.getCurrentItem() + 1;
    }
    if (mQuranPageAdapter != null) {
      String fragTag = "f" + mQuranPageAdapter.getItemId(page - 1);
      Fragment frag = getSupportFragmentManager().findFragmentByTag(fragTag);
      if (frag instanceof QuranPageFragment) {
        return (QuranPageFragment) frag;
      }
    }
    return null;
  }

  private void scrollRvToPos(@Nullable Integer page, int rvPos, boolean highlight) {
    QuranPageFragment pageFrag = getPageFrag(page);
    if (pageFrag != null) {
      pageFrag.scrollToPos(rvPos, 0, highlight);
    }
  }

  private boolean scrollRvToAayahId(int page) {
    synchronized (mScrollPos) {
      if (mScrollPos.page != null && mScrollPos.page == page) {
        QuranPageFragment pageFrag = getPageFrag(page);
        if (pageFrag != null) {
          int id = mScrollPos.aayahId;
          mScrollPos.reset();
          pageFrag.scrollToAayahId(id);
        }
        return true;
      }
      return false;
    }
  }

  Integer getScrollPos(int page) {
    synchronized (mScrollPos) {
      if (mScrollPos.page != null && mScrollPos.page == page) {
        int id = mScrollPos.aayahId;
        mScrollPos.reset();
        return id;
      }
      return null;
    }
  }

  private final AtomicInteger mLastPage = new AtomicInteger();

  private void onPageSelected(int page) {
    synchronized (mLastPage) {
      if (!scrollRvToAayahId(page) && page != mLastPage.get()) {
        scrollRvToPos(page, 0, false);
      }
      mLastPage.set(page);
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// DATABASES ///////////////////////////
  //////////////////////////////////////////////////////////////////

  private void showDbDialog(
      @ArrayRes int names,
      @ArrayRes int files,
      @StringRes int title,
      String current,
      boolean downloadFont) {
    String[] dbNames = getResources().getStringArray(files);
    int selected = Arrays.asList(dbNames).indexOf(current);

    AlertDialogFragment dialogFragment = new AlertDialogFragment();
    Builder builder =
        new Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(
                getResources().getStringArray(names),
                selected,
                (d, which) -> {
                  dialogFragment.dismissIt();
                  if (which == selected) {
                    return;
                  }
                  String dbName = dbNames[which];
                  if (SETTINGS.isDbBuilt(dbName)) {
                    setDbNameAndRefreshUi(dbName);
                  } else {
                    String fontFile = null;
                    if (downloadFont) {
                      fontFile = SETTINGS.getTransFontFile(which);
                    }
                    if (fontFile != null && SETTINGS.getFontFile(fontFile).exists()) {
                      fontFile = null;
                    }
                    askToDownloadDb(dbName, fontFile);
                  }
                });
    AlertDialogFragment.show(this, dialogFragment, builder.create(), "TEXT_TRANS_SELECTOR");
  }

  private void askToDownloadDb(String dbName, String fontFile) {
    Runnable callback = () -> onDbFileDownloaded(dbName, fontFile);
    FileDownload fd =
        new FileDownload(this, "/databases/", dbName + ".zip", callback, R.string.downloading_file);
    fd.askToDownload();
  }

  private void onDbFileDownloaded(String dbName, String fontFile) {
    File file = SETTINGS.getDownloadedFile(dbName + ".xml");
    File tmpFile = new File(file.getAbsolutePath() + ".tmp");
    try (BufferedReader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        PrintWriter writer = new PrintWriter(tmpFile)) {
      String line;
      while ((line = rdr.readLine()) != null) {
        if (!line.contains("-------")) {
          writer.println(line);
        }
      }
      if (!tmpFile.renameTo(file)) {
        Log.e(TAG, "onDbFileDownloaded: Renaming " + tmpFile.getAbsolutePath() + " failed");
        return;
      }
    } catch (IOException e) {
      Log.e(TAG, "onDbFileDownloaded: " + e.toString());
      return;
    }

    Utils.runUi(
        this,
        () -> {
          AlertDialogFragment frag = showDbBuildDialog();
          Utils.runBg(
              () -> {
                boolean result = DbBuilder.buildDb(dbName);
                Utils.runUi(this, frag::dismissIt);
                if (result) {
                  setDbNameAndRefreshUi(dbName);
                  if (fontFile != null) {
                    downloadFonts(fontFile + ".zip", null, false);
                  }
                }
              });
        });
  }

  private void setDbNameAndRefreshUi(String dbName) {
    boolean refreshUi = false;
    if (MySettings.isQuranDb(dbName)) {
      SETTINGS.setQuranDbName(dbName);
      refreshUi = true;
    } else if (MySettings.isTranslationDb(dbName)) {
      SETTINGS.setTransDbName(dbName);
      refreshUi = true;
    } else if (dbName.equals(getString(R.string.db_search))) {
      Utils.runUi(this, () -> setSearchViewVisibility(true));
    }
    if (refreshUi) {
      refreshUi(true);
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// FONTS /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private static final String QURAN_FONTS_ZIP = "arabic.zip";

  private void downloadFonts(String zip, String fontName, boolean askToDownload) {
    Runnable callback =
        () -> {
          SETTINGS.resetTypeface();
          if (fontName != null) {
            SETTINGS.setFont(fontName);
          }
          refreshUi(true);
        };
    FileDownload fd = new FileDownload(this, "/fonts/", zip, callback, R.string.downloading_font);
    if (askToDownload) {
      fd.askToDownload();
    } else {
      fd.downloadFile();
    }
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// SEARCH /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private static final List<Integer> PLAIN_ARABIC_CHARS = new ArrayList<>();
  private static final List<Integer> VOWEL_ARABIC_CHARS = new ArrayList<>();

  static {
    boolean vowels = false;
    int[] searchChars = App.getRes().getIntArray(R.array.search_chars);
    for (int i = 1; i < searchChars.length; i++) {
      int c = searchChars[i];
      if (c < 0) {
        vowels = true;
        continue;
      }
      if (vowels) {
        VOWEL_ARABIC_CHARS.add(c);
      } else {
        PLAIN_ARABIC_CHARS.add(c);
      }
    }
    PLAIN_ARABIC_CHARS.add((int) ' ');
  }

  private void setUpSearchView() {
    // Clear search query on activity refresh
    collapseSearchView();

    mB.bottomBar.searchV.setQueryHint(getString(R.string.search_menu_item));

    mB.bottomBar.searchV.setOnQueryTextListener(
        new OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            return handleSearchQuery();
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            if (!SETTINGS.doSearchInTranslation() && !TextUtils.isEmpty(newText)) {
              int c = newText.charAt(newText.length() - 1);
              if (!PLAIN_ARABIC_CHARS.contains(c)
                  && (!SETTINGS.doSearchWithVowels() || !VOWEL_ARABIC_CHARS.contains(c))) {
                Runnable select = () -> mB.bottomBar.actionSearchHelp.setSelected(true);
                Runnable unselect = () -> mB.bottomBar.actionSearchHelp.setSelected(false);
                select.run();
                mB.bottomBar.actionSearchHelp.postDelayed(unselect, 200);
                mB.bottomBar.actionSearchHelp.postDelayed(select, 400);
                mB.bottomBar.actionSearchHelp.postDelayed(unselect, 600);
              }
            }
            return handleSearchQuery();
          }
        });

    mB.bottomBar.searchV.setOnQueryTextFocusChangeListener(
        (v, hasFocus) -> {
          if (!hasFocus && TextUtils.isEmpty(mB.bottomBar.searchV.getQuery())) {
            collapseSearchView();
          }
        });
  }

  private void setSearchViewVisibility(boolean visible) {
    String searchDb = getString(R.string.db_search);
    if (visible && !SETTINGS.isDbBuilt(searchDb)) {
      askToDownloadDb(searchDb, null);
      return;
    }

    if (visible) {
      mB.bottomBar.searchV.setVisibility(View.VISIBLE);
      mB.bottomBar.actionSearchHelp.setVisibility(View.VISIBLE);
      mB.bottomBar.actionSearchSettings.setVisibility(View.VISIBLE);
      mB.bottomBar.searchV.setIconified(false);
      mB.bottomBar.searchV.requestFocus();
      mB.bottomBar.container.setBackgroundResource(R.drawable.app_bar_bg);
    } else {
      mB.bottomBar.searchV.setVisibility(View.GONE);
      mB.bottomBar.actionSearchHelp.setVisibility(View.GONE);
      mB.bottomBar.actionSearchSettings.setVisibility(View.GONE);
      mB.bottomBar.container.setBackgroundColor(Utils.getAttrColor(this, R.attr.accentTrans1));
    }
    SETTINGS.setSearchStarted(visible);
    // If showing SearchView, hide the header, submit single page, and save
    // scroll positions to restore after search.
    refreshUi(visible);
    toggleArrowsVisibility(visible); // Hide arrows when in search.
  }

  private void collapseSearchView() {
    mB.bottomBar.searchV.onActionViewCollapsed();
    setSearchViewVisibility(false);
    mB.bottomBar.searchV.setQuery(null, false);
    mLastSearchQuery = null; // SearchView.setQuery(null, true) doesn't work
    setProgBarVisibility(false);
    mB.bottomBar.searchSettingsCont.setVisibility(View.GONE);
  }

  private String mLastSearchQuery;

  private boolean handleSearchQuery() {
    mB.bottomBar.searchSettingsCont.setVisibility(View.GONE);

    String query =
        mB.bottomBar.searchV.getQuery() != null ? mB.bottomBar.searchV.getQuery().toString() : null;
    if (query != null && query.length() < 2) {
      query = null;
    }
    SETTINGS.setSearchQuery(query);
    QuranPageFragment page = getPageFrag(null);
    if (!Objects.equals(mLastSearchQuery, query) && page != null) {
      mLastSearchQuery = query;
      page.handleSearchQuery(query);
    }
    return true;
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// HEADER ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void updateHeaderCosmetics() {
    if (!SETTINGS.getShowHeader() || (!mB.bottomBar.searchV.isIconified())) {
      mB.headerContainer.setVisibility(View.GONE);
    } else {
      mB.headerContainer.setVisibility(View.VISIBLE);

      Typeface typeface = SETTINGS.getTypeface();

      mB.juzV.setTypeface(typeface);
      mB.manzilV.setTypeface(typeface);
      mB.surahV.setTypeface(typeface);

      int colorRes = SETTINGS.getFontColor();
      if (colorRes > 0) {
        int color = getColor(colorRes);
        mB.juzV.setTextColor(color);
        mB.juzValueV.setTextColor(color);
        mB.manzilV.setTextColor(color);
        mB.manzilValueV.setTextColor(color);
        mB.pageV.setTextColor(color);
        mB.surahV.setTextColor(color);
        mB.surahValueV.setTextColor(color);
      }

      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();

      mB.juzV.setTextSize(sizeAr);
      mB.juzValueV.setTextSize(size);
      mB.manzilV.setTextSize(sizeAr);
      mB.manzilValueV.setTextSize(size);
      mB.pageV.setTextSize(size);
      mB.surahV.setTextSize(sizeAr);
      mB.surahValueV.setTextSize(size);
    }
  }

  private int mCurrentPage = -1, mCurrentAayah = -1;

  void updateHeader(AayahEntity entity, SurahEntity surah) {
    if (SETTINGS.isPageModeAndNotInSearch() && mB.pager.getCurrentItem() != entity.page - 1) {
      return;
    }

    // Header is not visible in search mode.
    // Also we don't want to save scroll positions for search results.
    if (SETTINGS.isSearchStarted()) {
      return;
    }

    mCurrentPage = entity.page;
    mCurrentAayah = entity.id;

    mB.juzValueV.setText(getArNum(entity.juz));
    mB.manzilValueV.setText(getArNum(entity.manzil));
    mB.pageV.setText(getArNum(entity.page));

    if (surah != null) {
      mB.surahV.setText(getString(R.string.surah_name, surah.name));
      mB.surahValueV.setText(getArNum(surah.surahNum));
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////// IMPLEMENTATIONS /////////////////////////
  //////////////////////////////////////////////////////////////////

  private class ScrollListener extends OnPageChangeCallback {

    @Override
    public void onPageScrollStateChanged(int state) {
      if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
        toggleFullScreen(true);
      }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
      if (!SETTINGS.isPageModeAndNotInSearch()) {
        return;
      }

      int page = position + 1;
      MainActivity.this.onPageSelected(page);

      if (!mIsFullScreen) {
        toggleArrowsVisibility(false);
      }

      Utils.runBg(
          () -> {
            AayahEntity entity = SETTINGS.getQuranDb().getAayahEntities(page).get(0);
            if (entity != null) {
              SurahEntity surah = SETTINGS.getMetaDb().getSurah(entity.surahNum);
              Utils.runUi(MainActivity.this, () -> updateHeader(entity, surah));
            } else {
              Log.e(TAG, "onPageSelected: failed to get AayahEntity");
            }
          });
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////// FOR SUBCLASSES ////////////////////////
  //////////////////////////////////////////////////////////////////

  public ActivityMainBinding getRootView() {
    return mB;
  }
}
