package com.mirfatif.noorulhuda.ui.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class MyTextView extends AppCompatTextView {

  public MyTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mTapPosX = (int) event.getX();
    mTapPosY = (int) event.getY();
    return super.onTouchEvent(event);
  }

  private int mTapPosX, mTapPosY;

  public int getTouchOffset() {
    return super.getOffsetForPosition(mTapPosX, mTapPosY);
  }
}
