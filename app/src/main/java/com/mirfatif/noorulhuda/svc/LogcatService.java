package com.mirfatif.noorulhuda.svc;

import static com.mirfatif.noorulhuda.BuildConfig.APPLICATION_ID;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getPiFlags;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class LogcatService extends Service {

  private static final String TAG = "LogcatService";

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private final Object ON_START_CMD_LOCK = new Object();

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Utils.runBg(() -> onStartCommand(intent));
    // Do not restart the service if the process is killed
    return Service.START_NOT_STICKY;
  }

  private void onStartCommand(Intent intent) {
    synchronized (ON_START_CMD_LOCK) {
      String act = intent.getAction();
      Uri logFile;
      if (act != null && act.equals(ACTION_START_LOG) && (logFile = intent.getData()) != null) {
        doLogging(logFile);
        startSvc();
      } else {
        stopLoggingAndSvc();
      }
    }
  }

  private CountDownTimer mTimer;
  private NotificationManagerCompat mNotifMgr;
  private NotificationCompat.Builder mNotifBuilder;

  private void startSvc() {
    final String CHANNEL_ID = "channel_logcat_collection";
    final String CHANNEL_NAME = Utils.getString(R.string.channel_logcat_collection);

    mNotifMgr = NotificationManagerCompat.from(App.getCxt());
    Utils.createNotifChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManagerCompat.IMPORTANCE_HIGH);

    PendingIntent pi =
        PendingIntent.getService(
            App.getCxt(), UNIQUE_ID, new Intent(App.getCxt(), LogcatService.class), getPiFlags());

    mNotifBuilder =
        new Builder(App.getCxt(), CHANNEL_ID)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(Utils.getString(R.string.logging))
            .setColor(Utils.getAccentColor())
            .setStyle(new BigTextStyle().bigText(getString(R.string.logging_msg)))
            .addAction(0, getString(R.string.stop_logging), pi);

    startForeground(UNIQUE_ID, mNotifBuilder.build());
  }

  private final int UNIQUE_ID = Utils.getInteger(R.integer.channel_logcat_collection);

  private static final int TIMEOUT_SEC = 5 * 60;

  private void setNotificationProgress(int now) {
    if (mNotifMgr == null || mNotifBuilder == null) {
      return;
    }
    int min = now / 60;
    String text = String.format(Locale.getDefault(), "%02d:%02d", min, now - min * 60);
    mNotifBuilder.setProgress(TIMEOUT_SEC, now, false);
    mNotifBuilder.setContentText(text);
    mNotifMgr.notify(UNIQUE_ID, mNotifBuilder.build());
  }

  private void stopSvc() {
    stopSelf();
    if (mTimer != null) {
      mTimer.cancel();
      mTimer = null;
    }
  }

  private void stopSvcAndShowFailed() {
    stopSvc();
    Utils.showToast(R.string.logging_failed);
  }

  private void stopLoggingAndSvc() {
    stopSvc();
    stopLogging();
  }

  private void stopLogging() {
    synchronized (LOG_WRITER_LOCK) {
      if (!SETTINGS.isLogging()) {
        return;
      }
      SETTINGS.setLogging(false);
      if (mLogcatWriter != null) {
        mLogcatWriter.close();
      }
      mLogcatWriter = null;
      Log.i(TAG, STOP_LOGGING);
    }
  }

  private void startTimer() {
    mTimer =
        new CountDownTimer(TIMEOUT_SEC * 1000, 1000) {
          @Override
          public void onTick(long millisUntilFinished) {
            Utils.runBg(() -> setNotificationProgress((int) (millisUntilFinished / 1000)));
          }

          @Override
          public void onFinish() {
            stopLoggingAndSvc();
          }
        };
    mTimer.start();
  }

  private void doLogging(Uri logFile) {
    try {
      OutputStream os = App.getCxt().getContentResolver().openOutputStream(logFile, "rw");
      mLogcatWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)), false);
    } catch (IOException e) {
      e.printStackTrace();
      stopSvcAndShowFailed();
      return;
    }

    SETTINGS.setLogging(true);
    writeToLogFile(Utils.getDeviceInfo());

    Utils.runCommand(TAG + ": doLogging", "logcat", "-c");
    Log.d(TAG, "doLogging: Starting");

    if (!doLogging("sh", "exec logcat --pid " + android.os.Process.myPid())) {
      stopSvcAndShowFailed();
      stopLogging();
      return;
    }

    Utils.runUi(this::startTimer);
  }

  public boolean doLogging(String cmd1, String cmd2) {
    Process proc = Utils.runCommand(TAG + ": doLogging", true, cmd1);
    if (proc == null) {
      return false;
    }

    Utils.runBg(() -> readLogcatStream(proc));
    Log.i(TAG, "doLogging: sending command to shell: " + cmd2);
    new PrintWriter(proc.getOutputStream(), true).println(cmd2);

    return true;
  }

  // We don't have a sensible way to interrupt blocking read()
  private static final String STOP_LOGGING = APPLICATION_ID + ".STOP_LOGGING";

  private void readLogcatStream(Process process) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

    try {
      String line;
      while ((line = reader.readLine()) != null && !line.contains(STOP_LOGGING)) {
        if (Thread.interrupted()) {
          break;
        }
        writeToLogFile(line);
      }
    } catch (IOException e) {
      Log.e(TAG, "readLogcatStream: " + e.toString());
    } finally {
      // If process exited itself
      stopLoggingAndSvc();
      Utils.cleanStreams(process, TAG + ": readLogcatStream");
    }
  }

  private static PrintWriter mLogcatWriter;
  private static final Object LOG_WRITER_LOCK = new Object();
  private int mLinesWritten;

  private void writeToLogFile(String line) {
    synchronized (LOG_WRITER_LOCK) {
      if (mLogcatWriter == null || !SETTINGS.isLogging()) {
        return;
      }

      mLogcatWriter.println(line);

      // Let's be flash friendly
      mLinesWritten++;
      if (mLinesWritten >= 100) {
        mLogcatWriter.flush();
        mLinesWritten = 0;
      }
    }
  }

  private static final String ACTION_START_LOG = APPLICATION_ID + ".action.START_LOGCAT";

  public static void sendStartLogIntent(Uri logFile) {
    if (logFile != null) {
      Intent intent = new Intent(ACTION_START_LOG, logFile, App.getCxt(), LogcatService.class);
      App.getCxt().startService(intent);
    }
  }

  // Intent without start action would stop the service and logging.
  public static void sendStopLogIntent() {
    App.getCxt().startService(new Intent(App.getCxt(), LogcatService.class));
  }

  public static void appCrashed() {
    synchronized (LOG_WRITER_LOCK) {
      if (mLogcatWriter != null) {
        SystemClock.sleep(1000);
        mLogcatWriter.flush();
      }
    }
  }
}
