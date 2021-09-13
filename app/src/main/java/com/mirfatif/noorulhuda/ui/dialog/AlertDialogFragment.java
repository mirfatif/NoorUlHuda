package com.mirfatif.noorulhuda.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle.State;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.DialogListViewBinding;
import com.mirfatif.noorulhuda.ui.base.BaseActivity;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListCallback;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListItem;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.List;

public class AlertDialogFragment extends AppCompatDialogFragment {

  private static final String TAG = "AlertDialogFragment";

  public static final String DIALOG_TAG = AlertDialogFragment.class.getName() + ".DIALOG_TAG";

  private BaseActivity mA;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (BaseActivity) getActivity();
  }

  private AlertDialog mAlertDialog;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (mAlertDialog == null) {
      mAlertDialog = mA.createDialog(requireArguments().getString(DIALOG_TAG), this);
      if (mAlertDialog == null) {
        dismissAllowingStateLoss();
        return new Builder(mA).create();
      }
    }
    onCreateDialog(mAlertDialog);
    return mAlertDialog;
  }

  // We cannot use Dialog's OnDismiss and OnCancel Listeners, DialogFragment owns them.
  private OnDismissListener mDismissListener;

  @SuppressWarnings("UnusedDeclaration,UnusedReturnValue")
  public AlertDialogFragment setOnDismissListener(OnDismissListener dismissListener) {
    mDismissListener = dismissListener;
    return this;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);
    if (mDismissListener != null) {
      mDismissListener.onDismiss(dialog);
    }
  }

  public void dismissIt() {
    if (!isDetached() && !isRemoving() && !isHidden() && isResumed() && isAdded()) {
      dismissAllowingStateLoss();
    }
  }

  public static AlertDialogFragment show(
      FragmentActivity activity, AlertDialog alertDialog, String tag) {
    return show(activity, new AlertDialogFragment(), alertDialog, tag);
  }

  public static AlertDialogFragment show(
      FragmentActivity activity, AlertDialogFragment frag, AlertDialog alertDialog, String tag) {
    synchronized (AlertDialogFragment.class) {
      FragmentManager manager = activity.getSupportFragmentManager();
      FragmentTransaction transaction = manager.beginTransaction();
      Fragment fragment = manager.findFragmentByTag(tag);
      if (fragment != null) {
        transaction.remove(fragment);
      }

      frag.mAlertDialog = alertDialog;
      Bundle args = new Bundle();
      args.putString(DIALOG_TAG, tag);
      frag.setArguments(args);

      /*
       If Activity is in background, commitNow throws:
         "Can not perform this action after onSaveInstanceState"
       We don't have showNowAllowingStateLoss()
      */
      try {
        if (activity.getLifecycle().getCurrentState().isAtLeast(State.INITIALIZED)
            && !activity.isChangingConfigurations()) {
          frag.showNow(manager, tag);
        }
      } catch (IllegalStateException e) {
        Log.w(TAG, "show: " + e.toString());
      }
      return frag;
    }
  }

  public static void showListDialog(
      @NonNull FragmentActivity activity,
      @StringRes int titleResId,
      @StringRes int emptyResId,
      @NonNull List<DialogListItem> items,
      @NonNull DialogListCallback callback) {
    if (!Utils.isMainThread()) {
      Utils.runUi(
          activity, () -> showListDialog(activity, titleResId, emptyResId, items, callback));
      return;
    }

    AlertDialogFragment dialogFragment = new AlertDialogFragment();
    DialogListViewBinding b = DialogListViewBinding.inflate(activity.getLayoutInflater());
    b.listV.setAdapter(new DialogListAdapter(items, callback, dialogFragment));
    if (emptyResId != 0) {
      b.emptyV.setText(emptyResId);
    }
    b.listV.setEmptyView(b.emptyV);

    Builder builder = new Builder(activity).setTitle(titleResId).setView(b.getRoot());
    show(activity, dialogFragment, builder.create(), "LIST_SELECTOR");
  }

  public static AlertDialog onCreateDialog(AlertDialog dialog) {
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawableResource(R.drawable.alert_dialog_bg_bordered);
      window.setWindowAnimations(android.R.style.Animation_Dialog);
    }
    return dialog;
  }
}
