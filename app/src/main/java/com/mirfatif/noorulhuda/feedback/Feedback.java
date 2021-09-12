package com.mirfatif.noorulhuda.feedback;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.ActivityMainBinding;
import com.mirfatif.noorulhuda.feedback.MySwipeDismissBehavior.OnDismissListener;
import com.mirfatif.noorulhuda.quran.MainActivity;
import com.mirfatif.noorulhuda.util.Utils;

public class Feedback {

  private final MainActivity mA;
  private final ActivityMainBinding mB;

  public Feedback(MainActivity activity) {
    mA = activity;
    mB = mA.getRootView();
  }

  public void askForFeedback() {
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      Utils.runUi(mA, this::askForFeedback);
      return;
    }
    if (SETTINGS.shouldAskForFeedback()) {
      mB.bottomBar.feedbackCont.setVisibility(View.VISIBLE);
    }

    if (mB.bottomBar.feedbackCont.getVisibility() != View.VISIBLE) {
      return;
    }

    mB.bottomBar.likingAppYesButton.setOnClickListener(v -> showDialog(true));
    mB.bottomBar.likingAppNoButton.setOnClickListener(v -> showDialog(false));

    OnDismissListener listener = () -> mB.bottomBar.feedbackCont.setVisibility(View.GONE);
    MySwipeDismissBehavior dismissBehavior = new MySwipeDismissBehavior(listener);
    ((LayoutParams) mB.bottomBar.feedbackCont.getLayoutParams()).setBehavior(dismissBehavior);

    Animation animation = AnimationUtils.loadAnimation(App.getCxt(), R.anim.shake);
    Runnable shake = () -> mB.bottomBar.feedbackCont.startAnimation(animation);
    mB.bottomBar.feedbackCont.postDelayed(shake, 1000);
  }

  private void showDialog(boolean isYes) {
    FeedbackDialogFrag.show(mA, isYes);
    mB.bottomBar.feedbackCont.setVisibility(View.GONE);
  }
}
