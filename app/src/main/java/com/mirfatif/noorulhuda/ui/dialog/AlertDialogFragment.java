package com.mirfatif.noorulhuda.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Looper;
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
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.DialogListViewBinding;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListCallback;
import com.mirfatif.noorulhuda.ui.dialog.MyBaseAdapter.DialogListItem;
import com.mirfatif.noorulhuda.util.Utils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlertDialogFragment extends AppCompatDialogFragment {

  private static final String TAG = "AlertDialogFragment";

  public AlertDialogFragment() {}

  private AlertDialog mAlertDialog;

  public AlertDialogFragment(AlertDialog alertDialog) {
    setAlertDialog(alertDialog);
  }

  public AlertDialogFragment setAlertDialog(AlertDialog alertDialog) {
    mAlertDialog = onCreateDialog(alertDialog);
    return this;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return mAlertDialog;
  }

  private static final Set<String> ALL_TAGS = new HashSet<>();

  public void show(FragmentActivity activity, String tag, boolean removeAll) {
    synchronized (AlertDialogFragment.class) {
      FragmentManager manager = activity.getSupportFragmentManager();
      ALL_TAGS.add(tag);

      Set<Fragment> oldDialogs = new HashSet<>();
      if (removeAll) {
        oldDialogs = buildListToRemove(manager);
      } else {
        Fragment fragment = manager.findFragmentByTag(tag);
        if (fragment != null) {
          oldDialogs.add(fragment);
        }
      }

      Log.d(TAG, "Showing " + tag);

      /* If Activity is in background, commitNow throws:
       * "Can not perform this action after onSaveInstanceState"
       * We don't have showNowAllowingStateLoss()
       */
      try {
        if (!activity.isFinishing()
            && !activity.isDestroyed()
            && !activity.isChangingConfigurations()) {
          super.showNow(manager, tag);
        }
      } catch (IllegalStateException e) {
        Log.w(TAG, "show: " + e.toString());
      }

      removeFragments(manager, oldDialogs);
    }
  }

  public void dismissIt() {
    if (!isDetached() && !isRemoving() && !isHidden() && isResumed() && isAdded()) {
      dismissAllowingStateLoss();
    }
  }

  public static void removeAll(FragmentActivity activity) {
    FragmentManager manager = activity.getSupportFragmentManager();
    removeFragments(manager, buildListToRemove(manager));
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    /* Do not call super because:
     * 1. We cannot recreate DialogFragment after configuration change.
     * 2. We don't have showNowAllowingStateLoss()
     */
  }

  private static Set<Fragment> buildListToRemove(FragmentManager manager) {
    Set<Fragment> oldDialogs = new HashSet<>();
    Fragment fragment;
    for (String tag : ALL_TAGS) {
      fragment = manager.findFragmentByTag(tag);
      if (fragment != null) {
        Log.d(TAG, "Old dialog: " + tag);
        oldDialogs.add(fragment);
      }
    }
    return oldDialogs;
  }

  private static void removeFragments(FragmentManager manager, Set<Fragment> fragments) {
    FragmentTransaction transaction = manager.beginTransaction();
    Log.d(TAG, "Removing old dialogs");
    for (Fragment fragment : fragments) {
      transaction.remove(fragment);
    }
    transaction.commitNowAllowingStateLoss();
  }

  // We cannot use Dialog's OnDismiss and OnCancel Listeners, DialogFragment owns them.
  private OnDismissListener mDismissListener;

  @SuppressWarnings("UnusedDeclaration")
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

  public static void showListDialog(
      @NonNull FragmentActivity activity,
      @StringRes int titleResId,
      @StringRes int emptyResId,
      @NonNull List<DialogListItem> items,
      @NonNull DialogListCallback callback) {
    if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
      Utils.runUi(() -> showListDialog(activity, titleResId, emptyResId, items, callback));
      return;
    }

    DialogListViewBinding b = DialogListViewBinding.inflate(activity.getLayoutInflater());
    Builder builder = new Builder(activity).setTitle(titleResId).setView(b.getRoot());
    AlertDialogFragment dialog = new AlertDialogFragment(builder.create());
    b.listV.setAdapter(new DialogListAdapter(activity, items, callback, dialog));
    if (emptyResId != 0) {
      b.emptyV.setText(emptyResId);
    }
    b.listV.setEmptyView(b.emptyV);
    dialog.show(activity, "LIST_SELECTOR", false);
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
