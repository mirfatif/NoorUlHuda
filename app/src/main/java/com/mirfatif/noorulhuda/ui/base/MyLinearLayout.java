package com.mirfatif.noorulhuda.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {

  public MyLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    BaseActivity.onCreateLayout(this);
  }
}
