package com.squareup.anvil.compiler.fir

import com.squareup.anvil.compiler.mapToSet
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
public class AnvilFirDeclarationGenerationExtension(session: FirSession) :
  FirDeclarationGenerationExtension(session) {
  public companion object {
    private val FOO_PACKAGE = FqName.topLevel(Name.identifier("foo"))
    private val THING_FACTORY = ClassId(FOO_PACKAGE, Name.identifier("Thing_Factory2"))
    private val GENERATED_CLASS_ID = THING_FACTORY

    private val PREDICATE = LookupPredicate.create {
      annotated(FOO_PACKAGE.child("Freddy".nameAsSafeName()))
      // hasAnnotated(FOO_PACKAGE.child("Freddy".nameAsSafeName()))
    }
  }

  public object Key : GeneratedDeclarationKey() {
    override fun toString(): String {
      return "${AnvilFirDeclarationGenerationExtension::class.simpleName}-Key"
    }
  }

  private val predicateBasedProvider = session.predicateBasedProvider
  private val matchedClasses by lazy {
    predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
      .filterIsInstance<FirConstructorSymbol>()
      // TODO <Rick> delete me
      .also {
        if (it.isNotEmpty()) {
          error(
            """
            |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            | -- matchedClasses
            |${
              it.joinToString("\n") { c ->
                "${c.callableId}  --  ${c.callableId.classId}"
              }
            }
            |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            """.trimMargin(),
          )
        }
      }
    // .filterIsInstance<FirRegularClassSymbol>()
  }
  private val classIdsForMatchedClasses: Set<ClassId> by lazy {
    matchedClasses
      .mapToSet { it.callableId.classId!! }
  }

  override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
    if (classId != GENERATED_CLASS_ID) return null
    if (matchedClasses.isEmpty()) return null
    return createTopLevelClass(classId, Key).symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    return when (classSymbol.classId) {
      GENERATED_CLASS_ID -> setOf(SpecialNames.INIT)
      else -> error("Unexpected classId: ${classSymbol.classId}")
      // else -> emptySet()
    }
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    return emptyList()
    val classId = context.owner.classId
    if (classId != GENERATED_CLASS_ID && classId !in classIdsForMatchedClasses) return emptyList()
    return listOf(
      createConstructor(context.owner, Key, isPrimary = true) {
        val c = this
        // c.visibility = Visibilities.Private
        c.valueParameter(Name.identifier("thing"), session.builtinTypes.stringType.type)
      }.symbol,
    )
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?,
  ): List<FirNamedFunctionSymbol> {
    if (callableId.classId !in classIdsForMatchedClasses) return emptyList()
    val owner = context?.owner
    require(owner is FirRegularClassSymbol)
    val matchedClassId = owner.matchedClass ?: return emptyList()
    val matchedClassSymbol =
      session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()
    val function = createMemberFunction(
      owner,
      Key,
      callableId.callableName,
      matchedClassSymbol.constructStarProjectedType(),
    )
    return listOf(function.symbol)
  }

  override fun getTopLevelClassIds(): Set<ClassId> {
    return if (matchedClasses.isEmpty()) emptySet() else setOf(GENERATED_CLASS_ID)
  }

  override fun hasPackage(packageFqName: FqName): Boolean {
    return packageFqName == FOO_PACKAGE
  }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(PREDICATE)
  }
}

private fun FirSession.getRegularClassSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
  return symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
}

private object MatchedClassAttributeKey : FirDeclarationDataKey()

private var FirRegularClass.matchedClass: ClassId? by FirDeclarationDataRegistry.data(
  MatchedClassAttributeKey,
)
private val FirRegularClassSymbol.matchedClass: ClassId? by FirDeclarationDataRegistry.symbolAccessor(
  MatchedClassAttributeKey,
)
