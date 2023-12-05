package com.squareup.anvil

import com.rickbusarow.kgx.libsCatalog
import com.rickbusarow.kgx.pluginId
import com.squareup.anvil.benchmark.BenchmarkPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

open class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(BenchmarkPlugin::class.java)
    target.plugins.apply(KtlintConventionPlugin::class.java)

    // TODO (rbusarow) delete when all non-library projects have their own convention plugin
    //   That should be when this lands: https://github.com/square/anvil/pull/789
    target.subprojects { sub ->
      sub.plugins.withId("build-init") {
        sub.plugins.apply(KtlintConventionPlugin::class.java)
      }
    }
  }
}

open class LibraryPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(target.libsCatalog.pluginId("kotlin-jvm"))
    target.plugins.apply(KtlintConventionPlugin::class.java)
  }
}