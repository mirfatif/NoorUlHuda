package com.mirfatif.noorulhuda.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;

public class AlarmUtils {

  private AlarmUtils() {}

  private static final String EXACT_ALARM_PERM_DIALOG_TAG = "EXACT_ALARM_PERM_DIALOG_TAG";

  public static boolean canScheduleExactAlarms() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        || App.getCxt().getSystemService(AlarmManager.class).canScheduleExactAlarms();
  }

  @SuppressLint("ScheduleExactAlarm")
  public static void setExact(int type, long triggerAt, PendingIntent pi) {
    if (canScheduleExactAlarms()) {
      App.getCxt().getSystemService(AlarmManager.class).setExact(type, triggerAt, pi);
    }
  }

  @SuppressLint("ScheduleExactAlarm")
  public static void setExactAndAllowWhileIdle(int type, long triggerAt, PendingIntent pi) {
    if (canScheduleExactAlarms()) {
      App.getCxt()
          .getSystemService(AlarmManager.class)
          .setExactAndAllowWhileIdle(type, triggerAt, pi);
    }
  }

  public static void cancel(PendingIntent pi) {
    App.getCxt().getSystemService(AlarmManager.class).cancel(pi);
  }

  public static void askForExactAlarmPerm(FragmentActivity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()) {
      return;
    }

    AlertDialog.Builder builder =
        new AlertDialog.Builder(activity)
            .setTitle(R.string.alarm_perm_dialog_title)
            .setMessage(R.string.alarm_perm_dialog_text)
            .setPositiveButton(
                R.string.ok_button,
                (d, w) ->
                    activity.startActivity(
                        new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)))
            .setNegativeButton(R.string.cancel_button, null);

    AlertDialogFragment.show(activity, builder.create(), EXACT_ALARM_PERM_DIALOG_TAG);
  }
}
