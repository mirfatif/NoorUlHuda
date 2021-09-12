package com.mirfatif.noorulhuda.feedback;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.FeedbackDialogBinding;
import com.mirfatif.noorulhuda.quran.MainActivity;
import com.mirfatif.noorulhuda.ui.AboutActivity;
import com.mirfatif.noorulhuda.util.Utils;

public class FeedbackDialogFrag extends BottomSheetDialogFragment {

  private MainActivity mA;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setDismissWithAnimation(true);
    return dialog;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    boolean isYes = requireArguments().getBoolean(YES);
    FeedbackDialogBinding b =
        FeedbackDialogBinding.inflate(getLayoutInflater(), container, container != null);

    b.msgV.setText(isYes ? R.string.rate_the_app : R.string.ask_to_provide_feedback);
    b.neutralButton.setText(R.string.do_not_ask);
    b.posButton.setText(isYes ? R.string.rate : R.string.contact);

    b.neutralButton.setOnClickListener(
        v -> {
          SETTINGS.setAskForFeedbackTs(Long.MAX_VALUE);
          dismiss();
        });

    b.posButton.setOnClickListener(
        v -> {
          dismiss();
          if (isYes) {
            Utils.openWebUrl(mA, Utils.getString(R.string.play_store_url));
          } else {
            startActivity(new Intent(App.getCxt(), AboutActivity.class));
          }
          Utils.showToast(R.string.thank_you);
        });

    b.negButton.setOnClickListener(v -> dismiss());

    return b.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    /*
     Replace the default white background with the custom one which has round corners and
     background color set.
     Another option is to override the bottomSheetDialog theme in style.xml

     If directly using setBackgroundResource(), "?android:attr/colorBackground" doesn't honor
     day-night theme. So we get the drawable manually.
    */
    ((View) view.getParent())
        .setBackground(
            ResourcesCompat.getDrawable(App.getRes(), R.drawable.bottom_sheet_bg, mA.getTheme()));
  }

  private static final String YES = "IS_YES";

  public static void show(FragmentActivity activity, boolean isYes) {
    FeedbackDialogFrag frag = new FeedbackDialogFrag();
    Bundle args = new Bundle();
    args.putBoolean(YES, isYes);
    frag.setArguments(args);
    frag.show(activity.getSupportFragmentManager(), "FEEDBACK_RATING");
  }
}
