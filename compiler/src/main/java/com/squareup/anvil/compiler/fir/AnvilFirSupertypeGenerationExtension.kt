package com.squareup.anvil.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

public class AnvilFirSupertypeGenerationExtension(session: FirSession) :
  FirSupertypeGenerationExtension(session) {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(PREDICATE)
  }

  override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>,
    typeResolver: TypeResolveService,
  ): List<FirResolvedTypeRef> {

    // val symbol = classLikeDeclaration.symbol as? FirClassSymbol<*> ?: return emptyList()

    val ballClassLike = BALL_CLASS_ID.constructClassLikeType()

    val ballRegularSymbol = ballClassLike.toRegularClassSymbol(session)

    val ballUserType = buildUserTypeFromQualifierParts(isMarkedNullable = false) {
      BALL_CLASS_ID.asSingleFqName()
        .pathSegments()
        .forEach(::part)
    }

    val ballResolvedUserType = typeResolver.resolveUserType(ballUserType)

    return listOf(ballResolvedUserType)
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    return session.predicateBasedProvider.matches(PREDICATE, declaration)
  }

  public companion object {
    private val BALL_CLASS_ID = ClassId.topLevel(FqName("foo.Ball"))
    private val annotationClassId = ClassId.topLevel(FqName("foo.Freddy"))
    private val PREDICATE = DeclarationPredicate.create {
      annotated(annotationClassId.asSingleFqName())
    }
  }
}
