package com.squareup.anvil.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

public class AnvilFirExtensionRegistrar : FirExtensionRegistrar() {
  @OptIn(FirExtensionApiInternals::class)
  override fun ExtensionRegistrarContext.configurePlugin() {

    /** [org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension] */
    // +::AnvilFactoryDelegateDeclarationGenerationExtension
    // +::AnvilFirInjectConstructorGenerationExtension
    // +::AnvilComponentSubtypeDeclarationGenerationExtension

    /** [org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension] */
    +::AnvilFirSupertypeGenerationExtension

    /** [org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension] */
    // +::AnvilFirStatusTransformerExtension

    /** [org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension] */
    // +::AnvilFirExpressionResolutionExtension

    /** [org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension] */
    // +::AnvilFirAssignExpressionAltererExtension

    /** [org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension] */
    // +::AnvilFirFunctionCallRefinementExtension
  }
}