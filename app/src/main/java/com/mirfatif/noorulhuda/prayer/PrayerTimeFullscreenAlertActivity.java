package com.mirfatif.noorulhuda.prayer;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.svc.PrayerAdhanSvc;

public class PrayerTimeFullscreenAlertActivity extends AppCompatActivity {

  private String whichAdhan = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    showWhenLockedAndTurnScreenOn();
    super.onCreate(savedInstanceState);

    this.whichAdhan = getIntent().getStringExtra("whichAdhan");
    setContentView(R.layout.activity_prayer_time_fullscreen_alert);

    setAdhanLabel();
  }

  private void showWhenLockedAndTurnScreenOn() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);
    }

    // Deprecated flags are required on some devices, even with API>=27
    getWindow()
        .addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
  }

  public void dismiss(View view) {
    try {
      PrayerAdhanSvc.createStopSvcIntent().send();
    } catch (PendingIntent.CanceledException e) {
      e.printStackTrace();
    }
    this.onBackPressed();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    this.whichAdhan = getIntent().getExtras().getString("whichAdhan");
    this.setAdhanLabel();
  }

  private void setAdhanLabel() {
    TextView tv = findViewById(R.id.adhan_label);
    tv.setText(this.whichAdhan);
  }
}
