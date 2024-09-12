package com.squareup.anvil.compiler.fir

import com.squareup.anvil.compiler.fir.internal.Names
import com.squareup.anvil.compiler.fir.internal.classId
import com.squareup.anvil.compiler.fir.internal.createUserType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PrivateForInline

public class AnvilFirSupertypeGenerationExtension(session: FirSession) :
  FirSupertypeGenerationExtension(session) {

  private companion object {
    private val annotationClassId = Names.mergeComponentFir.classId()
    private val PREDICATE = DeclarationPredicate.create {
      annotated(annotationClassId.asSingleFqName())
    }
  }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(PREDICATE)
  }

  @OptIn(PrivateForInline::class)
  override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>,
    typeResolver: TypeResolveService,
  ): List<FirResolvedTypeRef> {

    // error("@@@@@@@@@@@@@ 1")
    // clazz.addMergedComponentAnnotation(session)
    // error("@@@@@@@@@@@@@ 2")

    // clazz.transformAnnotations(MyAnnotationTransformer(mergedModules), Unit)

    // val symbol = clazz.symbol as? FirClassSymbol<*> ?: return emptyList()

    val supertypeUserType = Names.componentBase.createUserType()

    fun FirTypeRef.userType() = buildUserTypeFromQualifierParts(
      isMarkedNullable = isMarkedNullable ?: false,
    ) {
      this@userType.coneType.classId?.asSingleFqName()?.pathSegments()?.forEach(::part)
    }

    fun FirAnnotation.id(): FqName? = when (val t = this.annotationTypeRef) {
      is FirResolvedTypeRef -> t.type.classId?.asSingleFqName()
      is FirUserTypeRef ->
        typeResolver
          .resolveUserType(type = t)
          .type
          .classId
          ?.asSingleFqName()
      else -> error("~~~~~~~~~~~~~~ huh? $t")
    }

    // val componentAnnotation = clazz.annotations
    //   .getAnnotationByClassId(Names.dagger.component.classId(), session)
    //   as FirAnnotationCall ?: return emptyList()

    // val componentAnnotation = classLikeDeclaration.annotations
    //   .onEach { requireNotNull(it.id()) }
    //   .singleOrNull { it.id() == Names.dagger.component }
    //   ?: error("~~~~~~~~~~~~~~ huh? ${classLikeDeclaration.classId.asSingleFqName()}")

    // This should always be true?
    // componentAnnotation as FirAnnotationCall

    classLikeDeclaration.transformAnnotations(
      MyAnnotationTransformer(typeResolver) {
        listOf(
          session.symbolProvider
            .getClassLikeSymbolByClassId(Names.emptyModule.classId()) as FirRegularClassSymbol,
        )
      },
      Unit,
    )

    // classLikeDeclaration.replaceAnnotations(
    //   classLikeDeclaration.annotations
    //     .filterNot { it == componentAnnotation } +
    //     createMergedComponentAnnotation(session),
    // )

    // val evalBefore = FirExpressionEvaluator
    //   .evaluateAnnotationArguments(componentAnnotation, session)

    // error("@@@@@@@@@@@@@@ component annotation -- ${componentAnnotation.render()}  --  $evalBefore")

    // clazz.annotations.single().replaceAnnotationTypeRef(daggerComponentTypeRef)

    // clazz.annotations.forEach {
    //   println("Annotation: $it")
    //   it
    //   typeResolver.resolveUserType(it.rep)
    // }

    val superResolved = typeResolver.resolveUserType(supertypeUserType)

    return listOf(superResolved)
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    return session.predicateBasedProvider.matches(PREDICATE, declaration)
  }
}
