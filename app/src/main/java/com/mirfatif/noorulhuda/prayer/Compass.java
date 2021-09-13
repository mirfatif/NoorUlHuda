package com.mirfatif.noorulhuda.prayer;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import com.batoulapps.adhan.Qibla;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.CompassBinding;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class Compass implements SensorEventListener {

  private final FragmentActivity mA;
  private final SensorManager mSensorManager;
  private final Display mDisplay;

  public Compass(FragmentActivity activity) {
    mA = activity;
    mSensorManager = (SensorManager) App.getCxt().getSystemService(Context.SENSOR_SERVICE);
    mDisplay =
        ((WindowManager) App.getCxt().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
  }

  private CompassBinding mCompassView;
  private Float mLastAngle;
  private Qibla mQibla;

  public void show() {
    Sensor aSensor = getAccelerometer();
    Sensor mSensor = getMagnetometer();

    mSensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_UI);
    mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    mCompassView = CompassBinding.inflate(mA.getLayoutInflater());
    mQibla = new Qibla(SETTINGS.getLngLat());
    float angle = (float) mQibla.direction;
    if (angle > 180) {
      angle -= 360;
    }
    String text = String.format(Locale.getDefault(), "%.1f%sN", angle, (char) 176);
    mCompassView.angleV.setText(text);
    AlertDialog dialog =
        new Builder(mA).setTitle(R.string.qibla).setView(mCompassView.getRoot()).create();
    AlertDialogFragment.show(mA, dialog, "COMPASS")
        .setOnDismissListener(
            d -> {
              mSensorManager.unregisterListener(this);
              mCompassView = null;
              mLastAngle = null;
            });
  }

  public Sensor getAccelerometer() {
    return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
  }

  public Sensor getMagnetometer() {
    return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
  }

  private static final float ALPHA = 0.9f;
  private final float[] mGravity = new float[3];
  private final float[] mGeomagnetic = new float[3];
  private final float[] R_ = new float[9];
  private final float[] I_ = new float[9];
  private final float[] mOrientation = new float[3];

  private final ReentrantLock SENSOR_EVENT_LOCK = new ReentrantLock();

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (!SENSOR_EVENT_LOCK.tryLock()) {
      return;
    }
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      mGravity[0] = ALPHA * mGravity[0] + (1 - ALPHA) * event.values[0];
      mGravity[1] = ALPHA * mGravity[1] + (1 - ALPHA) * event.values[1];
      mGravity[2] = ALPHA * mGravity[2] + (1 - ALPHA) * event.values[2];
    }

    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
      mGeomagnetic[0] = ALPHA * mGeomagnetic[0] + (1 - ALPHA) * event.values[0];
      mGeomagnetic[1] = ALPHA * mGeomagnetic[1] + (1 - ALPHA) * event.values[1];
      mGeomagnetic[2] = ALPHA * mGeomagnetic[2] + (1 - ALPHA) * event.values[2];
    }

    if (SensorManager.getRotationMatrix(R_, I_, mGravity, mGeomagnetic)) {
      SensorManager.getOrientation(R_, mOrientation);
      float angle = (float) (mQibla.direction - Math.toDegrees(mOrientation[0]));
      angle -= getRotation();
      if (mCompassView != null && (mLastAngle == null || Math.abs(angle - mLastAngle) >= 1)) {
        mCompassView.arrowQibla.setRotation(angle);
        mCompassView.qibla.setRotation(-angle);
        mLastAngle = angle;
      }
    }
    SENSOR_EVENT_LOCK.unlock();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  private float getRotation() {
    switch (mDisplay.getRotation()) {
      default:
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
    }
  }
}
