package com.mirfatif.noorulhuda.dua;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.mirfatif.noorulhuda.R;

public class DuaPageAdapter extends FragmentStateAdapter {

  public static final int[] TAB_LABELS =
      new int[] {R.string.quranic, R.string.masnoon, R.string.occasions};
  public static final String DUA_TYPE = "DUA_TYPE";
  public static int DUA_TYPE_QURANIC = 0;
  public static int DUA_TYPE_MASNOON = 1;
  public static int DUA_TYPE_OCCASIONS = 2;

  public DuaPageAdapter(@NonNull FragmentActivity fragmentActivity) {
    super(fragmentActivity);
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    DuaPageFragment fragment = new DuaPageFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(DUA_TYPE, position);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public int getItemCount() {
    return TAB_LABELS.length;
  }

  @SuppressLint("NotifyDataSetChanged")
  void refreshUi() {
    mIdDiff += getItemCount();
    notifyDataSetChanged();
  }

  // Force recreate fragments when notifyDataSetChanged() is called
  // in order to update UI state.
  private int mIdDiff;

  @Override
  public long getItemId(int position) {
    return position + mIdDiff;
  }

  @Override
  public boolean containsItem(long itemId) {
    return itemId >= getItemId(0) && itemId < getItemId(getItemCount());
  }
}
