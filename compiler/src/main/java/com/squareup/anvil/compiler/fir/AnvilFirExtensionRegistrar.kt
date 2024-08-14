package com.squareup.anvil.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

public class AnvilFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    // +::AnvilFirSupertypeGenerationExtension
    +::AnvilFirDeclarationGenerationExtension
    // +::AnvilFirInjectConstructorGenerationExtension
    // @OptIn(FirExtensionApiInternals::class)
    // +::FunctionCallRefinement
    // +::MyExpressionTransform
  }
}
