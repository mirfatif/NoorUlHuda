package com.mirfatif.noorulhuda.tags;

import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.TagsListItemBinding;
import com.mirfatif.noorulhuda.db.TagEntity;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class TagsAdapter extends MyBaseAdapter {

  public TagsAdapter(DialogListCallback callback) {
    super(callback);
  }

  private int mAayahId = -1;
  private TagCheckboxCallback mCheckCallback;

  void setAayahId(int aayahId, TagCheckboxCallback callback) {
    mAayahId = aayahId;
    mCheckCallback = callback;
  }

  private final List<TagEntity> mItems = new ArrayList<>();

  void submitList(List<TagEntity> entities) {
    synchronized (mItems) {
      mItems.clear();
      mItems.addAll(entities);
      notifyDataSetChanged();
    }
  }

  @Override
  public int getCount() {
    return mItems.size();
  }

  @Override
  public TagEntity getItem(int position) {
    return mItems.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TagEntity item = mItems.get(position);
    if (item == null) {
      return null;
    }

    TagsListItemBinding b;
    if (convertView == null) {
      b = TagsListItemBinding.inflate(LayoutInflater.from(parent.getContext()));
    } else {
      b = TagsListItemBinding.bind(convertView);
    }

    b.titleV.setText(item.title);

    int aayahCnt = item.aayahIds.size();
    String count = Utils.getQtyString(R.plurals.aayah_count, aayahCnt, aayahCnt);
    if (aayahCnt > 0) {
      count = Utils.getQtyString(R.plurals.surah_count, item.surahCount, count, item.surahCount);
    }
    b.aayahCountV.setText(count);

    if (mAayahId >= 0) {
      b.checkbox.setVisibility(View.VISIBLE);
      b.checkbox.setOnCheckedChangeListener(null);
      b.checkbox.setChecked(item.aayahIds.contains(mAayahId));
      b.checkbox.setOnCheckedChangeListener(
          (v, checked) -> mCheckCallback.checkboxChanged(position, checked));
    } else {
      b.checkbox.setVisibility(View.GONE);
    }

    b.getRoot()
        .setOnClickListener(
            v -> {
              v.playSoundEffect(SoundEffectConstants.CLICK);
              mCallback.onItemSelect(position);
            });

    b.deleteV.setVisibility(View.GONE);
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

    return b.getRoot();
  }

  private final ButtonHider BUTTON_HIDER = new ButtonHider();

  public interface TagCheckboxCallback {

    void checkboxChanged(int pos, boolean checked);
  }
}
