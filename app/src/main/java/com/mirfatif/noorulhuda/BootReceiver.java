package com.mirfatif.noorulhuda;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.mirfatif.noorulhuda.svc.PrayerNotifySvc;

public class BootReceiver extends BroadcastReceiver {

  private static final String TAG = "BootReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      Log.i(TAG, "Starting " + PrayerNotifySvc.TAG);
      PrayerNotifySvc.reset(true);
    }
  }

  public static void setState(boolean enable) {
    ComponentName receiver = new ComponentName(App.getCxt(), BootReceiver.class);
    PackageManager pm = App.getCxt().getPackageManager();
    int state = enable ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED;
    pm.setComponentEnabledSetting(receiver, state, DONT_KILL_APP);
  }
}
