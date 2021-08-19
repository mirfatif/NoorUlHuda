package com.mirfatif.noorulhuda.svc;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static com.mirfatif.noorulhuda.prayer.PrayerData.getPrayerData;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.svc.PrayerNotifySvc.EXTRA_PRAYER;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.prayer.PrayerTimeActivity;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.File;
import java.io.IOException;

public class PrayerAdhanSvc extends Service
    implements OnPreparedListener, OnCompletionListener, OnAudioFocusChangeListener {

  private static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP";

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean stop = ACTION_STOP.equals(intent.getAction());
    if (stop) {
      // User has noted, do not show notification.
      if (mMediaPlayer != null) {
        mMediaPlayer.setOnCompletionListener(null);
      }
      cleanUp();
      stopSelf();
      return START_NOT_STICKY;
    }

    int prayer = intent.getIntExtra(EXTRA_PRAYER, -1);
    if (prayer < 0) {
      stopSelf(startId);
      return START_NOT_STICKY;
    }

    mPrayer = prayer;
    createNotification();

    if (!SETTINGS.getPrayerAdhan(prayer)) {
      showNotification(false);
      return START_NOT_STICKY;
    }

    File file = SETTINGS.getDownloadedFile(ADHAN_FILE);
    if (file.exists()) {
      try {
        playAdhan(file.getAbsolutePath());
        return START_NOT_STICKY;
      } catch (IOException | IllegalArgumentException e) {
        e.printStackTrace();
      }
    }
    // File does not exist or failed to read.
    showNotification(false);
    return START_NOT_STICKY;
  }

  private NotificationCompat.Builder mNotifBuilder;
  private int mPrayer;
  private MediaPlayer mMediaPlayer;

  public static final String ADHAN_FILE = "adhan.mp3";

  private static final int ADHAN_NOTIF_ID = Utils.getInteger(R.integer.channel_prayer_adhan);
  private static final String ADHAN_CHANNEL_ID = "channel_prayer_adhan";
  private static final String ADHAN_CHANNEL_NAME = Utils.getString(R.string.channel_prayer_adhan);

  private final int[] NAMES =
      new int[] {
        R.string.salat_fajr,
        R.string.salat_sunrise,
        R.string.salat_zuhr,
        R.string.salat_asr,
        R.string.salat_maghrib,
        R.string.salat_isha
      };

  private void createNotification() {
    NotificationManagerCompat nm = NotificationManagerCompat.from(App.getCxt());
    NotificationChannelCompat ch = nm.getNotificationChannelCompat(ADHAN_CHANNEL_ID);
    if (ch == null) {
      ch =
          new NotificationChannelCompat.Builder(ADHAN_CHANNEL_ID, IMPORTANCE_DEFAULT)
              .setName(ADHAN_CHANNEL_NAME)
              .setLightsEnabled(true)
              .setSound(null, null)
              .build();
      nm.createNotificationChannel(ch);
    }

    PendingIntent pi = createStopSvcIntent();

    mNotifBuilder =
        new Builder(App.getCxt(), ADHAN_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon)
            .setShowWhen(true)
            .setWhen(getPrayerData().times[mPrayer])
            .setContentTitle(getString(NAMES[mPrayer]))
            .setContentText(SETTINGS.getCityName())
            .setGroup(getString(R.string.prayer_notification_group))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pi)
            .addAction(0, getString(R.string.stop), pi);
  }

  private PendingIntent createStopSvcIntent() {
    return PendingIntent.getService(
        App.getCxt(),
        ADHAN_NOTIF_ID,
        new Intent(App.getCxt(), this.getClass()).setAction(ACTION_STOP),
        getCancelPiFlag());
  }

  private int getCancelPiFlag() {
    return PendingIntent.FLAG_CANCEL_CURRENT;
  }

  private AudioFocusRequest mFocusRequest;

  private void playAdhan(String file) throws IOException {
    if (mMediaPlayer != null) {
      cleanMp();
    }
    AudioAttributes attrs =
        new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build();
    mMediaPlayer = new MediaPlayer();
    mMediaPlayer.setAudioAttributes(attrs);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      mFocusRequest =
          new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(attrs)
              .build();
    }

    mMediaPlayer.setDataSource(file);
    mMediaPlayer.setWakeMode(App.getCxt(), PowerManager.PARTIAL_WAKE_LOCK);
    mMediaPlayer.setLooping(false);

    mMediaPlayer.setOnCompletionListener(this);
    mMediaPlayer.setOnPreparedListener(this);
    mMediaPlayer.prepareAsync();
  }

  private final AudioManager mAm =
      (AudioManager) App.getCxt().getSystemService(Context.AUDIO_SERVICE);

  @Override
  public void onPrepared(MediaPlayer mp) {
    if (mMediaPlayer != null) {
      int res;
      if (VERSION.SDK_INT >= VERSION_CODES.O) {
        res = mAm.requestAudioFocus(mFocusRequest);
      } else {
        res = mAm.requestAudioFocus(this, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
      }
      if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        mMediaPlayer.start();
        if (mNotifBuilder != null) {
          startForeground(ADHAN_NOTIF_ID, mNotifBuilder.build());
        }
        return;
      }
    }
    showNotification(false);
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    showNotification(true);
  }

  private static final int PRAYER_NOTIF_ID = Utils.getInteger(R.integer.channel_prayer_time);
  private static final String PRAYER_CHANNEL_ID = "channel_prayer_time";
  private static final String PRAYER_CHANNEL_NAME = Utils.getString(R.string.channel_prayer_time);

  private void showNotification(boolean afterAdhan) {
    if (SETTINGS.getPrayerNotify(mPrayer) && mNotifBuilder != null) {
      NotificationManagerCompat nm = NotificationManagerCompat.from(App.getCxt());
      NotificationChannelCompat ch = nm.getNotificationChannelCompat(PRAYER_CHANNEL_ID);
      if (ch == null) {
        ch =
            new NotificationChannelCompat.Builder(PRAYER_CHANNEL_ID, IMPORTANCE_DEFAULT)
                .setName(PRAYER_CHANNEL_NAME)
                .setLightsEnabled(true)
                .build();
        nm.createNotificationChannel(ch);
      }

      mNotifBuilder
          .setChannelId(PRAYER_CHANNEL_ID)
          .setCategory(null)
          .setContentIntent(PrayerTimeActivity.getPendingIntent(ADHAN_NOTIF_ID))
          .clearActions();

      if (!afterAdhan) {
        mNotifBuilder.setSilent(false);
        mNotifBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
      }
      nm.notify(PRAYER_NOTIF_ID, mNotifBuilder.build());
      cleanUp();
    }
    stopSelf();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    cleanUp();
  }

  private void cleanUp() {
    cleanMp();
    mNotifBuilder = null;
  }

  private void cleanMp() {
    if (mMediaPlayer != null) {
      try {
        mMediaPlayer.stop();
      } catch (IllegalStateException ignored) {
      }
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
    if (VERSION.SDK_INT < VERSION_CODES.O) {
      mAm.abandonAudioFocus(this);
    } else if (mFocusRequest != null) {
      mAm.abandonAudioFocusRequest(mFocusRequest);
    }
  }

  @Override
  public void onAudioFocusChange(int focusChange) {}
}
