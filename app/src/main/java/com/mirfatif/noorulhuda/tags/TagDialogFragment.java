package com.mirfatif.noorulhuda.tags;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.quran.AayahAdapter.removeUnsupportedChars;
import static com.mirfatif.noorulhuda.tags.TagsDialogFragment.PARENT_FRAG_TAG;
import static com.mirfatif.noorulhuda.util.Utils.getArNum;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.common.collect.Iterables;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.TagDialogViewBinding;
import com.mirfatif.noorulhuda.db.AayahEntity;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.SurahEntity;
import com.mirfatif.noorulhuda.db.TagAayahsDao;
import com.mirfatif.noorulhuda.db.TagEntity;
import com.mirfatif.noorulhuda.db.TagsDao;
import com.mirfatif.noorulhuda.quran.MainActivity;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListCallback;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListItem;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TagDialogFragment extends AppCompatDialogFragment {

  public static final String TAG_ID = "TAG_ID";
  public static final String CREATED_NEW = "CREATED_NEW";

  private static final int TITLE_LEN = 50;

  private final TagsDao mTagsDb = SETTINGS.getTagsDb();
  private final TagAayahsDao mTagAayahsDb = SETTINGS.getTagAayahsDb();

  private MainActivity mA;
  private TagsDialogFragment mTagsListFrag;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();

    Fragment frag = getParentFragmentManager().findFragmentByTag(PARENT_FRAG_TAG);
    if (frag instanceof TagsDialogFragment) {
      mTagsListFrag = (TagsDialogFragment) frag;
    }
  }

  private TagDialogViewBinding mB;
  private TagAayahsAdapter mAdapter;
  private Button mSaveButton;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    mB = TagDialogViewBinding.inflate(getLayoutInflater());

    Bundle args = getArguments();
    boolean isNew = args != null && args.getBoolean(CREATED_NEW, false);

    if (!isNew) {
      mAdapter = new TagAayahsAdapter(new DialogListCallbackImpl());
      mB.recyclerV.setAdapter(mAdapter);
      mB.recyclerV.setLayoutManager(new LinearLayoutManager(mA));
      mB.recyclerV.addItemDecoration(new DividerItemDecoration(mA, DividerItemDecoration.VERTICAL));
    }

    Utils.runBg(this::createUi);

    Builder builder =
        new Builder(mA)
            .setPositiveButton(R.string.save, (d, which) -> saveTagDetails())
            .setView(mB.getRoot());

    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(
        d -> {
          mSaveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
          mSaveButton.setVisibility(View.GONE);
        });
    return AlertDialogFragment.onCreateDialog(dialog);
  }

  private TagEntity mTag;

  private void saveTagDetails() {
    CharSequence title = mB.titleV.getText();
    mTag.title = title == null ? null : title.toString();
    CharSequence desc = mB.descV.getText();
    mTag.desc = desc == null ? null : desc.toString();
    Utils.runBg(
        () -> {
          mTagsDb.updateTag(mTag);
          if (mRemovedAayahs.size() > 0) {
            for (List<Integer> ids : Iterables.partition(mRemovedAayahs, 999)) {
              mTagAayahsDb.remove(mTag.id, ids);
            }
          }
          if (mTagsListFrag != null) {
            mTagsListFrag.submitList();
          }
        });
  }

  private long mLastClick;
  private int mLastView;
  private final Runnable TOASTER = () -> Utils.showShortToast(R.string.double_click_to_edit);
  private final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> mToastFuture;

  private final OnClickListener EDIT_LISTENER =
      v -> {
        boolean ret = mLastView != v.hashCode() || System.currentTimeMillis() - mLastClick > 500;
        mLastView = v.hashCode();
        mLastClick = System.currentTimeMillis();
        if (mToastFuture != null) {
          mToastFuture.cancel(false);
        }
        if (ret) {
          mToastFuture = EXECUTOR.schedule(TOASTER, 500, TimeUnit.MILLISECONDS);
        } else if (v instanceof EditText) {
          openKeyboard((EditText) v);
        }
      };

  private void setFocusable(EditText view) {
    view.setFocusable(true);
    view.setFocusableInTouchMode(true);
    view.setOnClickListener(null);
  }

  private void openKeyboard(EditText view) {
    setFocusable(mB.titleV);
    setFocusable(mB.descV);
    view.requestFocus();
    InputMethodManager imm =
        (InputMethodManager) App.getCxt().getSystemService(Context.INPUT_METHOD_SERVICE);
    view.postDelayed(() -> imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT), 100);
  }

  private List<AayahEntity> mAayahs;
  private final List<Integer> mRemovedAayahs = new ArrayList<>();

  private void createUi() {
    Bundle args = getArguments();
    int tagId;
    if (args == null
        || (tagId = args.getInt(TAG_ID, -1)) < 0
        || (mTag = mTagsDb.getTag(tagId)) == null) {
      dismissAllowingStateLoss();
      return;
    }

    boolean isNew = args.getBoolean(CREATED_NEW, false);

    Utils.runUi(
        this,
        () -> {
          mB.titleV.setText(mTag.title);
          mB.descV.setText(mTag.desc);

          mB.titleV.setOnClickListener(EDIT_LISTENER);
          mB.descV.setOnClickListener(EDIT_LISTENER);
          mB.titleV.addTextChangedListener(new TitleWatcher(true));
          mB.descV.addTextChangedListener(new TitleWatcher(false));

          if (isNew) {
            mB.titleV.post(
                () -> {
                  openKeyboard(mB.titleV);
                  mB.titleV.selectAll();
                });
          }
        });

    if (!isNew) {
      List<DialogListItem> items = new ArrayList<>();
      mTag.aayahIds.addAll(mTagAayahsDb.getAayahIds(mTag.id));

      mAayahs = QuranDao.getAayahEntities(SETTINGS.getQuranDb(), mTag.aayahIds);

      for (AayahEntity aayah : mAayahs) {
        SurahEntity surah;
        if (aayah == null || (surah = SETTINGS.getMetaDb().getSurah(aayah.surahNum)) == null) {
          continue;
        }
        DialogListItem item = new DialogListItem();
        item.title = getString(R.string.surah_name, surah.name);
        item.subTitle = getArNum(aayah.aayahNum);
        item.text = removeUnsupportedChars(aayah.text);
        items.add(item);
      }
      Utils.runUi(this, () -> mAdapter.submitList(items));
    }
  }

  private void setButtonVisibility() {
    if (mSaveButton == null) {
      return;
    }
    String desc = mB.descV.getText() == null ? "" : mB.descV.getText().toString();
    if (!mRemovedAayahs.isEmpty()
        || !mTag.title.contentEquals(mB.titleV.getText())
        || !desc.equals(mTag.desc == null ? "" : mTag.desc)) {
      mSaveButton.setVisibility(View.VISIBLE);
    } else {
      mSaveButton.setVisibility(View.GONE);
    }
  }

  private class TitleWatcher implements TextWatcher {

    private final boolean mIsTitle;

    TitleWatcher(boolean isTitle) {
      mIsTitle = isTitle;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      setButtonVisibility();
      if (mIsTitle) {
        mB.titleLenV.setVisibility(View.VISIBLE);
      } else {
        return;
      }

      if (TextUtils.isEmpty(s)) {
        if (mSaveButton != null) {
          mSaveButton.setEnabled(false);
        }
        mB.titleV.setError(getString(R.string.required), null);
        mB.titleV.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        mB.titleLenV.setText(String.valueOf(TITLE_LEN));
      } else {
        if (mSaveButton != null) {
          mSaveButton.setEnabled(true);
        }
        mB.titleLenV.setText(String.valueOf(TITLE_LEN - s.length()));
      }
    }

    @Override
    public void afterTextChanged(Editable s) {}
  }

  private class DialogListCallbackImpl implements DialogListCallback {

    @Override
    public void onItemSelect(int pos) {
      if (mTagsListFrag != null) {
        mTagsListFrag.dismissAllowingStateLoss();
      }
      dismissAllowingStateLoss();
      mA.goTo(mAayahs.get(pos));
    }

    @Override
    public void onDelete(int pos) {
      mRemovedAayahs.add(mAayahs.get(pos).id);
      mAayahs.remove(pos);
      setButtonVisibility();
    }
  }
}
