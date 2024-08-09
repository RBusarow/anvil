package com.squareup.anvil.compiler.fir.internal

import org.jetbrains.kotlin.name.FqName

public fun String.fqn(): FqName = FqName("bar.$this")
