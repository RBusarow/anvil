pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
  }

  includeBuild("build-logic/conventions")
  includeBuild("build-logic/settings")
}

plugins {
  id("com.squareup.anvil.gradle-settings")
  id("com.gradle.develocity") version "3.17.5"
}

develocity {
  buildScan {
    uploadInBackground = true
    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    termsOfUseAgree = "yes"
  }
}

rootProject.name = "anvil"

include(":annotations")
include(":annotations-optional")
include(":compiler")
include(":compiler-api")
include(":compiler-utils")
include(":gradle-plugin")

// The delegate build is only necessary for convenience, when this build is the root.
// If this build is being included elsewhere, there's no need for it. If the root build is actually
// the delegate build, then including it here would create a circular dependency.
if (gradle.parent == null) {
  // includeBuild("build-logic/delegate")
}

includeBuild("../kotlin-compile-testing") {
  dependencySubstitution {
    substitute(module("dev.zacsweers.kctfork:core")).using(project(":core"))
    substitute(module("dev.zacsweers.kctfork:ksp")).using(project(":ksp"))
  }
}
