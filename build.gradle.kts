plugins {
  id("spotless-root-conventions")
  alias(libs.plugins.task.tree)
  alias(libs.plugins.gradle.dependency.analysis)
}

allprojects { plugins.apply("dependency-updates-conventions") }

// Not using version catalog b/c of: https://github.com/gradle/gradle/issues/20383
GlobalState.androidConfig = AndroidConfig(36, 36, 24, "36.0.0", "28.2.13676358")

deleteRootBuildDirOnClean()
