package com.mirfatif.noorulhuda;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import androidx.annotation.NonNull;
import com.mirfatif.noorulhuda.prayer.WidgetProvider;
import com.mirfatif.noorulhuda.svc.LogcatService;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.time4j.android.ApplicationStarter;

public class App extends Application {

  private static final String TAG = "App";

  private static Context mAppContext;
  private Thread.UncaughtExceptionHandler defaultExceptionHandler;

  @Override
  public void onCreate() {
    super.onCreate();
    ApplicationStarter.initialize(this, true);

    mAppContext = getApplicationContext();
    defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> {
          Log.e(TAG, e.toString());
          LogcatService.appCrashed();

          StringWriter stringWriter = new StringWriter();
          PrintWriter writer = new PrintWriter(stringWriter, true);
          e.printStackTrace(writer);
          writer.close();
          Utils.writeCrashLog(stringWriter.toString());

          defaultExceptionHandler.uncaughtException(t, e);
        });

    SETTINGS.rebuildDb();
  }

  public static Context getCxt() {
    return mAppContext;
  }

  public static Resources getRes() {
    return mAppContext.getResources();
  }

  private int mOrientation = -1;

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    if (newConfig.orientation != mOrientation) {
      mOrientation = newConfig.orientation;
      WidgetProvider.reset();
    }
  }
}
