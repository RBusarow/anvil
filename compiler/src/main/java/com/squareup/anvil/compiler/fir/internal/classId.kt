package com.squareup.anvil.compiler.fir.internal

import com.squareup.anvil.compiler.internal.asClassName
import com.squareup.anvil.compiler.internal.joinSimpleNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal fun ClassId.factory(): ClassId {
  return asClassName().joinSimpleNames(suffix = "_Factory2")
    .toString()
    .let { ClassId.topLevel(FqName(it)) }
}
