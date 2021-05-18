package com.mirfatif.noorulhuda.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class MyBaseAdapter extends BaseAdapter {

  public final LayoutInflater mInflater;
  public final DialogListCallback mCallback;

  public MyBaseAdapter(Context context, DialogListCallback callback) {
    mInflater = LayoutInflater.from(context);
    mCallback = callback;
  }

  public static class DialogListItem {

    public String title, subTitle, text;
  }

  public interface DialogListCallback {

    // ListView#setOnItemClickListener() doesn't work when list is dynamically updated.
    void onItemSelect(int pos);

    void onDelete(int pos);
  }

  public static class ButtonHider {

    private View mButton;
    private final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor();
    private Future<?> mButtonHider;

    public void show(View button) {
      synchronized (EXEC) {
        if (mButtonHider != null) {
          mButtonHider.cancel(false);
        }
        if (mButton != null && mButton != button) {
          mButton.setVisibility(View.GONE);
        }
        mButton = button;
        mButton.setVisibility(View.VISIBLE);
        mButtonHider =
            EXEC.schedule(
                () -> {
                  synchronized (EXEC) {
                    if (mButton != null) {
                      Utils.runUi(() -> mButton.setVisibility(View.GONE)).waitForMe();
                      mButton = null;
                    }
                  }
                },
                5,
                TimeUnit.SECONDS);
      }
    }
  }
}
