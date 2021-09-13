package com.mirfatif.noorulhuda.ui.base;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;

public class BaseActivity extends AppCompatActivity {

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
    if (theme.equals(Utils.getString(R.string.theme_color_green))) {
      setTheme(R.style.AppThemeGreen);
    } else if (theme.equals(Utils.getString(R.string.theme_color_blue))) {
      setTheme(R.style.AppThemeBlue);
    } else if (theme.equals(Utils.getString(R.string.theme_color_red))) {
      setTheme(R.style.AppThemeRed);
    } else if (theme.equals(Utils.getString(R.string.theme_color_gray))) {
      setTheme(R.style.AppThemeGray);
    }
  }

  public static void onCreateLayout(ViewGroup view) {
    LayoutTransition transition = new LayoutTransition();
    transition.enableTransitionType(LayoutTransition.CHANGING);
    view.setLayoutTransition(transition);
  }
}
