package com.mirfatif.noorulhuda.prefs;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static com.mirfatif.noorulhuda.util.Utils.getString;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;
import androidx.room.RoomDatabase.Builder;
import com.batoulapps.adhan.Coordinates;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.db.DbBuilder;
import com.mirfatif.noorulhuda.db.QuranDao;
import com.mirfatif.noorulhuda.db.QuranDatabase;
import com.mirfatif.noorulhuda.db.QuranMetaDao;
import com.mirfatif.noorulhuda.db.QuranMetaDatabase;
import com.mirfatif.noorulhuda.db.TagAayahsDao;
import com.mirfatif.noorulhuda.db.TagAayahsDatabase;
import com.mirfatif.noorulhuda.db.TagsDao;
import com.mirfatif.noorulhuda.db.TagsDatabase;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public enum MySettings {
  SETTINGS;

  private static final String TAG = "MySettings";

  private final SharedPreferences mPrefs, mNoBkpPrefs;

  MySettings() {
    mPrefs = Utils.getDefPrefs();
    mNoBkpPrefs = Utils.getNoBkpPrefs();
  }

  public boolean getBoolPref(int keyResId) {
    String prefKey = getString(keyResId);
    Integer boolKeyId =
        Utils.getStaticIntField(prefKey + "_default", R.bool.class, TAG + ": getBoolPref");
    if (boolKeyId == null) {
      return false;
    }
    if (prefKey.endsWith("_nb")) {
      return mNoBkpPrefs.getBoolean(prefKey, App.getRes().getBoolean(boolKeyId));
    } else {
      return mPrefs.getBoolean(prefKey, App.getRes().getBoolean(boolKeyId));
    }
  }

  public int getIntPref(int keyResId) {
    String prefKey = getString(keyResId);
    Integer intKeyId =
        Utils.getStaticIntField(prefKey + "_default", R.integer.class, TAG + ": getIntPref");
    if (intKeyId == null) {
      return -1;
    }
    if (prefKey.endsWith("_nb")) {
      return mNoBkpPrefs.getInt(prefKey, App.getCxt().getResources().getInteger(intKeyId));
    } else {
      return mPrefs.getInt(prefKey, App.getCxt().getResources().getInteger(intKeyId));
    }
  }

  private long getLongPref(int keyResId) {
    String prefKey = getString(keyResId);
    if (prefKey.endsWith("_nb")) {
      return mNoBkpPrefs.getLong(prefKey, 0);
    } else {
      return mPrefs.getLong(prefKey, 0);
    }
  }

  public float getFloatPref(int keyResId) {
    String prefKey = getString(keyResId);
    Integer intKeyId =
        Utils.getStaticIntField(prefKey + "_default", R.integer.class, TAG + ": getIntPref");
    if (intKeyId == null) {
      return -1;
    }
    if (prefKey.endsWith("_nb")) {
      return mNoBkpPrefs.getFloat(prefKey, App.getCxt().getResources().getInteger(intKeyId));
    } else {
      return mPrefs.getFloat(prefKey, App.getCxt().getResources().getInteger(intKeyId));
    }
  }

  private String getStringPref(int keyResId) {
    String prefKey = getString(keyResId);
    Integer strKeyId =
        Utils.getStaticIntField(prefKey + "_default", R.string.class, TAG + ": getStringPref");
    if (strKeyId == null) {
      return null;
    }
    if (prefKey.endsWith("_nb")) {
      return mNoBkpPrefs.getString(prefKey, getString(strKeyId));
    } else {
      return mPrefs.getString(prefKey, getString(strKeyId));
    }
  }

  @SuppressWarnings("SameParameterValue")
  private Set<String> getSetPref(int keyId) {
    return mPrefs.getStringSet(getString(keyId), null);
  }

  public void savePref(int key, boolean bool) {
    String prefKey = getString(key);
    if (prefKey.endsWith("_nb")) {
      mNoBkpPrefs.edit().putBoolean(prefKey, bool).apply();
    } else {
      mPrefs.edit().putBoolean(prefKey, bool).apply();
    }
  }

  public void savePref(int key, int integer) {
    String prefKey = getString(key);
    if (prefKey.endsWith("_nb")) {
      mNoBkpPrefs.edit().putInt(prefKey, integer).apply();
    } else {
      mPrefs.edit().putInt(prefKey, integer).apply();
    }
  }

  private void savePref(int key, long _long) {
    String prefKey = getString(key);
    if (prefKey.endsWith("_nb")) {
      mNoBkpPrefs.edit().putLong(prefKey, _long).apply();
    } else {
      mPrefs.edit().putLong(prefKey, _long).apply();
    }
  }

  public void savePref(int key, float _float) {
    String prefKey = getString(key);
    if (prefKey.endsWith("_nb")) {
      mNoBkpPrefs.edit().putFloat(prefKey, _float).apply();
    } else {
      mPrefs.edit().putFloat(prefKey, _float).apply();
    }
  }

  public void savePref(int key, String string) {
    String prefKey = getString(key);
    if (prefKey.endsWith("_nb")) {
      mNoBkpPrefs.edit().putString(prefKey, string).apply();
    } else {
      mPrefs.edit().putString(prefKey, string).apply();
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void savePref(int key, Set<String> stringSet) {
    mPrefs.edit().putStringSet(getString(key), stringSet).apply();
  }

  @SuppressLint("ApplySharedPref")
  public boolean shouldAskToSendCrashReport() {
    int crashCount = getIntPref(R.string.pref_main_crash_report_count_nb_key);
    long lastTS = getLongPref(R.string.pref_main_crash_report_ts_nb_key);
    long currTime = System.currentTimeMillis();

    Editor prefEditor = mNoBkpPrefs.edit();
    try {
      if (crashCount >= 5 || (currTime - lastTS) >= TimeUnit.DAYS.toMillis(1)) {
        prefEditor.putLong(getString(R.string.pref_main_crash_report_ts_nb_key), currTime);
        prefEditor.putInt(getString(R.string.pref_main_crash_report_count_nb_key), 1);
        return true;
      }

      prefEditor.putInt(getString(R.string.pref_main_crash_report_count_nb_key), crashCount + 1);
      return false;
    } finally {
      prefEditor.commit();
    }
  }

  public boolean isDbBuilt(String dbName) {
    if (dbName == null) {
      return false;
    }

    if (!dbName.equals(TRANS_NONE) && isDbInvalid(dbName)) {
      return false;
    }

    return dbName.equals(TRANS_NONE) || mNoBkpPrefs.getBoolean(getDbPrefString(dbName), false);
  }

  @SuppressLint("ApplySharedPref")
  private boolean isDbInvalid(String dbName) {
    // Force recreate Quran databases after upgrading to v2
    Builder<QuranDatabase> dbBuilder =
        Room.databaseBuilder(App.getCxt(), QuranDatabase.class, dbName + ".db");
    QuranDatabase db = null;
    try {
      db = dbBuilder.allowMainThreadQueries().build();
      db.getOpenHelper().getReadableDatabase().getVersion();
      return false;
    } catch (IllegalStateException e) {
      mNoBkpPrefs.edit().putBoolean(getDbPrefString(dbName), false).commit();
      if (db != null) {
        db.close();
      }
      db = dbBuilder.allowMainThreadQueries().fallbackToDestructiveMigration().build();
      db.getOpenHelper().getReadableDatabase().getVersion();
      return true;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  public void rebuildDb() {
    String dbName = getQuranDbName();
    if (isDbInvalid(dbName)) {
      // It'll be built in MainActivity#onCreate()
      setQuranDbName(DbBuilder.MAIN_DB);
    }
    dbName = getTransDbName();
    if (!TRANS_NONE.equals(dbName) && isDbInvalid(dbName)) {
      setTransDbName(TRANS_NONE);
    }
  }

  public void setDbBuilt(String dbName) {
    mNoBkpPrefs.edit().putBoolean(getDbPrefString(dbName), true).apply();
  }

  private String getDbPrefString(String dbName) {
    return "pref_db_" + dbName.replaceAll("-", "_") + "_nb";
  }

  public boolean shouldCheckForUpdates() {
    if (!getCheckForUpdates()) {
      return false;
    }
    long lastTS = getLongPref(R.string.pref_settings_check_for_updates_ts_nb_key);
    return (System.currentTimeMillis() - lastTS) >= TimeUnit.DAYS.toMillis(1);
  }

  public void setCheckForUpdatesTs(long timeStamp) {
    savePref(R.string.pref_settings_check_for_updates_ts_nb_key, timeStamp);
  }

  public boolean getCheckForUpdates() {
    return getBoolPref(R.string.pref_settings_check_for_updates_key);
  }

  public void setCheckForUpdates(boolean check) {
    savePref(R.string.pref_settings_check_for_updates_key, check);
  }

  private static final Object DB_LOCK = new Object();
  private String mLastQuranDb;
  private QuranDatabase mQuranDb;

  public QuranDao getQuranDb() {
    synchronized (DB_LOCK) {
      String dbName = getQuranDbName();
      if (!isDbBuilt(dbName)) {
        dbName = DbBuilder.MAIN_DB;
      }
      if (mQuranDb == null || !dbName.equals(mLastQuranDb)) {
        if (mQuranDb != null) {
          mQuranDb.close();
        }
        mQuranDb = Room.databaseBuilder(App.getCxt(), QuranDatabase.class, dbName + ".db").build();
        mLastQuranDb = dbName;
      }
      return mQuranDb.getDao();
    }
  }

  public String getQuranDbName() {
    return getStringPref(R.string.pref_main_quran_db_nb_key);
  }

  public void setQuranDbName(String dbName) {
    savePref(R.string.pref_main_quran_db_nb_key, dbName);
  }

  private final QuranMetaDao mQuranMetaDao =
      Room.databaseBuilder(
              App.getCxt(), QuranMetaDatabase.class, getString(R.string.db_meta) + ".db")
          .build()
          .getDao();

  public QuranMetaDao getMetaDb() {
    return mQuranMetaDao;
  }

  private String mLastTransDb;
  private QuranDatabase mTransDb;

  public QuranDao getTransDb() {
    synchronized (DB_LOCK) {
      String dbName = getTransDbName();
      if (dbName.equals(TRANS_NONE) || !isDbBuilt(dbName)) {
        return null;
      }
      if (mTransDb == null || !dbName.equals(mLastTransDb)) {
        if (mTransDb != null) {
          mTransDb.close();
        }
        mTransDb = Room.databaseBuilder(App.getCxt(), QuranDatabase.class, dbName + ".db").build();
        mLastTransDb = dbName;
      }
      return mTransDb.getDao();
    }
  }

  private final QuranDao mSearchDb =
      Room.databaseBuilder(App.getCxt(), QuranDatabase.class, getString(R.string.db_search) + ".db")
          .build()
          .getDao();

  public QuranDao getSearchDb() {
    return mSearchDb;
  }

  private final TagsDao mTagsDb =
      Room.databaseBuilder(App.getCxt(), TagsDatabase.class, "tags.db").build().getDao();

  public TagsDao getTagsDb() {
    return mTagsDb;
  }

  private final TagAayahsDao mTagAayahsDb =
      Room.databaseBuilder(App.getCxt(), TagAayahsDatabase.class, "tag_aayahs.db").build().getDao();

  public TagAayahsDao getTagAayahsDb() {
    return mTagAayahsDb;
  }

  private static final String TRANS_NONE = getString(R.string.db_trans_none);

  public boolean transEnabled() {
    return !getTransDbName().equals(TRANS_NONE);
  }

  public boolean showTransWithText() {
    return getBoolPref(R.string.pref_main_trans_with_text_key);
  }

  public void setShowTransWithText(boolean transWithText) {
    savePref(R.string.pref_main_trans_with_text_key, transWithText);
  }

  public String getTransDbName() {
    return getStringPref(R.string.pref_main_trans_db_nb_key);
  }

  public void setTransDbName(String dbName) {
    savePref(R.string.pref_main_trans_db_nb_key, dbName);
  }

  public String[] getQuranicDuaTrans() {
    return getDuaTrans(R.array.quranic_duas_trans_arrays);
  }

  public String[] getMasnoonDuaTrans() {
    return getDuaTrans(R.array.masnoon_duas_trans_arrays);
  }

  public String[] getOccasionsDuaTrans() {
    return getDuaTrans(R.array.occasions_duas_trans_arrays);
  }

  private String[] getDuaTrans(@ArrayRes int resId) {
    List<String> transDbs = Arrays.asList(App.getRes().getStringArray(R.array.db_trans_files));
    int index = transDbs.indexOf(getTransDbName());

    TypedArray ar = App.getRes().obtainTypedArray(resId);
    int transArrayResId = ar.getResourceId(index, 0);
    ar.recycle();

    if (transArrayResId == 0) {
      return null;
    }
    return App.getRes().getStringArray(transArrayResId);
  }

  public String[] getDuaTitles() {
    List<String> transDbs = Arrays.asList(App.getRes().getStringArray(R.array.db_trans_files));
    int index = transDbs.indexOf(getTransDbName());

    TypedArray ar = App.getRes().obtainTypedArray(R.array.occasions_duas_titles_arrays);
    int titlesArrayResId = ar.getResourceId(index, 0);
    ar.recycle();

    if (titlesArrayResId == 0) {
      titlesArrayResId = R.array.occasion_duas_titles_en;
    }
    return App.getRes().getStringArray(titlesArrayResId);
  }

  public boolean getForceDarkMode() {
    return getBoolPref(R.string.pref_main_dark_theme_key);
  }

  public void setForceDarkMode(boolean force) {
    savePref(R.string.pref_main_dark_theme_key, force);
  }

  public String getThemeColor() {
    return getStringPref(R.string.pref_main_theme_color_key);
  }

  public void setThemeColor(String color) {
    savePref(R.string.pref_main_theme_color_key, color);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean getShowHeader() {
    return getBoolPref(R.string.pref_main_show_page_header_key);
  }

  public void toggleShowHeader() {
    savePref(R.string.pref_main_show_page_header_key, !getShowHeader());
  }

  public boolean isSlideMode() {
    return getBoolPref(R.string.pref_main_page_view_key);
  }

  public boolean isSlideModeAndNotInSearch() {
    return isSlideMode() && !mIsSearchStarted;
  }

  public void toggleSlideMode() {
    savePref(R.string.pref_main_page_view_key, !isSlideMode());
  }

  public boolean breakAayahs() {
    return getBoolPref(R.string.pref_main_break_aayahs_key);
  }

  public void toggleAayahBreaks() {
    savePref(R.string.pref_main_break_aayahs_key, !breakAayahs());
  }

  public boolean showSingleAayah() {
    return breakAayahs() || mIsSearchStarted || (transEnabled() && showTransWithText());
  }

  private boolean mIsSearchStarted = false;

  public void setSearchStarted(boolean searchStarted) {
    mIsSearchStarted = searchStarted;
  }

  private String mSearchQuery;

  public void setSearchQuery(String query) {
    mSearchQuery = query;
  }

  public boolean isSearchStarted() {
    return mIsSearchStarted;
  }

  public boolean isSearching() {
    return mSearchQuery != null;
  }

  public String getSearchQuery() {
    return mSearchQuery;
  }

  private int mLastFontFile = -1;
  private Typeface mTypeface;

  public Typeface getTypeface() {
    String font = getFontName();
    int fontFile = 0;
    if (getString(R.string.font_warsh).equals(font)) {
      fontFile = R.string.font_file_warsh;
    } else if (getString(R.string.font_saleem).equals(font)) {
      fontFile = R.string.font_file_saleem;
    } else if (getString(R.string.font_scheherazade).equals(font)) {
      fontFile = R.string.font_file_scheherazade;
    } else if (getString(R.string.font_noor_e_huda).equals(font)) {
      fontFile = R.string.font_file_noor_e_huda;
    } else if (getString(R.string.font_me_quran).equals(font)) {
      fontFile = R.string.font_file_me_quran;
    } else if (getString(R.string.font_kitab).equals(font)) {
      fontFile = R.string.font_file_kitab;
    }

    if (fontFile == mLastFontFile) {
      return mTypeface;
    }

    mLastFontFile = fontFile;

    File file;
    if (fontFile != 0 && (file = getFontFile(getString(fontFile))).exists()) {
      mTypeface = createTypeface(file);
    } else {
      mTypeface = null;
    }
    if (mTypeface == null) {
      mTypeface = ResourcesCompat.getFont(App.getCxt(), R.font.uthmanic_hafs1_ver17);
    }
    return mTypeface;
  }

  public File getFontFile(String fontFileName) {
    return getDownloadedFile(fontFileName + ".ttf");
  }

  private Typeface createTypeface(File file) {
    try {
      return Typeface.createFromFile(file);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getFontName() {
    return getStringPref(R.string.pref_main_font_nb_key);
  }

  public void setFont(String fontName) {
    savePref(R.string.pref_main_font_nb_key, fontName);
  }

  public boolean fontSupportsSymbols() {
    String font = getFontName();
    return getString(R.string.font_hafs).equals(font)
        || getString(R.string.font_warsh).equals(font);
  }

  public String getTransFontFile(int index) {
    TypedArray ar = App.getRes().obtainTypedArray(R.array.trans_font_file_names);
    int fontFileResId = ar.getResourceId(index, 0);
    ar.recycle();
    if (fontFileResId != 0) {
      return getString(fontFileResId);
    }
    return null;
  }

  private String mLastTransForTypeface;
  private Typeface mTransTypeface;

  public Typeface getTransTypeface() {
    String dbName = getTransDbName();
    if (dbName.equals(mLastTransForTypeface)) {
      return mTransTypeface;
    }
    mLastTransForTypeface = dbName;

    List<String> transDbs = Arrays.asList(App.getRes().getStringArray(R.array.db_trans_files));
    String fontFile = getTransFontFile(transDbs.indexOf(dbName));

    File file;
    if (fontFile != null && (file = getFontFile(fontFile)).exists()) {
      mTransTypeface = createTypeface(file);
    } else {
      mTransTypeface = Typeface.DEFAULT; // Or null
    }
    return mTransTypeface;
  }

  public void resetTypeface() {
    mLastFontFile = -1;
    mLastTransForTypeface = null;
  }

  private static final int COLOR_DEFAULT = -1;
  private static final int COLOR_V_LIGHT = 0;
  private static final int COLOR_LIGHT = 1;
  private static final int COLOR_MEDIUM = 2;
  private static final int COLOR_SHARP = 3;
  private static final int COLOR_V_SHARP = 4;
  public static final int COLOR_COUNT = 5;

  public int getBgColorSliderVal() {
    int color = getIntPref(R.string.pref_main_bg_color_key);
    if (color < COLOR_DEFAULT || color > COLOR_V_SHARP) {
      color = COLOR_DEFAULT;
    }
    return color + 1;
  }

  public void setBgColor(int color) {
    color--;
    if (color < COLOR_DEFAULT || color > COLOR_V_SHARP) {
      color = COLOR_DEFAULT;
    }
    if (color == COLOR_DEFAULT) {
      Utils.showShortToast(R.string._default);
    }
    savePref(R.string.pref_main_bg_color_key, color);
  }

  public @ColorRes int getBgColor() {
    int color = getIntPref(R.string.pref_main_bg_color_key);
    if (color == COLOR_V_SHARP) {
      return R.color.bgSharp2;
    } else if (color == COLOR_SHARP) {
      return R.color.bgSharp1;
    } else if (color == COLOR_MEDIUM) {
      return R.color.bgMedium;
    } else if (color == COLOR_LIGHT) {
      return R.color.bgLight1;
    } else if (color == COLOR_V_LIGHT) {
      return R.color.bgLight2;
    } else {
      return -1;
    }
  }

  public int getFontColorSliderVal() {
    int color = getIntPref(R.string.pref_main_font_color_key);
    if (color < COLOR_DEFAULT || color > COLOR_V_SHARP) {
      color = COLOR_DEFAULT;
    }
    return color + 1;
  }

  public void setFontColor(int color) {
    color--;
    if (color < COLOR_DEFAULT || color > COLOR_V_SHARP) {
      color = COLOR_DEFAULT;
    }
    if (color == COLOR_DEFAULT) {
      Utils.showShortToast(R.string._default);
    }
    savePref(R.string.pref_main_font_color_key, color);
  }

  public @ColorRes int getFontColor() {
    int color = getIntPref(R.string.pref_main_font_color_key);
    if (color == COLOR_V_SHARP) {
      return R.color.fgSharp2;
    } else if (color == COLOR_SHARP) {
      return R.color.fgSharp1;
    } else if (color == COLOR_MEDIUM) {
      return R.color.fgMedium;
    } else if (color == COLOR_LIGHT) {
      return R.color.fgLight1;
    } else if (color == COLOR_V_LIGHT) {
      return R.color.fgLight2;
    } else {
      return COLOR_DEFAULT;
    }
  }

  public static final int FONT_SIZE_MIN = 12, FONT_SIZE_MAX = 28;

  public int getFontSizeSliderVal() {
    return getFontSize() - FONT_SIZE_MIN;
  }

  public void setFontSize(int size) {
    size += 12;
    if (size > FONT_SIZE_MAX) {
      size = FONT_SIZE_MAX;
    } else if (size < FONT_SIZE_MIN) {
      size = FONT_SIZE_MIN;
    }
    savePref(R.string.pref_main_font_size_key, size);
    onFontSizeChanged();
  }

  public void increaseFontSize() {
    int size = getIntPref(R.string.pref_main_font_size_key);
    if (size >= FONT_SIZE_MAX) {
      return;
    }
    savePref(R.string.pref_main_font_size_key, Math.min(FONT_SIZE_MAX, size + 1));
    onFontSizeChanged();
  }

  public void decreaseFontSize() {
    int size = getIntPref(R.string.pref_main_font_size_key);
    if (size <= FONT_SIZE_MIN) {
      return;
    }
    savePref(R.string.pref_main_font_size_key, Math.max(FONT_SIZE_MIN, size - 1));
    onFontSizeChanged();
  }

  public int getFontSize() {
    return getIntPref(R.string.pref_main_font_size_key);
  }

  public int getArabicFontSize() {
    int size = getFontSize();
    String font = getFontName();
    if (getString(R.string.font_saleem).equals(font)) {
      size *= 1.1;
    } else if (getString(R.string.font_kitab).equals(font)) {
      size *= 0.95;
    } else if (getString(R.string.font_scheherazade).equals(font)) {
      size *= 0.9;
    } else if (getString(R.string.font_me_quran).equals(font)) {
      size *= 0.85;
    }
    return size;
  }

  private final MutableLiveData<Void> mFontSizeChangedNotifier = new MutableLiveData<>();

  private void onFontSizeChanged() {
    Utils.runBg(() -> mFontSizeChangedNotifier.postValue(null));
  }

  public LiveData<Void> getFontSizeChanged() {
    return mFontSizeChangedNotifier;
  }

  public int getLastPage() {
    return getIntPref(R.string.pref_main_last_page_key);
  }

  public int getLastAayah() {
    return getIntPref(R.string.pref_main_last_aayah_key);
  }

  public void setScrollPosition(int page, int aayahId) {
    if (page < 0 || aayahId < 0) {
      return;
    }
    savePref(R.string.pref_main_last_page_key, page);
    savePref(R.string.pref_main_last_aayah_key, aayahId);
  }

  public int getLastQuranicDua() {
    return getIntPref(R.string.pref_main_last_quranic_dua_key);
  }

  public void setQuranicDuaScrollPosition(int pos) {
    savePref(R.string.pref_main_last_quranic_dua_key, pos);
  }

  public int getLastMasnoonDua() {
    return getIntPref(R.string.pref_main_last_masnoon_dua_key);
  }

  public void setMasnoonDuaScrollPosition(int pos) {
    savePref(R.string.pref_main_last_masnoon_dua_key, pos);
  }

  public int getLastOccasionsDua() {
    return getIntPref(R.string.pref_main_last_occasions_dua_key);
  }

  public void setOccasionsDuaScrollPosition(int pos) {
    savePref(R.string.pref_main_last_occasions_dua_key, pos);
  }

  public void setDuaPageScrollPos(int page) {
    savePref(R.string.pref_main_last_dua_page_key, page);
  }

  public int getLastDuaPage() {
    return getIntPref(R.string.pref_main_last_dua_page_key);
  }

  public int getNavigatorType() {
    return getIntPref(R.string.pref_main_number_picker_type_key);
  }

  public int getNavigatorValue() {
    return getIntPref(R.string.pref_main_number_picker_value_key);
  }

  public void setNavigatorState(int type, int value) {
    savePref(R.string.pref_main_number_picker_type_key, type);
    savePref(R.string.pref_main_number_picker_value_key, value);
  }

  public Set<Integer> getBookmarks() {
    Set<String> bookmarks = getSetPref(R.string.pref_main_bookmarks_key);
    if (bookmarks == null) {
      return new HashSet<>();
    }
    Set<Integer> aayahIds = new HashSet<>();
    for (String id : bookmarks) {
      aayahIds.add(Integer.parseInt(id));
    }
    return aayahIds;
  }

  private final Object BOOKMARK_LOCK = new Object();

  public void addBookmark(int aayahId) {
    synchronized (BOOKMARK_LOCK) {
      Set<String> bookmarks = getSetPref(R.string.pref_main_bookmarks_key);
      if (bookmarks == null) {
        bookmarks = new HashSet<>();
      } else {
        bookmarks = new HashSet<>(bookmarks);
      }
      bookmarks.add(String.valueOf(aayahId));
      savePref(R.string.pref_main_bookmarks_key, bookmarks);
    }
  }

  public void removeBookmark(int aayahId) {
    synchronized (BOOKMARK_LOCK) {
      Set<String> bookmarks = getSetPref(R.string.pref_main_bookmarks_key);
      if (bookmarks == null) {
        return;
      }
      bookmarks = new HashSet<>(bookmarks);
      bookmarks.remove(String.valueOf(aayahId));
      savePref(R.string.pref_main_bookmarks_key, bookmarks);
    }
  }

  public File getDownloadedFile(String file) {
    File downloadDir = new File(App.getCxt().getExternalFilesDir(null), "downloads");
    if (!downloadDir.exists() && !downloadDir.mkdirs()) {
      throw new Error("Failed to create directory " + downloadDir);
    }
    return new File(downloadDir, file);
  }

  public static boolean isQuranDb(String dbName) {
    return Arrays.asList(App.getRes().getStringArray(R.array.db_text_files)).contains(dbName);
  }

  public static boolean isTranslationDb(String dbName) {
    return Arrays.asList(App.getRes().getStringArray(R.array.db_trans_files)).contains(dbName);
  }

  public boolean doSearchInTranslation() {
    return getBoolPref(R.string.pref_main_trans_search_key);
  }

  public void toggleSearchInTranslation() {
    savePref(R.string.pref_main_trans_search_key, !doSearchInTranslation());
  }

  public boolean doSearchWithVowels() {
    return getBoolPref(R.string.pref_main_vowels_search_key);
  }

  public void toggleSearchWithVowels() {
    savePref(R.string.pref_main_vowels_search_key, !doSearchWithVowels());
  }

  private float mBrightness = BRIGHTNESS_OVERRIDE_NONE;

  public float getBrightness() {
    return mBrightness;
  }

  public void saveBrightness(float brightness) {
    mBrightness = brightness;
  }

  public String getCountryCode() {
    String code = getStringPref(R.string.pref_prayer_country_code_key);
    if (code == null || code.equals("null")) {
      return null;
    }
    return code;
  }

  public void setCountryCode(String countryCode) {
    savePref(R.string.pref_prayer_country_code_key, countryCode);
  }

  public String getCityName() {
    String name = getStringPref(R.string.pref_prayer_city_name_key);
    if (name == null || name.equals("null")) {
      return null;
    }
    return name;
  }

  public void setCityName(String cityName) {
    savePref(R.string.pref_prayer_city_name_key, cityName);
  }

  public Coordinates getLngLat() {
    int invalid = Utils.getInteger(R.integer.invalid_coordinate);
    float lat = getFloatPref(R.string.pref_prayer_latitude_key);
    float lng = getFloatPref(R.string.pref_prayer_longitude_key);
    if (lat == invalid || lng == invalid) {
      return null;
    }
    return new Coordinates(lat, lng);
  }

  public void saveLngLat(float lng, float lat) {
    savePref(R.string.pref_prayer_longitude_key, lng);
    savePref(R.string.pref_prayer_latitude_key, lat);
  }

  public void removeLngLat() {
    float invalid = Utils.getInteger(R.integer.invalid_coordinate);
    savePref(R.string.pref_prayer_longitude_key, invalid);
    savePref(R.string.pref_prayer_latitude_key, invalid);
  }

  public String getLocTimeZoneId() {
    String id = getStringPref(R.string.pref_prayer_loc_timezone_key);
    if (id == null || id.equals("null")) {
      return null;
    }
    return id;
  }

  public void setLocTimeZoneId(String timeZoneId) {
    savePref(R.string.pref_prayer_loc_timezone_key, timeZoneId);
  }

  public int getHijriOffset() {
    return getIntPref(R.string.pref_prayer_hijri_offset_key);
  }

  public void setHijriOffset(int offset) {
    savePref(R.string.pref_prayer_hijri_offset_key, offset);
  }

  public boolean getPrayerNotify(int order) {
    return mPrefs.getBoolean(getPrayerNotifyPrefString(order), order != 1);
  }

  public void setPrayerNotify(int order, boolean notify) {
    mPrefs.edit().putBoolean(getPrayerNotifyPrefString(order), notify).apply();
  }

  private String getPrayerNotifyPrefString(int order) {
    return "pref_prayer_notify_" + order;
  }

  public boolean getPrayerAdhan(int order) {
    return mPrefs.getBoolean(getPrayerAdhanPrefString(order), false);
  }

  public void setPrayerAdhan(int order, boolean adhan) {
    mPrefs.edit().putBoolean(getPrayerAdhanPrefString(order), adhan).apply();
  }

  private String getPrayerAdhanPrefString(int order) {
    return "pref_prayer_adhan_" + order + "_nb";
  }

  public int getPrayerNotifyOffset(int order) {
    return mPrefs.getInt(getPrayerNotifyOffsetPrefString(order), 0);
  }

  public void setPrayerNotifyOffset(int order, int offset) {
    mPrefs.edit().putInt(getPrayerNotifyOffsetPrefString(order), offset).apply();
  }

  private String getPrayerNotifyOffsetPrefString(int order) {
    return "pref_prayer_notify_offset_" + order;
  }

  public int getCalcMethod() {
    return getIntPref(R.string.pref_prayer_calc_method_key);
  }

  public void setCalcMethod(int method) {
    savePref(R.string.pref_prayer_calc_method_key, method);
  }

  public int getPrayerAdj(int order) {
    return mPrefs.getInt(getPrayerAdjPrefString(order), 0);
  }

  public void setPrayerAdj(int order, int adj) {
    mPrefs.edit().putInt(getPrayerAdjPrefString(order), adj).apply();
  }

  private String getPrayerAdjPrefString(int order) {
    return "pref_prayer_time_adj_" + order;
  }

  public int getAsrCalcMethod() {
    return getIntPref(R.string.pref_prayer_asr_calc_method_key);
  }

  public void setAsrCalcMethod(int method) {
    savePref(R.string.pref_prayer_asr_calc_method_key, method);
  }

  public int getHighLatMethod() {
    return getIntPref(R.string.pref_prayer_high_lat_method_key);
  }

  public void setHighLatMethod(int method) {
    savePref(R.string.pref_prayer_high_lat_method_key, method);
  }

  public boolean getShowWidgetNotif() {
    return getBoolPref(R.string.pref_prayer_widget_notif_key);
  }

  public void setShowWidgetNotif(boolean show) {
    savePref(R.string.pref_prayer_widget_notif_key, show);
  }

  public boolean shouldAskToExcBatteryOpt() {
    return getShowWidgetNotif() && getBoolPref(R.string.pref_prayer_ask_to_exclude_batt_opt_nb_key);
  }

  public void doNotAskToExcBatteryOpt() {
    savePref(R.string.pref_prayer_ask_to_exclude_batt_opt_nb_key, false);
  }

  public int getHomeWidgetStyle() {
    return getIntPref(R.string.pref_prayer_home_widget_style_key);
  }

  public void setHomeWidgetStyle(int style) {
    savePref(R.string.pref_prayer_home_widget_style_key, style);
  }

  public boolean isValidBkpPrefKey(String key) {
    for (int i = 0; i < 6; i++) {
      if (key.equals(getPrayerAdjPrefString(i))
          || key.equals(getPrayerNotifyPrefString(i))
          || key.equals(getPrayerNotifyOffsetPrefString(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean mLogging = false;

  public boolean isLogging() {
    return mLogging;
  }

  public void setLogging(boolean logging) {
    mLogging = logging;
  }

  public void plusAppLaunchCount() {
    int appLaunchCountId = R.string.pref_main_app_launch_count_for_feedback_nb_key;
    savePref(appLaunchCountId, getIntPref(appLaunchCountId) + 1);
  }

  public boolean shouldAskForFeedback() {
    long lastTS = getLongPref(R.string.pref_main_ask_for_feedback_ts_nb_key);
    if (lastTS == 0) {
      setAskForFeedbackTs(System.currentTimeMillis());
      return false;
    }
    int appLaunchCountId = R.string.pref_main_app_launch_count_for_feedback_nb_key;
    boolean ask = getIntPref(appLaunchCountId) >= 10;
    ask = ask && (System.currentTimeMillis() - lastTS) >= TimeUnit.DAYS.toMillis(10);
    if (ask) {
      savePref(appLaunchCountId, 0);
      setAskForFeedbackTs(System.currentTimeMillis());
    }
    return ask;
  }

  public void setAskForFeedbackTs(long ts) {
    savePref(R.string.pref_main_ask_for_feedback_ts_nb_key, ts);
  }
}
