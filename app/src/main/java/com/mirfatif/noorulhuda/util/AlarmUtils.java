package com.mirfatif.noorulhuda.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import java.util.Calendar;

public class AlarmUtils {

  private AlarmUtils() {}

  private static final String EXACT_ALARM_PERM_DIALOG_TAG = "EXACT_ALARM_PERM_DIALOG_TAG";

  public static boolean canScheduleExactAlarms() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        || App.getCxt().getSystemService(AlarmManager.class).canScheduleExactAlarms();
  }

  @SuppressLint("ScheduleExactAlarm")
  public static void setExact(int type, long triggerAt, PendingIntent pi, @Nullable String tag) {
    if (canScheduleExactAlarms()) {
      App.getCxt().getSystemService(AlarmManager.class).setExact(type, triggerAt, pi);
      logAlarm(tag, triggerAt);
    }
  }

  @SuppressLint("ScheduleExactAlarm")
  public static void setAlarmClock(long triggerAt, PendingIntent pi, @Nullable String tag) {
    if (canScheduleExactAlarms()) {
      App.getCxt()
          .getSystemService(AlarmManager.class)
          .setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, null), pi);
      logAlarm(tag, triggerAt);
    }
  }

  private static void logAlarm(@Nullable String tag, long triggerAt) {
    if (tag == null) {
      return;
    }
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(triggerAt);
    Log.i(
        tag,
        "Alarm set at "
            + c.get(Calendar.DAY_OF_MONTH)
            + "-"
            + (c.get(Calendar.MONTH) + 1)
            + " "
            + c.get(Calendar.HOUR_OF_DAY)
            + ":"
            + c.get(Calendar.MINUTE)
            + ":"
            + c.get(Calendar.SECOND)
            + ":"
            + c.get(Calendar.MILLISECOND));
  }

  public static void cancel(PendingIntent pi) {
    App.getCxt().getSystemService(AlarmManager.class).cancel(pi);
  }

  public static void askForExactAlarmPerm(FragmentActivity activity, int requestCode) {
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
                    activity.startActivityForResult(
                        new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM), requestCode))
            .setNegativeButton(R.string.cancel_button, null);

    AlertDialogFragment.show(activity, builder.create(), EXACT_ALARM_PERM_DIALOG_TAG);
  }
}
