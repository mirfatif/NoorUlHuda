package com.mirfatif.noorulhuda.prayer;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.batoulapps.adhan.CalculationMethod.JAFARI;
import static com.batoulapps.adhan.CalculationMethod.TEHRAN;
import static com.mirfatif.noorulhuda.prayer.PrayerData.ASR_CALC_NAMES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.CALC_METHODS;
import static com.mirfatif.noorulhuda.prayer.PrayerData.CALC_METHOD_NAMES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.COUNTRY_CODES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.COUNTRY_NAMES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.HIGH_LAT_NAMES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.METHOD_LOCATIONS;
import static com.mirfatif.noorulhuda.prayer.PrayerData.WIDGET_STYLES;
import static com.mirfatif.noorulhuda.prayer.PrayerData.getCalcParams;
import static com.mirfatif.noorulhuda.prayer.PrayerData.getPrayerData;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.svc.PrayerAdhanSvc.ADHAN_FILE;
import static com.mirfatif.noorulhuda.util.Utils.setNightTheme;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.SearchView.OnSuggestionListener;
import androidx.core.view.MenuCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.ActivityPrayerTimeBinding;
import com.mirfatif.noorulhuda.databinding.OffsetPickerBinding;
import com.mirfatif.noorulhuda.svc.PrayerNotifySvc;
import com.mirfatif.noorulhuda.ui.base.BaseActivity;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.FileDownload;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PrayerTimeActivity extends BaseActivity {

  private static final String TAG = "PrayerTimeActivity";

  private static final String INVALID_DATE = Utils.getString(R.string.invalid_date);

  private ActivityPrayerTimeBinding mB;
  private TextView[] mTimeViews;
  private View[] mActiveViews;

  private String mCountryCode = SETTINGS.getCountryCode();
  private String mCity = SETTINGS.getCityName();

  private Compass mCompass;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    boolean showHome = true;
    if (extras != null) {
      int id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
      if (id > 0) {
        setResult(RESULT_OK, intent);
        if (SETTINGS.getLngLat() != null) {
          finish();
          return;
        }
        Utils.showToast(R.string.set_location);
        showHome = false;
      }
    }

    if (setNightTheme(this)) {
      return;
    }

    mB = ActivityPrayerTimeBinding.inflate(getLayoutInflater());
    setContentView(mB.getRoot());

    ActionBar actionbar = getSupportActionBar();
    if (actionbar != null) {
      actionbar.setTitle(R.string.prayer_time_menu_item);
      if (!showHome || ACTION_NO_PARENT.equals(intent.getAction())) {
        actionbar.setDisplayHomeAsUpEnabled(false);
      }
    }

    initWidget();
    initLocContainer();
    initNotifContainer();
    initCalcContainer();
    initWidgetContainer();
    intiButtons();

    mCompass = new Compass(this);
  }

  @Override
  protected void onPause() {
    if (mTimer != null) {
      mTimer.cancel();
    }
    cancelHttpTask();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.runBg(this::updateWidget);
    startTimer();
  }

  @Override
  public void onBackPressed() {
    if (!onButtonClick(null)) {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.prayer, menu);
    MenuCompat.setGroupDividerEnabled(menu, true);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean enable = mCompass.getAccelerometer() != null && mCompass.getMagnetometer() != null;
    menu.findItem(R.id.action_qibla_dir).setEnabled(enable && SETTINGS.getLngLat() != null);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_qibla_dir) {
      mCompass.show();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// GENERAL ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private View[] mButtons, mContainers;

  private void intiButtons() {
    mButtons = new View[] {mB.locButton, mB.notifButton, mB.calcButton, mB.widgetButton};
    mContainers =
        new View[] {
          mB.locCont.getRoot(),
          mB.notifyCont.getRoot(),
          mB.calcCont.getRoot(),
          mB.widgetCont.getRoot()
        };

    for (int i = 0; i < 4; i++) {
      int order = i;
      mButtons[i].setOnClickListener(v -> onButtonClick(order));
    }
  }

  private boolean onButtonClick(Integer order) {
    if (mButtons == null || mContainers == null) {
      return false;
    }

    boolean changed = false;

    for (int i = 0; i < 4; i++) {
      if (mButtons[i].getVisibility() == View.GONE
          || mContainers[i].getVisibility() == View.VISIBLE) {
        changed = true;
      }

      if (order != null && order == i) {
        mButtons[i].setVisibility(View.GONE);
        mContainers[i].setVisibility(View.VISIBLE);
      } else {
        mButtons[i].setVisibility(View.VISIBLE);
        mContainers[i].setVisibility(View.GONE);
      }
    }
    return changed;
  }

  private void showOffsetDialog(int min, int max, int val, OffsetDialogCallback callback) {
    OffsetPickerBinding b = OffsetPickerBinding.inflate(getLayoutInflater());
    int fix = 0;
    if (min < 0) {
      fix = min;
      min -= fix;
      max -= fix;
      val -= fix;
    }
    int fx = fix;
    b.picker.setFormatter(value -> String.format(Locale.getDefault(), "%d", value + fx));
    b.picker.setMinValue(min);
    b.picker.setMaxValue(max);
    b.picker.setValue(val);
    Builder builder =
        new Builder(this)
            .setTitle(R.string.select_offset)
            .setPositiveButton(R.string.save, (d, w) -> callback.onSave(b.picker.getValue() + fx))
            .setNegativeButton(android.R.string.cancel, null)
            .setView(b.getRoot());
    new AlertDialogFragment(builder.create()).show(this, "OFFSET_PICK", false);
  }

  private interface OffsetDialogCallback {

    void onSave(int val);
  }

  private void setupHelpDialog(View view, @StringRes int msgResId) {
    view.setOnClickListener(
        v -> Utils.showTextDialog(this, R.string.search_help_menu_item, msgResId, "HELP", true));
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////// UPDATE VIEWS //////////////////////////
  //////////////////////////////////////////////////////////////////

  private void updateCityView() {
    if (mCity != null) {
      mB.widget.cityNameV.setText(mCity);
      mB.widget.cityNameV.setVisibility(View.VISIBLE);
    } else {
      mB.widget.cityNameV.setVisibility(View.GONE);
    }
  }

  private void updateLngLatViews() {
    String lngStr;
    String latStr;
    Coordinates loc = SETTINGS.getLngLat();
    if (loc == null) {
      lngStr = latStr = "-";
    } else {
      float lng = (float) loc.longitude;
      float lat = (float) loc.latitude;
      lngStr = String.valueOf(lng);
      latStr = String.valueOf(lat);
    }
    mB.locCont.lngV.setText(lngStr);
    mB.locCont.latV.setText(latStr);
  }

  private void updateParamsViews() {
    String text;
    CalculationParameters params = getCalcParams();
    text = String.format(Locale.getDefault(), "%.1f%c", params.fajrAngle, 176);
    mB.calcCont.fajrParams.setText(text);

    if (params.methodAdjustments.sunrise != 0) {
      text = getString(R.string.min, params.methodAdjustments.sunrise);
    } else {
      text = "-";
    }
    mB.calcCont.sunParams.setText(text);

    if (params.methodAdjustments.dhuhr != 0) {
      text = getString(R.string.min, params.methodAdjustments.dhuhr);
    } else {
      text = "-";
    }
    mB.calcCont.zuhrParams.setText(text);

    if (params.methodAdjustments.asr != 0) {
      mB.calcCont.asrParams.setText(getString(R.string.min, params.methodAdjustments.asr));
    } else {
      mB.calcCont.asrParams.setText("-");
    }

    CalculationMethod method = CALC_METHODS[SETTINGS.getCalcMethod()];
    text = "-";
    if (method == JAFARI || method == TEHRAN) {
      text = String.format(Locale.getDefault(), "%.1f%c", params.maghribAngle, 176);
    } else if (params.methodAdjustments.maghrib != 0) {
      text = getString(R.string.min, params.methodAdjustments.maghrib);
    }
    mB.calcCont.maghribParams.setText(text);

    if (params.ishaInterval > 0) {
      text = getString(R.string.min, params.ishaInterval);
    } else {
      text = String.format(Locale.getDefault(), "%.1f%c", params.ishaAngle, 176);
    }
    mB.calcCont.ishaParams.setText(text);
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// WIDGET /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void initWidget() {
    mTimeViews =
        new TextView[] {
          mB.widget.fajrTime,
          mB.widget.sunTime,
          mB.widget.zuhrTime,
          mB.widget.asrTime,
          mB.widget.maghribTime,
          mB.widget.ishaTime
        };

    mActiveViews =
        new View[] {
          mB.widget.fajrActive,
          mB.widget.sunActive,
          mB.widget.zuhrActive,
          mB.widget.asrActive,
          mB.widget.maghribActive,
          mB.widget.ishaActive
        };

    mB.widget.hijriDateCont.setOnClickListener(
        v -> showOffsetDialog(-2, 2, SETTINGS.getHijriOffset(), SETTINGS::setHijriOffset));
    mB.widget.remTimeV.setCountDown(true);

    updateCityView();
    updateLocDependents();
  }

  private Timer mTimer;

  private void startTimer() {
    if (mTimer != null) {
      mTimer.cancel();
    }
    if (SETTINGS.getLngLat() != null) {
      Utils.runBg(this::updateWidget);
      mTimer = new Timer();
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.MINUTE, 1);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      mTimer.scheduleAtFixedRate(
          new TimerTask() {
            @Override
            public void run() {
              updateWidget();
            }
          },
          cal.getTime(),
          60000);
    }
  }

  private void updateWidget() {
    if (SETTINGS.getLngLat() != null) {
      PrayerData data = getPrayerData();
      Utils.runUi(() -> updateWidget(data));
    }
  }

  private void updateWidget(PrayerData data) {
    mB.widget.dateV.setText(data.hijDate);
    mB.widget.monthV.setText(data.hijMonth);
    mB.widget.yearV.setText(data.hijYear);

    mB.widget.remTimeV.start();
    mB.widget.remTimeV.setBase(SystemClock.elapsedRealtime() + data.untilNextPrayer());

    for (int i = 0; i < 6; i++) {
      mTimeViews[i].setText(data.strTimes[i]);

      if (i != data.curPrayer) {
        mActiveViews[i].setVisibility(View.INVISIBLE);
      } else {
        mActiveViews[i].setVisibility(View.VISIBLE);
      }
    }

    mB.widget.gregDateV.setText(data.date);
    mB.widget.dayV.setText(data.day);
    mB.widget.timeV.setText(data.time);
  }

  private void clearWidget() {
    mB.widget.dateV.setText(null);
    mB.widget.monthV.setText(null);
    mB.widget.yearV.setText(INVALID_DATE);

    String nullTime = getString(R.string.null_time);
    mB.widget.remTimeV.stop();
    mB.widget.remTimeV.setBase(SystemClock.elapsedRealtime());

    for (int i = 0; i < 6; i++) {
      mTimeViews[i].setText(nullTime);
      mActiveViews[i].setVisibility(View.VISIBLE);
    }

    mB.widget.gregDateV.setText(INVALID_DATE);
    mB.widget.dayV.setText(INVALID_DATE);
    mB.widget.timeV.setText(nullTime);
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// LOCATION ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private void initLocContainer() {
    setupHelpDialog(mB.locCont.helpV, R.string.location_help);

    mB.locCont.clearV.setOnClickListener(
        v -> {
          saveLocation(null, null);
          updateLngLatViews();
          clearWidget();
          invalidateOptionsMenu();
          mCountryCode = null;
          SETTINGS.setCountryCode(null);
          updateSearchView();
        });

    mB.locCont.mapV.setOnClickListener(
        v -> {
          Coordinates location = SETTINGS.getLngLat();
          if (location != null) {
            String loc = location.latitude + "," + location.longitude;
            String uri = "geo:" + loc + "?q=" + loc + "(" + mCity + ")";
            try {
              startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            } catch (ActivityNotFoundException ignored) {
              Utils.showToast(R.string.no_maps_installed);
            }
          }
        });

    mB.locCont.countryCodeV.setOnClickListener(
        v -> {
          mCountryCode = null;
          updateSearchView();
        });

    SimpleCursorAdapter suggestionAdapter =
        new SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1,
            null,
            new String[] {COLUMN_NAME},
            new int[] {android.R.id.text1},
            0);
    mB.locCont.searchV.setSuggestionsAdapter(suggestionAdapter);
    suggestionAdapter.setFilterQueryProvider(this::updateCursor);
    mB.locCont.searchV.setOnSuggestionListener(new SearchSuggestionListener());

    mB.locCont.searchV.setOnQueryTextListener(
        new OnQueryTextListener() {
          @Override
          public boolean onQueryTextSubmit(String query) {
            return false;
          }

          @Override
          public boolean onQueryTextChange(String newText) {
            cancelHttpTask();
            return false;
          }
        });

    mB.locCont.searchV.setOnQueryTextFocusChangeListener(
        (v, hasFocus) -> {
          if (!hasFocus) {
            cancelHttpTask();
            setCountryCodeVis(false);
            mB.locCont.searchV.setQuery(null, true);
            mB.locCont.searchV.onActionViewCollapsed();
          } else {
            updateSearchView();
          }
        });

    List<InputFilter> filters = new ArrayList<>(Arrays.asList(mB.locCont.lngV.getFilters()));
    filters.add(new LngLatFilter(-180, 180));
    mB.locCont.lngV.setFilters(filters.toArray(new InputFilter[] {}));

    filters = new ArrayList<>(Arrays.asList(mB.locCont.latV.getFilters()));
    filters.add(new LngLatFilter(-90, 90));
    mB.locCont.latV.setFilters(filters.toArray(new InputFilter[] {}));

    updateLngLatViews();

    for (EditText et : new EditText[] {mB.locCont.latV, mB.locCont.lngV}) {
      et.setOnFocusChangeListener(
          (v, hasFocus) -> {
            et.removeTextChangedListener(COORDINATE_LISTENER);
            if (hasFocus) {
              et.addTextChangedListener(COORDINATE_LISTENER);
            } else if (!mB.locCont.latV.hasFocus() && !mB.locCont.lngV.hasFocus()) {
              updateLngLatViews();
            }
          });
    }
  }

  private void updateSearchView() {
    if (mCountryCode == null) {
      setCountryCodeVis(false);
      mB.locCont.searchV.setQueryHint(getString(R.string.country));
    } else {
      mB.locCont.countryCodeV.setText(mCountryCode);
      setCountryCodeVis(true);
      mB.locCont.searchV.setQueryHint(getString(R.string.city_area));
    }
    Utils.runUi(() -> setProgBarVis(false));
    mB.locCont.searchV.setQuery(null, false);
  }

  private void setCountryCodeVis(boolean show) {
    mB.locCont.searchV.postDelayed(
        () -> mB.locCont.countryCodeV.setVisibility(show ? View.VISIBLE : View.GONE), 100);
  }

  private void saveLocation(String city, String tzId, float... lngLat) {
    if (lngLat.length != 2) {
      SETTINGS.removeLngLat();
    } else {
      SETTINGS.saveLngLat(lngLat[0], lngLat[1]);
    }
    SETTINGS.setLocTimeZoneId(tzId);

    mCity = city;
    SETTINGS.setCityName(mCity);

    Utils.runUi(
        () -> {
          updateCityView();
          updateLocDependents();
          invalidateOptionsMenu();
        });

    startTimer();
    Utils.runBg(
        () -> {
          setNearestMethod();
          onParamsChanged();
        });
  }

  private void updateLocDependents() {
    Coordinates location = SETTINGS.getLngLat();
    if (location != null) {
      mB.locCont.clearV.setEnabled(true);
      mB.locCont.mapV.setEnabled(true);
    } else {
      mB.locCont.clearV.setEnabled(false);
      mB.locCont.mapV.setEnabled(false);
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////// SUGGESTIONS ///////////////////////////
  //////////////////////////////////////////////////////////////////

  private static final int GEO_NAMES_QUERY_MIN_LENGTH = 3;

  // This method is called synchronously, so we need to
  // cancel Futures from OnQueryTextChange listener.
  private Cursor updateCursor(CharSequence text) {
    MatrixCursor cursor = null;
    cancelHttpTask();
    if (!TextUtils.isEmpty(text) && text.length() >= GEO_NAMES_QUERY_MIN_LENGTH) {
      Utils.runUi(() -> setProgBarVis(true));

      long delay = mCountryCode == null ? 0 : getDelay();
      mCursorFuture = HTTP_EXECUTOR.schedule(() -> updateCursor(text.toString()), delay, SECONDS);
      try {
        cursor = mCursorFuture.get();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (CancellationException | InterruptedException ignored) {
        return null;
      }
    }
    Utils.runUi(() -> setProgBarVis(false));
    return cursor;
  }

  private final String COLUMN_NAME = "SUGGESTION";
  private final String[] COLUMN_NAMES = new String[] {BaseColumns._ID, COLUMN_NAME};

  private final List<String> mCountryCodes = new ArrayList<>();
  private final List<City> mCities = new ArrayList<>();

  private MatrixCursor updateCursor(String query) {
    query = query.toUpperCase();
    List<String> suggestions = new ArrayList<>();

    if (mCountryCode == null) {
      synchronized (mCountryCodes) {
        mCountryCodes.clear();
        for (int i = 0; i < COUNTRY_NAMES.length; i++) {
          String name = COUNTRY_NAMES[i];
          if (name != null && name.toUpperCase().contains(query)) {
            suggestions.add(COUNTRY_NAMES[i]);
            mCountryCodes.add(COUNTRY_CODES[i]);
          }
        }
      }
    } else {
      suggestions.addAll(getCityNames(query));
    }

    if (Thread.interrupted()) {
      return null;
    }

    int count = 0;
    MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
    for (String sug : suggestions) {
      cursor.addRow(new Object[] {count, sug});
      count++;
    }
    return cursor;
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// GEOCODING ///////////////////////////
  //////////////////////////////////////////////////////////////////

  private final ScheduledExecutorService HTTP_EXECUTOR =
      Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<MatrixCursor> mCursorFuture;
  private ScheduledFuture<?> mRevGeocodeFuture;

  private void cancelHttpTask() {
    if (mCursorFuture != null) {
      mCursorFuture.cancel(true);
    }
    if (mRevGeocodeFuture != null) {
      mRevGeocodeFuture.cancel(true);
    }
    Utils.runUi(() -> setProgBarVis(false));
  }

  private static final String URL = "https://secure.geonames.org/";
  private static final String G_CODE_URL =
      URL + "search?username=mirfatif&style=full&type=json&&maxRows=100&";
  private static final String RG_CODE_URL =
      URL + "findNearbyPlaceNameJSON?username=mirfatif&style=full&maxRows=1&";

  private List<String> getCityNames(String query) {
    mCities.clear();
    try {
      String url =
          G_CODE_URL + "country=" + mCountryCode + "&name=" + URLEncoder.encode(query, "UTF8");
      for (int i = 1; i <= 2; i++) {
        List<String> cities = buildCitiesList(getResponse(url));
        if (cities.size() > 0) {
          return cities;
        }
        if (i != 2) {
          waitFor(1000);
        }
      }
      Utils.showShortToast(R.string.no_location_found);
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      Utils.showToast(R.string.geocoding_failed);
    } catch (ConnectionException | InterruptException ignored) {
    }
    return new ArrayList<>();
  }

  private List<String> buildCitiesList(JSONObject response) throws JSONException {
    synchronized (mCities) {
      List<String> cityNames = new ArrayList<>();
      JSONArray geoNames = response.getJSONArray("geonames");
      String country = null;
      int index = Arrays.asList(COUNTRY_CODES).indexOf(mCountryCode);
      if (index >= 0 && COUNTRY_NAMES.length > index) {
        country = COUNTRY_NAMES[index];
      }
      for (int i = 0; i < geoNames.length(); i++) {
        JSONObject geoName = geoNames.getJSONObject(i);
        City city = new City();
        city.name = buildCityName(geoName.getString("toponymName"), geoName, country);
        city.lng = Float.parseFloat(geoName.getString("lng"));
        city.lat = Float.parseFloat(geoName.getString("lat"));
        JSONObject tz = geoName.optJSONObject("timezone");
        if (tz == null) {
          Log.e(TAG, "Skipping " + city.name + " without time zone info");
          continue;
        }
        city.tzId = tz.getString("timeZoneId");
        mCities.add(city);
        cityNames.add(city.name);
      }
      return cityNames;
    }
  }

  private void doRevGeocoding(double lng, double lat) {
    cancelHttpTask();
    Utils.runUi(() -> setProgBarVis(true));
    mRevGeocodeFuture = HTTP_EXECUTOR.schedule(() -> setCityName(lng, lat), getDelay(), SECONDS);
  }

  private void setCityName(double lng, double lat) {
    String url = RG_CODE_URL + "lat=" + lat + "&lng=" + lng;
    boolean interrupted = false;
    try {
      for (int i = 1; i <= 3; i++) {
        JSONArray geoNames = getResponse(url).getJSONArray("geonames");
        boolean receivedResults = false;
        for (int j = 0; j < geoNames.length(); j++) {
          JSONObject geoName = geoNames.getJSONObject(j);
          String city = geoName.getString("toponymName");
          if (city.length() != 0) {
            city = buildCityName(city, geoName, geoName.optString("countryName"));
            JSONObject tz = geoName.optJSONObject("timezone");
            if (tz == null) {
              Log.e(TAG, "Skipping " + city + " without time zone info");
              continue;
            }
            String tzId = tz.getString("timeZoneId");
            saveLocation(city, tzId, (float) lng, (float) lat);
            return;
          }
          receivedResults = true;
        }
        if (receivedResults) {
          break;
        }
        if (i != 3) {
          waitFor(5000);
        }
      }
      Utils.showShortToast(R.string.no_location_found);
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      Utils.showToast(R.string.rev_geocoding_failed);
    } catch (InterruptException ignored) {
      interrupted = true;
    } catch (ConnectionException ignored) {
    } finally {
      if (!interrupted) {
        Utils.runUi(
            () -> {
              setProgBarVis(false);
              mB.locCont.lngV.clearFocus();
              mB.locCont.latV.clearFocus();
              updateLngLatViews();
            });
      }
    }
  }

  private void waitFor(long ms) throws InterruptException {
    synchronized (HTTP_EXECUTOR) {
      try {
        HTTP_EXECUTOR.wait(ms);
      } catch (InterruptedException e) {
        throw new InterruptException();
      }
    }
  }

  private String buildCityName(String city, JSONObject geoName, String country) {
    StringBuilder name = new StringBuilder(city);
    for (int k = 3; k >= 1; k--) {
      String admin = geoName.optString("adminName" + k);
      if (!TextUtils.isEmpty(admin) && !admin.equals(city)) {
        name.append(", ").append(admin);
      }
    }
    if (!TextUtils.isEmpty(country)) {
      name.append(", ").append(country);
    }
    return name.toString();
  }

  private long mLastRequest;

  private long getDelay() {
    long diff = System.currentTimeMillis() - mLastRequest;
    if (diff == 0) {
      diff = 1;
    }
    long sec = Math.min(Math.max(30000 / diff, 3), 10);
    Utils.runUi(() -> showRequestDelay(sec * 1000));
    return sec;
  }

  private JSONObject getResponse(String url)
      throws ConnectionException, InterruptException, IOException, JSONException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      if (!Utils.isInternetReachable()) {
        Utils.showToast(R.string.no_internet);
        throw new ConnectionException();
      }

      conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setConnectTimeout(30000);
      conn.setReadTimeout(30000);
      conn.setUseCaches(false);

      int status = conn.getResponseCode();
      if (status != HttpURLConnection.HTTP_OK) {
        throw new IOException("Bad response: " + status);
      }

      mLastRequest = System.currentTimeMillis();

      is = conn.getInputStream();
      byte[] buffer = new byte[8192];
      StringBuilder builder = new StringBuilder();
      int len;
      while ((len = is.read(buffer)) > 0) {
        if (Thread.interrupted()) {
          throw new InterruptException();
        }
        builder.append(new String(buffer, 0, len));
      }
      return new JSONObject(builder.toString());

    } catch (InterruptedIOException e) {
      throw new InterruptException();
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException ignored) {
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static class ConnectionException extends Exception {}

  private static class InterruptException extends Exception {}

  //////////////////////////////////////////////////////////////////
  /////////////////////// GEOCODING PROGRESS ///////////////////////
  //////////////////////////////////////////////////////////////////

  private CountDownTimer mDelayTimer;
  private final Object REQUEST_TIMER_LOCK = new Object();

  private void showRequestDelay(long ms) {
    synchronized (REQUEST_TIMER_LOCK) {
      if (mDelayTimer != null) {
        mDelayTimer.cancel();
      }

      mB.locCont.waitV.setText(String.valueOf(ms / 1000));
      mB.locCont.waitV.setVisibility(View.VISIBLE);

      mDelayTimer =
          new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
              mB.locCont.waitV.setText(
                  String.valueOf(Math.round((float) millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
              mB.locCont.waitV.setVisibility(View.GONE);
            }
          };

      mDelayTimer.start();
    }
  }

  private class PBarRunnable implements Runnable {

    private final boolean mShow;

    PBarRunnable(boolean show) {
      mShow = show;
    }

    @Override
    public void run() {
      mB.locCont.pBarCont.setVisibility(mShow ? View.VISIBLE : View.INVISIBLE);
    }
  }

  private final Runnable P_BAR_SHOW = new PBarRunnable(true);
  private final Runnable P_BAR_HIDE = new PBarRunnable(false);

  private void setProgBarVis(boolean show) {
    mB.locCont.pBarCont.removeCallbacks(P_BAR_SHOW);
    mB.locCont.pBarCont.removeCallbacks(P_BAR_HIDE);
    mB.locCont.pBarCont.postDelayed(show ? P_BAR_SHOW : P_BAR_HIDE, 500);
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////// NOTIFICATIONS /////////////////////////
  //////////////////////////////////////////////////////////////////

  private CheckBox[] mAdhanCheckBoxes;

  private void initNotifContainer() {
    CheckBox[] checkBoxes =
        new CheckBox[] {
          mB.notifyCont.fajrNotify,
          mB.notifyCont.sunNotify,
          mB.notifyCont.zuhrNotify,
          mB.notifyCont.asrNotify,
          mB.notifyCont.maghribNotify,
          mB.notifyCont.ishaNotify
        };
    for (int i = 0; i < 6; i++) {
      checkBoxes[i].setChecked(SETTINGS.getPrayerNotify(i));
      int order = i;
      checkBoxes[i].setOnCheckedChangeListener(
          (v, isChecked) -> {
            SETTINGS.setPrayerNotify(order, isChecked);
            resetWidgets();
          });
    }

    mAdhanCheckBoxes =
        new CheckBox[] {
          mB.notifyCont.fajrAdhan,
          mB.notifyCont.sunAdhan,
          mB.notifyCont.zuhrAdhan,
          mB.notifyCont.asrAdhan,
          mB.notifyCont.maghribAdhan,
          mB.notifyCont.ishaAdhan
        };
    for (int i = 0; i < 6; i++) {
      mAdhanCheckBoxes[i].setChecked(SETTINGS.getPrayerAdhan(i));
      int order = i;
      mAdhanCheckBoxes[i].setOnCheckedChangeListener(
          ((v, isChecked) -> {
            if (isChecked) {
              if (SETTINGS.getDownloadedFile(ADHAN_FILE).exists()) {
                SETTINGS.setPrayerAdhan(order, true);
                resetWidgets();
              } else {
                v.setChecked(false);
                Runnable callback =
                    () -> {
                      if (!isFinishing() && !isDestroyed()) {
                        Utils.runUi(() -> mAdhanCheckBoxes[order].setChecked(true));
                      }
                    };
                new FileDownload(this, "/adhan/", ADHAN_FILE, callback, R.string.downloading_file)
                    .askToDownload();
              }
            } else {
              SETTINGS.setPrayerAdhan(order, false);
              resetWidgets();
            }
          }));
    }

    TextView[] offsetViews =
        new TextView[] {
          mB.notifyCont.fajrOffset,
          mB.notifyCont.sunOffset,
          mB.notifyCont.zuhrOffset,
          mB.notifyCont.asrOffset,
          mB.notifyCont.maghribOffset,
          mB.notifyCont.ishaOffset
        };
    for (int i = 0; i < 6; i++) {
      offsetViews[i].setText(String.valueOf(SETTINGS.getPrayerNotifyOffset(i)));
      int order = i;
      offsetViews[i].setOnClickListener(
          v ->
              showOffsetDialog(
                  -60,
                  60,
                  SETTINGS.getPrayerNotifyOffset(order),
                  val -> {
                    SETTINGS.setPrayerNotifyOffset(order, val);
                    offsetViews[order].setText(String.valueOf(val));
                    resetWidgets();
                  }));
    }
  }

  private void resetWidgets() {
    PrayerNotifySvc.reset(false);
    WidgetProvider.reset();

    if (!SETTINGS.shouldAskToExcBatteryOpt() || SETTINGS.getLngLat() == null) {
      return;
    }

    PowerManager pm = (PowerManager) App.getCxt().getSystemService(Context.POWER_SERVICE);
    if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
      return;
    }

    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    Builder builder =
        new Builder(this)
            .setTitle(R.string.battery_optimization)
            .setMessage(R.string.battery_optimization_detail)
            .setNegativeButton(R.string.no, (d, w) -> SETTINGS.doNotAskToExcBatteryOpt())
            .setNeutralButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.yes, (d, w) -> startActivity(intent));
    Utils.runUi(() -> new AlertDialogFragment(builder.create()).show(this, "BATTERY_OPT", false));
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////// CALCULATIONS //////////////////////////
  //////////////////////////////////////////////////////////////////

  private void initCalcContainer() {
    setupHelpDialog(mB.calcCont.helpV, R.string.calc_help);

    TextView[] adjViews =
        new TextView[] {
          mB.calcCont.fajrAdj,
          mB.calcCont.sunAdj,
          mB.calcCont.zuhrAdj,
          mB.calcCont.asrAdj,
          mB.calcCont.maghribAdj,
          mB.calcCont.ishaAdj
        };
    for (int i = 0; i < 6; i++) {
      adjViews[i].setText(String.valueOf(SETTINGS.getPrayerAdj(i)));
      int order = i;
      adjViews[i].setOnClickListener(
          v ->
              showOffsetDialog(
                  -60,
                  60,
                  SETTINGS.getPrayerAdj(order),
                  val -> {
                    SETTINGS.setPrayerAdj(order, val);
                    adjViews[order].setText(String.valueOf(val));
                    onParamsChanged();
                  }));
    }

    int resId = android.R.layout.simple_spinner_dropdown_item;

    mB.calcCont.methodPicker.setAdapter(new ArrayAdapter<>(this, resId, CALC_METHOD_NAMES));
    mB.calcCont.methodPicker.setSelection(SETTINGS.getCalcMethod());
    mB.calcCont.methodPicker.setOnItemSelectedListener(
        new OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SETTINGS.setCalcMethod(position);
            onParamsChanged();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    mB.calcCont.asrMethodPicker.setAdapter(new ArrayAdapter<>(this, resId, ASR_CALC_NAMES));
    mB.calcCont.asrMethodPicker.setSelection(SETTINGS.getAsrCalcMethod());
    mB.calcCont.asrMethodPicker.setOnItemSelectedListener(
        new OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SETTINGS.setAsrCalcMethod(position);
            onParamsChanged();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    mB.calcCont.highLatPicker.setAdapter(new ArrayAdapter<>(this, resId, HIGH_LAT_NAMES));
    mB.calcCont.highLatPicker.setSelection(SETTINGS.getHighLatMethod());
    mB.calcCont.highLatPicker.setOnItemSelectedListener(
        new OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SETTINGS.setHighLatMethod(position);
            onParamsChanged();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  private void setNearestMethod() {
    Coordinates location = SETTINGS.getLngLat();
    if (location == null) {
      return;
    }
    Location loc1 = new Location("");
    loc1.setLatitude(location.latitude);
    loc1.setLongitude(location.longitude);
    Location loc2 = new Location("");
    float minDist = Float.MAX_VALUE;
    int order = -1;
    for (int i = 0; i < METHOD_LOCATIONS.length; i++) {
      Coordinates lngLat = METHOD_LOCATIONS[i];
      if (lngLat.latitude == 0 && lngLat.longitude == 0) {
        continue;
      }
      loc2.setLatitude(lngLat.latitude);
      loc2.setLongitude(lngLat.longitude);
      float dist = loc1.distanceTo(loc2);
      if (dist < minDist) {
        minDist = dist;
        order = i;
      }
    }
    if (order >= 0) {
      SETTINGS.setCalcMethod(order);
      int finalOrder = order;
      Utils.runUi(() -> mB.calcCont.methodPicker.setSelection(finalOrder));
    }
  }

  private void onParamsChanged() {
    Utils.runUi(this::updateParamsViews);
    Utils.runBg(this::updateWidget);
    resetWidgets();
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////// WIDGET SETTINGS ////////////////////////
  //////////////////////////////////////////////////////////////////

  private void initWidgetContainer() {
    mB.widgetCont.widgetNotifV.setChecked(SETTINGS.getShowWidgetNotif());
    mB.widgetCont.widgetNotifV.setOnCheckedChangeListener(
        (v, isChecked) -> {
          SETTINGS.setShowWidgetNotif(isChecked);
          resetWidgets();
        });

    int resId = android.R.layout.simple_spinner_dropdown_item;
    mB.widgetCont.stylePicker.setAdapter(new ArrayAdapter<>(this, resId, WIDGET_STYLES));
    mB.widgetCont.stylePicker.setSelection(SETTINGS.getHomeWidgetStyle());
    mB.widgetCont.stylePicker.setOnItemSelectedListener(
        new OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            SETTINGS.setHomeWidgetStyle(position);
            WidgetProvider.reset();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////// IMPLEMENTATIONS /////////////////////////
  //////////////////////////////////////////////////////////////////

  private class SearchSuggestionListener implements OnSuggestionListener {

    @Override
    public boolean onSuggestionSelect(int position) {
      return true;
    }

    @Override
    public boolean onSuggestionClick(int position) {
      City city;
      if (mCountryCode == null && mCountryCodes.size() > position) {
        mCountryCode = mCountryCodes.get(position);
        SETTINGS.setCountryCode(mCountryCode);
        updateSearchView();
      } else if (mCities.size() > position && (city = mCities.get(position)) != null) {
        saveLocation(city.name, city.tzId, city.lng, city.lat);
        updateLngLatViews();
        mB.locCont.searchV.clearFocus();
      }
      return true;
    }
  }

  private final CoordinateChangeListener COORDINATE_LISTENER = new CoordinateChangeListener();

  private class CoordinateChangeListener implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      Editable lngVal = mB.locCont.lngV.getText(), latVal = mB.locCont.latV.getText();
      if (!TextUtils.isEmpty(lngVal) && !TextUtils.isEmpty(latVal)) {
        String lngStr = lngVal.toString(), latStr = latVal.toString();
        if (!lngStr.equals("+")
            && !lngStr.equals("-")
            && !latStr.equals("+")
            && !latStr.equals("-")) {
          float lng = Float.parseFloat(lngStr);
          float lat = Float.parseFloat(latStr);
          doRevGeocoding(lng, lat);
          return;
        }
      }
      // Cancel previous call if editing is in progress.
      cancelHttpTask();
    }

    @Override
    public void afterTextChanged(Editable s) {}
  }

  private static class LngLatFilter implements InputFilter {

    private final int mMin, mMax;

    LngLatFilter(int min, int max) {
      mMin = min;
      mMax = max;
    }

    @Override
    public CharSequence filter(
        CharSequence src, int start, int end, Spanned dst, int dstart, int dend) {
      String oldVal = dst.toString();
      String newVal =
          oldVal.substring(0, dstart)
              + src.toString().substring(start, end)
              + oldVal.substring(dend);
      if (!newVal.equals("") && !newVal.equals("+") && !newVal.equals("-")) {
        try {
          float val = Float.parseFloat(newVal);
          if (val < mMin || val > mMax) {
            return "";
          }
        } catch (NumberFormatException e) {
          return "";
        }
      }
      return null;
    }
  }

  private static class City {

    String name, tzId;
    float lng, lat;
  }

  private static final String ACTION_NO_PARENT = BuildConfig.APPLICATION_ID + ".action.NO_PARENT";

  public static PendingIntent getPendingIntent(int reqCode) {
    Intent intent = new Intent(App.getCxt(), PrayerTimeActivity.class).setAction(ACTION_NO_PARENT);
    return PendingIntent.getActivity(App.getCxt(), reqCode, intent, FLAG_UPDATE_CURRENT);
  }
}
