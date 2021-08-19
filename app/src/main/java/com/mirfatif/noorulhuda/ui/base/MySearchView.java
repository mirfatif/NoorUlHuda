package com.mirfatif.noorulhuda.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

public class MySearchView extends SearchView {

  public MySearchView(@NonNull Context context) {
    super(context);
    init();
  }

  public MySearchView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    BaseActivity.onCreateLayout(this);
    setMaxWidth(Integer.MAX_VALUE);
  }
}
