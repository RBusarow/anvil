package com.squareup.anvil.compiler.fir

import com.squareup.anvil.compiler.fir.internal.Names
import com.squareup.anvil.compiler.fir.internal.classId
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayLiteral
import org.jetbrains.kotlin.fir.expressions.builder.buildClassReferenceExpression
import org.jetbrains.kotlin.fir.extensions.buildUserTypeFromQualifierParts
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.Name

internal fun FirClassLikeDeclaration.addMergedComponentAnnotation(session: FirSession) {
  replaceAnnotations(annotations + listOf(createMergedComponentAnnotation(session)))
}

internal fun createMergedComponentAnnotation(session: FirSession): FirAnnotation = buildAnnotation {

  val componentAnnotationClassSymbol = session.symbolProvider
    .getClassLikeSymbolByClassId(Names.dagger.component.classId())
    as FirRegularClassSymbol

  annotationTypeRef = componentAnnotationClassSymbol.defaultType().toFirResolvedTypeRef()

  val mergedModules = listOf(
    // TODO - Hard-code `EmptyModule` for now, but this would need to happen for all merged modules.
    session.symbolProvider.getClassLikeSymbolByClassId(Names.emptyModule.classId())
      as FirRegularClassSymbol,
  )

  val moduleArgs = buildAnnotationArgumentMapping {
    mapping[Name.identifier("modules")] = buildArrayLiteral array@{
      this@array.argumentList = buildArgumentList argList@{
        this@argList.arguments += mergedModules.map { module ->
          buildClassReferenceExpression {
            classTypeRef = buildUserTypeFromQualifierParts(false) {
              module.classId.asSingleFqName().pathSegments().forEach(::part)
            }
            coneTypeOrNull = null
          }
        }
      }
    }
  }

  argumentMapping = moduleArgs
}
