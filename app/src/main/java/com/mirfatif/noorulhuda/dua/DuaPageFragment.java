package com.mirfatif.noorulhuda.dua;

import static com.mirfatif.noorulhuda.dua.DuaPageAdapter.DUA_TYPE;
import static com.mirfatif.noorulhuda.dua.DuaPageAdapter.DUA_TYPE_MASNOON;
import static com.mirfatif.noorulhuda.dua.DuaPageAdapter.DUA_TYPE_OCCASIONS;
import static com.mirfatif.noorulhuda.dua.DuaPageAdapter.DUA_TYPE_QURANIC;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.quran.QuranPageFragment.POPUP_HEIGHT;
import static com.mirfatif.noorulhuda.quran.QuranPageFragment.POPUP_ICON_WIDTH;
import static com.mirfatif.noorulhuda.quran.QuranPageFragment.POPUP_PADDING;
import static com.mirfatif.noorulhuda.util.Utils.setTooltip;
import static com.mirfatif.noorulhuda.util.Utils.toPx;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
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
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.AayahContextMenuBinding;
import com.mirfatif.noorulhuda.databinding.RecyclerViewBinding;
import com.mirfatif.noorulhuda.databinding.RvItemAayahBinding;
import com.mirfatif.noorulhuda.dua.DuasAdapter.Dua;
import com.mirfatif.noorulhuda.dua.DuasAdapter.DuaLongClickListener;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.List;

public class DuaPageFragment extends Fragment {

  private DuaActivity mA;
  private ScaleGestureDetector mRvScaleGestureDetector;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (DuaActivity) getActivity();
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

  private RecyclerViewBinding mB;
  private LinearLayoutManager mLayoutManager;
  private DuasAdapter mAdapter;
  private int mDuaType;

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mDuaType = requireArguments().getInt(DUA_TYPE);

    mLayoutManager = new LinearLayoutManager(mA);
    mB.recyclerV.setLayoutManager(mLayoutManager);

    mAdapter = new DuasAdapter(new DuaLongClickImpl());
    Utils.runBg(this::submitList);

    mB.recyclerV.setAdapter(mAdapter);
    mB.recyclerV.addItemDecoration(new DividerItemDecoration(mA, DividerItemDecoration.VERTICAL));
    mB.recyclerV.addOnItemTouchListener(new RvTouchListener());
    mB.recyclerV.addOnScrollListener(new ScrollListener());
  }

  private void submitList() {
    String[] titles = null, texts, trans;
    int lastDua;

    if (mDuaType == DUA_TYPE_QURANIC) {
      texts = App.getRes().getStringArray(R.array.quranic_duas);
      trans = SETTINGS.getQuranicDuaTrans();
      lastDua = SETTINGS.getLastQuranicDua();
    } else if (mDuaType == DUA_TYPE_MASNOON) {
      texts = App.getRes().getStringArray(R.array.masnoon_duas);
      trans = SETTINGS.getMasnoonDuaTrans();
      lastDua = SETTINGS.getLastMasnoonDua();
    } else {
      titles = SETTINGS.getDuaTitles();
      texts = App.getRes().getStringArray(R.array.occasion_duas);
      trans = SETTINGS.getOccasionsDuaTrans();
      lastDua = SETTINGS.getLastOccasionsDua();
    }

    List<Dua> duas = new ArrayList<>();
    for (int i = 0; i < texts.length; i++) {
      String text = texts[i];
      Dua dua = new Dua();
      String[] splitText = text.split("\\|");
      dua.text = splitText[0];
      if (mDuaType == DUA_TYPE_QURANIC) {
        dua.surahNum = Integer.parseInt(splitText[1]);
        dua.aayahNum = Integer.parseInt(splitText[2]);
        dua.ref = getString(R.string.surah_name, SETTINGS.getMetaDb().getSurahName(dua.surahNum));
      } else {
        dua.ref = splitText[1];
      }
      if (titles != null && mDuaType == DUA_TYPE_OCCASIONS) {
        dua.title = titles[i];
      }
      if (trans != null) {
        dua.trans = trans[i];
      }
      duas.add(dua);
    }
    Utils.runUi(this, () -> submitList(duas, lastDua));
  }

  private void submitList(List<Dua> duas, int lastDua) {
    mAdapter.submitList(duas);
    mLayoutManager.scrollToPositionWithOffset(lastDua, 0);
    mB.recyclerV.addOnScrollListener(
        new OnScrollListener() {
          @Override
          public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
            mA.setCurrentDua(mLayoutManager.findFirstVisibleItemPosition(), mDuaType);
          }
        });
  }

  boolean onBackPressed() {
    if (mPopup != null) {
      mPopup.dismiss();
      return true;
    }
    return false;
  }

  void resetFontSize() {
    if (mAdapter != null) {
      mAdapter.resetFontSize();
    }
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////////// LONG CLICK ///////////////////////////
  //////////////////////////////////////////////////////////////////

  private PopupWindow mPopup;
  private int mTapPosX, mTapPosY;

  private class DuaLongClickImpl implements DuaLongClickListener {

    @Override
    public void onLongClick(Dua dua, View view) {
      AayahContextMenuBinding b = AayahContextMenuBinding.inflate(getLayoutInflater());
      setTooltip(b.copyButton);
      setTooltip(b.shareButton);
      setTooltip(b.gotoButton);
      setTooltip(b.transButton);

      b.addTagButton.setVisibility(View.GONE);

      b.copyButton.setOnClickListener(v -> shareDua(dua, true));
      b.shareButton.setOnClickListener(v -> shareDua(dua, false));

      int iconCount = 2; // Copy and Share

      if (mDuaType == DUA_TYPE_QURANIC) {
        iconCount++;
        b.gotoButton.setVisibility(View.VISIBLE);
        b.gotoButton.setOnClickListener(
            v -> {
              mPopup.dismiss();
              mA.goTo(dua);
            });
      }

      if (dua.trans != null && !SETTINGS.showTransWithText()) {
        iconCount++;
        b.transButton.setVisibility(View.VISIBLE);
        b.transButton.setOnClickListener(
            v -> {
              mPopup.dismiss();

              RvItemAayahBinding binding = RvItemAayahBinding.inflate(getLayoutInflater());
              int color = mA.getColor(R.color.fgSharp2);
              binding.titleV.setTextColor(color);
              binding.textV.setTextColor(color);
              binding.transV.setTextColor(color);
              binding.refV.setTextColor(color);

              int sizeAr = SETTINGS.getArabicFontSize();
              binding.titleV.setTextSize(sizeAr);
              binding.textV.setTextSize(sizeAr * 1.5f);
              binding.transV.setTextSize(SETTINGS.getFontSize());
              binding.refV.setTextSize(sizeAr * 0.8f);

              Typeface typeface = SETTINGS.getTypeface();
              Typeface transTypeface = SETTINGS.getTransTypeface();
              binding.textV.setTypeface(typeface);
              binding.titleV.setTypeface(transTypeface);
              binding.transV.setTypeface(transTypeface);
              binding.refV.setTypeface(typeface);

              binding.textV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
              binding.transV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

              if (dua.title != null) {
                binding.titleV.setText(dua.title);
                binding.titleV.setVisibility(View.VISIBLE);
              }
              binding.textV.setText(dua.text);
              binding.transV.setText(dua.trans);
              binding.refV.setText(dua.ref);

              NestedScrollView scrollView = new NestedScrollView(mA);
              scrollView.setPadding(toPx(8), toPx(8), toPx(8), toPx(8));
              scrollView.addView(binding.getRoot());
              AlertDialog dialog = new AlertDialog.Builder(mA).setView(scrollView).create();
              AlertDialogFragment.show(mA, dialog, "TEXT_TRANS");
            });
      }

      if (mPopup != null) {
        mPopup.dismiss();
      }
      mPopup = new PopupWindow(b.getRoot(), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      mPopup.setElevation(500);
      mPopup.setOverlapAnchor(true);
      mPopup.setOutsideTouchable(true); // Dismiss on outside touch.

      view.setBackgroundColor(Utils.getAttrColor(mA, R.attr.accentTrans3));
      mPopup.setOnDismissListener(
          () -> {
            view.setBackgroundColor(Color.TRANSPARENT);
            mPopup = null;
          });

      int popupWidth = POPUP_PADDING + iconCount * POPUP_ICON_WIDTH;
      int xOff = Math.max(0, mTapPosX - Utils.toPx(popupWidth / 2));
      int yOff = mTapPosY - Utils.toPx(POPUP_HEIGHT);
      if (yOff < 0) {
        yOff = mTapPosY + Utils.toPx(POPUP_HEIGHT / 2);
      }

      mPopup.showAsDropDown(mB.recyclerV, xOff, yOff);
    }
  }

  private void shareDua(Dua dua, boolean toClipboard) {
    mPopup.dismiss();

    StringBuilder string = new StringBuilder();
    if (dua.title != null) {
      string.append(dua.title).append("\n\n");
    }
    string.append(dua.text).append("\n\n");
    if (dua.trans != null) {
      string.append(dua.trans).append("\n\n");
    }
    string.append(dua.ref);

    if (toClipboard) {
      ClipboardManager clipboard =
          (ClipboardManager) App.getCxt().getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData data = ClipData.newPlainText("dua", string.toString());
      clipboard.setPrimaryClip(data);
      Utils.showShortToast(R.string.copied);
    } else {
      Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
      intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
      startActivity(
          Intent.createChooser(intent.putExtra(Intent.EXTRA_TEXT, string.toString()), null));
    }
  }

  private class RvScaleGestureListener implements OnScaleGestureListener {

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      mScaleFactor = 1;
      mRvScaling = true;
      return true;
    }

    private float mScaleFactor = 1;

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      float scaleFactor = detector.getScaleFactor();
      if (scaleFactor / mScaleFactor > 1.1) {
        SETTINGS.increaseFontSize();
      } else if (scaleFactor / mScaleFactor < 0.9) {
        SETTINGS.decreaseFontSize();
      } else {
        return false;
      }
      mScaleFactor = scaleFactor;
      return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      mRvScaling = false;
    }
  }

  private boolean mRvScaling = false;

  private class RvTouchListener implements OnItemTouchListener {

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
      mRvScaleGestureDetector.onTouchEvent(e);
      if (mRvScaling) {
        return true;
      }

      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        mScrolling = false;
      } else if (e.getAction() == MotionEvent.ACTION_UP
          && !mScrolling
          && (mPopup == null || !mPopup.isShowing())
          && SystemClock.uptimeMillis() - e.getDownTime() < 200) {
        mA.toggleFullScreen(null);
      }

      mTapPosX = (int) e.getX();
      mTapPosY = (int) e.getY();

      return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
      mRvScaleGestureDetector.onTouchEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
  }

  private boolean mScrolling = false;

  private class ScrollListener extends OnScrollListener {

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
      mScrolling = true;
      if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        mA.toggleFullScreen(true);
      }
    }
  }
}
