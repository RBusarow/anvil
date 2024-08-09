package com.squareup.anvil.compiler.fir

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
public class AnvilCompilerPluginRegistrar : CompilerPluginRegistrar() {

  override val supportsK2: Boolean get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (!configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) return

    FirExtensionRegistrarAdapter.registerExtension(AnvilFirPluginExtensionRegistrar())

    // throw AnvilCompilationException(
    //   registeredExtensions.renderAsDataClassToString()
    //     .prettyToString(),
    // )
  }
}
