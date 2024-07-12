package com.squareup.anvil.compiler.codegen.reference

import com.squareup.anvil.compiler.api.AnvilCompilationException
import com.squareup.anvil.compiler.fqName
import com.squareup.anvil.compiler.internal.reference.Visibility
import com.squareup.anvil.compiler.internal.reference.Visibility.INTERNAL
import com.squareup.anvil.compiler.internal.reference.Visibility.PRIVATE
import com.squareup.anvil.compiler.internal.reference.Visibility.PROTECTED
import com.squareup.anvil.compiler.internal.reference.Visibility.PUBLIC
import com.squareup.anvil.compiler.requireClassId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.LazyThreadSafetyMode.NONE

internal class ClassReferenceIr(
  val clazz: IrClassSymbol,
  val context: IrPluginContext,
) : AnnotatedReferenceIr {
  val fqName: FqName = clazz.fqName
  @UnsafeDuringIrConstructionAPI
  val packageFqName: FqName? = clazz.owner.packageFqName
  val classId: ClassId = clazz.requireClassId()

  val shortName: String
    get() = fqName.shortName().asString()

  @UnsafeDuringIrConstructionAPI
  val enclosingClassesWithSelf: List<ClassReferenceIr> by lazy {
    clazz.owner.parents
      .filterIsInstance<IrClass>()
      .map { it.symbol.toClassReference(context) }
      .toList()
      .reversed()
      .plus(this)
  }

  @UnsafeDuringIrConstructionAPI
  val isInterface: Boolean = clazz.owner.isInterface

  @UnsafeDuringIrConstructionAPI
  val visibility: Visibility = parseVisibility()

  @UnsafeDuringIrConstructionAPI
  private fun parseVisibility(): Visibility {
    return when (clazz.owner.visibility) {
      DescriptorVisibilities.PUBLIC -> PUBLIC
      DescriptorVisibilities.PRIVATE -> PRIVATE
      DescriptorVisibilities.INTERNAL -> INTERNAL
      DescriptorVisibilities.PROTECTED -> PROTECTED
      else -> throw AnvilCompilationExceptionClassReferenceIr(
        this,
        "Encountered an unsupported visibility ${clazz.owner.visibility.name} for class $fqName",
      )
    }
  }

  @UnsafeDuringIrConstructionAPI
  override val annotations: List<AnnotationReferenceIr> by lazy(NONE) {
    clazz.owner.annotations.map { it.toAnnotationReference(context, this) }
  }

  override fun toString(): String {
    return "${this::class.simpleName}($fqName)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ClassReferenceIr) return false

    if (fqName != other.fqName) return false

    return true
  }

  override fun hashCode(): Int {
    return fqName.hashCode()
  }
}

internal fun IrClassSymbol.toClassReference(context: IrPluginContext) =
  ClassReferenceIr(this, context)


@UnsafeDuringIrConstructionAPI
@Suppress("FunctionName")
internal fun AnvilCompilationExceptionClassReferenceIr(
  classReference: ClassReferenceIr,
  message: String,
  cause: Throwable? = null,
): AnvilCompilationException = AnvilCompilationException(
  element = classReference.clazz,
  message = message,
  cause = cause,
)
