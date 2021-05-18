# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Default preferences are accessed through Reflection in MySettings
-keep class com.mirfatif.noorulhuda.R$integer {
  int pref_*_default;
}
-keep class com.mirfatif.noorulhuda.R$bool {
  int pref_*_default;
}
-keep class com.mirfatif.noorulhuda.R$string {
  int pref_*_default;
}
# String preferences are accessed through Reflection in BackupRestore
-keep class com.mirfatif.noorulhuda.R$string {
  int pref_*_key;
}

# Throwable names must not be obfuscated to correctly print e.toString()
-keepnames class com.mirfatif.noorulhuda.db.DbBuilder$BadXmlFormatException
-keepnames class com.mirfatif.noorulhuda.prayer.PrayerTimeActivity$ConnectionException
-keepnames class com.mirfatif.noorulhuda.prayer.PrayerTimeActivity$InterruptException

# Do not obfuscate, only shrink
#-dontobfuscate
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
