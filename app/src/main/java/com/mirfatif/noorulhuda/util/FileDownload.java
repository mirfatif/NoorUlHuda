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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileDownload {

  private static final String TAG = "FileDownload";

  private final FragmentActivity mA;
  private final String mUrl, mFile;
  private final File mFilePath;
  private final Runnable mCallback;
  private final int mTitleResId, mMsgResId;

  public FileDownload(
      FragmentActivity activity,
      String url,
      String file,
      Runnable callback,
      @StringRes int msgResId,
      @StringRes int titleResId) {
    mA = activity;
    mUrl = url;
    mFile = file;
    mFilePath = SETTINGS.getDownloadedFile(mFile);
    mCallback = callback;
    mMsgResId = msgResId;
    mTitleResId = titleResId;
  }

  public void askToDownload() {
    Utils.runUi(
        mA,
        () -> {
          Builder builder =
              new Builder(mA)
                  .setTitle(R.string.download)
                  .setMessage(mMsgResId)
                  .setNegativeButton(android.R.string.cancel, null)
                  .setPositiveButton(android.R.string.ok, (d, which) -> downloadFile());
          AlertDialogFragment.show(mA, builder.create(), "DOWNLOAD_FILE");
        });
  }

  public void downloadFile() {
    Utils.runUi(
        mA,
        () -> {
          DialogProgressBinding b = DialogProgressBinding.inflate(mA.getLayoutInflater());
          Builder builder = new Builder(mA).setTitle(mTitleResId).setView(b.getRoot());
          AlertDialogFragment dialog =
              AlertDialogFragment.show(mA, builder.create(), "DOWNLOAD_FILE");
          dialog.setCancelable(false);

          Utils.runBg(
              () -> {
                Integer errResId = null;
                if (!Utils.isInternetReachable()) {
                  errResId = R.string.no_internet;
                } else if (!downloadFile(b.progressBar, b.progressBarDet)) {
                  errResId = R.string.download_failed;
                }
                Utils.runUi(mA, dialog::dismissIt);
                if (errResId != null) {
                  Utils.showToast(errResId);
                  if (mFilePath.exists() && !mFilePath.delete()) {
                    Log.e(TAG, "Deleting " + mFilePath.getAbsolutePath() + " failed");
                  }
                } else if (mFile.endsWith(".zip") && !extractZip()) {
                  Utils.showToast(R.string.extraction_failed);
                } else if (mCallback != null) {
                  mCallback.run();
                }
              });
        });
  }

  private static final String DOWNLOAD_URL =
      "https://raw.githubusercontent.com/mirfatif/NoorUlHuda/data";

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
            mA,
            () -> {
              pBar.setVisibility(View.GONE);
              pBarDet.setVisibility(View.VISIBLE);
              pBarDet.setMax(finalSize);
              pBarDet.setProgress(0);
            });
      }

      is = conn.getInputStream();
      os = new FileOutputStream(mFilePath);

      byte[] buf = new byte[8192];
      long count = 0;
      int len;
      while ((len = is.read(buf)) > 0) {
        os.write(buf, 0, len);
        count += len;
        if (fileSize != -1) {
          int progress = (int) count;
          Utils.runUi(mA, () -> pBarDet.setProgress(progress));
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

  private boolean extractZip() {
    File file = null;
    ZipInputStream zis = null;
    FileOutputStream os = null;
    try {
      zis = new ZipInputStream(new FileInputStream(mFilePath));
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        file = SETTINGS.getDownloadedFile(entry.getName());
        os = new FileOutputStream(file);
        byte[] buf = new byte[(int) entry.getSize()];
        int len;
        while ((len = zis.read(buf)) > 0) {
          os.write(buf, 0, len);
        }
        os.close();
        zis.closeEntry();
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      if (os != null) {
        try {
          os.close();
        } catch (IOException ignored) {
        }
      }
      if (file != null && !file.delete()) {
        Log.e(TAG, "Deleting " + file.getAbsolutePath() + " failed");
      }
      return false;
    } finally {
      try {
        if (zis != null) {
          zis.close();
        }
        if (os != null) {
          os.close();
        }
      } catch (IOException ignored) {
      }
      if (!mFilePath.delete()) {
        Log.e(TAG, "Deleting " + mFilePath.getAbsolutePath() + " failed");
      }
    }
  }
}
