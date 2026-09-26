package com.mirfatif.noorulhuda.prayer;

import static com.batoulapps.adhan2.CalculationMethod.DUBAI;
import static com.batoulapps.adhan2.CalculationMethod.EGYPTIAN;
import static com.batoulapps.adhan2.CalculationMethod.FRANCE;
import static com.batoulapps.adhan2.CalculationMethod.GULF;
import static com.batoulapps.adhan2.CalculationMethod.JAFARI;
import static com.batoulapps.adhan2.CalculationMethod.KARACHI;
import static com.batoulapps.adhan2.CalculationMethod.KUWAIT;
import static com.batoulapps.adhan2.CalculationMethod.MOON_SIGHTING_COMMITTEE;
import static com.batoulapps.adhan2.CalculationMethod.MUSLIM_WORLD_LEAGUE;
import static com.batoulapps.adhan2.CalculationMethod.NORTH_AMERICA;
import static com.batoulapps.adhan2.CalculationMethod.QATAR;
import static com.batoulapps.adhan2.CalculationMethod.RUSSIA;
import static com.batoulapps.adhan2.CalculationMethod.SINGAPORE;
import static com.batoulapps.adhan2.CalculationMethod.TEHRAN;
import static com.batoulapps.adhan2.CalculationMethod.TURKEY;
import static com.batoulapps.adhan2.CalculationMethod.UMM_AL_QURA;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;
import static com.mirfatif.noorulhuda.util.UtilsKt.fajrTimeMillis;
import static com.mirfatif.noorulhuda.util.UtilsKt.getPrayerCalcParams;
import static com.mirfatif.noorulhuda.util.UtilsKt.kotlinEnumOrdinal;
import static com.mirfatif.noorulhuda.util.UtilsKt.nextPrayer;
import static com.mirfatif.noorulhuda.util.UtilsKt.timeForPrayerMillis;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import com.batoulapps.adhan2.CalculationMethod;
import com.batoulapps.adhan2.CalculationParameters;
import com.batoulapps.adhan2.Coordinates;
import com.batoulapps.adhan2.HighLatitudeRule;
import com.batoulapps.adhan2.Madhab;
import com.batoulapps.adhan2.Prayer;
import com.batoulapps.adhan2.PrayerTimes;
import com.batoulapps.adhan2.data.DateComponents;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import net.time4j.SystemClock;
import net.time4j.calendar.HijriCalendar;
import net.time4j.calendar.HijriCalendar.Unit;
import net.time4j.calendar.astro.SolarTime;
import net.time4j.engine.StartOfDay;

public class PrayerData {

  public static final String CLOCK_FORMAT = "hh:mmaa";

  public long[] times = new long[6];

  public Spannable[] strTimes = new Spannable[6];
  public Spannable[] boldTimes = new Spannable[6];
  public Spannable[] compactTimes = new Spannable[6];

  public int curPrayer, nextPrayer;
  public String date, day, hijDate, hijMonth, hijYear;
  public Spannable time;

  public long nextPrayerTime;

  public long untilNextPrayer() {
    return nextPrayerTime - System.currentTimeMillis();
  }

  public long midnightTime;

  public NextAlarm nextAlarm;

  public static class NextAlarm {

    public int prayer;
    public long time;
  }

  //////////////////////////////////////////////////////////////////
  /////////////////////////// CALCULATION //////////////////////////
  //////////////////////////////////////////////////////////////////

  public static PrayerData getPrayerData() {
    Coordinates location = SETTINGS.getLngLat();
    if (location == null) {
      throw new Error("Location is null");
    }

    Calendar gregCal;
    String tzId = SETTINGS.getLocTimeZoneId();
    if (tzId == null) {
      tzId = TimeZone.getDefault().getID();
    }
    gregCal = Calendar.getInstance(TimeZone.getTimeZone(tzId));

    DateComponents comp =
        new DateComponents(
            gregCal.get(Calendar.YEAR),
            gregCal.get(Calendar.MONTH) + 1,
            gregCal.get(Calendar.DAY_OF_MONTH));
    PrayerTimes times = new PrayerTimes(location, comp, getCalcParams());

    SimpleDateFormat formatter = new SimpleDateFormat(CLOCK_FORMAT, Locale.ENGLISH);
    formatter.setTimeZone(gregCal.getTimeZone());

    long nextTime;
    int current;
    Prayer next = nextPrayer(times, gregCal.toInstant());
    if (next != Prayer.NONE) {
      nextTime = timeForPrayerMillis(times, next);
      current = kotlinEnumOrdinal(next) - 2;
      if (current < 0) {
        current = 5;
      }
    } else {
      nextTime = fajrTimeMillis(times) + DateUtils.DAY_IN_MILLIS;
      current = 5;
    }

    PrayerData data = new PrayerData();

    data.curPrayer = current;
    data.nextPrayer = current == 5 ? 0 : current + 1;
    data.nextPrayerTime = nextTime;

    Prayer[] prayers = Prayer.values();
    for (int i = 0; i < 6; i++) {
      Date time = new Date(timeForPrayerMillis(times, prayers[i + 1]));
      data.times[i] = time.getTime();
      String strTime = formatter.format(time);
      data.strTimes[i] = setSmallerAmPmSpan(strTime, false, false);
      data.boldTimes[i] = setSmallerAmPmSpan(strTime, false, i == current);
      data.compactTimes[i] = setSmallerAmPmSpan(strTime, true, false);
    }

    int nextAlarmPrayer = current + 1;
    for (int i = 0; i < 6; i++) {
      if (nextAlarmPrayer > 5) {
        nextAlarmPrayer = 0;
      }
      if (SETTINGS.getPrayerNotify(nextAlarmPrayer) || SETTINGS.getPrayerAdhan(nextAlarmPrayer)) {
        data.nextAlarm = new NextAlarm();
        data.nextAlarm.prayer = nextAlarmPrayer;
        data.nextAlarm.time = timeForPrayerMillis(times, prayers[nextAlarmPrayer + 1]);
        if (data.nextAlarm.time <= System.currentTimeMillis()) {
          data.nextAlarm.time += DateUtils.DAY_IN_MILLIS;
        }
        break;
      }
      nextAlarmPrayer++;
    }

    SolarTime solarTime = SolarTime.ofLocation(location.getLatitude(), location.getLongitude());
    StartOfDay dayStart = StartOfDay.definedBy(solarTime.sunset());
    // HijriCalendar#nowInSystemTime()
    HijriCalendar hijriCal =
        SystemClock.inZonalView(tzId)
            .now(HijriCalendar.family(), HijriCalendar.VARIANT_UMALQURA, dayStart)
            .toDate();
    hijriCal = hijriCal.plus(SETTINGS.getHijriOffset(), Unit.DAYS);

    Locale arabicLocale = new Locale("AR");
    data.hijDate = String.valueOf(hijriCal.getDayOfMonth());
    data.hijMonth = hijriCal.getMonth().getDisplayName(arabicLocale);
    data.hijYear = String.valueOf(hijriCal.getYear());

    data.day = gregCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    data.time = setSmallerAmPmSpan(formatter.format(gregCal.getTime()), false, false);
    formatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    data.date = formatter.format(gregCal.getTime());

    gregCal.add(Calendar.DAY_OF_MONTH, 1);
    gregCal.set(Calendar.HOUR_OF_DAY, 0);
    gregCal.set(Calendar.MINUTE, 0);
    gregCal.set(Calendar.SECOND, 0);
    gregCal.set(Calendar.MILLISECOND, 0);
    data.midnightTime = gregCal.getTimeInMillis();

    return data;
  }

  private static final RelativeSizeSpan SPAN = new RelativeSizeSpan(0.5f);

  private static Spannable setSmallerAmPmSpan(String time, boolean compact, boolean bold) {
    if (compact) {
      time = time.substring(0, time.length() - 1);
    }
    return setSmallerAmPmSpan(new SpannableString(time), bold);
  }

  public static Spannable setSmallerAmPmSpan(SpannableString string, boolean bold) {
    var len = string.length();
    string.setSpan(SPAN, 5, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (bold) {
      string.setSpan(new StyleSpan(Typeface.BOLD), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return string;
  }

  public static CalculationParameters getCalcParams() {
    return getPrayerCalcParams();
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// ARRAYS /////////////////////////////
  //////////////////////////////////////////////////////////////////

  public static final String[] COUNTRY_NAMES = App.getRes().getStringArray(R.array.county_names);
  public static final String[] COUNTRY_CODES = App.getRes().getStringArray(R.array.county_codes);

  public static final CalculationMethod[] CALC_METHODS =
      new CalculationMethod[] {
        MOON_SIGHTING_COMMITTEE,
        MUSLIM_WORLD_LEAGUE,
        EGYPTIAN,
        KARACHI,
        UMM_AL_QURA,
        NORTH_AMERICA,
        GULF,
        DUBAI,
        KUWAIT,
        QATAR,
        SINGAPORE,
        FRANCE,
        TURKEY,
        RUSSIA,
        JAFARI,
        TEHRAN
      };
  public static final String[] CALC_METHOD_NAMES =
      App.getRes().getStringArray(R.array.method_names);
  public static final Coordinates[] METHOD_LOCATIONS =
      new Coordinates[] {
        getLoc(0, 0), // Moon-sighting
        getLoc(-0.1360365, 51.5194682), // MWL
        getLoc(31.2357116, 30.0444196), // Egypt
        getLoc(67.009938, 24.8614622), // Karachi
        getLoc(39.8579118, 21.3890824), // Makkah
        getLoc(-86.3994386, 39.70421229), // ISNA
        getLoc(0, 0), // Gulf
        getLoc(55.2707828, 25.2048493), // Dubai
        getLoc(47.9774052, 29.375859), // Kuwait
        getLoc(51.5310398, 25.2854473), // Qatar
        getLoc(103.819836, 1.352083), // Singapore
        getLoc(2.3522219, 48.856614), // France
        getLoc(32.8597419, 39.9333635), // Turkey
        getLoc(55.9578555, 54.7347909), // Russia
        getLoc(50.8746035, 34.6415764), // Qum
        getLoc(51.3889736, 35.6891975) // Tehran
      };

  public static Coordinates getLoc(double lng, double lat) {
    return new Coordinates(lat, lng);
  }

  public static final Madhab[] ASR_CALC_METHODS = new Madhab[] {Madhab.SHAFI, Madhab.HANAFI};
  public static final String[] ASR_CALC_NAMES = App.getRes().getStringArray(R.array.asr_calc_names);

  public static final HighLatitudeRule[] HIGH_LAT_METHODS =
      new HighLatitudeRule[] {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
        HighLatitudeRule.TWILIGHT_ANGLE
      };
  public static final String[] HIGH_LAT_NAMES = App.getRes().getStringArray(R.array.high_lat_names);

  public static final String[] WIDGET_STYLES =
      App.getRes().getStringArray(R.array.home_widget_styles);
}
