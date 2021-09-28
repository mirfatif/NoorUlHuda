package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.tags.TagsDialogFragment.AAYAH_ID;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.getAttrColor;
import static com.mirfatif.noorulhuda.util.Utils.setTooltip;
import static com.mirfatif.noorulhuda.util.Utils.toPx;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.recyclerview.widget.RecyclerView.State;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.AayahContextMenuBinding;
import com.mirfatif.noorulhuda.databinding.RecyclerViewBinding;
import com.mirfatif.noorulhuda.databinding.RvItemAayahBinding;
import com.mirfatif.noorulhuda.db.AayahEntity;
import com.mirfatif.noorulhuda.db.DbBuilder;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.SurahEntity;
import com.mirfatif.noorulhuda.quran.AayahAdapter.AayahGroup;
import com.mirfatif.noorulhuda.quran.AayahAdapter.AayahLongClickListener;
import com.mirfatif.noorulhuda.quran.AayahAdapter.AayahViewHolder;
import com.mirfatif.noorulhuda.quran.AayahAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.quran.AayahAdapter.SpanMarks;
import com.mirfatif.noorulhuda.quran.AayahAdapter.TasmiaViewHolder;
import com.mirfatif.noorulhuda.quran.MainActivity.ScrollPos;
import com.mirfatif.noorulhuda.tags.TagsDialogFragment;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.SingleTaskExecutor;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class QuranPageFragment extends Fragment {

  private MainActivity mA;
  private ScaleGestureDetector mRvScaleGestureDetector;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
    mRvScaleGestureDetector = new ScaleGestureDetector(mA, new RvScaleGestureListener());
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mB = RecyclerViewBinding.inflate(inflater, container, false);
    return mB.getRoot();
  }

  private DividerItemDecoration mDivider;

  private DividerItemDecoration getDivider() {
    if (mDivider == null) {
      int pad = Utils.toPx(12);
      mDivider =
          new DividerItemDecoration(mA, DividerItemDecoration.VERTICAL) {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
              outRect.set(0, pad, 0, pad);
            }
          };
    }
    return mDivider;
  }

  private RecyclerViewBinding mB;
  private AayahAdapter mAayahAdapter;
  private LinearLayoutManager mLayoutManager;

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mAayahAdapter = new AayahAdapter(mA, new LongClickListener());
    mB.recyclerV.setAdapter(mAayahAdapter);

    mLayoutManager = new LinearLayoutManager(mA);
    mB.recyclerV.setLayoutManager(mLayoutManager);

    mB.recyclerV.addOnScrollListener(new ScrollListener());
    mB.recyclerV.addOnItemTouchListener(new RvTouchListener());
    mB.getRoot().setOnClickListener(new RvContainerClickListener());

    // Update header when scroll is in progress.
    mB.recyclerV.setOnScrollChangeListener((v, x, y, oldX, oldY) -> updateHeader());

    refreshUi();
  }

  @Override
  public void onStart() {
    super.onStart();
    HEADER_UPDATER = new SingleTaskExecutor();
  }

  private final Object EXECUTOR_LOCK = new Object();

  @Override
  public void onStop() {
    synchronized (EXECUTOR_LOCK) {
      HEADER_UPDATER = null;
    }
    super.onStop();
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// GENERAL ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void refreshUi() {
    mB.recyclerV.removeItemDecoration(getDivider());
    if ((SETTINGS.transEnabled() && SETTINGS.showTransWithText()) || SETTINGS.isSearchStarted()) {
      mB.recyclerV.addItemDecoration(getDivider());
    }

    Utils.runBg(this::submitList);
  }

  private void submitList() {
    final List<AayahGroup> aayahGroups = new ArrayList<>();
    final List<Integer> tasmiaPositions = new ArrayList<>();
    Integer page = null;

    if (SETTINGS.isSlideModeAndNotInSearch()) {
      page = requireArguments().getInt(QuranPageAdapter.KEY_PAGE);

      AayahGroup aayahGroup = null;
      int lastPos = -1;

      List<AayahEntity> aayahEntities = SETTINGS.getQuranDb().getAayahEntities(page);
      for (AayahEntity entity : aayahEntities) {
        if (SETTINGS.showSingleAayah() || entity.aayahGroupPos != lastPos) {
          aayahGroup = new AayahGroup();
          aayahGroups.add(aayahGroup);
          lastPos = entity.aayahGroupPos;
        }
        if (aayahGroup != null) {
          aayahGroup.entities.add(entity);
        }
      }

      if (SETTINGS.showSingleAayah()) {
        for (int i = 0; i < aayahEntities.size(); i++) {
          if (aayahEntities.get(i).aayahNum == 0) {
            tasmiaPositions.add(i);
          }
        }
      } else {
        tasmiaPositions.addAll(SETTINGS.getQuranDb().getTasmiaGroupPosInPage(page));
      }

    } else if (!SETTINGS.showSingleAayah()) {
      int aayahGroupCount = SETTINGS.getQuranDb().getAayahGroupsCount(-1);
      aayahGroups.addAll(Arrays.asList(new AayahGroup[aayahGroupCount]));
      tasmiaPositions.addAll(SETTINGS.getQuranDb().getTasmiaGroupPos());

    } else {
      aayahGroups.addAll(Arrays.asList(new AayahGroup[DbBuilder.TOTAL_AAYAHS]));
      tasmiaPositions.addAll(SETTINGS.getQuranDb().getTasmiaIds(-1));
    }

    Utils.runUi(this, () -> mAayahAdapter.submitList(aayahGroups, tasmiaPositions));

    /*
     In case of slide-page mode (and not in search), restoring RV scroll position
     is handled in MainActivity if the page already exists in Pager.
    */
    ScrollPos scrollPos = mA.getScrollPos(page);
    if (scrollPos != null) {
      scrollToAayah(scrollPos.aayahId, scrollPos.blink);
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////// SCROLL TO POSITION //////////////////////
  //////////////////////////////////////////////////////////////////

  private static final int TMP_OFFSET = -Utils.toPx(24);
  private final Runnable SMOOTH_SCROLL_TASK = this::smoothScrollRv;

  private void smoothScrollRv() {
    if (mB.recyclerV.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
      if (mB != null && mB.recyclerV.canScrollVertically(1)) {
        mB.recyclerV.smoothScrollBy(0, TMP_OFFSET);
      }
    }
  }

  // Scroll to a RV item (with an optional offset from top), and optionally blink the RV item.
  void scrollToRvItem(int rvPos, int offset, boolean blink) {
    if (mLayoutManager == null) {
      return;
    }

    mB.recyclerV.removeCallbacks(SMOOTH_SCROLL_TASK);
    if (blink) {
      mA.toggleFullScreen(true);
    }

    // To avoid jump to top or bottom on first scrolling afterwards.
    boolean revScrollRv = false;
    if (offset == 0 && !SETTINGS.isSlideModeAndNotInSearch()) {
      offset = TMP_OFFSET;
      revScrollRv = true;
    }
    int finalOffset = offset;

    mLayoutManager.scrollToPositionWithOffset(rvPos, finalOffset);
    mB.recyclerV.postDelayed(
        () -> {
          if (mLayoutManager.findFirstVisibleItemPosition() != rvPos) {
            mLayoutManager.scrollToPositionWithOffset(rvPos, finalOffset);
          }
        },
        100);

    if (revScrollRv) {
      mB.recyclerV.postDelayed(SMOOTH_SCROLL_TASK, 800);
    }

    if (blink) {
      mB.recyclerV.postDelayed(() -> blinkRvItem(rvPos), 1000);
    }

    mB.recyclerV.postDelayed(this::updateHeader, 100);
  }

  // Blink the whole RV item.
  private void blinkRvItem(int rvPos) {
    ItemViewHolder h = (ItemViewHolder) mB.recyclerV.findViewHolderForAdapterPosition(rvPos);
    if (h != null) {
      View v = h.itemView;
      v.setVisibility(View.INVISIBLE);
      v.postDelayed(() -> v.setVisibility(View.VISIBLE), 250);
      v.postDelayed(() -> v.setVisibility(View.INVISIBLE), 500);
      v.postDelayed(() -> v.setVisibility(View.VISIBLE), 750);
    }
  }

  /*
   Find the Aayah (RV item), scroll RV there, and highlight the item.
   Or find the AayahGroup (RV item) containing an Aayah, scroll to the single Aayah
   inside it, and blink the Aayah.
  */
  void scrollToAayah(int aayahId, boolean blink) {
    if (Utils.isMainThread()) {
      Utils.runBg(() -> scrollToAayah(aayahId, blink));
      return;
    }

    AayahEntity aayah = SETTINGS.getQuranDb().getAayahEntity(aayahId);

    int rvPos;
    if (SETTINGS.isSlideModeAndNotInSearch()) {
      if (SETTINGS.showSingleAayah()) {
        rvPos = SETTINGS.getQuranDb().getAayahIds(aayah.page).indexOf(aayahId);
      } else {
        rvPos = SETTINGS.getQuranDb().getAayahGroupPosInPage(aayahId);
      }
    } else if (!SETTINGS.showSingleAayah()) {
      rvPos = SETTINGS.getQuranDb().getAayahGroupPos(aayahId);
    } else {
      rvPos = aayahId;
    }

    if (rvPos < 0) {
      return;
    }

    boolean isTasmia = aayah.aayahNum == 0;
    if (SETTINGS.showSingleAayah() || isTasmia) {
      // Move to the RV item and blink the whole item.
      Utils.runUi(this, () -> scrollToRvItem(rvPos, 0, blink && rvPos != 0 && !isTasmia));
    } else {
      // Move to the selected Aayah in AayahGroup and blink only the Aayah;
      scrollToAayahWithOffset(aayahId, rvPos, blink);
    }
  }

  /*
   Get the AayahGroup (RV item) containing an Aayah, find Aayah's offset from
   the top of RV item, scroll RV there, and blink the Aayah text.
  */
  private void scrollToAayahWithOffset(int aayahId, int rvPos, boolean blink) {
    /*
     Force create the view at the required position by moving there.
     Another way is to call RecyclerView.mRecycler.getViewForPosition() using Reflection.
    */
    Utils.runUi(this, () -> scrollToRvItem(rvPos, 0, false)).waitForMe();

    AayahGroup aayahGroup = null;
    ViewHolder holder = null;
    for (int i = 0; i < 50; i++) {
      if (aayahGroup == null) {
        aayahGroup = mAayahAdapter.getAayahGroup(rvPos);
      } else if (holder == null) {
        holder = mB.recyclerV.findViewHolderForAdapterPosition(rvPos);
      } else if (aayahGroup.bound) {
        for (SpanMarks marks : aayahGroup.aayahSpans) {
          if (marks.entity.id == aayahId) {
            final ViewHolder h = holder;
            Utils.runUi(this, () -> scrollToAayahWithOffset(h, marks, rvPos, blink));
            break;
          }
        }
        break;
      }
      SystemClock.sleep(100);
    }
  }

  private void scrollToAayahWithOffset(ViewHolder h, SpanMarks marks, int rvPos, boolean blink) {
    if (!(h instanceof AayahViewHolder)) {
      return;
    }
    TextView tv = ((AayahViewHolder) h).getTextView();
    if (tv == null) {
      return;
    }

    int line = tv.getLayout().getLineForOffset(marks.start);
    if (blink) {
      line = Math.max(0, line - 1);
    }
    int offset = tv.getLineHeight() * line;

    blink = blink && (rvPos != 0 || offset != 0);

    if (blink) {
      mA.toggleFullScreen(true);
    }

    scrollToRvItem(rvPos, -offset, false);

    if (blink) {
      Spanned oldString = (Spanned) tv.getText();
      SpannableString newString = new SpannableString(oldString);
      InvisibleFontSpan span = new InvisibleFontSpan();
      newString.setSpan(span, marks.start, marks.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      tv.postDelayed(() -> tv.setText(newString), 1250);
      tv.postDelayed(() -> tv.setText(oldString), 1500);
      tv.postDelayed(() -> tv.setText(newString), 1750);
      tv.postDelayed(() -> tv.setText(oldString), 2000);
    }
  }

  public static class InvisibleFontSpan extends MetricAffectingSpan {

    @Override
    public void updateMeasureState(TextPaint tp) {
      tp.setColor(Color.TRANSPARENT);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
      updateMeasureState(tp);
    }
  }

  boolean onBackPressed() {
    if (mPopup != null && mPopupShowing) {
      dismissPopup();
      return true;
    }
    return false;
  }

  void refresh() {
    if (mAayahAdapter != null) {
      mAayahAdapter.refreshUi();
    }
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// SEARCH /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private boolean mShowingSearchResults = false;
  private final Object SEARCH_LOCK = new Object();
  private Future<?> mSearchFuture;

  void handleSearchQuery(String query) {
    synchronized (SEARCH_LOCK) {
      if (mSearchFuture != null) {
        mSearchFuture.cancel(true);
      }
      if (TextUtils.isEmpty(query)) {
        stopToastTask();
        refreshUi();
        mA.setProgBarVisibility(false);
        mShowingSearchResults = false;
        return;
      }
      mA.setProgBarVisibility(true);
      mSearchFuture = Utils.runBg(() -> handleSearchQueryInBg(query));
    }
  }

  private final ScheduledExecutorService TOASTER = Executors.newSingleThreadScheduledExecutor();
  private Future<?> mToastFuture;

  private void handleSearchQueryInBg(String query) {
    Set<Integer> idList;
    try {
      idList = containsORed(query);
    } catch (InterruptedException e) {
      return;
    }

    List<AayahEntity> entities = QuranDao.getAayahEntities(SETTINGS.getQuranDb(), idList);

    List<AayahGroup> aayahGroups = new ArrayList<>();
    for (AayahEntity entity : entities) {
      if (Thread.interrupted()) {
        return;
      }
      AayahGroup aayahGroup = new AayahGroup();
      aayahGroup.entities.add(entity);
      aayahGroups.add(aayahGroup);
    }
    Utils.runUi(
        this,
        () -> {
          mAayahAdapter.submitList(aayahGroups, new ArrayList<>());
          mA.setProgBarVisibility(false);
        });

    mShowingSearchResults = true;

    stopToastTask();
    Runnable task = () -> Utils.showShortToast(R.string.results, aayahGroups.size());
    mToastFuture = TOASTER.schedule(task, 1500, TimeUnit.MILLISECONDS);
  }

  private Set<Integer> containsORed(String queryText) throws InterruptedException {
    Set<Integer> idList = new HashSet<>();
    for (String str : queryText.split("\\|")) {
      if (!TextUtils.isEmpty(str)) {
        Set<Integer> list = containsANDed(str);
        idList.addAll(list);
      }
    }
    return idList;
  }

  private Set<Integer> containsANDed(String queryText) throws InterruptedException {
    Set<Integer> idList = new HashSet<>();
    for (String str : queryText.split("&")) {
      if (TextUtils.isEmpty(str)) {
        continue;
      }
      if (idList.isEmpty()) {
        idList = contains(str);
      } else {
        idList.retainAll(contains(str));
      }
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      if (idList.isEmpty()) {
        break;
      }
    }
    return idList;
  }

  private Set<Integer> contains(String queryText) {
    boolean contains = true;
    while (queryText.startsWith("!")) {
      queryText = queryText.replaceAll("^!", "");
      contains = false;
    }
    QuranDao db;
    if (SETTINGS.doSearchInTranslation()
        && SETTINGS.transEnabled()
        && SETTINGS.showTransWithText()) {
      db = SETTINGS.getTransDb();
    } else if (SETTINGS.doSearchWithVowels()) {
      db = SETTINGS.getQuranDb();
    } else {
      db = SETTINGS.getSearchDb();
    }
    if (contains) {
      return new HashSet<>(Objects.requireNonNull(db).matchQuery(queryText));
    } else {
      return new HashSet<>(Objects.requireNonNull(db).misMatchQuery(queryText));
    }
  }

  private void stopToastTask() {
    if (mToastFuture != null) {
      mToastFuture.cancel(false);
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// HEADER ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private boolean mUpdatingHeader;

  private SingleTaskExecutor HEADER_UPDATER;
  private final Object HEADER_UPDATE_LOCK = new Object();
  private long mLastHeaderUpdate;

  private void headerUpdated() {
    synchronized (HEADER_UPDATE_LOCK) {
      mUpdatingHeader = false;
      HEADER_UPDATE_LOCK.notifyAll();
    }
  }

  private void updateHeader() {
    synchronized (EXECUTOR_LOCK) {
      if (HEADER_UPDATER != null && HEADER_UPDATER.getPendingTasks() == 0) {
        long delay = 500 + mLastHeaderUpdate - System.currentTimeMillis();
        delay = Math.max(0, delay);
        HEADER_UPDATER.schedule(this::waitForHeaderUpdated, delay, TimeUnit.MILLISECONDS);
      }
    }
  }

  private void waitForHeaderUpdated() {
    mUpdatingHeader = true;
    Utils.runUi(this, this::updateHeaderUi);
    synchronized (HEADER_UPDATE_LOCK) {
      while (mUpdatingHeader) {
        try {
          HEADER_UPDATE_LOCK.wait();
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  private void updateHeaderUi() {
    int topItem = mLayoutManager.findFirstVisibleItemPosition();
    if (topItem == RecyclerView.NO_POSITION) {
      headerUpdated();
      return;
    }

    if (SETTINGS.showSingleAayah()) {
      updateHeader(true, topItem, -1);
      return;
    }

    View view = mLayoutManager.findViewByPosition(topItem);
    ViewHolder h = mB.recyclerV.findViewHolderForLayoutPosition(topItem);
    if (h instanceof TasmiaViewHolder) {
      updateHeader(true, topItem, -1);
      return;
    }

    if (view == null || !(h instanceof AayahViewHolder)) {
      updateHeader(true, topItem, -1);
      return;
    }

    /*
     Using the first Aayah from AayahGroup does not reflect the exact position of
     the view. So we take into account the offset of the visible RV item from top.
    */
    TextView tv = ((AayahViewHolder) h).getTextView();
    int line;
    if (tv != null && (line = view.getTop() / tv.getLineHeight()) < 0) {
      int offset = tv.getLayout().getLineEnd(line * -1);
      updateHeader(false, topItem, Math.max(0, offset - 1));
    } else {
      updateHeader(true, topItem, -1);
    }
  }

  private void updateHeader(boolean isSingleAayah, int topItem, int textOffset) {
    if (Utils.isMainThread()) {
      Utils.runBg(() -> updateHeader(isSingleAayah, topItem, textOffset));
      return;
    }

    AayahGroup aayahGroup = mAayahAdapter.getAayahGroup(topItem);
    if (aayahGroup == null || aayahGroup.entities.isEmpty()) {
      headerUpdated();
      return;
    }

    if (isSingleAayah) {
      updateHeader(aayahGroup.entities.get(0));
    } else {
      AayahEntity lastEntity = null;
      boolean found = false;
      int lastMarkEnd = 0;
      for (SpanMarks marks : aayahGroup.aayahSpans) {
        if (textOffset < marks.start) {
          // Handle Aayah end mark within AayahGroup which is outside SpanMarks.
          if (lastEntity != null) {
            updateHeader(lastEntity);
            found = true;
          }
          break;
        } else if (marks.start <= textOffset && marks.end >= textOffset) {
          updateHeader(marks.entity);
          found = true;
          break;
        }
        lastEntity = marks.entity;
        lastMarkEnd = marks.end;
      }
      if (!found && lastMarkEnd < textOffset && lastEntity != null) {
        // Handle Aayah end mark at the end of AayahGroup which is outside SpanMarks.
        updateHeader(lastEntity);
      }
    }

    headerUpdated();
  }

  private void updateHeader(AayahEntity entity) {
    SurahEntity surah = SETTINGS.getMetaDb().getSurah(entity.surahNum);
    mLastHeaderUpdate = System.currentTimeMillis();
    Utils.runUi(this, () -> mA.updateHeader(entity, surah));
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////////// LONG CLICK ///////////////////////////
  //////////////////////////////////////////////////////////////////

  public static final int POPUP_HEIGHT = 100;
  public static final int POPUP_ICON_WIDTH = 36;
  public static final int POPUP_PADDING = 16;

  private PopupWindow mPopup;
  private int mTapPosX, mTapPosY;

  /* Positioning a window on screen: https://gist.github.com/romannurik/3982005.
    Other option is to use ActionMode with ActionMode.TYPE_FLOATING. But it's
    available only on SDK 23+. Also FloatingToolbar's background cannot be customized.
  */
  private class LongClickListener implements AayahLongClickListener {

    @Override
    public void onLongClick(AayahGroup aayahGroup, int index, View view) {
      showPopupMenu(
          aayahGroup,
          index,
          () -> {
            view.setBackgroundColor(getAttrColor(mA, R.attr.accentTrans3));
            setPopupDismissListener(() -> view.setBackgroundColor(Color.TRANSPARENT));
          });
    }

    @Override
    public void onTextSelected(
        AayahGroup aayahGroup, int index, TextView textView, int start, int end) {
      showPopupMenu(
          aayahGroup,
          index,
          () -> {
            Spanned oldString = (Spanned) textView.getText();
            SpannableString newString = new SpannableString(oldString);
            BackgroundColorSpan span =
                new BackgroundColorSpan(getAttrColor(mA, R.attr.accentTrans3));
            newString.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(newString);
            setPopupDismissListener(() -> textView.setText(oldString));
          });
    }

    private void showPopupMenu(AayahGroup aayahGroup, int index, Runnable callback) {
      AayahContextMenuBinding b = AayahContextMenuBinding.inflate(getLayoutInflater());
      setTooltip(b.copyButton);
      setTooltip(b.shareButton);
      setTooltip(b.bookmarkButton);
      setTooltip(b.addTagButton);
      setTooltip(b.gotoButton);
      setTooltip(b.transButton);

      AayahEntity entity = aayahGroup.entities.get(index);
      String text = aayahGroup.aayahSpans.get(index).text;
      Spanned trans = aayahGroup.aayahSpans.get(index).trans;

      setButtonListener(b.copyButton, () -> shareAayah(entity, trans, true));
      setButtonListener(b.shareButton, () -> shareAayah(entity, trans, false));
      setButtonListener(
          b.addTagButton, () -> Utils.runUi(QuranPageFragment.this, () -> openTags(entity.id)));

      AtomicInteger iconCount = new AtomicInteger(3); // Copy, Share and Tag

      if (mShowingSearchResults) {
        iconCount.addAndGet(1);
        b.gotoButton.setVisibility(View.VISIBLE);
        b.gotoButton.setOnClickListener(
            v -> {
              dismissPopup();
              mA.goTo(entity);
            });
      }

      if (trans != null && !SETTINGS.showTransWithText()) {
        iconCount.addAndGet(1);
        b.transButton.setVisibility(View.VISIBLE);
        b.transButton.setOnClickListener(
            v -> {
              dismissPopup();

              RvItemAayahBinding binding = RvItemAayahBinding.inflate(getLayoutInflater());
              binding.refV.setVisibility(View.GONE);

              int color = mA.getColor(R.color.fgSharp2);
              binding.textV.setTextColor(color);
              binding.transV.setTextColor(color);

              int sizeAr = SETTINGS.getArabicFontSize();
              binding.textV.setTextSize(sizeAr * 1.5f);
              binding.transV.setTextSize(SETTINGS.getFontSize());

              Typeface typeface = SETTINGS.getTypeface();
              Typeface transTypeface = SETTINGS.getTransTypeface();
              binding.textV.setTypeface(typeface);
              binding.transV.setTypeface(transTypeface);

              binding.textV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
              binding.transV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
              binding.textV.setText(text);
              binding.transV.setText(trans);

              NestedScrollView scrollView = new NestedScrollView(mA);
              scrollView.setPadding(toPx(8), toPx(8), toPx(8), toPx(8));
              scrollView.addView(binding.getRoot());
              AlertDialog dialog = new AlertDialog.Builder(mA).setView(scrollView).create();
              AlertDialogFragment.show(mA, dialog, "TEXT_TRANS");
            });
      }

      Utils.runBg(
          () -> {
            if (!SETTINGS.getBookmarks().contains(entity.id)) {
              iconCount.addAndGet(1);
              Utils.runUi(
                  QuranPageFragment.this,
                  () -> {
                    b.bookmarkButton.setVisibility(View.VISIBLE);
                    setButtonListener(b.bookmarkButton, () -> saveBookmark(entity.id));
                  });
            }

            Utils.runUi(
                QuranPageFragment.this,
                () -> {
                  showPopupMenu(b.getRoot(), iconCount.get());
                  callback.run();
                });
          });
    }

    private void showPopupMenu(View contentView, int iconCount) {
      dismissPopup();
      mPopup = new PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mPopup.setElevation(500);
      mPopup.setOverlapAnchor(true);
      mPopup.setOutsideTouchable(true); // Dismiss on outside touch.

      int popupWidth = POPUP_PADDING + iconCount * POPUP_ICON_WIDTH;
      int xOff = Math.max(0, mTapPosX - Utils.toPx(popupWidth / 2));
      int yOff = mTapPosY - Utils.toPx(POPUP_HEIGHT);
      if (yOff < 0) {
        yOff = mTapPosY + Utils.toPx(POPUP_HEIGHT / 2);
      }

      mPopup.showAsDropDown(mB.recyclerV, xOff, yOff);
      mPopupShowing = true;
    }

    private void setButtonListener(View view, Runnable runnable) {
      view.setOnClickListener(v -> Utils.runBg(runnable));
    }

    private void setPopupDismissListener(Runnable callback) {
      if (mPopup != null) {
        mPopup.setOnDismissListener(
            () -> {
              callback.run();
              mPopup = null;
            });
      }
    }
  }

  private void shareAayah(AayahEntity entity, Spanned trans, boolean toClipboard) {
    Utils.runUi(this, this::dismissPopup);

    StringBuilder string = new StringBuilder(entity.text).append("\n\n");
    if (SETTINGS.transEnabled() && trans != null) {
      string.append(trans).append("\n\n");
    }
    SurahEntity surah = SETTINGS.getMetaDb().getSurah(entity.surahNum);
    if (surah != null) {
      string.append(getString(R.string.surah_name, surah.name));
    }
    string.append(": ").append(getArNum(entity.aayahNum));

    if (toClipboard) {
      ClipboardManager clipboard =
          (ClipboardManager) App.getCxt().getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData data = ClipData.newPlainText("aayah", string.toString());
      clipboard.setPrimaryClip(data);
      Utils.showShortToast(R.string.copied);
    } else {
      Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
      intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
      startActivity(
          Intent.createChooser(intent.putExtra(Intent.EXTRA_TEXT, string.toString()), null));
    }
  }

  private void saveBookmark(int aayahId) {
    Utils.runUi(this, this::dismissPopup);
    SETTINGS.addBookmark(aayahId);
    Utils.showToast(R.string.bookmarked);
  }

  private void openTags(int aayahId) {
    dismissPopup();
    TagsDialogFragment frag = new TagsDialogFragment();
    Bundle args = new Bundle();
    args.putInt(AAYAH_ID, aayahId);
    frag.setArguments(args);
    frag.showNow(mA.getSupportFragmentManager(), TagsDialogFragment.PARENT_FRAG_TAG);
  }

  private void dismissPopup() {
    if (mPopup != null) {
      mPopup.dismiss();
      mPopupShowing = false;
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////// IMPLEMENTATIONS /////////////////////////
  //////////////////////////////////////////////////////////////////

  private class RvScaleGestureListener implements OnScaleGestureListener {

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      mOldScaleFactor = 1;
      mRvScaling = true;
      return true;
    }

    private float mOldScaleFactor = 1;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      float scaleFactor = detector.getScaleFactor();
      if (scaleFactor / mOldScaleFactor > 1.1) {
        SETTINGS.increaseFontSize();
      } else if (scaleFactor / mOldScaleFactor < 0.9) {
        SETTINGS.decreaseFontSize();
      } else {
        return false;
      }
      mOldScaleFactor = scaleFactor;
      return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      mRvScaling = false;
    }
  }

  private boolean mRvScaling = false;

  private boolean mToggleFullScreen = false;

  /* We cannot rely on Popup.isShowing() (to decide whether to hide Popup or
    to toggle full screen) because Popup is dismissed on ACTION_DOWN while we
    toggle full screen on ACTION_UP.
    We cannot toggle full screen on ACTION_DOWN because tap might be intended
    to scroll, not to click.
    So we record PopupShowing manually. Still if Popup is dismissed because of
    a tap outside RecyclerView or RvContainer (e.g. on top header or on bottom
    app bar, PopupShowing won't be recorded and an extra tap would be required
    to toggle full screen.
  */
  private boolean mPopupShowing;

  private class RvTouchListener implements OnItemTouchListener {

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
      if (!mShowingSearchResults) {
        mRvScaleGestureDetector.onTouchEvent(e);
      }

      if (mRvScaling) {
        // Now we'll receive events in onTouchEvent()
        return true;
      }

      mTapPosX = (int) e.getX();
      mTapPosY = (int) e.getY();

      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        mToggleFullScreen = true;
      } else if (e.getAction() == MotionEvent.ACTION_UP) {
        if (!mPopupShowing
            && SystemClock.uptimeMillis() - e.getDownTime() < 200
            && mToggleFullScreen) {
          mA.toggleFullScreen(null);
        }
        if (mPopup == null || !mPopup.isShowing()) {
          mPopupShowing = false;
        }
      }
      return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
      mRvScaleGestureDetector.onTouchEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
  }

  private class RvContainerClickListener implements OnClickListener {

    @Override
    public void onClick(View v) {
      if (!mPopupShowing) {
        mA.toggleFullScreen(null);
      }
      if (mPopup == null || !mPopup.isShowing()) {
        mPopupShowing = false;
      }
    }
  }

  private class ScrollListener extends OnScrollListener {

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
      mToggleFullScreen = false;
      if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        mA.toggleFullScreen(true);
      }
      // Update header when scroll ends.
      updateHeader();
    }
  }
}
