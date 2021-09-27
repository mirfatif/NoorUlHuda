# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Move all obfuscated classes into the root package.
-repackageclasses
-allowaccessmodification

# Default preferences are accessed through Reflection in MySettings
-keepclassmembers class com.mirfatif.noorulhuda.R$integer {
  int pref_*_default;
}
-keepclassmembers class com.mirfatif.noorulhuda.R$bool {
  int pref_*_default;
}
-keepclassmembers class com.mirfatif.noorulhuda.R$string {
  int pref_*_default;
}
# String preferences are accessed through Reflection in BackupRestore
-keepclassmembers class com.mirfatif.noorulhuda.R$string {
  int pref_*_key;
}

# Throwable names must not be obfuscated to correctly print e.toString()
-keepnames class * extends java.lang.Exception

# Do not obfuscate, only shrink
#-dontobfuscate
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
