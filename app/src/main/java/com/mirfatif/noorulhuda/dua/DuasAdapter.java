package com.mirfatif.noorulhuda.dua;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.mirfatif.noorulhuda.databinding.RvItemAayahBinding;
import com.mirfatif.noorulhuda.dua.DuasAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.quran.AayahAdapter.HafsFontSpan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class DuasAdapter extends RecyclerView.Adapter<ItemViewHolder> {

  private final DuaLongClickListener mClickListener;

  public DuasAdapter(DuaLongClickListener listener) {
    mClickListener = listener;
  }

  private final List<Dua> mDuas = new ArrayList<>();

  @SuppressLint("NotifyDataSetChanged")
  void submitList(List<Dua> duas) {
    synchronized (mDuas) {
      mDuas.clear();
      mDuas.addAll(duas);
      notifyDataSetChanged();
    }
  }

  private final Map<DuasAdapter.ItemViewHolder, Integer> mViewHolders =
      Collections.synchronizedMap(new WeakHashMap<>());

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    RvItemAayahBinding b = RvItemAayahBinding.inflate(inflater, parent, false);
    ItemViewHolder holder = new ItemViewHolder(b);
    mViewHolders.put(holder, null);
    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    holder.bind(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return mDuas.size();
  }

  void resetFontSize() {
    Set<DuasAdapter.ItemViewHolder> viewHolders = mViewHolders.keySet();
    for (DuasAdapter.ItemViewHolder holder : viewHolders) {
      if (holder != null) {
        holder.resetFontSize();
      }
    }
  }

  class ItemViewHolder extends ViewHolder implements OnLongClickListener {

    private final RvItemAayahBinding mB;

    public ItemViewHolder(RvItemAayahBinding binding) {
      super(binding.getRoot());
      mB = binding;

      int colorRes = SETTINGS.getFontColor();
      if (colorRes > 0) {
        int color = mB.getRoot().getContext().getColor(colorRes);
        mB.titleV.setTextColor(color);
        mB.textV.setTextColor(color);
        mB.transV.setTextColor(color);
        mB.refV.setTextColor(color);
      }

      Typeface typeface = SETTINGS.getTypeface();
      Typeface transTypeface = SETTINGS.getTransTypeface();

      mB.textV.setTypeface(typeface);
      mB.titleV.setTypeface(transTypeface);
      mB.transV.setTypeface(transTypeface);
      mB.refV.setTypeface(typeface);

      resetFontSize();

      mB.getRoot().setOnLongClickListener(this);
    }

    private void resetFontSize() {
      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();
      mB.titleV.setTextSize(size);
      mB.textV.setTextSize(sizeAr * 1.5f);
      mB.transV.setTextSize(size);
      mB.refV.setTextSize(sizeAr * 0.8f);
    }

    void bind(int pos) {
      Dua dua = mDuas.get(pos);
      if (dua == null) {
        return;
      }
      if (dua.title != null) {
        mB.titleV.setVisibility(View.VISIBLE);
        mB.titleV.setText(dua.title);
      }
      mB.textV.setText(applySpan(dua.text));
      if (dua.trans != null && SETTINGS.showTransWithText()) {
        mB.transV.setText(dua.trans);
      } else {
        mB.transV.setVisibility(View.GONE);
      }
      mB.refV.setText(dua.ref);
    }

    private Spannable applySpan(String text) {
      if (text == null) {
        return null;
      }
      Spannable spannable = new SpannableString(text);
      int pos, from = 0;
      while ((pos = text.indexOf(1757, from)) >= 0) {
        spannable.setSpan(new HafsFontSpan(), pos, pos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        from = pos + 1;
      }

      return spannable;
    }

    @Override
    public boolean onLongClick(View v) {
      int pos = getBindingAdapterPosition();
      if (pos != RecyclerView.NO_POSITION) {
        mClickListener.onLongClick(mDuas.get(pos), v);
      }
      return true;
    }
  }

  static class Dua {

    String title, text, trans, ref;
    int surahNum, aayahNum;
  }

  public interface DuaLongClickListener {

    void onLongClick(Dua dua, View view);
  }
}
