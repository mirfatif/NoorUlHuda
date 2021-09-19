package com.mirfatif.noorulhuda.svc;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;
import static com.mirfatif.noorulhuda.BuildConfig.APPLICATION_ID;
import static com.mirfatif.noorulhuda.prayer.PrayerData.getPrayerData;
import static com.mirfatif.noorulhuda.prayer.WidgetProvider.getPiFlags;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationCompat.DecoratedCustomViewStyle;
import androidx.core.app.NotificationManagerCompat;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BootReceiver;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.prayer.PrayerData;
import com.mirfatif.noorulhuda.prayer.PrayerTimeActivity;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Future;

public class PrayerNotifySvc extends Service {

  public static final String TAG = "PrayerNotifySvc";

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private static final String ACTION_RESCHEDULE = APPLICATION_ID + ".action.RESCHEDULE";
  public static final String EXTRA_PRAYER = APPLICATION_ID + ".extra.PRAYER";
  private static final String EXTRA_POST_PRAYER_TIME = APPLICATION_ID + ".extra.POST_PRAYER_TIME";
  public static final String EXTRA_RUN_FG = APPLICATION_ID + ".extra.RUN_FG";

  private static final Object LOCK = new Object();
  private Future<?> mFuture;

  private AlarmManager mAlarmManager;

  @Override
  public synchronized int onStartCommand(Intent intent, int flags, int startId) {
    if (mFuture != null) {
      mFuture.cancel(true);
    }

    mAlarmManager = (AlarmManager) App.getCxt().getSystemService(Context.ALARM_SERVICE);
    synchronized (LOCK) {
      PendingIntent pi = createIntent(true, null, null);
      if (pi != null) {
        mAlarmManager.cancel(pi);
        pi.cancel();
      }
      mNotifBuilder = null;
    }

    boolean runFgSvc = SETTINGS.getShowWidgetNotif();
    boolean locSet = SETTINGS.getLngLat() != null;
    boolean showWidget = runFgSvc && locSet;
    boolean showNotif = intent != null && intent.getBooleanExtra(EXTRA_RUN_FG, false);

    // Show persistent widget or temporary notification.
    if (showWidget || showNotif) {
      showNotif(showWidget);
    }

    if (!locSet) {
      BootReceiver.setState(false);
      stopSelf();
      return Service.START_NOT_STICKY;
    }

    holdWakeLock();

    PostPrayerNotif ppn = null;
    if (intent != null) {
      long ppTime;
      int prayer = intent.getIntExtra(EXTRA_PRAYER, -1);
      if ((ppTime = intent.getLongExtra(EXTRA_POST_PRAYER_TIME, -1)) > 0) {
        ppn = new PostPrayerNotif(ppTime, prayer);
      } else if (prayer >= 0) {
        startService(new Intent(App.getCxt(), PrayerAdhanSvc.class).putExtra(EXTRA_PRAYER, prayer));
      }
    }
    PostPrayerNotif postPrayerNotif = ppn;

    try {
      mFuture = Utils.runBg(() -> create(runFgSvc, postPrayerNotif));
      if (runFgSvc) {
        return Service.START_STICKY;
      } else {
        return Service.START_NOT_STICKY;
      }
    } finally {
      releaseWakeLock();
    }
  }

  private Builder mNotifBuilder;

  private static final int WIDGET_NOTIF_ID = Utils.getInteger(R.integer.channel_prayer_widget);
  private static final String WIDGET_CHANNEL_ID = "channel_prayer_widget";
  private static final String WIDGET_CHANNEL_NAME = Utils.getString(R.string.channel_prayer_widget);

  private void showNotif(boolean showWidget) {
    NotificationManagerCompat nm = NotificationManagerCompat.from(App.getCxt());
    NotificationChannelCompat ch = nm.getNotificationChannelCompat(WIDGET_CHANNEL_ID);
    if (ch == null) {
      ch =
          new NotificationChannelCompat.Builder(WIDGET_CHANNEL_ID, IMPORTANCE_LOW)
              .setName(WIDGET_CHANNEL_NAME)
              .setLightsEnabled(false)
              .setVibrationEnabled(false)
              .build();
      nm.createNotificationChannel(ch);
    }

    mNotifBuilder =
        new Builder(App.getCxt(), WIDGET_CHANNEL_ID)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.notification_icon);

    if (showWidget) {
      mNotifBuilder
          .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For N and below
          .setContentIntent(PrayerTimeActivity.getPendingIntent(WIDGET_NOTIF_ID))
          .setAutoCancel(false)
          .setOngoing(true)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          .setStyle(new DecoratedCustomViewStyle())
          .setShowWhen(false)
          .setGroup(getString(R.string.prayer_notification_group));
    } else {
      mNotifBuilder
          .setPriority(NotificationCompat.PRIORITY_MIN)
          .setVisibility(NotificationCompat.VISIBILITY_SECRET)
          .setContentTitle(getString(R.string.setting_alarms));
    }

    startForeground(WIDGET_NOTIF_ID, mNotifBuilder.build());
    if (!showWidget) {
      stopForeground(true);
    }
  }

  private PrayerData mData;
  private static final int DELAY = 1000;

  private void create(boolean runFgSvc, PostPrayerNotif postPrayerNotif) {
    synchronized (LOCK) {
      try {
        LOCK.wait(DELAY);
      } catch (InterruptedException ignored) {
        return;
      }

      mData = getPrayerData();

      if (!runFgSvc && mData.nextAlarm == null) {
        // We don't have to show persistent notification and there's
        // no notification or adhan set for any prayer.
        BootReceiver.setState(false);
      } else {
        createAlarm(runFgSvc, postPrayerNotif);
        BootReceiver.setState(true);
      }

      if (runFgSvc) {
        mNotifBuilder
            .setCustomContentView(createNotifView())
            .setCustomBigContentView(createNotifBigView());
        NotificationManagerCompat.from(App.getCxt()).notify(WIDGET_NOTIF_ID, mNotifBuilder.build());
      } else {
        // We are not showing a persistent notification.
        stopSelf();
      }
    }
  }

  public static final int[] NAME_VIEWS =
      new int[] {
        R.id.prayer1_name,
        R.id.prayer2_name,
        R.id.prayer3_name,
        R.id.prayer4_name,
        R.id.prayer5_name,
        R.id.prayer6_name
      };

  public static final int[] TIME_VIEWS =
      new int[] {
        R.id.prayer1_time,
        R.id.prayer2_time,
        R.id.prayer3_time,
        R.id.prayer4_time,
        R.id.prayer5_time,
        R.id.prayer6_time
      };

  private static final int[] NAMES =
      new int[] {
        R.string.fajr,
        R.string.sunrise,
        R.string.zuhr,
        R.string.asr,
        R.string.maghrib,
        R.string.isha
      };

  private RemoteViews createNotifView() {
    RemoteViews view = new RemoteViews(APPLICATION_ID, R.layout.pr_time_notif_widget);
    view.setChronometer(
        R.id.rem_time_v,
        SystemClock.elapsedRealtime() + mData.untilNextPrayer() + DELAY,
        null,
        true);
    view.setChronometerCountDown(R.id.rem_time_v, true);

    int order = mData.curPrayer - 2;
    if (order < 0) {
      order += 6;
    }
    for (int i = 0; i < 6; i++) {
      if (order > 5) {
        order = 0;
      }
      view.setTextViewText(NAME_VIEWS[i], getString(NAMES[order]));
      view.setTextViewText(TIME_VIEWS[i], mData.compactTimes[order]);
      order++;
    }
    return view;
  }

  private RemoteViews createNotifBigView() {
    RemoteViews bigView = new RemoteViews(APPLICATION_ID, R.layout.pr_time_notif_widget_big);

    String text =
        String.format(Locale.ENGLISH, "%s %s %s", mData.hijDate, mData.hijMonth, mData.hijYear);
    bigView.setTextViewText(R.id.hijri_date_v, text);

    bigView.setTextViewText(R.id.day_v, mData.day);

    bigView.setChronometer(
        R.id.rem_time_v,
        SystemClock.elapsedRealtime() + mData.untilNextPrayer() + DELAY,
        null,
        true);
    bigView.setChronometerCountDown(R.id.rem_time_v, true);

    for (int i = 0; i < 6; i++) {
      bigView.setTextViewText(TIME_VIEWS[i], mData.boldTimes[i]);
    }

    bigView.setTextViewText(R.id.city_name_v, SETTINGS.getCityName());
    return bigView;
  }

  private void createAlarm(boolean runFgSvc, PostPrayerNotif postPrayerNotif) {
    Integer alarmPrayer = null;
    long triggerAt;
    Long ppnTriggerAt = null;

    if (postPrayerNotif != null) {
      // Last alarm was set to show post-prayer notification.
      alarmPrayer = postPrayerNotif.prayer;
      triggerAt = postPrayerNotif.triggerAt;
    } else {
      if (runFgSvc) {
        // Showing persistent notification. So UI must be updated on next prayer.
        triggerAt = mData.nextPrayerTime;

        if (mData.nextAlarm != null && mData.nextAlarm.prayer == mData.nextPrayer) {
          // We've to notify on next prayer.
          alarmPrayer = mData.nextPrayer;
        }
      } else {
        // Not showing persistent notification. Set alarm for the prayer when we've to notify.
        alarmPrayer = mData.nextAlarm.prayer;
        triggerAt = mData.nextAlarm.time;
      }

      if (alarmPrayer != null) {
        // There's a prayer number set to notify on.
        long triggerWithOffset = triggerAt + SETTINGS.getPrayerNotifyOffset(alarmPrayer) * 60000L;
        if (triggerWithOffset < triggerAt) {
          // We've to notify before the prayer time.
          if (triggerWithOffset < System.currentTimeMillis()) {
            // Pre-prayer notification has been shown but prayer time hasn't come yet.
            alarmPrayer = null;
          } else {
            // Pre-prayer notification has not been shown yet.
            triggerAt = triggerWithOffset;
          }
        } else if (triggerWithOffset > triggerAt) {
          // We've to notify after the prayer time. But once the prayer time comes, we'll get times
          // for the next prayer. So we've to retain post-prayer notification time before hand.
          ppnTriggerAt = triggerWithOffset;
        }
      }
    }

    if (triggerAt > mData.midnightTime) {
      // We must set UI at midnight. But the next alarm is going to come after midnight. So
      // we change it to come at midnight.
      triggerAt = mData.midnightTime;

      if (postPrayerNotif != null) {
        // This prayer time alarm was set to retain post-prayer notification time. We'll retain
        // it again.
        ppnTriggerAt = triggerAt;
      } else {
        // Cancel any prayer or post-prayer alarm. It'll be set again at midnight.
        alarmPrayer = null;
        ppnTriggerAt = null;
      }
    }

    if (!Thread.interrupted()) {
      PendingIntent pi = createIntent(false, alarmPrayer, ppnTriggerAt);
      mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
      Calendar c = Calendar.getInstance();
      c.setTimeInMillis(triggerAt);
      Log.i(
          TAG,
          "Alarm set at "
              + c.get(Calendar.DAY_OF_MONTH)
              + "-"
              + (c.get(Calendar.MONTH) + 1)
              + " "
              + c.get(Calendar.HOUR)
              + ":"
              + c.get(Calendar.MINUTE)
              + ":"
              + c.get(Calendar.SECOND)
              + ":"
              + c.get(Calendar.MILLISECOND));
    }
  }

  private PendingIntent createIntent(boolean cancel, Integer alarmPrayer, Long ppnTriggerAt) {
    // For cancellation Intent#filterEquals() must be true, so using a unique action.
    Intent intent = new Intent(App.getCxt(), this.getClass()).setAction(ACTION_RESCHEDULE);

    if (alarmPrayer != null) {
      int val = alarmPrayer;
      intent.putExtra(EXTRA_PRAYER, val);
    }
    if (ppnTriggerAt != null) {
      long val = ppnTriggerAt;
      intent.putExtra(EXTRA_POST_PRAYER_TIME, val);
    }
    return PendingIntent.getService(App.getCxt(), WIDGET_NOTIF_ID, intent, getPiFlags(cancel));
  }

  private final WakeLock mWakeLock;

  {
    PowerManager pm = (PowerManager) App.getCxt().getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
  }

  private void holdWakeLock() {
    synchronized (mWakeLock) {
      if (!mWakeLock.isHeld()) {
        mWakeLock.acquire(10000);
      }
    }
  }

  private void releaseWakeLock() {
    synchronized (mWakeLock) {
      if (mWakeLock.isHeld()) {
        mWakeLock.release();
      }
    }
  }

  private static class PostPrayerNotif {

    private final long triggerAt;
    private final int prayer;

    PostPrayerNotif(long triggerAt, int prayer) {
      this.triggerAt = triggerAt;
      this.prayer = prayer;
    }
  }

  public static void reset(boolean callFg) {
    Intent intent = new Intent(App.getCxt(), PrayerNotifySvc.class);
    if (callFg && VERSION.SDK_INT >= VERSION_CODES.O) {
      intent.putExtra(EXTRA_RUN_FG, true);
      App.getCxt().startForegroundService(intent);
    } else {
      App.getCxt().startService(intent);
    }
  }
}
