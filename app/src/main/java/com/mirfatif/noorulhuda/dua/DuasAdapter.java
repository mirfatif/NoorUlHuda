package com.mirfatif.noorulhuda.dua;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

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
import java.util.List;

public class DuasAdapter extends RecyclerView.Adapter<ItemViewHolder> {

  private final DuaLongClickListener mClickListener;

  public DuasAdapter(DuaLongClickListener listener) {
    mClickListener = listener;
  }

  private final List<Dua> mDuas = new ArrayList<>();

  void submitList(List<Dua> duas) {
    synchronized (mDuas) {
      mDuas.clear();
      mDuas.addAll(duas);
      notifyDataSetChanged();
    }
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    RvItemAayahBinding b = RvItemAayahBinding.inflate(inflater, parent, false);
    return new ItemViewHolder(b);
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

      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();

      Typeface typeface = SETTINGS.getTypeface();
      Typeface transTypeface = SETTINGS.getTransTypeface();

      mB.titleV.setTextSize(size);
      mB.textV.setTextSize(sizeAr * 1.5f);
      mB.textV.setTypeface(typeface);
      mB.transV.setTextSize(size);
      if (transTypeface != null) {
        mB.titleV.setTypeface(transTypeface);
        mB.transV.setTypeface(transTypeface);
      }
      mB.refV.setTextSize(sizeAr * 0.8f);
      mB.refV.setTypeface(typeface);

      mB.getRoot().setOnLongClickListener(this);
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
      if (dua.trans == null) {
        mB.transV.setVisibility(View.GONE);
      } else {
        mB.transV.setText(dua.trans);
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
