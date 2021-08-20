package com.mirfatif.noorulhuda.ui;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.setNightTheme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.MovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.core.view.MenuCompat;
import androidx.lifecycle.Lifecycle.State;
import com.google.android.material.snackbar.Snackbar;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.ActivityAboutBinding;
import com.mirfatif.noorulhuda.databinding.TranslationDialogBinding;
import com.mirfatif.noorulhuda.prefs.AppUpdate;
import com.mirfatif.noorulhuda.prefs.AppUpdate.UpdateInfo;
import com.mirfatif.noorulhuda.svc.LogcatService;
import com.mirfatif.noorulhuda.ui.base.BaseActivity;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.bettermovementmethod.BetterLinkMovementMethod.OnLinkClickListener;

public class AboutActivity extends BaseActivity {

  private ActivityAboutBinding mB;
  private ActivityResultLauncher<String> mLoggingLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mB = ActivityAboutBinding.inflate(getLayoutInflater());
    setContentView(mB.getRoot());

    if (setNightTheme(this)) {
      return;
    }

    ActionBar actionbar = getSupportActionBar();
    if (actionbar != null) {
      actionbar.setTitle(R.string.about_menu_item);
    }

    mB.version.setText(BuildConfig.VERSION_NAME);
    mB.thirdPartyRes.setOnClickListener(v -> Utils.showThirdPartyCredits(this, true));
    openWebUrl(mB.telegram, R.string.telegram_link);
    openWebUrl(mB.sourceCode, R.string.source_url);
    openWebUrl(mB.issues, R.string.issues_url);
    openWebUrl(mB.rating, R.string.play_store_url);
    mB.contact.setOnClickListener(v -> Utils.sendMail(this, null));
    setLogTitle(SETTINGS.isLogging() ? R.string.stop_logging : R.string.collect_logs);
    mB.logging.setOnClickListener(v -> handleLogging());
    openWebUrl(mB.privacyPolicy, R.string.privacy_policy_link);
    mB.checkUpdate.setOnClickListener(v -> checkForUpdates());
    mB.translate.setOnClickListener(v -> showLocaleDialog());
    mB.shareApp.setOnClickListener(v -> sendShareIntent());

    // registerForActivityResult() must be called before onStart() is called
    ActivityResultCallback<Uri> callback = LogcatService::sendStartLogIntent;
    mLoggingLauncher = registerForActivityResult(new CreateDocument(), callback);
  }

  private void openWebUrl(View view, int linkResId) {
    view.setOnClickListener(v -> Utils.openWebUrl(this, getString(linkResId)));
  }

  private void setLogTitle(int resId) {
    mB.loggingTitle.setText(resId);
  }

  private void handleLogging() {
    if (SETTINGS.isLogging()) {
      LogcatService.sendStopLogIntent();
      setLogTitle(R.string.collect_logs);
      Snackbar.make(mB.logging, R.string.logging_stopped, 5000).show();
      return;
    }

    Utils.showToast(R.string.select_log_file);
    mLoggingLauncher.launch("NoorUlHuda_" + Utils.getCurrDateTime(false) + ".log");
  }

  private boolean mCheckForUpdateInProgress = false;

  private synchronized void checkForUpdates() {
    if (mCheckForUpdateInProgress) {
      return;
    }
    mCheckForUpdateInProgress = true;

    mB.checkUpdateSummary.setText(R.string.check_in_progress);
    Utils.runBg(this::checkForUpdatesInBg);
  }

  private void checkForUpdatesInBg() {
    AppUpdate appUpdate = new AppUpdate();
    UpdateInfo info = appUpdate.check(false);

    int messageResId;
    boolean showDialog = false;
    if (info == null) {
      messageResId = R.string.check_for_updates_failed;
    } else if (info.version == null) {
      messageResId = R.string.app_is_up_to_date;
    } else {
      messageResId = R.string.new_version_available;
      showDialog = true;
    }

    Utils.runUi(this, () -> mB.checkUpdateSummary.setText(R.string.update_summary));
    mCheckForUpdateInProgress = false;

    if (!showDialog || !getLifecycle().getCurrentState().isAtLeast(State.INITIALIZED)) {
      Utils.showToast(messageResId);
      return;
    }

    Builder builder =
        new Builder(this)
            .setTitle(R.string.update)
            .setMessage(Utils.getString(messageResId) + ": " + info.version)
            .setPositiveButton(
                R.string.download,
                (d, w) -> Utils.runUi(this, () -> Utils.openWebUrl(this, info.url)))
            .setNegativeButton(android.R.string.cancel, null);
    Utils.runUi(
        this, () -> new AlertDialogFragment(builder.create()).show(this, "APP_UPDATE", false));
  }

  private void showLocaleDialog() {
    String url = getString(R.string.translation_link);
    OnLinkClickListener listener = (tv, u) -> Utils.openWebUrl(this, url);
    MovementMethod method = BetterLinkMovementMethod.newInstance().setOnLinkClickListener(listener);

    TranslationDialogBinding b = TranslationDialogBinding.inflate(getLayoutInflater());
    b.langCreditsV.setText(Utils.htmlToString(R.string.language_credits));
    b.langCreditsV.setMovementMethod(method);
    Builder builder = new Builder(this).setTitle(R.string.translations).setView(b.getRoot());
    new AlertDialogFragment(builder.create()).show(this, "LOCALE", false);
  }

  private void sendShareIntent() {
    Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
    String text = getString(R.string.share_text, getString(R.string.play_store_url));
    startActivity(Intent.createChooser(intent.putExtra(Intent.EXTRA_TEXT, text), null));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.about, menu);
    MenuCompat.setGroupDividerEnabled(menu, true);
    menu.findItem(R.id.action_auto_update).setChecked(SETTINGS.getCheckForUpdates());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_auto_update) {
      SETTINGS.setCheckForUpdates(!item.isChecked());
      item.setChecked(!item.isChecked());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
