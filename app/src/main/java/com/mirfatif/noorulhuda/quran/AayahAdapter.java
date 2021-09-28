package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.getString;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class AayahAdapter extends RecyclerView.Adapter<ItemViewHolder> {

  private static final String TRANSLITERATION_EN = getString(R.string.en_transliteration);

  private final FragmentActivity mA;
  private final AayahLongClickListener mLongClickListener;

  AayahAdapter(FragmentActivity activity, AayahLongClickListener listener) {
    mA = activity;
    mLongClickListener = listener;
  }

  private final List<AayahGroup> mAayahGroupList = new ArrayList<>();
  private final List<Integer> mTasmiaPositions = new ArrayList<>();

  @SuppressLint("NotifyDataSetChanged")
  void submitList(List<AayahGroup> aayahGroups, List<Integer> tasmiaPos) {
    synchronized (mAayahGroupList) {
      mAayahGroupList.clear();
      mAayahGroupList.addAll(aayahGroups);
      mTasmiaPositions.clear();
      mTasmiaPositions.addAll(tasmiaPos);
      notifyDataSetChanged();
    }
  }

  AayahGroup getAayahGroup(int position) {
    return mAayahGroupList.get(position);
  }

  private final Map<ItemViewHolder, Integer> mViewHolders =
      Collections.synchronizedMap(new WeakHashMap<>());

  @NonNull
  @Override
  public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemViewHolder holder;
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_TASMIA) {
      holder = new TasmiaViewHolder(RvItemTasmiaBinding.inflate(inflater, parent, false));
    } else {
      holder = new AayahViewHolder(RvItemAayahBinding.inflate(inflater, parent, false));
    }
    mViewHolders.put(holder, null);
    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    Utils.runBg(() -> holder.bind(position));
  }

  @Override
  public int getItemCount() {
    return mAayahGroupList.size();
  }

  private static final int TYPE_AAYAH = 0;
  private static final int TYPE_TASMIA = 1;

  @Override
  public int getItemViewType(int position) {
    if (mTasmiaPositions.contains(position)) {
      return TYPE_TASMIA;
    }
    return TYPE_AAYAH;
  }

  void refreshUi() {
    TextView tv = new TextView(mA);
    int color = tv.getCurrentTextColor();
    Set<ItemViewHolder> viewHolders = mViewHolders.keySet();
    for (ItemViewHolder holder : viewHolders) {
      if (holder != null) {
        holder.refreshUi(color);
      }
    }
  }

  protected abstract class ItemViewHolder extends RecyclerView.ViewHolder {

    private ItemViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    private void bind(int position) {
      AayahGroup aayahGroup = mAayahGroupList.get(position);
      if (aayahGroup == null) {
        aayahGroup = new AayahGroup();
        synchronized (mAayahGroupList) {
          mAayahGroupList.remove(position);
          mAayahGroupList.add(position, aayahGroup);
        }
      }

      // If it's non-slide and non-search mode.
      if (aayahGroup.entities.isEmpty()) {
        if (SETTINGS.showSingleAayah()) {
          aayahGroup.entities.add(SETTINGS.getQuranDb().getAayahEntity(position));
        } else {
          aayahGroup.entities.addAll(SETTINGS.getQuranDb().getAayahsAtGroupPos(position));
        }
      }

      if (!aayahGroup.entities.isEmpty()) {
        bind(aayahGroup);
      }
    }

    abstract void bind(AayahGroup aayahGroup);

    abstract void refreshUi(@ColorInt Integer defColor);
  }

  class TasmiaViewHolder extends ItemViewHolder {

    private final RvItemTasmiaBinding mB;

    private TasmiaViewHolder(RvItemTasmiaBinding binding) {
      super(binding.getRoot());
      mB = binding;
      refreshUi(null);
    }

    @Override
    void bind(AayahGroup aayahGroup) {
      SurahEntity surah = SETTINGS.getMetaDb().getSurah(aayahGroup.entities.get(0).surahNum);
      if (surah != null) {
        Utils.runUi(mA, () -> bindSurahHeader(surah)).waitForMe();
        aayahGroup.bound = true;
      }
    }

    @Override
    public void refreshUi(Integer defColor) {
      Typeface typeface = SETTINGS.getTypeface();

      mB.durationV.setTypeface(typeface);
      mB.rukuCountV.setTypeface(typeface);
      mB.aayahCountV.setTypeface(typeface);
      mB.nameV.setTypeface(typeface);
      mB.tasmiaV.setTypeface(typeface);

      int colorRes = SETTINGS.getFontColor();
      Integer color = null;
      if (colorRes > 0) {
        color = mB.getRoot().getContext().getColor(colorRes);
      } else if (defColor != null) {
        color = defColor;
      }

      if (color != null) {
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
      refreshUi(null);

      if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
        mB.transV.setVisibility(View.VISIBLE);
      } else {
        mB.transV.setVisibility(View.GONE);
      }

      if (SETTINGS.showSingleAayah()) {
        mB.getRoot().setOnLongClickListener(this);
        mB.textV.setOnLongClickListener(null);
      } else {
        mB.getRoot().setOnLongClickListener(null);
        mB.textV.setOnLongClickListener(this);
      }
    }

    TextView getTextView() {
      return mB.textV;
    }

    private AayahGroup mAayahGroup;

    @Override
    void bind(AayahGroup aayahGroup) {
      mAayahGroup = aayahGroup;
      if (aayahGroup.prettyText == null) {
        QuranDao db = SETTINGS.getTransDb();
        StringBuilder text = new StringBuilder();
        List<Pair<Integer, Integer>> aayahStartEndMarks = new ArrayList<>();
        List<Integer> rukuEndMarks = new ArrayList<>();

        for (AayahEntity entity : aayahGroup.entities) {
          if (text.length() != 0) {
            text.append(" ");
          }

          SpannableString trans = null;
          if (SETTINGS.transEnabled() && db != null) {
            String translation = db.getTrans(entity.id);
            if (TRANSLITERATION_EN.equals(SETTINGS.getTransDbName())) {
              trans = Utils.htmlToString(translation);
            } else {
              trans = new SpannableString(translation);
            }
          }

          String entityText = removeUnsupportedChars(entity.text);

          SpanMarks span =
              new SpanMarks(
                  entity, entityText, trans, text.length(), text.length() + entityText.length());
          aayahGroup.aayahSpans.add(span);

          text.append(entityText).append(" ");
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

        if (SETTINGS.isSearching()) {
          if (SETTINGS.doSearchInTranslation()) {
            if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
              Utils.getHighlightString(
                  aayahGroup.aayahSpans.get(0).trans, getHighlight(), SETTINGS.getSearchQuery());
            }
          } else if (SETTINGS.doSearchWithVowels()) {
            Utils.getHighlightString(
                aayahGroup.prettyText, getHighlight(), SETTINGS.getSearchQuery());
          }

          SurahEntity surah = SETTINGS.getMetaDb().getSurah(aayahGroup.entities.get(0).surahNum);
          aayahGroup.surahName = getString(R.string.surah_name, surah.name);
        }
      }

      Utils.runUi(mA, () -> bindAayah(aayahGroup)).waitForMe();
      aayahGroup.bound = true;
    }

    @Override
    void refreshUi(Integer defColor) {
      int colorRes = SETTINGS.getFontColor();
      Integer color = null;
      if (colorRes > 0) {
        color = mB.getRoot().getContext().getColor(colorRes);
      } else if (defColor != null) {
        color = defColor;
      }

      if (color != null) {
        mB.textV.setTextColor(color);
        mB.transV.setTextColor(color);
        mB.refV.setTextColor(color);
      }

      int sizeAr = SETTINGS.getArabicFontSize();
      int size = SETTINGS.getFontSize();

      Typeface typeface = SETTINGS.getTypeface();

      mB.textV.setTextSize(sizeAr * 1.5f);
      mB.textV.setTypeface(typeface);
      mB.transV.setTextSize(size);
      mB.transV.setTypeface(SETTINGS.getTransTypeface());
      mB.refV.setTextSize(sizeAr * 0.8f);
      mB.refV.setTypeface(typeface);
    }

    private void bindAayah(AayahGroup aayahGroup) {
      mB.textV.setText(aayahGroup.prettyText);
      if (SETTINGS.transEnabled() && SETTINGS.showTransWithText()) {
        mB.transV.setText(aayahGroup.aayahSpans.get(0).trans);
      }

      if (SETTINGS.isSearching()) {
        mB.refV.setText(aayahGroup.surahName);
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
      mAayahGroup.prettyText = spannable;
    }

    private TextAppearanceSpan HIGHLIGHT;

    private TextAppearanceSpan getHighlight() {
      if (HIGHLIGHT == null) {
        HIGHLIGHT = Utils.getHighlight(Utils.getColor(mA, R.attr.accent));
      }
      return HIGHLIGHT;
    }

    @Override
    public boolean onLongClick(View v) {
      if (SETTINGS.showSingleAayah()) {
        mLongClickListener.onLongClick(mAayahGroup, 0, v);
      } else {
        int offset = mB.textV.getTouchOffset();
        for (int i = 0; i < mAayahGroup.aayahSpans.size(); i++) {
          SpanMarks span = mAayahGroup.aayahSpans.get(i);
          if (offset >= span.start && offset <= span.end) {
            mLongClickListener.onTextSelected(mAayahGroup, i, mB.textV, span.start, span.end);
            break;
          }
        }
      }
      return true;
    }
  }

  static class AayahGroup {

    // Quranic text with Aayah signs and Ruku signs added.
    SpannableString prettyText;

    // To show in reference in search mode.
    String surahName;

    // Indicates that the AayahGroup has passed from bind() at least once.
    boolean bound = false;

    // Single Aayah (in single Aayah mode or with translation below Quranic text) or
    // a list of Aayahs (to show Quranic text as a block).
    public final List<AayahEntity> entities = new ArrayList<>();

    // Aayah start end positions (when showing Quranic text as a block).
    public final List<SpanMarks> aayahSpans = new ArrayList<>();
  }

  static class SpanMarks {

    final int start, end;
    final AayahEntity entity;
    final String text;
    final SpannableString trans;

    private SpanMarks(AayahEntity entity, String text, SpannableString trans, int start, int end) {
      this.entity = entity;
      this.text = text;
      this.trans = trans;
      this.start = start;
      this.end = end;
    }
  }

  interface AayahLongClickListener {

    void onLongClick(AayahGroup aayahGroup, int index, View view);

    void onTextSelected(AayahGroup aayahGroup, int index, TextView textView, int start, int end);
  }

  // Remove characters not rendered by Hafs font.
  public static String removeUnsupportedChars(String text) {
    if (getString(R.string.font_hafs).equals(SETTINGS.getFontName())) {
      String regex = "[" + (char) 1759 + (char) 1763 + (char) 1771 + "]";
      return text.replaceAll(regex, "");
    }
    return text;
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
