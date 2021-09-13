package com.mirfatif.noorulhuda.ui.dialog;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import com.mirfatif.noorulhuda.databinding.BmarkTagListItemBinding;
import java.util.List;

// Even more simplified SimpleAdapter
public class DialogListAdapter extends MyBaseAdapter {

  private final List<DialogListItem> mItems;
  private final AlertDialogFragment mDialogFragment;

  public DialogListAdapter(
      List<DialogListItem> items, DialogListCallback callback, AlertDialogFragment fragment) {
    super(callback);
    mItems = items;
    mDialogFragment = fragment;
  }

  @Override
  public int getCount() {
    return mItems.size();
  }

  @Override
  public Object getItem(int position) {
    return mItems.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    DialogListItem item = mItems.get(position);
    if (item == null) {
      return null;
    }

    BmarkTagListItemBinding b;
    if (convertView == null) {
      b = BmarkTagListItemBinding.inflate(LayoutInflater.from(parent.getContext()));
    } else {
      b = BmarkTagListItemBinding.bind(convertView);
    }

    b.titleV.setTypeface(SETTINGS.getTypeface());
    b.textV.setTypeface(SETTINGS.getTypeface());
    b.textV.setText(item.text);

    if (item.title == null) {
      b.titleV.setVisibility(View.GONE);
    } else {
      b.titleV.setText(item.title);
    }

    if (item.subTitle == null) {
      b.subTitleV.setVisibility(View.GONE);
    } else {
      b.subTitleV.setText(item.subTitle);
    }

    if (item.text == null) {
      b.textV.setVisibility(View.GONE);
    } else {
      b.textV.setText(item.text);
    }

    b.getRoot()
        .setOnClickListener(
            v -> {
              v.playSoundEffect(SoundEffectConstants.CLICK);
              mCallback.onItemSelect(position);
              mDialogFragment.dismissAllowingStateLoss();
            });

    b.deleteV.setVisibility(View.GONE);
    if (mCallback != null) {
      b.deleteV.setOnClickListener(
          v -> {
            b.deleteV.setVisibility(View.GONE);
            mCallback.onDelete(position);
            mItems.remove(position); // Must be after onDelete() call
            notifyDataSetChanged();
          });

      b.getRoot()
          .setOnLongClickListener(
              v -> {
                BUTTON_HIDER.show(b.deleteV);
                return true;
              });
    }

    return b.getRoot();
  }

  private final ButtonHider BUTTON_HIDER = new ButtonHider();
}
