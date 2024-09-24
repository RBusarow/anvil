plugins {
  id("conventions.library")
  id("conventions.publish")
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.buildconfig)
}

buildConfig {
  className("BuildProperties")
  packageName("com.squareup.anvil.compiler")
  useKotlinOutput { topLevelConstants = true }

  buildConfigField("boolean", "FULL_TEST_RUN", libs.versions.config.fullTestRun.get())
  buildConfigField("boolean", "INCLUDE_KSP_TESTS", libs.versions.config.includeKspTests.get())
}

conventions {
  kotlinCompilerArgs.addAll(
    // The flag is needed because we extend an interface that uses @JvmDefault and the Kotlin
    // compiler requires this flag when doing so.
    "-Xjvm-default=all",
    "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
  )
  addTasksToIdeSync("generateBuildConfig")
}

publish {
  configurePom(
    artifactId = "compiler",
    pomName = "Anvil Compiler",
    pomDescription = "The core implementation module for Anvil, responsible for hooking into " +
      "the Kotlin compiler and orchestrating code generation",
  )
}

dependencies {

  api(libs.dagger2.compiler)

  implementation(libs.auto.service.annotations)
  api(libs.kotlin.compiler)
  compileOnly(libs.ksp.api)
  compileOnly(libs.ksp.compilerPlugin)

  implementation(libs.classgraph)
  implementation(libs.dagger2)
  implementation(libs.jsr250)
  implementation(libs.kotlin.compiler)
  api(libs.kotlin.kapt.compiler)
  api(libs.kotlin.kapt.embeddable)
  // implementation(libs.kotlin.scriptingCompiler)
  implementation(libs.kotlinpoet)
  implementation(libs.kotlinpoet.ksp)
  implementation(platform(libs.kotlin.bom))
  implementation(project(":annotations"))
  implementation(project(":compiler-api"))
  implementation(project(":compiler-utils"))

  kapt(libs.auto.service.processor)

  testImplementation(libs.dagger2.compiler)
  testImplementation(testFixtures(project(":compiler-utils")))
  // Force later guava version for Dagger's needs
  testImplementation(libs.guava)
  testImplementation(libs.kase)
  testImplementation(libs.kotest.assertions.core.jvm)
  testImplementation(libs.kotlin.compileTesting)
  testImplementation(libs.kotlin.compileTesting.ksp)
  testImplementation(libs.kotlin.compiler)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.ksp.compilerPlugin)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.kotest.assertions.core.jvm)
  testRuntimeOnly(libs.junit.vintage.engine)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
