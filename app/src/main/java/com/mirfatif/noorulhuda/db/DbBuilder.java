package com.mirfatif.noorulhuda.db;

import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.Utils.getString;

import android.util.Log;
import android.util.Xml;
import androidx.room.Room;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.prefs.MySettings;
import com.mirfatif.noorulhuda.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DbBuilder {

  private static final String TAG = "DbBuilder";

  private DbBuilder() {}

  public static final int TOTAL_AAYAHS = 6350; // 6236 + 114
  public static final int TOTAL_PAGES = 604;
  public static final int TOTAL_SURAHS = 114;
  public static final int TOTAL_MANZILS = 7;
  public static final int TOTAL_JUZS = 30;
  private static final int LAST_SURAH_AAYAHS = 6;

  public static final String MAIN_DB = getString(R.string.db_main);

  private static final String QURAN_TAG = "quran";
  private static final String SURAH_TAG = "sura";
  private static final String AAYAH_TAG = "aya";
  private static final String ATTR_INDEX = "index";
  private static final String ATTR_TEXT = "text";

  private static final String SURAHS_TAG = "suras";
  private static final String ATTR_AAYAHS = "ayas";
  private static final String ATTR_RUKUS = "rukus";
  private static final String ATTR_NAME = "name";
  private static final String ATTR_ORDER = "order";
  private static final String ATTR_TYPE = "type";

  private static final String JUZS_TAG = "juzs";
  private static final String JUZ_TAG = "juz";
  private static final String MANZILS_TAG = "manzils";
  private static final String MANZIL_TAG = "manzil";
  private static final String PAGES_TAG = "pages";
  private static final String PAGE_TAG = "page";
  private static final String RUKUS_TAG = "rukus";
  private static final String RUKU_TAG = "ruku";
  private static final String HIZBS_TAG = "hizbs";
  private static final String HIZB_TAG = "quarter";
  private static final String SAJDAS_TAG = "sajdas";
  private static final String SAJDA_TAG = "sajda";

  // Handle concurrent calls e.g. from MainActivity#onCreate()
  private static final Object DB_BUILD_LOCK = new Object();

  public static boolean buildDb(String dbName) {
    synchronized (DB_BUILD_LOCK) {
      if (SETTINGS.isDbBuilt(dbName)) {
        return true;
      }
      return new DbBuilder().build(dbName);
    }
  }

  private boolean build(String dbName) {
    QuranDatabase db =
        Room.databaseBuilder(App.getCxt(), QuranDatabase.class, dbName + ".db").build();
    QuranDao dao = db.getDao();

    try (InputStream inputStream = getStream(dbName)) {
      XmlPullParser xmlParser = Xml.newPullParser();
      boolean quranTag = false;
      int surahNum = -1, lastSurahNum = 0, lastAayahNum = -1;

      xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlParser.setInput(inputStream, null);

      List<AayahEntity> aayahEntities = new ArrayList<>();

      while (true) {
        int eventType = xmlParser.next(); // Get the next parsing event
        if (eventType == XmlPullParser.END_DOCUMENT) {
          break;
        }

        String tagName = xmlParser.getName();

        if (eventType == XmlPullParser.START_TAG) {
          switch (tagName) {
            case QURAN_TAG:
              quranTag = true;
              break;
            case SURAH_TAG:
              surahNum = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_INDEX));
              lastSurahNum++;
              if (surahNum != lastSurahNum) {
                throw new BadXmlFormatException(
                    "Surah " + surahNum + " comes after " + lastSurahNum);
              }
              int id = getId();
              aayahEntities.add(new AayahEntity(id, surahNum, 0));
              lastAayahNum = 0;
              break;
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          switch (tagName) {
            case QURAN_TAG:
              quranTag = false;
              break;
            case SURAH_TAG:
              surahNum = lastAayahNum = -1;
              break;
          }
        }

        if (!quranTag
            || surahNum == -1
            || eventType != XmlPullParser.START_TAG
            || !tagName.equals(AAYAH_TAG)) {
          continue;
        }

        int aayahNum = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_INDEX));
        lastAayahNum++;
        if (aayahNum != lastAayahNum) {
          throw new BadXmlFormatException(
              "Aayah " + aayahNum + " comes after " + lastAayahNum + " in Surah " + surahNum);
        }
        AayahEntity entity = new AayahEntity(getId(), surahNum, aayahNum);
        entity.text = xmlParser.getAttributeValue(null, ATTR_TEXT);
        aayahEntities.add(entity);
      }

      dao.insertAll(aayahEntities);

      if (MySettings.isQuranDb(dbName)) {
        mQuranMetaDao = SETTINGS.getMetaDb();
        if (dbName.equals(MAIN_DB)) {
          buildMetadata();
        }
        insertMetadata(JUZS_TAG, JUZ_TAG, dao::insertJuz, dao::addJuzStarts);
        insertMetadata(MANZILS_TAG, MANZIL_TAG, dao::insertManzil, dao::addManzilStarts);
        insertMetadata(PAGES_TAG, PAGE_TAG, dao::insertPage, null);
        insertMetadata(RUKUS_TAG, RUKU_TAG, null, dao::addRukuEnds);
        insertMetadata(HIZBS_TAG, HIZB_TAG, null, dao::addHizbEnds);
        insertMetadata(SAJDAS_TAG, SAJDA_TAG, null, dao::addHasSajda);
        insertAayahGroupPositions(dao);
      }

      SETTINGS.setDbBuilt(dbName);

      return true;
    } catch (IOException | XmlPullParserException | NumberFormatException e) {
      e.printStackTrace();
    } catch (BadXmlFormatException e) {
      Log.e(TAG, e.toString());
    } finally {
      File file = SETTINGS.getDownloadedFile(dbName + ".xml");
      if (file.exists() && !file.delete()) {
        Log.e(TAG, "Deleting " + file.getAbsolutePath() + " failed");
      }
      db.close();
    }

    Utils.showToast(R.string.failed_to_build_database);
    return false;
  }

  private InputStream getStream(String dbName) throws IOException {
    if (dbName.equals(MAIN_DB)) {
      return App.getCxt().getAssets().open(dbName + ".xml");
    } else {
      return new FileInputStream(SETTINGS.getDownloadedFile(dbName + ".xml"));
    }
  }

  private QuranMetaDao mQuranMetaDao;

  private void buildMetadata() throws IOException, XmlPullParserException, BadXmlFormatException {
    try (InputStream inputStream =
        App.getCxt().getAssets().open(getString(R.string.db_meta) + ".xml")) {
      XmlPullParser xmlParser = Xml.newPullParser();
      xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlParser.setInput(inputStream, null);

      boolean quranTag = false, surahsTag = false;
      int lastSurahNum = 0;

      while (true) {
        int eventType = xmlParser.next();
        if (eventType == XmlPullParser.END_DOCUMENT) {
          break;
        }

        String tagName = xmlParser.getName();

        if (eventType == XmlPullParser.START_TAG) {
          if (tagName.equals(QURAN_TAG)) {
            quranTag = true;
          } else if (tagName.equals(SURAHS_TAG)) {
            surahsTag = true;
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (tagName.equals(QURAN_TAG) || tagName.equals(SURAHS_TAG)) {
            return;
          }
        }

        if (!quranTag
            || !surahsTag
            || eventType != XmlPullParser.START_TAG
            || !tagName.equals(SURAH_TAG)) {
          continue;
        }

        int surahNum = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_INDEX));
        lastSurahNum++;
        if (surahNum != lastSurahNum) {
          throw new BadXmlFormatException(
              "Index " + surahNum + " comes after " + lastSurahNum + " in " + SURAHS_TAG);
        }
        int aayahs = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_AAYAHS));
        int rukus = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_RUKUS));
        String name = xmlParser.getAttributeValue(null, ATTR_NAME);
        int order = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_ORDER));
        boolean isMeccan = xmlParser.getAttributeValue(null, ATTR_TYPE).equals("Meccan");
        mQuranMetaDao.insert(new SurahEntity(surahNum, aayahs, rukus, order, name, isMeccan));
      }
    }
  }

  private void insertMetadata(
      String outTag, String inTag, EntryCallback entryCallback, MarkCallback markCallback)
      throws IOException, XmlPullParserException, BadXmlFormatException {
    try (InputStream inputStream =
        App.getCxt().getAssets().open(getString(R.string.db_meta) + ".xml")) {
      XmlPullParser xmlParser = Xml.newPullParser();
      xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlParser.setInput(inputStream, null);

      boolean quranTag = false, tag = false;
      int index = -1, lastIndex = -1, surahNumMin = 0, surahNumMax, aayahNumMin = 0, aayahNumMax;

      while (true) {
        int eventType = xmlParser.next();
        if (eventType == XmlPullParser.END_DOCUMENT) {
          break;
        }

        String tagName = xmlParser.getName();

        if (eventType == XmlPullParser.START_TAG) {
          if (tagName.equals(QURAN_TAG)) {
            quranTag = true;
          } else if (tagName.equals(outTag)) {
            lastIndex = 0;
            tag = true;
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if (tagName.equals(QURAN_TAG)) {
            return;
          } else if (tagName.equals(outTag)) {
            makeCallback(
                index,
                surahNumMin,
                aayahNumMin,
                TOTAL_SURAHS,
                LAST_SURAH_AAYAHS,
                entryCallback,
                markCallback,
                outTag.equals(RUKUS_TAG) || outTag.equals(HIZBS_TAG));
            return;
          }
        }

        if (!quranTag || !tag || eventType != XmlPullParser.START_TAG || !tagName.equals(inTag)) {
          continue;
        }

        surahNumMax = Integer.parseInt(xmlParser.getAttributeValue(null, SURAH_TAG));
        aayahNumMax = Integer.parseInt(xmlParser.getAttributeValue(null, AAYAH_TAG)) - 1;
        makeCallback(
            index,
            surahNumMin,
            aayahNumMin,
            surahNumMax,
            aayahNumMax,
            entryCallback,
            markCallback,
            outTag.equals(RUKUS_TAG) || outTag.equals(HIZBS_TAG));

        index = Integer.parseInt(xmlParser.getAttributeValue(null, ATTR_INDEX));
        lastIndex++;
        if (index != lastIndex) {
          throw new BadXmlFormatException(
              "Index " + index + " comes after " + lastIndex + " in " + outTag);
        }
        surahNumMin = surahNumMax;
        aayahNumMin = aayahNumMax + 1;
        if (aayahNumMin == 1) {
          aayahNumMin = 0;
        }
      }
    }
  }

  private void makeCallback(
      int index,
      int surahNumMin,
      int aayahNumMin,
      int surahNumMax,
      int aayahNumMax,
      EntryCallback entryCallback,
      MarkCallback markCallback,
      boolean isRukuOrHizb) {

    if (index != -1 && entryCallback != null) {
      entryCallback.insert(index, surahNumMin, aayahNumMin, surahNumMax, aayahNumMax);
    }

    if (markCallback == null) {
      return;
    }

    int markSurahNum;
    int markAayahNum;
    if (isRukuOrHizb) {
      markSurahNum = surahNumMax;
      markAayahNum = aayahNumMax;
      if (markAayahNum == 0) {
        markSurahNum--;
        markAayahNum = mQuranMetaDao.getAayahs(markSurahNum);
      }
    } else if (index != -1) {
      markSurahNum = surahNumMin;
      markAayahNum = aayahNumMin;
    } else {
      return;
    }
    markCallback.insertMark(markSurahNum, markAayahNum);
  }

  /*
   Create a new AayahGroup if:
   - Showing single Aayahs, or
   - It's Tasmia at the start of Surah, or
   - It's the first Aayah of a Surah.
  */
  private void insertAayahGroupPositions(QuranDao dao) {
    int groupPos = 0;
    List<Integer> aayahIds = new ArrayList<>();

    for (int page = 1; page <= TOTAL_PAGES; page++) {
      int groupPosInPage = 0;
      aayahIds.clear();
      List<AayahEntity> entities = dao.getAayahEntities(page);

      for (AayahEntity entity : entities) {
        if (entity.aayahNum == 0 || entity.aayahNum == 1) {
          if (!aayahIds.isEmpty()) {
            dao.insertAayahGroupPositions(aayahIds, groupPos, groupPosInPage);
            aayahIds.clear();
            groupPosInPage++;
            groupPos++;
          }
        }
        aayahIds.add(entity.id);
      }

      if (!aayahIds.isEmpty()) {
        dao.insertAayahGroupPositions(aayahIds, groupPos, groupPosInPage);
        groupPos++;
      }
    }
  }

  private int mId = 0;

  private int getId() {
    mId++;
    return mId - 1;
  }

  private interface EntryCallback {

    void insert(int index, int surahNumMin, int aayahNumMin, int surahNumMax, int aayahNumMax);
  }

  private interface MarkCallback {

    void insertMark(int surahNum, int aayahNum);
  }

  private static class BadXmlFormatException extends Exception {

    BadXmlFormatException(String message) {
      super(message);
    }
  }
}
