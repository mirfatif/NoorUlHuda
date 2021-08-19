package com.mirfatif.noorulhuda.tags;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.mirfatif.noorulhuda.databinding.BmarkTagListItemBinding;
import com.mirfatif.noorulhuda.tags.TagAayahsAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.ButtonHider;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListCallback;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListItem;
import java.util.ArrayList;
import java.util.List;

public class TagAayahsAdapter extends Adapter<ItemViewHolder> {

  private final DialogListCallback mCallback;

  public TagAayahsAdapter(DialogListCallback callback) {
    mCallback = callback;
  }

  private final List<DialogListItem> mItems = new ArrayList<>();

  @SuppressLint("NotifyDataSetChanged")
  void submitList(List<DialogListItem> entities) {
    synchronized (mItems) {
      mItems.clear();
      mItems.addAll(entities);
      notifyDataSetChanged();
    }
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    return new ItemViewHolder(BmarkTagListItemBinding.inflate(inflater, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    holder.bind(position);
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }

  protected class ItemViewHolder extends ViewHolder {

    private final BmarkTagListItemBinding mB;

    public ItemViewHolder(@NonNull BmarkTagListItemBinding binding) {
      super(binding.getRoot());
      mB = binding;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void bind(int position) {
      DialogListItem item = mItems.get(position);
      if (item == null) {
        return;
      }

      Typeface typeface = SETTINGS.getTypeface();

      mB.titleV.setTypeface(typeface);
      mB.textV.setTypeface(typeface);

      mB.titleV.setText(item.title);
      mB.subTitleV.setText(item.subTitle);
      mB.textV.setText(item.text);
      mB.textV.setMaxLines(Integer.MAX_VALUE);

      mB.getRoot()
          .setOnClickListener(
              v -> {
                v.playSoundEffect(SoundEffectConstants.CLICK);
                mCallback.onItemSelect(position);
              });

      mB.deleteV.setVisibility(View.GONE);
      mB.deleteV.setOnClickListener(
          v -> {
            mB.deleteV.setVisibility(View.GONE);
            mCallback.onDelete(position);
            mItems.remove(position); // Must be after onDelete() call
            notifyDataSetChanged();
          });

      mB.getRoot()
          .setOnLongClickListener(
              v -> {
                BUTTON_HIDER.show(mB.deleteV);
                return true;
              });
    }
  }

  private final ButtonHider BUTTON_HIDER = new ButtonHider();
}
