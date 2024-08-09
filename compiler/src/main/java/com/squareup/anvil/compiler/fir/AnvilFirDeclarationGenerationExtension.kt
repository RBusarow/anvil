package com.squareup.anvil.compiler.fir

import com.squareup.anvil.compiler.fir.internal.fqn
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public class AnvilFirDeclarationGenerationExtension(session: FirSession) :
  FirDeclarationGenerationExtension(session) {
  public companion object {
    private val FOO_PACKAGE = FqName.topLevel(Name.identifier("foo"))
    private val TEST_PACKAGE = FqName.topLevel(Name.identifier("com.squareup.test"))
    private val GENERATED_CLASS_ID = ClassId(TEST_PACKAGE, Name.identifier("TestComponent"))
    private val MATERIALIZE_NAME = Name.identifier("materialize")

    private val PREDICATE = LookupPredicate.create { annotated("ExternalClassWithNested".fqn()) }
  }

  public object Key : GeneratedDeclarationKey() {
    override fun toString(): String {
      return "${AnvilFirDeclarationGenerationExtension::class.simpleName}-Key"
    }
  }

  // @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
  // override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
  //   if (classId != GENERATED_CLASS_ID) return null
  //   return createTopLevelClass(classId, Key).symbol
  // }
}
