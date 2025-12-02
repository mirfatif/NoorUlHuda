pluginManagement { includeBuild("${System.getenv("JAVA_LIBS_DIR")!!}/build_logic") }

plugins { id("settings-conventions") }

with(IncludeHelper(settings)) {
  includeAndSetDir(MyLibs.android_java_utils_base, "v1.01")
  includeAndSetDir(MyLibs.android_java_utils, "v1.01")
  includeAndSetDir(MyLibs.jni_utils, "v1.01")
  includeAndSetDir(MyLibs.android_utils, "v1.01")
  includeAndSetDir(MyLibs.app_utils, "v1.01")

  include(":app")
  includeAndSetDir(":adhan", "adhan.kotlin.dir", null)

  printProjectStatus()
}
