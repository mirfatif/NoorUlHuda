package com.mirfatif.noorulhuda.quran;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getString;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.db.TagAayahsDao;
import com.mirfatif.noorulhuda.db.TagAayahsEntity;
import com.mirfatif.noorulhuda.db.TagEntity;
import com.mirfatif.noorulhuda.db.TagsDao;
import com.mirfatif.noorulhuda.prayer.WidgetProvider;
import com.mirfatif.noorulhuda.quran.MainActivity.RestorePosType;
import com.mirfatif.noorulhuda.svc.PrayerNotifySvc;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BackupRestore {

  private static final String TAG = "BackupRestore";

  private final MainActivity mA;
  private final ActivityResultLauncher<String> mBackupLauncher;
  private final ActivityResultLauncher<String[]> mRestoreLauncher;

  BackupRestore(MainActivity activity) {
    mA = activity;

    // registerForActivityResult() must be called before onStart() is called
    ActivityResultCallback<Uri> backupCallback =
        uri -> Utils.runBg(() -> doBackupRestore(true, uri));
    mBackupLauncher =
        mA.registerForActivityResult(new ActivityResultContracts.CreateDocument(), backupCallback);

    ActivityResultCallback<Uri> restoreCallback =
        uri -> Utils.runBg(() -> doBackupRestore(false, uri));
    mRestoreLauncher =
        mA.registerForActivityResult(new ActivityResultContracts.OpenDocument(), restoreCallback);
  }

  AlertDialog createDialog() {
    return new Builder(mA)
        .setPositiveButton(R.string.backup, (d, which) -> doBackupRestore(true))
        .setNegativeButton(R.string.restore, (d, which) -> doBackupRestore(false))
        .setTitle(getString(R.string.backup) + " / " + getString(R.string.restore))
        .setMessage(R.string.choose_backup_restore)
        .create();
  }

  private void doBackupRestore(boolean isBackup) {
    Utils.showToast(R.string.select_backup_file);
    if (isBackup) {
      mBackupLauncher.launch("NoorUlHuda_" + Utils.getCurrDateTime(false) + ".xml");
    } else {
      mRestoreLauncher.launch(new String[] {"text/xml"});
    }
  }

  private void doBackupRestore(boolean isBackup, Uri uri) {
    if (uri == null) {
      return;
    }

    Utils.runUi(mA, () -> showProgressDialog(isBackup ? R.string.backup : R.string.restore))
        .waitForMe();

    if (isBackup) {
      try (OutputStream os = App.getCxt().getContentResolver().openOutputStream(uri, "w")) {
        backup(os);
        Utils.showToast(R.string.backup_success);
      } catch (IOException | TransformerException e) {
        e.printStackTrace();
        Utils.showToast(R.string.backup_failed);
      }
    } else {
      try (InputStream is = App.getCxt().getContentResolver().openInputStream(uri)) {
        restore(is);
        Utils.showToast(R.string.restore_success);
        PrayerNotifySvc.reset(false);
        WidgetProvider.reset();
        mA.refreshUi(RestorePosType.SAVED);
      } catch (IOException | XmlPullParserException e) {
        e.printStackTrace();
        Utils.showToast(R.string.restore_failed);
      }
    }
    dismissProgressDialog();
  }

  // Root element tag
  private static final String ROOT = "NoorUlHuda";

  // SharedPreferences XML tags
  private static final String PREFERENCES = "preferences";
  private static final String PREF = "pref";

  // SharedPreferences XML entry attributes
  private static final String KEY = "key";
  private static final String VALUE = "value";
  private static final String TYPE = "type";

  // SharedPreferences types
  private static final String BOOLEAN = "boolean";
  private static final String FLOAT = "float";
  private static final String INT = "int";
  private static final String LONG = "long";
  private static final String SET = "Set";
  private static final String STRING = "String";

  // XML tags for Aayah tags
  private static final String AAYAH_TAGS = "tags";
  private static final String AAYAH_TAG = "tag";

  // Aayah tags XML entry attributes
  private static final String TITLE = "title";
  private static final String DESC = "desc";
  private static final String AAYAH_IDS = "aayah_ids";
  private static final String TIMESTAMP = "timestamp";

  // Separator for Set / List elements
  private static final String SEPARATOR = ",";

  private void backup(OutputStream outputStream) throws IOException, TransformerException {
    XmlSerializer serializer = Xml.newSerializer();
    StringWriter stringWriter = new StringWriter();

    serializer.setOutput(stringWriter);
    serializer.startDocument("UTF-8", true);
    serializer.startTag(null, ROOT);
    serializer.startTag(null, PREFERENCES);

    // Preferences
    Map<String, ?> prefEntries = Utils.getDefPrefs().getAll();
    for (Map.Entry<String, ?> entry : prefEntries.entrySet()) {
      String key = entry.getKey();
      if (!isValidPrefKey(key)) {
        Log.i(TAG, "Backup: Skipping " + key);
        continue;
      }

      Object value = entry.getValue();
      String type;

      if (value instanceof Boolean) {
        type = BOOLEAN;
      } else if (value instanceof Float) {
        type = FLOAT;
      } else if (value instanceof Integer) {
        type = INT;
      } else if (value instanceof Long) {
        type = LONG;
      } else if (value instanceof Set) {
        type = SET;
        StringBuilder stringBuilder = new StringBuilder();
        for (Object object : (Set<?>) value) {
          if (stringBuilder.length() != 0) {
            // Append String split separator after every package/permission name
            stringBuilder.append(SEPARATOR);
          }
          stringBuilder.append(object.toString());
        }
        value = stringBuilder;
      } else if (value instanceof String) {
        type = STRING;
      } else {
        Log.e(TAG, "Unknown preference type: " + value.toString());
        continue;
      }

      serializer.startTag(null, PREF);
      serializer.attribute(null, KEY, key);
      serializer.attribute(null, VALUE, value.toString());
      serializer.attribute(null, TYPE, type);
      serializer.endTag(null, PREF);
    }

    serializer.endTag(null, PREFERENCES);

    // Tags
    serializer.startTag(null, AAYAH_TAGS);
    List<TagEntity> tags = SETTINGS.getTagsDb().getTags();
    TagAayahsDao tagAayahsDb = SETTINGS.getTagAayahsDb();

    for (TagEntity tag : tags) {
      StringBuilder stringBuilder = new StringBuilder();
      for (Integer id : tagAayahsDb.getAayahIds(tag.id)) {
        if (stringBuilder.length() != 0) {
          stringBuilder.append(SEPARATOR);
        }
        stringBuilder.append(id.toString());
      }
      String aayahIds = stringBuilder.toString();

      serializer.startTag(null, AAYAH_TAG);
      serializer.attribute(null, TITLE, tag.title);
      serializer.attribute(null, DESC, tag.desc == null ? "" : tag.desc);
      serializer.attribute(null, AAYAH_IDS, aayahIds);
      serializer.attribute(null, TIMESTAMP, String.valueOf(tag.timeStamp));
      serializer.endTag(null, AAYAH_TAG);
    }

    serializer.endTag(null, AAYAH_TAGS);
    serializer.endTag(null, ROOT);
    serializer.endDocument();
    serializer.flush();

    // Pretty formatting
    Source input = new StreamSource(new StringReader(stringWriter.toString()));
    StreamResult output = new StreamResult(outputStream);

    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(input, output);

    stringWriter.flush();
  }

  private void restore(InputStream inputStream) throws IOException, XmlPullParserException {
    // Create a copy of InputStream before consuming it
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = inputStream.read(buffer)) > -1) {
      byteArrayOutputStream.write(buffer, 0, len);
    }
    byteArrayOutputStream.flush();

    InputStream inputStream1 = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    InputStream inputStream2 = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    XmlPullParser xmlParser = Xml.newPullParser();
    xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

    // Preferences
    SharedPreferences.Editor prefEdit = Utils.getDefPrefs().edit();
    xmlParser.setInput(inputStream1, null);
    boolean rootTagFound = false;
    boolean mainTagFound = false;
    while (true) {
      int eventType = xmlParser.next(); // Get the next parsing event
      if (eventType == XmlPullParser.END_DOCUMENT) {
        break;
      }

      String tagName = xmlParser.getName();
      if (eventType == XmlPullParser.START_TAG && tagName.equals(ROOT)) {
        rootTagFound = true;
      }
      if (eventType == XmlPullParser.START_TAG && tagName.equals(PREFERENCES)) {
        mainTagFound = true;
      }

      if (!rootTagFound || !mainTagFound) {
        continue;
      }

      // If we reach the end of "preferences" tag
      if (eventType == XmlPullParser.END_TAG && tagName.equals(PREFERENCES)) {
        break;
      }

      // If we are at the start of "pref" tag
      if (eventType == XmlPullParser.START_TAG && tagName.equals(PREF)) {
        String key = xmlParser.getAttributeValue(null, KEY);
        String value = xmlParser.getAttributeValue(null, VALUE);
        String type = xmlParser.getAttributeValue(null, TYPE);
        if (!isValidPrefKey(key)) {
          Log.e(TAG, "Invalid preference: " + key);
          continue;
        }

        switch (type) {
          case BOOLEAN:
            prefEdit.putBoolean(key, Boolean.parseBoolean(value));
            break;
          case FLOAT:
            prefEdit.putFloat(key, Float.parseFloat(value));
            break;
          case INT:
            prefEdit.putInt(key, Integer.parseInt(value));
            break;
          case LONG:
            prefEdit.putLong(key, Long.parseLong(value));
            break;
          case SET:
            if (value.length() == 0) {
              // Do not save empty string to Set
              prefEdit.putStringSet(key, new HashSet<>());
            } else {
              prefEdit.putStringSet(key, new HashSet<>(Arrays.asList(value.split(SEPARATOR))));
            }
            break;
          case STRING:
            prefEdit.putString(key, value);
            break;
          default:
            Log.e(TAG, "Unknown preference type: " + type);
            break;
        }
      }
    }

    prefEdit.apply();
    PrayerNotifySvc.reset(false);
    WidgetProvider.reset();

    // Tags
    List<TagEntity> tags = new ArrayList<>();
    xmlParser.setInput(inputStream2, null);
    rootTagFound = mainTagFound = false;
    while (true) {
      int eventType = xmlParser.next(); // Get the next parsing event
      if (eventType == XmlPullParser.END_DOCUMENT) {
        break;
      }

      String tagName = xmlParser.getName();
      if (eventType == XmlPullParser.START_TAG && tagName.equals(ROOT)) {
        rootTagFound = true;
      }
      if (eventType == XmlPullParser.START_TAG && tagName.equals(AAYAH_TAGS)) {
        mainTagFound = true;
      }

      if (!rootTagFound || !mainTagFound) {
        continue;
      }

      // If we reach the end of "tags" tag
      if (eventType == XmlPullParser.END_TAG && tagName.equals(AAYAH_TAGS)) {
        break;
      }

      // If we are at the start of "tag" tag
      if (eventType == XmlPullParser.START_TAG && tagName.equals(AAYAH_TAG)) {
        TagEntity tag = new TagEntity();
        tag.title = xmlParser.getAttributeValue(null, TITLE);
        tag.desc = xmlParser.getAttributeValue(null, DESC);
        if (TextUtils.isEmpty(tag.desc)) {
          tag.desc = null;
        }
        for (String id : xmlParser.getAttributeValue(null, AAYAH_IDS).split(SEPARATOR)) {
          if (!TextUtils.isEmpty(id)) {
            tag.aayahIds.add(Integer.parseInt(id));
          }
        }
        tag.timeStamp = Long.parseLong(xmlParser.getAttributeValue(null, TIMESTAMP));
        tags.add(tag);
      }
    }

    TagsDao tagsDb = SETTINGS.getTagsDb();
    TagAayahsDao tagAayahsDb = SETTINGS.getTagAayahsDb();

    for (TagEntity newTag : tags) {
      TagEntity oldTag = tagsDb.getTag(newTag.title, newTag.desc, newTag.timeStamp);
      if (oldTag != null) {
        List<Integer> aayahIds = tagAayahsDb.getAayahIds(oldTag.id);
        if (new HashSet<>(aayahIds).equals(newTag.aayahIds)) {
          continue;
        }
      }
      int tagId = (int) tagsDb.create(newTag);
      for (int aayahId : newTag.aayahIds) {
        tagAayahsDb.create(new TagAayahsEntity(tagId, aayahId));
      }
    }
  }

  private final List<String> mPrefKeys = new ArrayList<>();

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isValidPrefKey(String prefKey) {
    if (SETTINGS.isValidBkpPrefKey(prefKey)) {
      return true;
    }
    if (mPrefKeys.isEmpty()) {
      for (Field field : R.string.class.getDeclaredFields()) {
        String strName = field.getName();
        if (!strName.startsWith("pref_")
            || !strName.endsWith("_key")
            || strName.endsWith("_nb_key")) {
          continue;
        }

        Integer strKeyResId =
            Utils.getStaticIntField(strName, R.string.class, TAG + ": isValidPrefKey");
        if (strKeyResId != null) {
          mPrefKeys.add(getString(strKeyResId));
        }
      }
    }
    return mPrefKeys.contains(prefKey);
  }

  private final Handler DELAYED_POSTER = new Handler(Looper.getMainLooper());
  private Runnable mCallback;
  private AlertDialogFragment mDialog;

  private void showProgressDialog(@StringRes int resId) {
    Builder builder = new Builder(mA).setTitle(resId).setView(R.layout.dialog_progress);
    mDialog = new AlertDialogFragment();
    mDialog.setCancelable(false);
    mCallback = () -> AlertDialogFragment.show(mA, mDialog, builder.create(), "BACKUP_RESTORE");
    DELAYED_POSTER.postDelayed(mCallback, 500);
  }

  private void dismissProgressDialog() {
    DELAYED_POSTER.removeCallbacks(mCallback);
    DELAYED_POSTER.postDelayed(() -> mDialog.dismissIt(), 500);
  }
}
