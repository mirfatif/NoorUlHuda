package com.mirfatif.noorulhuda.ui.base;

import android.animation.LayoutTransition;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewbinding.ViewBinding;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.StatusBarBgContBinding;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;

public class BaseActivity extends AppCompatActivity {

  private final boolean hasActionBar;

  protected BaseActivity(boolean hasActionBar) {
    this.hasActionBar = hasActionBar;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    onCreateStart();
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Do not recreate parent (Main) activity
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /*
   We use the same AlertDialogFragment to show all AlertDialogs. This method
   is called to recreate the AlertDialog after configuration change.
  */
  public AlertDialog createDialog(String tag, AlertDialogFragment dialogFragment) {
    return null;
  }

  private void onCreateStart() {
    String theme =
        Utils.getDefPrefs()
            .getString(
                Utils.getString(R.string.pref_main_theme_color_key),
                Utils.getString(R.string.pref_main_theme_color_default));
    final int themeResId;
    if (theme.equals(Utils.getString(R.string.theme_color_green))) {
      setTheme(R.style.AppThemeGreen);
    } else if (theme.equals(Utils.getString(R.string.theme_color_blue))) {
      setTheme(R.style.AppThemeBlue);
    } else if (theme.equals(Utils.getString(R.string.theme_color_red))) {
      setTheme(R.style.AppThemeRed);
    } else if (theme.equals(Utils.getString(R.string.theme_color_gray))) {
      setTheme(R.style.AppThemeGray);
    } else {
      return;
    }
    if (!hasActionBar) {
      getTheme().applyStyle(androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar, true);
    }
  }

  public static void onCreateLayout(ViewGroup view) {
    LayoutTransition transition = new LayoutTransition();
    transition.enableTransitionType(LayoutTransition.CHANGING);
    view.setLayoutTransition(transition);
  }

  public void setContentView(ViewBinding binding) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      setContentView(binding.getRoot());
      return;
    }

    EdgeToEdge.enable(this);

    var window = getWindow();
    var isNightMode = Utils.isNightMode(this);
    var controller = WindowCompat.getInsetsController(window, window.getDecorView());
    controller.setAppearanceLightStatusBars(!isNightMode);
    controller.setAppearanceLightNavigationBars(!isNightMode);
    window.setNavigationBarContrastEnforced(false); // Remove bg 80% opaque scrim

    final var b = StatusBarBgContBinding.inflate(getLayoutInflater());

    setContentView(b.getRoot());
    b.cont.addView(
        binding.getRoot(),
        0,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    ViewCompat.setOnApplyWindowInsetsListener(
        b.getRoot(),
        (v, insets) -> {
          var actionBar = getSupportActionBar();
          if (actionBar != null) {
            actionBar.setBackgroundDrawable(null);
          }

          final View statusBarBg;
          int statusBarHeight;

          if (hasActionBar) {
            statusBarBg = b.statusBarBg;
            var type =
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            statusBarHeight = insets.getInsets(type).top;
          } else {
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            var cutoutHeight = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top;
            if (cutoutHeight > 0) {
              if (cutoutHeight > statusBarHeight) {
                statusBarHeight = cutoutHeight;
              }
              statusBarBg = b.statusBarBg;
              b.statusBarBgOverlapping.setVisibility(View.GONE);
            } else {
              statusBarBg = b.statusBarBgOverlapping;
              b.statusBarBg.setVisibility(View.GONE);
            }
          }
          var lp = statusBarBg.getLayoutParams();
          lp.height = statusBarHeight;
          statusBarBg.setLayoutParams(lp);

          lp = b.navBarBg.getLayoutParams();
          lp.height =
              insets.getInsets(
                      WindowInsetsCompat.Type.navigationBars()
                          | WindowInsetsCompat.Type.displayCutout())
                  .bottom;
          b.navBarBg.setLayoutParams(lp);

          // setOnApplyWindowInsetsListener() won't work if we
          // return WindowInsetsCompat.CONSUMED here.
          return insets.inset(0, 0, 0, lp.height);
        });
  }
}
