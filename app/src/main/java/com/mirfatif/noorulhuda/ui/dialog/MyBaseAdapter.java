package com.mirfatif.noorulhuda.ui.dialog;

import android.view.View;
import android.widget.BaseAdapter;

public abstract class MyBaseAdapter extends BaseAdapter {

  public final DialogListCallback mCallback;

  public MyBaseAdapter(DialogListCallback callback) {
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
    private Runnable mButtonHider;

    public synchronized void show(View button) {
      if (mButton != null && mButton != button) {
        mButton.setVisibility(View.GONE);
      }
      mButton = button;
      mButton.setVisibility(View.VISIBLE);

      mButton.removeCallbacks(mButtonHider);
      mButtonHider = () -> mButton.setVisibility(View.GONE);
      mButton.postDelayed(mButtonHider, 5000);
    }
  }
}
