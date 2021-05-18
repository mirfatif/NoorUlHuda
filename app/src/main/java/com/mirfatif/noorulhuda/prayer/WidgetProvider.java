package com.mirfatif.noorulhuda.prayer;

import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static com.mirfatif.noorulhuda.prayer.PrayerData.getPrayerData;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.ColorRes;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.svc.PrayerNotifySvc;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

public class WidgetProvider extends AppWidgetProvider {

  public static final String TAG = "WidgetProvider";

  private AlarmManager mAlarmManager;
  private AppWidgetManager mWidgetManager;

  @Override
  public void onReceive(Context context, Intent intent) {
    mAlarmManager = (AlarmManager) App.getCxt().getSystemService(Context.ALARM_SERVICE);
    mWidgetManager = AppWidgetManager.getInstance(App.getCxt());

    if (ACTION_UPDATE.equals(intent.getAction())) {
      int[] ids =
          mWidgetManager.getAppWidgetIds(new ComponentName(App.getCxt(), WidgetProvider.class));
      onUpdate(context, mWidgetManager, ids);
    } else {
      super.onReceive(context, intent);
    }
  }

  private Future<?> mFuture;

  @Override
  public synchronized void onUpdate(Context context, AppWidgetManager wm, int[] ids) {
    if (mFuture != null) {
      mFuture.cancel(true);
    }

    PendingIntent pi = createIntent(true);
    if (pi != null) {
      mAlarmManager.cancel(pi);
      pi.cancel();
    }

    if (SETTINGS.getLngLat() == null || ids == null || ids.length == 0) {
      if (ids != null) {
        RemoteViews v = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.pr_time_widget_dummy);
        pi = PrayerTimeActivity.getPendingIntent(REQUEST_CODE_OPEN_APP);
        v.setOnClickPendingIntent(R.id.widget_root_v, pi);
        for (int id : ids) {
          wm.updateAppWidget(id, v);
        }
      }
      return;
    }

    mFuture = Utils.runBg(() -> create(ids));
  }

  @Override
  public void onAppWidgetOptionsChanged(Context cxt, AppWidgetManager wm, int id, Bundle newOpts) {
    onUpdate(cxt, wm, new int[] {id});
  }

  private final Object LOCK = new Object();
  private static final int DELAY = 1000;

  private void create(int[] ids) {
    synchronized (LOCK) {
      try {
        LOCK.wait(DELAY);
      } catch (InterruptedException ignored) {
        return;
      }
    }
    createView(ids);
    createAlarm();
  }

  private void createView(int[] ids) {
    RemoteViews view = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.pr_time_widget);

    PendingIntent pi = PrayerTimeActivity.getPendingIntent(REQUEST_CODE_OPEN_APP);
    view.setOnClickPendingIntent(R.id.widget_root_v, pi);

    boolean darkText;
    Integer bg = null;
    int style = SETTINGS.getHomeWidgetStyle();
    // In accordance with R.array.home_widget_styles
    if (style == 1) {
      bg = R.drawable.widget_bg_light;
      darkText = true;
    } else if (style == 2) {
      darkText = true;
    } else if (style == 3) {
      darkText = false;
    } else {
      bg = R.drawable.widget_bg_dark;
      darkText = false;
    }

    if (bg != null) {
      view.setInt(R.id.widget_root_v, "setBackgroundResource", bg);
    } else {
      view.setInt(R.id.widget_root_v, "setBackgroundColor", Color.TRANSPARENT);
    }

    int col1 = getColor(darkText ? R.color.widgetTextDark1 : R.color.widgetTextLight1);
    int col2 = getColor(darkText ? R.color.widgetTextDark2 : R.color.widgetTextLight2);
    int col3 = getColor(darkText ? R.color.widgetTextDark3 : R.color.widgetTextLight3);

    PrayerData d = getPrayerData();

    view.setChronometer(
        R.id.rem_time_v, SystemClock.elapsedRealtime() + d.untilNextPrayer() + DELAY, null, true);
    view.setChronometerCountDown(R.id.rem_time_v, true);
    view.setTextColor(R.id.rem_time_v, col2);

    for (int i = 0; i < 6; i++) {
      view.setTextViewText(PrayerNotifySvc.TIME_VIEWS[i], d.boldTimes[i]);
      view.setTextColor(PrayerNotifySvc.NAME_VIEWS[i], col3);
      view.setTextColor(PrayerNotifySvc.TIME_VIEWS[i], col3);
    }

    String text = String.format(Locale.ENGLISH, "%s %s %s", d.hijDate, d.hijMonth, d.hijYear);
    view.setTextViewText(R.id.hijri_date_v, text);
    view.setTextColor(R.id.hijri_date_v, col2);

    view.setTextViewText(R.id.city_name_v, SETTINGS.getCityName());
    view.setTextViewText(R.id.date_v, d.date);
    view.setTextViewText(R.id.day_v, d.day);
    view.setTextViewText(R.id.time_v, d.time);
    view.setTextColor(R.id.city_name_v, col1);
    view.setTextColor(R.id.date_v, col1);
    view.setTextColor(R.id.day_v, col1);
    view.setTextColor(R.id.time_v, col1);

    for (int id : ids) {
      Bundle opts = mWidgetManager.getAppWidgetOptions(id);
      float width, height;
      if (Utils.isLandscape()) {
        width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
      } else {
        width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
      }

      float reqRatio = 2.5f, ratio = width / height;
      if (ratio > reqRatio) {
        reqRatio = 4.15f;
        view.setViewVisibility(R.id.bottom_cont, View.GONE);
      } else {
        view.setViewVisibility(R.id.bottom_cont, View.VISIBLE);
      }
      if (ratio > reqRatio) {
        reqRatio = 6.25f;
        view.setViewVisibility(R.id.top_cont, View.GONE);
      } else {
        view.setViewVisibility(R.id.top_cont, View.VISIBLE);
      }
      if (ratio > reqRatio) {
        width = height * reqRatio;
      }

      int padS = Utils.toPx((int) (width / 90));
      int padL = Utils.toPx((int) (width / 45));

      view.setViewPadding(R.id.widget_root_v, padL, padL, padL, padL);
      view.setViewPadding(R.id.top_cont, 0, 0, 0, padL);
      view.setViewPadding(R.id.prayer2_cont, 0, 0, padS, 0);
      view.setViewPadding(R.id.prayer3_cont, 0, 0, padS, 0);
      view.setViewPadding(R.id.prayer4_cont, 0, 0, padS, 0);
      view.setViewPadding(R.id.prayer5_cont, 0, 0, padS, 0);
      view.setViewPadding(R.id.prayer6_cont, 0, 0, padS, 0);
      view.setViewPadding(R.id.city_name_v, padL * 2, padL, padL * 2, 0);
      view.setViewPadding(R.id.date_time_cont, 0, padL, 0, 0);

      float size = Math.min(width / 22, 18);
      for (int i : getIdList()) {
        view.setTextViewTextSize(i, COMPLEX_UNIT_SP, size);
      }

      mWidgetManager.updateAppWidget(id, view);
    }
  }

  private int getColor(@ColorRes int color) {
    return App.getCxt().getColor(color);
  }

  private final List<Integer> ALL_IDS = new ArrayList<>();

  private List<Integer> getIdList() {
    if (ALL_IDS.isEmpty()) {
      for (int i : PrayerNotifySvc.NAME_VIEWS) {
        ALL_IDS.add(i);
      }
      for (int i : PrayerNotifySvc.TIME_VIEWS) {
        ALL_IDS.add(i);
      }
      ALL_IDS.add(R.id.hijri_date_v);
      ALL_IDS.add(R.id.rem_time_v);
      ALL_IDS.add(R.id.city_name_v);
      ALL_IDS.add(R.id.date_v);
      ALL_IDS.add(R.id.day_v);
      ALL_IDS.add(R.id.time_v);
    }
    return ALL_IDS;
  }

  private void createAlarm() {
    PendingIntent pi = createIntent(false);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MINUTE, 1);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    if (!Thread.interrupted()) {
      mAlarmManager.setExact(AlarmManager.RTC, cal.getTimeInMillis(), pi);
    }
  }

  private static final String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".action.WIDGET_UPDATE";
  private static final int REQUEST_CODE_UPDATE = 10000;
  private static final int REQUEST_CODE_OPEN_APP = 10001;

  private PendingIntent createIntent(boolean cancel) {
    int flag = PendingIntent.FLAG_CANCEL_CURRENT;
    if (cancel) {
      flag = PendingIntent.FLAG_NO_CREATE;
    }
    Intent intent = new Intent(App.getCxt(), WidgetProvider.class).setAction(ACTION_UPDATE);
    return PendingIntent.getBroadcast(App.getCxt(), REQUEST_CODE_UPDATE, intent, flag);
  }

  public static void reset() {
    Intent intent = new Intent(App.getCxt(), WidgetProvider.class);
    App.getCxt().sendBroadcast(intent.setAction(ACTION_UPDATE));
  }
}
