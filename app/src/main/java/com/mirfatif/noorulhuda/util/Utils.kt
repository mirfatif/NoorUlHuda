package com.mirfatif.noorulhuda.util

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Prayer
import com.batoulapps.adhan2.PrayerTimes
import com.mirfatif.noorulhuda.prayer.PrayerData
import com.mirfatif.noorulhuda.prefs.MySettings
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

@OptIn(ExperimentalTime::class)
fun nextPrayer(times: PrayerTimes, instant: java.time.Instant): Prayer =
    times.nextPrayer(instant.toKotlinInstant())

@OptIn(ExperimentalTime::class)
fun fajrTimeMillis(times: PrayerTimes): Long = times.fajr.toEpochMilliseconds()

fun kotlinEnumOrdinal(enum: Enum<*>): Int = enum.ordinal

@OptIn(ExperimentalTime::class)
fun timeForPrayerMillis(times: PrayerTimes, prayer: Prayer): Long =
    times.timeForPrayer(prayer)!!.toEpochMilliseconds()

val prayerCalcParams: CalculationParameters
  get() =
      PrayerData.CALC_METHODS[MySettings.SETTINGS.calcMethod].parameters.let { params ->
        params.copy(
            prayerAdjustments =
                params.prayerAdjustments.copy(
                    fajr = MySettings.SETTINGS.getPrayerAdj(0),
                    sunrise = MySettings.SETTINGS.getPrayerAdj(1),
                    dhuhr = MySettings.SETTINGS.getPrayerAdj(2),
                    asr = MySettings.SETTINGS.getPrayerAdj(3),
                    maghrib = MySettings.SETTINGS.getPrayerAdj(4),
                    isha = MySettings.SETTINGS.getPrayerAdj(5)),
            madhab = PrayerData.ASR_CALC_METHODS[MySettings.SETTINGS.asrCalcMethod],
            highLatitudeRule =
                MySettings.SETTINGS.highLatMethod
                    .takeIf { it > 0 }
                    ?.let { index -> PrayerData.HIGH_LAT_METHODS[index - 1] })
      }
