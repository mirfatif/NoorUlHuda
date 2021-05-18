package com.mirfatif.noorulhuda.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {

  public MyFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    BaseActivity.onCreateLayout(this);
  }
}
