package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import com.mirfatif.noorulhuda.R;

public class GoToPicker extends NumberPicker {

  public GoToPicker(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    super.addView(child, params);
    if (child instanceof EditText) {
      ((EditText) child).setTextSize(18);
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);
    if (child instanceof EditText) {
      ((EditText) child).setTextSize(18);
      int col = getContext().getColor(R.color.fgSharp2);
      ((EditText) child).setTextColor(col);
      if (getId() == R.id.type_picker) {
        ((EditText) child).setTypeface(SETTINGS.getTypeface());
      }
    }
  }
}
