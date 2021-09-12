package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.tags.TagsDialogFragment.AAYAH_ID;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;
import static com.mirfatif.noorulhuda.util.Utils.getAttrColor;
import static com.mirfatif.noorulhuda.util.Utils.setTooltip;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.mirfatif.noorulhuda.db.AayahEntity;
import com.mirfatif.noorulhuda.db.DbBuilder;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.SurahEntity;
import com.mirfatif.noorulhuda.quran.AayahAdapter.Aayah;
import com.mirfatif.noorulhuda.quran.AayahAdapter.AayahLongClickListener;
import com.mirfatif.noorulhuda.quran.AayahAdapter.AayahViewHolder;
import com.mirfatif.noorulhuda.quran.AayahAdapter.ItemViewHolder;
import com.mirfatif.noorulhuda.quran.AayahAdapter.SpanMarks;
import com.mirfatif.noorulhuda.tags.TagsDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class QuranPageFragment extends Fragment {

  private static final String TAG = "QuranPageFragment";

  private MainActivity mA;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
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

    mAayahAdapter = new AayahAdapter(this, new LongClickListener());
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

  private void refreshUi() {
    mB.recyclerV.removeItemDecoration(getDivider());
    if (SETTINGS.showTranslation() || SETTINGS.isSearchStarted()) {
      mB.recyclerV.addItemDecoration(getDivider());
    }

    if (SETTINGS.isPageMode()) {
      Utils.runBg(this::submitPageAayahs);
    } else {
      mAayahAdapter.submitList(Arrays.asList(new Aayah[DbBuilder.TOTAL_AAYAHS]));
      // Restore scroll position.
      scrollToPos(SETTINGS.getLastAayah(), 0, false);
    }
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// GENERAL ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private final List<Aayah> mAayahs = new ArrayList<>();
  private final Map<Integer, Integer> mIdPosMap = new HashMap<>();

  private void submitPageAayahs() {
    int page = requireArguments().getInt(QuranPageAdapter.KEY_PAGE);
    List<Aayah> aayahs = new ArrayList<>();
    List<AayahEntity> entities = SETTINGS.getQuranDb().getAayahEntities(page);
    mAayahs.clear();
    mIdPosMap.clear();
    if (SETTINGS.showSingleAayah()) {
      for (int i = 0; i < entities.size(); i++) {
        AayahEntity entity = entities.get(i);
        Aayah aayah = new Aayah();
        mAayahs.add(aayah);
        aayah.entities.add(entity);
        aayahs.add(aayah);
        mIdPosMap.put(entity.id, i);
      }
    } else {
      int lastSurahNum = 0;
      Aayah aayah = null;
      int itemPos = -1;
      for (int i = 0; i < entities.size(); i++) {
        AayahEntity entity = entities.get(i);
        if (entity.surahNum != lastSurahNum) {
          if (aayah != null) {
            aayahs.add(aayah);
          }
          if (entity.aayahNum == 0) {
            lastSurahNum = 0;
          } else {
            lastSurahNum = entity.surahNum;
          }
          aayah = new Aayah();
          mAayahs.add(aayah);
          itemPos++;
        }
        if (aayah != null) {
          aayah.entities.add(entity);
          mIdPosMap.put(entity.id, itemPos);
        }
      }
      if (aayah != null) {
        aayahs.add(aayah);
      }
    }
    Integer aayahId = mA.getScrollPos(page);
    Utils.runUi(
        this,
        () -> {
          mAayahAdapter.submitList(aayahs);
          if (aayahId != null) {
            scrollToAayahId(aayahId);
          }
        });
  }

  private final Handler SCROLLER = new Handler(Looper.getMainLooper());

  // Continuous mode
  void scrollToPos(int pos, int offset, boolean highlight) {
    if (mLayoutManager != null) {
      mLayoutManager.scrollToPositionWithOffset(pos, offset);
      SCROLLER.postDelayed(() -> mLayoutManager.scrollToPositionWithOffset(pos, offset), 100);
      if (highlight) {
        SCROLLER.postDelayed(() -> highlight(pos), 1000);
      }
    }
  }

  private void highlight(int pos) {
    ItemViewHolder h = (ItemViewHolder) mB.recyclerV.findViewHolderForAdapterPosition(pos);
    View v;
    if (h != null && (v = h.getView()) != null) {
      v.setVisibility(View.INVISIBLE);
      v.postDelayed(() -> v.setVisibility(View.VISIBLE), 250);
      v.postDelayed(() -> v.setVisibility(View.INVISIBLE), 500);
      v.postDelayed(() -> v.setVisibility(View.VISIBLE), 750);
    }
  }

  // Page mode
  void scrollToAayahId(int aayahId) {
    Integer pos = mIdPosMap.get(aayahId);
    if (pos == null) {
      return;
    }
    if (SETTINGS.showSingleAayah() || SETTINGS.isTasmia(aayahId)) {
      scrollToPos(pos, 0, true);
    } else {
      Utils.runBg(() -> highlightAayah(aayahId, pos));
    }
  }

  private void highlightAayah(int aayahId, int pos) {
    SystemClock.sleep(1000);
    if (mAayahs.size() > pos) {
      Aayah aayah = mAayahs.get(pos);
      for (SpanMarks marks : aayah.aayahSpans) {
        if (marks.entity.id == aayahId) {
          Utils.runUi(this, () -> highlightAayah(marks, pos));
          break;
        }
      }
    }
  }

  private void highlightAayah(SpanMarks marks, int pos) {
    ViewHolder h = mB.recyclerV.findViewHolderForAdapterPosition(pos);
    if (!(h instanceof AayahViewHolder)) {
      return;
    }
    TextView tv = ((AayahViewHolder) h).getTextView();
    if (tv == null) {
      return;
    }

    int line = tv.getLayout().getLineForOffset(marks.start);
    int offset = tv.getLineHeight() * line;
    scrollToPos(pos, -offset, false);

    Spanned oldString = (Spanned) tv.getText();
    SpannableString newString = new SpannableString(oldString);
    InvisibleFontSpan span = new InvisibleFontSpan();
    newString.setSpan(span, marks.start, marks.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    tv.postDelayed(() -> tv.setText(newString), 250);
    tv.postDelayed(() -> tv.setText(oldString), 500);
    tv.postDelayed(() -> tv.setText(newString), 750);
    tv.postDelayed(() -> tv.setText(oldString), 1000);
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

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// SEARCH /////////////////////////////
  //////////////////////////////////////////////////////////////////

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
    entities.sort(Comparator.comparingInt(a -> a.id));

    List<Aayah> aayahs = new ArrayList<>();
    for (AayahEntity entity : entities) {
      if (Thread.interrupted()) {
        return;
      }
      Aayah aayah = new Aayah();
      aayah.entities.add(entity);
      aayahs.add(aayah);
    }
    Utils.runUi(
        this,
        () -> {
          mAayahAdapter.submitList(aayahs);
          mA.setProgBarVisibility(false);
        });

    stopToastTask();
    Runnable task = () -> Utils.showShortToast(R.string.results, aayahs.size());
    mToastFuture = TOASTER.schedule(task, 1500, TimeUnit.MILLISECONDS);
  }

  public Set<Integer> containsORed(String queryText) throws InterruptedException {
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
    if (SETTINGS.getSearchInTranslation() && SETTINGS.showTranslation()) {
      db = SETTINGS.getTransDb();
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

  private static final ReentrantLock UPDATE_HEADER_LOCK = new ReentrantLock();
  private long mLastHeaderUpdate;

  private void releaseUpdateHeaderLock() {
    if (UPDATE_HEADER_LOCK.isHeldByCurrentThread()) {
      UPDATE_HEADER_LOCK.unlock();
    }
  }

  private void updateHeader() {
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      Utils.runBg(this::updateHeader);
      return;
    }

    if (!UPDATE_HEADER_LOCK.tryLock()) {
      return;
    }

    long sleepTime = 500 + mLastHeaderUpdate - System.currentTimeMillis();
    if (sleepTime > 0) {
      SystemClock.sleep(sleepTime);
    }

    int topItem = mLayoutManager.findFirstVisibleItemPosition();
    if (topItem != RecyclerView.NO_POSITION) {
      Aayah aayah;
      AayahEntity entity;
      if ((aayah = mAayahAdapter.getAayah(topItem)) != null && !aayah.entities.isEmpty()) {
        entity = aayah.entities.get(0);
      } else {
        entity = SETTINGS.getQuranDb().getAayahEntity(topItem);
      }

      if (entity != null) {
        SurahEntity surah = SETTINGS.getMetaDb().getSurah(entity.surahNum);
        mLastHeaderUpdate = System.currentTimeMillis();
        releaseUpdateHeaderLock();
        Utils.runUi(this, () -> mA.updateHeader(entity, surah));
      } else {
        Log.e(TAG, "updateHeader: failed to get AayahEntity");
      }
    } else {
      Log.e(TAG, "updateHeaderLocked: failed to get Aayah");
    }

    releaseUpdateHeaderLock();
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////////// LONG CLICK ///////////////////////////
  //////////////////////////////////////////////////////////////////

  private static final int POPUP_WIDTH = 200, POPUP_HEIGHT = 100;
  private PopupWindow mPopup;
  private int mTapPosX, mTapPosY;

  /* Positioning a window on screen: https://gist.github.com/romannurik/3982005.
    Other option is to use ActionMode with ActionMode.TYPE_FLOATING. But it's
    available only on SDK 23+. Also FloatingToolbar's background cannot be customized.
  */
  private class LongClickListener implements AayahLongClickListener {

    @Override
    public void onLongClick(AayahEntity entity, String trans, View view) {
      showPopupMenu(
          entity,
          trans,
          () -> {
            view.setBackgroundColor(getAttrColor(mA, R.attr.accentTrans3));
            setPopupDismissListener(() -> view.setBackgroundColor(Color.TRANSPARENT));
          });
    }

    @Override
    public void onTextSelected(AayahEntity entity, TextView textView, int start, int end) {
      showPopupMenu(
          entity,
          null,
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

    private void showPopupMenu(AayahEntity entity, String trans, Runnable callback) {
      AayahContextMenuBinding b = AayahContextMenuBinding.inflate(getLayoutInflater());
      setTooltip(b.copyButton);
      setTooltip(b.shareButton);
      setTooltip(b.bookmarkButton);
      setTooltip(b.addTagButton);

      setButtonListener(b.copyButton, () -> shareAayah(entity, trans, true));
      setButtonListener(b.shareButton, () -> shareAayah(entity, trans, false));
      setButtonListener(b.bookmarkButton, () -> saveBookmark(entity.id));
      setButtonListener(
          b.addTagButton, () -> Utils.runUi(QuranPageFragment.this, () -> openTags(entity.id)));

      AtomicBoolean hideBookmarkButton = new AtomicBoolean(false);

      Utils.runBg(
          () -> {
            if (SETTINGS.getBookmarks().contains(entity.id)) {
              hideBookmarkButton.set(true);
            }
            Utils.runUi(
                QuranPageFragment.this,
                () -> {
                  if (hideBookmarkButton.get()) {
                    b.bookmarkButton.setVisibility(View.GONE);
                  }
                  showPopupMenu(b.getRoot(), hideBookmarkButton.get());
                  callback.run();
                });
          });
    }

    private void showPopupMenu(View contentView, boolean reduceWidth) {
      dismissPopup();
      mPopup = new PopupWindow(contentView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mPopup.setElevation(500);
      mPopup.setOverlapAnchor(true);
      mPopup.setOutsideTouchable(true); // Dismiss on outside touch.

      int popupWidth = POPUP_WIDTH;
      if (reduceWidth) {
        popupWidth -= popupWidth / 4;
      }

      int xOff = mTapPosX - Utils.toPx(popupWidth);
      int yOff = mTapPosY - Utils.toPx(POPUP_HEIGHT);
      if (xOff < 0) {
        xOff = mTapPosX + Utils.toPx(popupWidth / 4);
      }
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

  private void shareAayah(AayahEntity entity, String trans, boolean toClipboard) {
    Utils.runUi(this, this::dismissPopup);

    StringBuilder string = new StringBuilder(entity.text).append("\n\n");
    if (SETTINGS.showTranslation() && trans != null) {
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
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

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
