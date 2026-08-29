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

-keepclassmembers class net.time4j.android.spi.AndroidResourceLoader {
  <init>();
}

-keepclassmembers class androidx.viewpager2.widget.ViewPager2 {
  androidx.recyclerview.widget.RecyclerView mRecyclerView;
}

-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
  int mTouchSlop;
}
