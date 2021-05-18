package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.databinding.ArCharItemBinding;
import com.mirfatif.noorulhuda.quran.SearchHelpAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.List;

public class SearchHelpAdapter extends RecyclerView.Adapter<ItemViewHolder> {

  private final List<Pair<Integer, String>> mChars;

  public SearchHelpAdapter(List<Pair<Integer, String>> chars) {
    mChars = chars;
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    ArCharItemBinding b = ArCharItemBinding.inflate(inflater, parent, false);
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
    return mChars.size();
  }

  class ItemViewHolder extends ViewHolder implements OnClickListener {

    private final ArCharItemBinding mB;

    public ItemViewHolder(ArCharItemBinding binding) {
      super(binding.getRoot());
      mB = binding;
      mB.charV.setTypeface(SETTINGS.getTypeface());
      mB.getRoot().setOnClickListener(this);
    }

    void bind(int pos) {
      Pair<Integer, String> item = mChars.get(pos);
      if (item != null) {
        mB.charV.setText(String.valueOf((char) item.first.intValue()));
        mB.descV.setText(item.second);
      }
    }

    @Override
    public void onClick(View v) {
      int pos = getBindingAdapterPosition();
      if (pos != RecyclerView.NO_POSITION) {
        ClipboardManager clipboard =
            (ClipboardManager) App.getCxt().getSystemService(Context.CLIPBOARD_SERVICE);
        String text = String.valueOf((char) mChars.get(pos).first.intValue());
        ClipData data = ClipData.newPlainText("search_char", text);
        clipboard.setPrimaryClip(data);
        Utils.showShortToast(text);
      }
    }
  }
}
