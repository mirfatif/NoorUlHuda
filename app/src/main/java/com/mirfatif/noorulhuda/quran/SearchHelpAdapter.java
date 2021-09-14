package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.toPx;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
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

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return mChars.size();
  }

  private static final int VIEW_TYPE_CHAR = 0;
  private static final int VIEW_TYPE_HEADER = 1;

  @Override
  public int getItemViewType(int position) {
    if (mChars.get(position).first < 0) {
      return VIEW_TYPE_HEADER;
    }
    return VIEW_TYPE_CHAR;
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    ArCharItemBinding b = ArCharItemBinding.inflate(inflater, parent, false);
    return new ItemViewHolder(b, viewType == VIEW_TYPE_HEADER);
  }

  @Override
  public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    holder.bind(position);
  }

  class ItemViewHolder extends ViewHolder implements OnClickListener {

    private final ArCharItemBinding mB;
    private final boolean mIsHeader;

    public ItemViewHolder(ArCharItemBinding binding, boolean isHeader) {
      super(binding.getRoot());
      mB = binding;
      mIsHeader = isHeader;
      if (isHeader) {
        mB.charV.setVisibility(View.GONE);
        mB.descV.setTypeface(Typeface.create(mB.descV.getTypeface(), Typeface.BOLD));
        mB.getRoot()
            .setPadding(
                mB.getRoot().getPaddingLeft(), toPx(32), mB.getRoot().getPaddingRight(), toPx(8));
      } else {
        mB.charV.setTypeface(SETTINGS.getTypeface());
        mB.getRoot().setOnClickListener(this);
      }
    }

    void bind(int pos) {
      Pair<Integer, String> item = mChars.get(pos);
      if (item != null) {
        if (!mIsHeader) {
          mB.charV.setText(String.valueOf((char) item.first.intValue()));
        }
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
        Utils.showShortToast(mChars.get(pos).second);
      }
    }
  }
}
