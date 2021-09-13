package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.getString;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.RvItemAayahBinding;
import com.mirfatif.noorulhuda.databinding.RvItemTasmiaBinding;
import com.mirfatif.noorulhuda.db.AayahEntity;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.SurahEntity;
import com.mirfatif.noorulhuda.quran.AayahAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class AayahAdapter extends RecyclerView.Adapter<ItemViewHolder> {

  private static final String TRANSLITERATION_EN = getString(R.string.en_transliteration);

  private final Fragment mFrag;
  private final AayahLongClickListener mLongClickListener;

  AayahAdapter(Fragment frag, AayahLongClickListener listener) {
    mFrag = frag;
    mLongClickListener = listener;
  }

  private final List<Aayah> mAayahList = new ArrayList<>();

  @SuppressLint("NotifyDataSetChanged")
  void submitList(List<Aayah> list) {
    synchronized (mAayahList) {
      mAayahList.clear();
      mAayahList.addAll(list);
      notifyDataSetChanged();
    }
  }

  Aayah getAayah(int position) {
    return mAayahList.get(position);
  }

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_TASMIA) {
      return new TasmiaViewHolder(RvItemTasmiaBinding.inflate(inflater, parent, false));
    } else {
      return new AayahViewHolder(RvItemAayahBinding.inflate(inflater, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    Utils.runBg(() -> holder.bind(position));
  }

  @Override
  public int getItemCount() {
    return mAayahList.size();
  }

  private static final int TYPE_TASMIA = 1;

  @Override
  public int getItemViewType(int position) {
    int id = position;
    Aayah aayah = mAayahList.get(position);
    if (aayah != null && !aayah.entities.isEmpty() && aayah.entities.get(0) != null) {
      id = aayah.entities.get(0).id;
    }
    if (SETTINGS.isTasmia(id)) {
      return TYPE_TASMIA;
    }
    return 0;
  }

  protected abstract class ItemViewHolder extends RecyclerView.ViewHolder {

    private final View mView;

    private ItemViewHolder(@NonNull View itemView) {
      super(itemView);
      mView = itemView;
    }

    View getView() {
      return mView;
    }

    private void bind(int position) {
      Aayah aayah = mAayahList.get(position);
      if (aayah == null) {
        aayah = new Aayah();
        synchronized (mAayahList) {
          mAayahList.remove(position);
          mAayahList.add(position, aayah);
        }
      }
      if (aayah.entities.isEmpty()) {
        aayah.entities.add(SETTINGS.getQuranDb().getAayahEntity(position));
      }

      if (!aayah.entities.isEmpty()) {
        bind(aayah);
      }
    }

    abstract void bind(Aayah aayah);
  }

  private class TasmiaViewHolder extends ItemViewHolder {

    private final RvItemTasmiaBinding mB;

    private TasmiaViewHolder(RvItemTasmiaBinding binding) {
      super(binding.getRoot());
      mB = binding;

      Typeface typeface = SETTINGS.getTypeface();

      mB.durationV.setTypeface(typeface);
      mB.rukuCountV.setTypeface(typeface);
      mB.aayahCountV.setTypeface(typeface);
      mB.nameV.setTypeface(typeface);
      mB.tasmiaV.setTypeface(typeface);

      int colorRes = SETTINGS.getFontColor();
      if (colorRes > 0) {
        int color = mB.getRoot().getContext().getColor(colorRes);
        mB.numberV.setTextColor(color);
        mB.durationV.setTextColor(color);
        mB.rukuCountV.setTextColor(color);
        mB.rukuCountValueV.setTextColor(color);
        mB.aayahCountV.setTextColor(color);
        mB.aayahCountValueV.setTextColor(color);
        mB.nameV.setTextColor(color);
        mB.tasmiaV.setTextColor(color);
      }

      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();

      mB.numberV.setTextSize(size);
      mB.durationV.setTextSize(sizeAr);
      mB.rukuCountV.setTextSize(sizeAr);
      mB.rukuCountValueV.setTextSize(size);
      mB.aayahCountV.setTextSize(sizeAr);
      mB.aayahCountValueV.setTextSize(size);
      mB.nameV.setTextSize(sizeAr * 1.5f);
      mB.nameLeftV.setTextSize(size * 2.25f);
      mB.nameRightV.setTextSize(size * 2.25f);
      mB.tasmiaV.setTextSize(sizeAr * 1.5f);
    }

    @Override
    void bind(Aayah aayah) {
      SurahEntity surah = SETTINGS.getMetaDb().getSurah(aayah.entities.get(0).surahNum);
      if (surah != null) {
        Utils.runUi(mFrag, () -> bindSurahHeader(surah));
      }
    }

    private void bindSurahHeader(SurahEntity surah) {
      mB.numberV.setText(
          String.format("%s \t %s", getArNum(surah.order), getArNum(surah.surahNum)));
      mB.durationV.setText(surah.isMeccan ? R.string.makki : R.string.madni);
      mB.rukuCountValueV.setText(getArNum(surah.rukus));
      mB.aayahCountValueV.setText(getArNum(surah.aayahs));
      mB.nameV.setText(Utils.getString(R.string.surah_name, surah.name));

      if (surah.surahNum == 1 || surah.surahNum == 9) {
        mB.tasmiaV.setVisibility(View.GONE);
      } else {
        mB.tasmiaV.setVisibility(View.VISIBLE);
      }
    }
  }

  /* We are setting LongClickSpan on all aayahs in Page Mode.
  Other option is to ask the user to select one from aayahs shown in a block of text.
  Another option is to use TextView.setCustomSelectionActionModeCallback() instead
  of setOnLongClickListener in Adapter. But text selection doesn't make sense for
  Bookmarks and Tag actions. And TextView doesn't provide a way to set custom
  selection (on whole aayah) after we get a word selected.
  Another option is to use TextView.setMovementMethod with ClickableSpan. But
  MovementMethods distorts text wrapping.
  */
  class AayahViewHolder extends ItemViewHolder implements OnLongClickListener {

    private final RvItemAayahBinding mB;

    private AayahViewHolder(RvItemAayahBinding binding) {
      super(binding.getRoot());
      mB = binding;

      int colorRes = SETTINGS.getFontColor();
      if (colorRes > 0) {
        int color = mB.getRoot().getContext().getColor(colorRes);
        mB.textV.setTextColor(color);
        mB.transV.setTextColor(color);
        mB.refV.setTextColor(color);
      }

      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();

      Typeface typeface = SETTINGS.getTypeface();
      Typeface transTypeface = SETTINGS.getTransTypeface();

      mB.textV.setTextSize(sizeAr * 1.5f);
      mB.textV.setTypeface(typeface);
      mB.transV.setTextSize(size);
      if (transTypeface != null) {
        mB.transV.setTypeface(transTypeface);
      }
      mB.refV.setTextSize(sizeAr * 0.8f);
      mB.refV.setTypeface(typeface);

      if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
        mB.transV.setVisibility(View.VISIBLE);
      } else {
        mB.transV.setVisibility(View.GONE);
      }

      if (SETTINGS.showSingleAayah()) {
        binding.getRoot().setOnLongClickListener(this);
      } else {
        binding.textV.setOnLongClickListener(this);
      }
    }

    TextView getTextView() {
      return mB.textV;
    }

    private Aayah mAayah;

    @Override
    void bind(Aayah aayah) {
      mAayah = aayah;
      if (aayah.prettyText == null) {
        QuranDao db = SETTINGS.getTransDb();
        StringBuilder text = new StringBuilder();
        List<Pair<Integer, Integer>> aayahStartEndMarks = new ArrayList<>();
        List<Integer> rukuEndMarks = new ArrayList<>();
        for (AayahEntity entity : aayah.entities) {
          if (text.length() != 0) {
            text.append(" ");
          }

          Spanned trans = null;
          if (SETTINGS.transEnabled() && db != null) {
            String translation = db.getTrans(entity.id);
            if (TRANSLITERATION_EN.equals(SETTINGS.getTransDbName())) {
              trans = Utils.htmlToString(translation);
            } else {
              trans = new SpannableString(translation);
            }
          }

          SpanMarks span =
              new SpanMarks(entity, trans, text.length(), text.length() + entity.text.length());
          aayah.aayahSpans.add(span);

          text.append(entity.text).append(" ");
          int start = text.length(), end = start;

          if (entity.hizbEnds) {
            text.append((char) 1758);
            end++;
          }

          if (entity.hasSajda) {
            text.append((char) 1769);
            end++;
          }

          text.append((char) (entity.aayahNum + 64511));
          end++;

          // Superscript Ain (2262) char doesn't work with all fonts.
          if (entity.rukuEnds) {
            rukuEndMarks.add(text.length());
            text.append((char) 1593);
            end++;
          }

          aayahStartEndMarks.add(new Pair<>(start, end));
        }
        applySpan(text.toString(), aayahStartEndMarks, rukuEndMarks);
      }

      if (SETTINGS.isSearching()) {
        SurahEntity surah = SETTINGS.getMetaDb().getSurah(aayah.entities.get(0).surahNum);
        aayah.surahName = getString(R.string.surah_name, surah.name);
      }

      Utils.runUi(mFrag, () -> bindAayah(aayah));
    }

    private void bindAayah(Aayah aayah) {
      mB.textV.setText(aayah.prettyText);
      if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
        mB.transV.setText(aayah.aayahSpans.get(0).trans);
      }

      if (SETTINGS.isSearching()) {
        mB.refV.setText(aayah.surahName);
        mB.refV.setVisibility(View.VISIBLE);
      } else {
        mB.refV.setVisibility(View.GONE);
      }
    }

    private void applySpan(
        String text, List<Pair<Integer, Integer>> aayahStartEndMarks, List<Integer> rukuEndSpans) {
      if (text == null) {
        return;
      }
      SpannableString spannable = new SpannableString(text);
      for (Pair<Integer, Integer> pos : aayahStartEndMarks) {
        spannable.setSpan(
            new HafsFontSpan(), pos.first, pos.second, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      for (Integer pos : rukuEndSpans) {
        spannable.setSpan(new RukuSignSpan(), pos, pos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      mAayah.prettyText = spannable;
    }

    @Override
    public boolean onLongClick(View v) {
      if (SETTINGS.showSingleAayah()) {
        mLongClickListener.onLongClick(mAayah.entities.get(0), mAayah.aayahSpans.get(0).trans, v);
      } else {
        int offset = mB.textV.getTouchOffset();
        for (SpanMarks span : mAayah.aayahSpans) {
          if (offset >= span.start && offset <= span.end) {
            mLongClickListener.onTextSelected(
                span.entity, span.trans, mB.textV, span.start, span.end);
            break;
          }
        }
      }
      return true;
    }
  }

  static class Aayah {

    // Quranic text with Aayah signs and Ruku signs added.
    SpannableString prettyText;

    // To show in reference in search mode.
    String surahName;

    // Single Aayah (in single Aayah mode or with translation below Quranic text) or
    // a list of Aayahs (to show Quranic text as a block).
    public final List<AayahEntity> entities = new ArrayList<>();

    // Aayah start end positions (when showing Quranic text as a block).
    public final List<SpanMarks> aayahSpans = new ArrayList<>();
  }

  static class SpanMarks {

    final int start, end;
    final AayahEntity entity;
    final Spanned trans;

    private SpanMarks(AayahEntity entity, Spanned trans, int start, int end) {
      this.entity = entity;
      this.trans = trans;
      this.start = start;
      this.end = end;
    }
  }

  interface AayahLongClickListener {

    void onLongClick(AayahEntity entity, Spanned trans, View view);

    void onTextSelected(AayahEntity entity, Spanned trans, TextView textView, int start, int end);
  }

  // Combination of TextAppearanceSpan and TypefaceSpan.
  public static class HafsFontSpan extends MetricAffectingSpan {

    private static final Typeface HAFS_TYPEFACE =
        ResourcesCompat.getFont(App.getCxt(), R.font.uthmanic_hafs1_ver17);

    @Override
    public void updateMeasureState(TextPaint tp) {
      if (!SETTINGS.fontSupportsSymbols()) {
        tp.setTypeface(HAFS_TYPEFACE);
      }
      tp.setColor(Utils.getAccentColor());
    }

    @Override
    public void updateDrawState(TextPaint tp) {
      updateMeasureState(tp);
    }
  }

  // Combination of RelativeSizeSpan and SuperscriptSpan.
  private static class RukuSignSpan extends MetricAffectingSpan {

    @Override
    public void updateMeasureState(TextPaint tp) {
      tp.setTextSize(tp.getTextSize() / 2);
      tp.baselineShift += (int) tp.ascent();
      Typeface typeface = Typeface.create(tp.getTypeface(), Typeface.BOLD);
      tp.setTypeface(typeface);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
      updateMeasureState(tp);
    }
  }
}
