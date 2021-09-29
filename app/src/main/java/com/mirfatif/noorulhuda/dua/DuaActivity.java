package com.mirfatif.noorulhuda.dua;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.quran.MainActivity.FULL_SCREEN_FLAGS;
import static com.mirfatif.noorulhuda.quran.MainActivity.PRE_FULL_SCREEN_FLAGS;
import static com.mirfatif.noorulhuda.util.Utils.reduceDragSensitivity;
import static com.mirfatif.noorulhuda.util.Utils.setNightTheme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.databinding.ActivityDuaBinding;
import com.mirfatif.noorulhuda.db.DbBuilder;
import com.mirfatif.noorulhuda.dua.DuasAdapter.Dua;
import com.mirfatif.noorulhuda.quran.MainActivity;
import com.mirfatif.noorulhuda.ui.base.BaseActivity;

public class DuaActivity extends BaseActivity {

  private ActivityDuaBinding mB;
  private DuaPageAdapter mDuaPageAdapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (setNightTheme(this)) {
      return;
    }

    if (!SETTINGS.isDbBuilt(DbBuilder.MAIN_DB)) {
      startActivity(new Intent(App.getCxt(), MainActivity.class));
      finish();
      return;
    }

    mB = ActivityDuaBinding.inflate(getLayoutInflater());
    setContentView(mB.getRoot());

    ActionBar actionbar = getSupportActionBar();
    if (actionbar != null) {
      actionbar.hide();
    }

    mDuaPageAdapter = new DuaPageAdapter(this);
    mB.pager.setAdapter(mDuaPageAdapter);
    mB.pager.setCurrentItem(SETTINGS.getLastDuaPage());
    mB.pager.registerOnPageChangeCallback(
        new OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            SETTINGS.setDuaPageScrollPos(position);
          }
        });
    reduceDragSensitivity(mB.pager);
    TabConfigurationStrategy s = (tab, pos) -> tab.setText(DuaPageAdapter.TAB_LABELS[pos]);
    new TabLayoutMediator(mB.tabs, mB.pager, true, true, s).attach();

    int bgColorRes = SETTINGS.getBgColor();
    if (bgColorRes > 0) {
      mB.getRoot().setBackgroundColor(getColor(bgColorRes));
    }

    Window window = getWindow();
    if (window != null) {
      LayoutParams params = window.getAttributes();
      params.screenBrightness = SETTINGS.getBrightness();
      window.setAttributes(params);
    }

    SETTINGS.getFontSizeChanged().observe(this, empty -> resetFontSize());
  }

  private int mQuranicDuaPos = -1, mMasnoonDuaPos = -1, mOccDuaPos = -1;

  void setCurrentDua(int pos, int type) {
    if (type == DuaPageAdapter.DUA_TYPE_QURANIC) {
      mQuranicDuaPos = pos;
    } else if (type == DuaPageAdapter.DUA_TYPE_MASNOON) {
      mMasnoonDuaPos = pos;
    } else {
      mOccDuaPos = pos;
    }
  }

  @Override
  protected void onStop() {
    saveScrollPositions();
    super.onStop();
  }

  private void saveScrollPositions() {
    if (mQuranicDuaPos >= 0) {
      SETTINGS.setQuranicDuaScrollPosition(mQuranicDuaPos);
    }
    if (mMasnoonDuaPos >= 0) {
      SETTINGS.setMasnoonDuaScrollPosition(mMasnoonDuaPos);
    }
    if (mOccDuaPos >= 0) {
      SETTINGS.setOccasionsDuaScrollPosition(mOccDuaPos);
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      toggleFullScreen(true);
      Window window = getWindow();
      View view;
      if (window != null && (view = window.getDecorView()) != null) {
        view.setOnSystemUiVisibilityChangeListener(
            flags -> {
              if ((flags & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                view.removeCallbacks(FULL_SCREEN_SWITCHER);
                view.postDelayed(FULL_SCREEN_SWITCHER, 3000);
              }
            });
      }
    }
  }

  @Override
  public void onBackPressed() {
    DuaPageFragment page = getPageFrag();
    if (page != null && page.onBackPressed()) {
      return;
    }
    super.onBackPressed();
  }

  private DuaPageFragment getPageFrag() {
    int pos = mB.pager.getCurrentItem();
    if (mDuaPageAdapter != null) {
      String fragTag = "f" + mDuaPageAdapter.getItemId(pos);
      Fragment frag = getSupportFragmentManager().findFragmentByTag(fragTag);
      if (frag instanceof DuaPageFragment) {
        return (DuaPageFragment) frag;
      }
    }
    return null;
  }

  private final Runnable FULL_SCREEN_SWITCHER = () -> toggleFullScreen(true);

  synchronized void toggleFullScreen(Boolean hideControls) {
    Window window = getWindow();
    View view;
    if (window == null || (view = window.getDecorView()) == null) {
      return;
    }

    int flags;
    if (hideControls == null) {
      flags = view.getSystemUiVisibility();
      hideControls = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
    }

    flags = PRE_FULL_SCREEN_FLAGS;
    if (hideControls) {
      flags |= FULL_SCREEN_FLAGS;
    }

    view.setSystemUiVisibility(flags);
  }

  public static final String EXTRA_SURAH_NUM = BuildConfig.APPLICATION_ID + ".extra.SURAH_NUM";
  public static final String EXTRA_AAYAH_NUM = BuildConfig.APPLICATION_ID + ".extra.AAYAH_NUM";

  void goTo(Dua dua) {
    if (dua != null) {
      Intent intent = new Intent(App.getCxt(), MainActivity.class);
      intent.putExtra(EXTRA_SURAH_NUM, dua.surahNum);
      intent.putExtra(EXTRA_AAYAH_NUM, dua.aayahNum);
      intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      startActivity(intent);
    }
    finishAfterTransition();
  }

  private void resetFontSize() {
    DuaPageFragment frag = getPageFrag(null);
    if (frag == null) {
      return;
    }
    frag.resetFontSize();
    int pos = mB.pager.getCurrentItem();
    for (int page = pos + 2; page < Integer.MAX_VALUE; page++) {
      frag = getPageFrag(page);
      if (frag == null) {
        break;
      } else {
        frag.resetFontSize();
      }
    }
    for (int page = pos; page > -Integer.MAX_VALUE; page--) {
      frag = getPageFrag(page);
      if (frag == null) {
        break;
      } else {
        frag.resetFontSize();
      }
    }
  }

  // Null page means current page
  private DuaPageFragment getPageFrag(@Nullable Integer page) {
    if (page == null) {
      page = mB.pager.getCurrentItem() + 1;
    }
    if (mDuaPageAdapter != null) {
      String fragTag = "f" + mDuaPageAdapter.getItemId(page - 1);
      Fragment frag = getSupportFragmentManager().findFragmentByTag(fragTag);
      if (frag instanceof DuaPageFragment) {
        return (DuaPageFragment) frag;
      }
    }
    return null;
  }
}
