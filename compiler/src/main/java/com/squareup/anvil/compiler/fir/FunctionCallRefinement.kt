package com.squareup.anvil.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirFunctionCallRefinementExtension
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.text

@OptIn(FirExtensionApiInternals::class)
public class FunctionCallRefinement(
  session: FirSession,
) : FirFunctionCallRefinementExtension(session) {
  override fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType? {
    // if (symbol.name.asString().contains("Freddy")) {
    // error("symbol -- ${symbol.name}")
    // } else {
    //   return null
    // }

    return null
  }

  override fun transform(
    call: FirFunctionCall,
    originalSymbol: FirNamedFunctionSymbol,
  ): FirFunctionCall {
    // if (call.source?.text?.contains("Freddy") == true) {
    error("call -- ${call.source?.text}")
    // }
    // return call
  }
}

public class MyExpressionTransform(session: FirSession) :
  FirAssignExpressionAltererExtension(session) {

  override fun transformVariableAssignment(
    variableAssignment: FirVariableAssignment,
  ): FirStatement? {
    error("variableAssignment -- ${variableAssignment.source?.text}")
  }
}
