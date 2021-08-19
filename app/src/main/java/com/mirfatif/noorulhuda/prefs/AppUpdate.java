package com.mirfatif.noorulhuda.prefs;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getPiFlags;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

public class AppUpdate {

  private static final String TAG = "AppUpdate";

  private static final String CHECK_URL =
      "https://api.github.com/repos/mirfatif/NoorUlHuda/releases/latest";
  private static final String DOWNLOAD_URL =
      "https://github.com/mirfatif/NoorUlHuda/releases/latest";

  private static final String VERSION_TAG = "tag_name";

  public UpdateInfo check(boolean notify) {
    if (notify && !SETTINGS.shouldCheckForUpdates()) {
      return null;
    }

    HttpURLConnection connection = null;
    InputStream inputStream = null;
    try {
      connection = (HttpURLConnection) new URL(CHECK_URL).openConnection();
      connection.setConnectTimeout(60000);
      connection.setReadTimeout(60000);
      connection.setUseCaches(false);

      int status = connection.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        Log.e(
            TAG,
            "Response code:"
                + connection.getResponseCode()
                + ", msg: "
                + connection.getResponseMessage());
        return null;
      }

      inputStream = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }

      UpdateInfo info = new UpdateInfo();

      String ver = new JSONObject(builder.toString()).getString(VERSION_TAG);
      // Convert tag name to version code (v1.01-beta to 101)
      int version = Integer.parseInt(ver.substring(1, 5).replace(".", ""));
      if (version <= BuildConfig.VERSION_CODE) {
        Log.i(TAG, "App is up-to-date");
      } else {
        Log.i(TAG, "New update is available: " + ver);
        info.version = ver;
        if (!BuildConfig.GH_VERSION) {
          info.url = Utils.getString(R.string.play_store_url);
        } else {
          info.url = DOWNLOAD_URL;
        }
        if (notify) {
          showNotification(info);
          SETTINGS.setCheckForUpdatesTs(System.currentTimeMillis());
        }
      }
      return info;
    } catch (IOException | JSONException | NumberFormatException e) {
      Log.e(TAG, e.toString());
      return null;
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException ignored) {
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private void showNotification(UpdateInfo info) {
    final String CHANNEL_ID = "channel_app_update";
    final String CHANNEL_NAME = Utils.getString(R.string.channel_app_update);
    final int UNIQUE_ID = Utils.getInteger(R.integer.channel_app_update);

    Utils.createNotifChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManagerCompat.IMPORTANCE_HIGH);
    PendingIntent pi =
        PendingIntent.getActivity(
            App.getCxt(),
            UNIQUE_ID,
            new Intent(Intent.ACTION_VIEW, Uri.parse(info.url)),
            getPiFlags());

    Builder nb =
        new Builder(App.getCxt(), CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(Utils.getString(R.string.new_version_available))
            .setContentText(Utils.getString(R.string.tap_to_download) + " " + info.version)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

    NotificationManagerCompat.from(App.getCxt()).notify(UNIQUE_ID, nb.build());
  }

  public static class UpdateInfo {

    public String version, url;
  }
}
