package com.mirfatif.noorulhuda.util;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.DialogProgressBinding;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownload {

  private static final String TAG = "FileDownload";

  private final FragmentActivity mA;
  private final String mUrl, mFile;
  private final Runnable mCallback;
  private final int mTitleResId;

  public FileDownload(
      FragmentActivity activity,
      String url,
      String file,
      Runnable callback,
      @StringRes int titleResId) {
    mA = activity;
    mUrl = url;
    mFile = file;
    mCallback = callback;
    mTitleResId = titleResId;
  }

  public void askToDownload() {
    Utils.runUi(
        () -> {
          Builder builder =
              new Builder(mA)
                  .setTitle(R.string.download)
                  .setMessage(R.string.download_file)
                  .setNegativeButton(android.R.string.cancel, null)
                  .setPositiveButton(android.R.string.ok, (d, which) -> downloadFile());
          new AlertDialogFragment(builder.create()).show(mA, "DOWNLOAD_FILE", false);
        });
  }

  public void downloadFile() {
    Utils.runUi(
        () -> {
          DialogProgressBinding b = DialogProgressBinding.inflate(mA.getLayoutInflater());
          Builder builder = new Builder(mA).setTitle(mTitleResId).setView(b.getRoot());
          AlertDialogFragment dialog = new AlertDialogFragment(builder.create());
          dialog.setCancelable(false);
          dialog.show(mA, "DOWNLOAD_FILE", false);

          Utils.runBg(
              () -> {
                Integer errResId = null;
                if (!Utils.isInternetReachable()) {
                  errResId = R.string.no_internet;
                } else if (!downloadFile(b.progressBar, b.progressBarDet)) {
                  errResId = R.string.download_failed;
                }
                Utils.runUi(dialog::dismissIt);
                if (errResId != null) {
                  Utils.showToast(errResId);
                  File file = SETTINGS.getDownloadedFile(mFile);
                  if (file.exists() && !file.delete()) {
                    Log.e(TAG, "Deleting " + file.getAbsolutePath() + " failed");
                  }
                } else if (mCallback != null) {
                  Utils.runUi(mCallback);
                }
              });
        });
  }

  private static final String DOWNLOAD_URL =
      "https://raw.githubusercontent.com/mirfatif/NoorUlHuda/master";

  private boolean downloadFile(ProgressBar pBar, ProgressBar pBarDet) {
    HttpURLConnection conn = null;
    InputStream is = null;
    OutputStream os = null;
    try {
      conn = (HttpURLConnection) new URL(DOWNLOAD_URL + mUrl + mFile).openConnection();
      conn.setConnectTimeout(30000);
      conn.setReadTimeout(30000);
      conn.setUseCaches(false);

      int status = conn.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        String msg = "Response code:" + conn.getResponseCode();
        msg += ", msg: " + conn.getResponseMessage();
        Log.e(TAG, msg);
        return false;
      }

      int fileSize = -1;
      for (String method : new String[] {"HEAD", "GET"}) {
        conn.setRequestMethod(method);
        fileSize = conn.getContentLength();
        if (fileSize != -1) {
          break;
        }
      }
      conn.setRequestMethod("GET");

      if (fileSize != -1) {
        final int finalSize = fileSize;
        Utils.runUi(
            () -> {
              pBar.setVisibility(View.GONE);
              pBarDet.setVisibility(View.VISIBLE);
              pBarDet.setMax(finalSize);
              pBarDet.setProgress(0);
            });
      }

      is = conn.getInputStream();
      os = new FileOutputStream(SETTINGS.getDownloadedFile(mFile));

      byte[] buf = new byte[8192];
      long count = 0;
      int len;
      while ((len = is.read(buf)) > 0) {
        os.write(buf, 0, len);
        count += len;
        if (fileSize != -1) {
          int progress = (int) count;
          Utils.runUi(() -> pBarDet.setProgress(progress));
        }
      }

      return fileSize == -1 || count == fileSize;
    } catch (IOException e) {
      Log.e(TAG, e.toString());
      return false;
    } finally {
      try {
        if (is != null) {
          is.close();
        }
        if (os != null) {
          os.close();
        }
      } catch (IOException ignored) {
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
}
