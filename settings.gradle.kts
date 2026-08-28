pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { setUrl("https://jitpack.io") }
  }

  includeBuild("${System.getenv("JAVA_LIBS_DIR")!!}/build_logic")
}

plugins { id("settings-conventions") }

with(IncludeHelper(settings)) {
  include(":app")
  includeAndSetDir(":adhan", "adhan.kotlin.dir", null)

  printProjectStatus()
}
