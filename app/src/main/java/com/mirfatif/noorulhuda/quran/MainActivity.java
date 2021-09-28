package com.mirfatif.noorulhuda.quran;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_JUZS;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_MANZILS;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_PAGES;
import static com.mirfatif.noorulhuda.db.DbBuilder.TOTAL_SURAHS;
import static com.mirfatif.noorulhuda.dua.DuaActivity.EXTRA_AAYAH_NUM;
import static com.mirfatif.noorulhuda.dua.DuaActivity.EXTRA_SURAH_NUM;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.quran.AayahAdapter.removeUnsupportedChars;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.isLandscape;
import static com.mirfatif.noorulhuda.util.Utils.reduceDragSensitivity;
import static com.mirfatif.noorulhuda.util.Utils.setNightTheme;
import static com.mirfatif.noorulhuda.util.Utils.setTooltip;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.content.res.ResourcesCompat;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;

public class MainActivity extends BaseActivity {

  private static final String TAG = "MainActivity";

  private ActivityMainBinding mB;
  private BackupRestore mBackupRestore;

  private QuranPageAdapter mQuranPageAdapter;

  @Override
  protected synchronized void onCreate(Bundle savedInstanceState) {
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

    Window window = getWindow();
    if (window != null) {
      LayoutParams params = window.getAttributes();
      params.screenBrightness = SETTINGS.getBrightness();
      window.setAttributes(params);
    }

    PrayerNotifySvc.reset(false);
    WidgetProvider.reset();
    Utils.runBg(() -> new AppUpdate().check(true));

    SETTINGS.getFontSizeChanged().observe(this, empty -> refreshUi());

    if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
      SETTINGS.plusAppLaunchCount();
    }

    mBackupRestore = new BackupRestore(this);
    mFeedbackTask = () -> new Feedback(this).askForFeedback();

    if (SETTINGS.isDbBuilt(DbBuilder.MAIN_DB)) {
      if (goToAayah(getIntent())) {
        refreshUi(RestorePosType.NONE);
      } else {
        refreshUi(RestorePosType.SAVED);
      }
    } else {
      buildDbAndRefreshUi();
    }
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
      if (SETTINGS.isSearchStarted()) {
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

  private void buildDbAndRefreshUi() {
    AlertDialogFragment dialog = showDbBuildDialog();
    Utils.showThirdPartyCredits(this, false);
    Utils.runBg(
        () -> {
          if (DbBuilder.buildDb(DbBuilder.MAIN_DB)) {
            refreshUi(RestorePosType.NONE);
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

  private enum RestorePosType {
    SAVED,
    CURRENT,
    NONE
  }

  private void refreshUi() {
    refreshUi(RestorePosType.CURRENT);
  }

  private void refreshUi(RestorePosType restorePosType) {
    if (!Utils.isMainThread()) {
      Utils.runUi(this, () -> refreshUi(restorePosType)).waitForMe();
      return;
    }

    updateHeaderCosmetics();
    updateSearchSettingViews();

    int page = -1;
    if (restorePosType == RestorePosType.SAVED) {
      saveScrollPos(SETTINGS.getLastPage(), SETTINGS.getLastAayah(), false);
    } else if (restorePosType == RestorePosType.CURRENT) {
      page = mCurrentPage;
      saveScrollPos(mCurrentPage, mCurrentAayah, false);
    }

    mQuranPageAdapter.refresh();

    if (SETTINGS.isSlideModeAndNotInSearch()) {
      if (restorePosType == RestorePosType.SAVED) {
        mB.pager.setCurrentItem(SETTINGS.getLastPage() - 1, false);
      } else if (restorePosType == RestorePosType.CURRENT && page >= 1) {
        mB.pager.setCurrentItem(page - 1, false);
      }
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
    b.searchHelpV.setText(Utils.htmlToString(R.string.search_help));
    b.searchHelpV.setMovementMethod(
        BetterLinkMovementMethod.newInstance()
            .setOnLinkClickListener((tv, url) -> Utils.openWebUrl(this, url)));
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
  ///////////////////////////// SLIDERS ////////////////////////////
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

    final int MAX = 100;
    int thumb = R.drawable.brightness_thumb_auto;
    int curVal = 0;
    if (wParams.screenBrightness != BRIGHTNESS_OVERRIDE_NONE) {
      thumb = R.drawable.brightness_thumb;
      curVal = (int) (wParams.screenBrightness * MAX);
    }

    SliderPopup.SliderCallback callback =
        (seekBar, newValue) -> {
          wParams.screenBrightness = (float) newValue / MAX;

          int resId = R.drawable.brightness_thumb;
          if (wParams.screenBrightness == 0) {
            resId = R.drawable.brightness_thumb_auto;
            wParams.screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
          }
          Drawable drawable = ResourcesCompat.getDrawable(App.getRes(), resId, getTheme());
          seekBar.setThumb(drawable);

          window.setAttributes(wParams);
          SETTINGS.saveBrightness(wParams.screenBrightness);
        };

    new SliderPopup(this, MAX, curVal, thumb, callback).show(mB.bottomBar.getRoot());
  }

  private void showBgColorSlider() {
    SliderPopup.SliderCallback callback =
        (seekBar, newValue) -> {
          SETTINGS.setBgColor(newValue);
          setBgColor();
        };

    new SliderPopup(
            this,
            MySettings.COLOR_COUNT,
            SETTINGS.getBgColorSliderVal(),
            R.drawable.colorize,
            callback)
        .show(mB.bottomBar.getRoot());
  }

  private void showFontColorSlider() {
    SliderPopup.SliderCallback callback =
        (seekBar, newValue) -> {
          SETTINGS.setFontColor(newValue);
          refreshUi();
        };

    new SliderPopup(
            this,
            MySettings.COLOR_COUNT,
            SETTINGS.getFontColorSliderVal(),
            R.drawable.contrast,
            callback)
        .show(mB.bottomBar.getRoot());
  }

  private void showFontSizeSlider() {
    SliderPopup.SliderCallback callback = (seekBar, newValue) -> SETTINGS.setFontSize(newValue);

    new SliderPopup(
            this,
            MySettings.FONT_SIZE_MAX - MySettings.FONT_SIZE_MIN,
            SETTINGS.getFontSizeSliderVal(),
            R.drawable.text_size,
            callback)
        .show(mB.bottomBar.getRoot());
  }

  private static class SliderPopup {

    private final SeekBar mSeekBar;
    private final PopupWindow mPopup;
    private final Runnable mHider;

    private SliderPopup(
        Activity activity,
        int maxVal,
        int curVal,
        @DrawableRes int thumb,
        SliderCallback sliderCallback) {
      mSeekBar = SliderBinding.inflate(activity.getLayoutInflater()).getRoot();
      mSeekBar.setMax(maxVal);
      mSeekBar.setProgress(curVal);

      Drawable drawable = ResourcesCompat.getDrawable(App.getRes(), thumb, activity.getTheme());
      if (drawable != null) {
        drawable.setTint(Utils.getColor(activity, R.attr.accent));
      }
      mSeekBar.setThumb(drawable);

      int width = App.getRes().getDisplayMetrics().widthPixels * (isLandscape() ? 5 : 9) / 10;
      mPopup = new PopupWindow(mSeekBar, width, LayoutParams.WRAP_CONTENT);
      mPopup.setElevation(500);
      mPopup.setOverlapAnchor(true);
      mPopup.setOutsideTouchable(true); // Dismiss on outside touch.

      mHider = mPopup::dismiss;
      mSeekBar.setOnSeekBarChangeListener(
          new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              sliderCallback.onValueChanged(seekBar, progress);
              seekBar.removeCallbacks(mHider);
              seekBar.postDelayed(mHider, 5000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
          });
    }

    private static final int GRAVITY = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    private static final int Y_POS = 2 * Utils.toPx(48);

    private void show(View view) {
      mPopup.showAtLocation(view, GRAVITY, 0, Y_POS);
      mSeekBar.postDelayed(mHider, 5000);
    }

    private interface SliderCallback {

      void onValueChanged(SeekBar seekBar, int newValue);
    }
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
    if (mB == null || (mB.bottomBar.searchV.hasFocus() && mSoftKbVisible)) {
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
                    if (mB.bottomBar.feedbackCont.getVisibility() != View.VISIBLE
                        && !SETTINGS.isSearchStarted()) {
                      toggleFullScreen(true);
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
    showLeft = showRight = !mIsFullScreen && SETTINGS.isSlideModeAndNotInSearch() && !hide;
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
          if (SETTINGS.isSearchStarted()) {
            mB.bottomBar.searchV.setQuery(null, true);
          }
        });

    mB.bottomBar.searchSettingsVowels.setOnClickListener(
        v -> {
          SETTINGS.toggleSearchWithVowels();
          updateSearchSettingViews();
          if (SETTINGS.isSearchStarted()) {
            mB.bottomBar.searchV.setQuery(null, true);
          }
        });

    // For text marquee to work.
    mB.bottomBar.searchSettingsTrans.setSelected(true);
    mB.bottomBar.searchSettingsVowels.setSelected(true);

    updateSearchSettingViews();

    /*
     Display#getSize() and Display#getRealSize() give total diff (status bar + nav bar).
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
      showBgColorSlider();
    } else if (itemId == R.id.action_text_color) {
      showFontColorSlider();
    } else if (itemId == R.id.action_font_size) {
      showFontSizeSlider();
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
      PopupMenu popupMenu = new PopupMenu(this, mB.bottomBar.actionPageView);
      popupMenu.inflate(R.menu.main_page_view);
      popupMenu.setOnMenuItemClickListener(item -> handlePageViewItemClick(item.getItemId()));
      Menu menu = popupMenu.getMenu();
      menu.findItem(R.id.action_page_view).setChecked(SETTINGS.isSlideMode());
      MenuItem aayahBreakItem = menu.findItem(R.id.action_aayah_breaks);
      if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
        aayahBreakItem.setChecked(true);
        aayahBreakItem.setEnabled(false);
      } else {
        aayahBreakItem.setChecked(SETTINGS.breakAayahs());
      }
      popupMenu.show();
    } else if (itemId == R.id.action_info_header) {
      SETTINGS.toggleShowHeader();
      refreshUi();
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
          refreshUi();
        }
      } else {
        downloadFonts(QURAN_FONTS_ZIP, getString(fontName), true);
      }
      return true;
    }
    return false;
  }

  private boolean handlePageViewItemClick(int itemId) {
    if (itemId == R.id.action_page_view) {
      SETTINGS.toggleSlideMode();
      refreshUi();
      if (!mIsFullScreen) {
        toggleArrowsVisibility(false);
      }
      return true;
    }
    if (itemId == R.id.action_aayah_breaks) {
      SETTINGS.toggleAayahBreaks();
      refreshUi();
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
      refreshUi();
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

    for (AayahEntity aayah : aayahs) {
      SurahEntity surah;
      if (aayah == null || (surah = SETTINGS.getMetaDb().getSurah(aayah.surahNum)) == null) {
        continue;
      }
      DialogListItem item = new DialogListItem();
      item.title = getString(R.string.surah_name, surah.name);
      item.subTitle = getArNum(aayah.aayahNum);
      item.text = removeUnsupportedChars(aayah.text);
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

  public void goTo(AayahEntity aayah) {
    if (!Utils.isMainThread()) {
      Utils.runUi(this, () -> goTo(aayah));
      return;
    }

    /*
     If we are returning from search, restoring position is handled in
     setSearchViewVisibility(). Let's save the required positions to move to.
    */
    if (SETTINGS.isSearchStarted()) {
      saveScrollPosForSearch(aayah.page, aayah.id, true);
      collapseSearchView();
      return;
    }

    if (SETTINGS.isSlideModeAndNotInSearch() && mB.pager.getCurrentItem() != aayah.page - 1) {
      saveScrollPos(aayah.page, aayah.id, true);
      mB.pager.setCurrentItem(aayah.page - 1, true);
      return;
    }

    /*
     No need to scroll the pager if:
     - In slide (page) mode and not in search, but current page is already the required page, or
     - Not breaking Aayahs and not in search and not showing translation below text, or
     - In search mode, or
     - In non-slide (continuous) mode
    */
    scrollRvToAayah(null, aayah.id);
  }

  private boolean goToAayah(Intent intent) {
    int surahNum, aayahNum;
    if (intent != null
        && (surahNum = intent.getIntExtra(EXTRA_SURAH_NUM, 0)) > 0
        && (aayahNum = intent.getIntExtra(EXTRA_AAYAH_NUM, 0)) > 0) {
      Utils.runBg(
          () -> {
            AayahEntity aayah = SETTINGS.getQuranDb().getAayahEntity(surahNum, aayahNum);
            // Wait for QuranPageAdapter to come up;
            for (int i = 0; i < 50; i++) {
              if (getPageFrag(null) != null) {
                goTo(aayah);
                break;
              }
              SystemClock.sleep(100);
            }
          });
      return true;
    }
    return false;
  }

  // Null page means current page
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

  // Null page means current page. If Aayah ID is null, get the saved one.
  private boolean scrollRvToAayah(@Nullable Integer page, @Nullable Integer aayahId) {
    QuranPageFragment pageFrag = getPageFrag(page);
    if (pageFrag == null) {
      return false;
    }
    boolean blink = true;
    if (aayahId == null) {
      ScrollPos scrollPos = getScrollPos(page);
      if (scrollPos != null) {
        aayahId = scrollPos.aayahId;
        blink = scrollPos.blink;
      } else {
        return false;
      }
    }

    pageFrag.scrollToAayah(aayahId, blink);
    return true;
  }

  private final AtomicInteger mLastPage = new AtomicInteger();

  /*
   Restoring RV scroll position is handled in QuranPageFragment
   if the page does not already exists in Pager.
  */
  private void onPageSelected(int page) {
    synchronized (mLastPage) {
      // If there's no scroll info saved, and the page is scrolled, go to the top of the page.
      if (!scrollRvToAayah(page, null) && page != mLastPage.getAndSet(page)) {
        QuranPageFragment pageFrag = getPageFrag(page);
        if (pageFrag != null) {
          pageFrag.scrollToRvItem(0, 0, false);
        }
      }
    }
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////// PAGE SCROLL POSITION /////////////////////
  //////////////////////////////////////////////////////////////////

  /*
   If we want to go to an Aayah which is not on current slide, we save the Aayah ID
   and scroll to the required page. After the page is selected, it checks for a saved
   Aayah ID to scroll to.
  */
  static class ScrollPos {

    int page, aayahId;
    boolean blink;

    ScrollPos(int page, int aayahId, boolean blink) {
      this.page = page;
      this.aayahId = aayahId;
      this.blink = blink;
    }
  }

  private final Object SCROLL_POS_LOCK = new Object();
  private ScrollPos mScrollPos;

  private void saveScrollPos(int page, int aayahId, boolean blink) {
    synchronized (SCROLL_POS_LOCK) {
      if (page >= 1 && aayahId >= 0) {
        mScrollPos = new ScrollPos(page, aayahId, blink);
      }
    }
  }

  private ScrollPos mScrollPosSearch;

  private void saveScrollPosForSearch(int page, int aayahId, boolean blink) {
    synchronized (SCROLL_POS_LOCK) {
      if (page >= 1 && aayahId >= 0) {
        mScrollPosSearch = new ScrollPos(page, aayahId, blink);
      }
    }
  }

  // Null page means we are not in slide-page mode. So page is always one.
  ScrollPos getScrollPos(@Nullable Integer page) {
    synchronized (SCROLL_POS_LOCK) {
      if (mScrollPos != null) {
        if (page == null || mScrollPos.page == page) {
          try {
            return mScrollPos;
          } finally {
            mScrollPos = null;
          }
        }
      }
      return null;
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
      refreshUi();
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
          refreshUi();
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
      mB.bottomBar.searchV.setIconified(false); // requestFocus() is unnecessary.
      mB.bottomBar.container.setBackgroundResource(R.drawable.app_bar_bg);
    } else {
      mB.bottomBar.actionSearchHelp.setVisibility(View.GONE);
      mB.bottomBar.actionSearchSettings.setVisibility(View.GONE);
      mB.bottomBar.searchSettingsCont.setVisibility(View.GONE);
      mB.bottomBar.searchV.clearFocus();
      mB.bottomBar.searchV.setVisibility(View.GONE);
      mB.bottomBar.container.setBackgroundColor(Utils.getAttrColor(this, R.attr.accentTrans1));
    }

    SETTINGS.setSearchStarted(visible);

    /*
     If showing SearchView, hide the header, submit single page, and save
     scroll positions to restore after search.
     Current position is restored after Search. But if GoTo is selected from long press menu,
     current search is overridden in goTo() by a new position.
    */
    ScrollPos scrollPos;
    synchronized (SCROLL_POS_LOCK) {
      scrollPos = mScrollPosSearch;
      mScrollPosSearch = null;
    }
    if (visible) {
      saveScrollPosForSearch(mCurrentPage, mCurrentAayah, false);
      refreshUi(RestorePosType.NONE);
    } else if (scrollPos != null) {
      saveScrollPos(scrollPos.page, scrollPos.aayahId, scrollPos.blink);
      refreshUi(RestorePosType.NONE);
      if (SETTINGS.isSlideModeAndNotInSearch() && scrollPos.page >= 1) {
        mB.pager.setCurrentItem(scrollPos.page - 1, false);
      }
    }

    toggleArrowsVisibility(visible); // Hide arrows when in search.
  }

  private boolean mSearchViewCollapsing = false;

  private void collapseSearchView() {
    if (mSearchViewCollapsing) {
      return;
    }
    mSearchViewCollapsing = true;

    /*
     Calling SearchView#onActionViewCollapsed() is problematic. When called with SearchView not
     focused (if cleared in onBackPressed() or automatically on keyboard closed),
     LayoutManager#scrollToPosition() and RecyclerView#scrollToPosition() don't work immediately.
     So we only rely on SearchView#clearFocus()
    */

    setSearchViewVisibility(false);
    setProgBarVisibility(false);

    // SearchView.setQuery(null, true) doesn't work
    mB.bottomBar.searchV.setQuery(null, false);
    mLastSearchQuery = null;

    mSearchViewCollapsing = false;
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
    if (!mSearchViewCollapsing) {
      QuranPageFragment page = getPageFrag(null);
      if (!Objects.equals(mLastSearchQuery, query) && page != null) {
        mLastSearchQuery = query;
        page.handleSearchQuery(query);
      }
    }
    return true;
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// HEADER ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void updateHeaderCosmetics() {
    if (!SETTINGS.getShowHeader() || SETTINGS.isSearchStarted()) {
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
    // If updateHeader() is called for previous page after we have slided to a new page.
    if (SETTINGS.isSlideModeAndNotInSearch() && mB.pager.getCurrentItem() != entity.page - 1) {
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
      if (!SETTINGS.isSlideModeAndNotInSearch()) {
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
