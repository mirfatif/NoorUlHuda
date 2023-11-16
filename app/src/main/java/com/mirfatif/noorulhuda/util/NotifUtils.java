package com.mirfatif.noorulhuda.util;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.prefs.MySettings;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;

public class NotifUtils {

  public static final int PI_FLAGS = FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE;
  private static final String NOTIFICATION_PERM_DIALOG_TAG = "NOTIFICATION_PERM_DIALOG_TAG";

  private NotifUtils() {}

  public static boolean hasNotifPerm() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        || App.getCxt().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
  }

  public static void askForNotifPerm(FragmentActivity activity) {
    if (!hasNotifPerm()) {
      String perm = Manifest.permission.POST_NOTIFICATIONS;

      if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)) {
        Utils.showToast(R.string.notif_perm_missing_toast);
        activity.requestPermissions(new String[] {perm}, 0);
      } else {
        AlertDialog.Builder builder =
            new AlertDialog.Builder(activity)
                .setTitle(R.string.notif_perm_dialog_title)
                .setMessage(R.string.notif_perm_dialog_text)
                .setPositiveButton(
                    R.string.ok_button,
                    (d, w) -> activity.requestPermissions(new String[] {perm}, 0))
                .setNegativeButton(R.string.cancel_button, null);

        AlertDialogFragment.show(activity, builder.create(), NOTIFICATION_PERM_DIALOG_TAG);
      }
    }

    MySettings.SETTINGS.setAskForNotifPermTs();
  }

  @SuppressLint("MissingPermission")
  public static void notify(int id, Notification notif) {
    if (hasNotifPerm()) {
      NotificationManagerCompat.from(App.getCxt()).notify(id, notif);
    }
  }
}
