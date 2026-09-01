plugins { id("apk-conventions") }

android {
  namespace = "com.mirfatif.noorulhuda"

  defaultConfig {
    applicationId = namespace
    versionCode = 105
    versionName = "v1.05"

    buildConfigField(
        "boolean", "GH_VERSION", if (System.getenv("IS_PS_VERSION") != "true") "true" else "false")
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
  }

  packaging.jniLibs.useLegacyPackaging = true // Extract JNI libs from APK

  buildFeatures { viewBinding = true }
}

dependencies {
  implementation(project(path = ":adhan"))

  implementation(libs.time4j.android)

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.viewpager)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.room.runtime)
  annotationProcessor(libs.androidx.room.compiler)

  // For Snack bar, TabLayout and BottomSheetDialogFragment.
  // Also includes CoordinatorLayout.
  implementation(libs.google.android.material)

  // To handle hyperlink onClick in TextView
  implementation(libs.better.link.movement.method)

  // To use Iterables for splitting Collections
  implementation(libs.google.guava.android)

  debugImplementation(libs.leakcanary.android)
}
