package com.squareup.anvil.compiler.codegen

import com.google.common.truth.Truth.assertThat
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.internal.InternalBindingMarker
import com.squareup.anvil.compiler.compile
import com.squareup.anvil.compiler.contributingInterface
import com.squareup.anvil.compiler.generatedBindingModules
import com.squareup.anvil.compiler.internal.testing.AnvilCompilationMode
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class ContributesBindingGeneratorTest(
  private val mode: AnvilCompilationMode,
) {

  companion object {
    @Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun modes(): Collection<Any> {
      return buildList {
        add(AnvilCompilationMode.Embedded())
        add(AnvilCompilationMode.Ksp())
      }
    }
  }

  @Test fun `priority is correctly propagated`() {
    compile(
      """
      package com.squareup.test

      import com.squareup.anvil.annotations.ContributesBinding

      interface ParentInterface1
      interface ParentInterface2

      @ContributesBinding(Any::class, boundType = ParentInterface1::class) // Default case is NORMAL
      @ContributesBinding(Any::class, boundType = ParentInterface2::class, priority = ContributesBinding.PRIORITY_NORMAL)
      @ContributesBinding(Unit::class, boundType = ParentInterface1::class, priority = ContributesBinding.PRIORITY_HIGH)
      @ContributesBinding(Unit::class, boundType = ParentInterface2::class, priority = ContributesBinding.PRIORITY_HIGHEST)
      class ContributingInterface : ParentInterface1, ParentInterface2
      """,
      mode = mode,
      workingDir = File(
        System.getProperty("kase.baseWorkingDir"),
        "ints/${mode::class.simpleName}",
      ),
    ) {

      val bindingModules = contributingInterface.generatedBindingModules()
        .associate { clazz ->
          val bindingMarker = clazz.getAnnotation(InternalBindingMarker::class.java)
          clazz.simpleName to bindingMarker.priority
        }
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface1ToKotlinAnyBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_NORMAL)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface2ToKotlinAnyBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_NORMAL)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface1ToKotlinUnitBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_HIGH)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface2ToKotlinUnitBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_HIGHEST)
    }
  }

  @Test fun `legacy priority is correctly propagated`() {
    compile(
      """
      package com.squareup.test

      import com.squareup.anvil.annotations.ContributesBinding
      import com.squareup.anvil.annotations.ContributesBinding.Priority

      interface ParentInterface1
      interface ParentInterface2

      @ContributesBinding(Any::class, boundType = ParentInterface1::class) // Default case is NORMAL
      @ContributesBinding(Any::class, boundType = ParentInterface2::class, priorityDeprecated = Priority.NORMAL)
      @ContributesBinding(Unit::class, boundType = ParentInterface1::class, priorityDeprecated = Priority.HIGH)
      @ContributesBinding(Unit::class, boundType = ParentInterface2::class, priorityDeprecated = Priority.HIGHEST)
      class ContributingInterface : ParentInterface1, ParentInterface2
      """,
      mode = mode,
      workingDir = File(
        System.getProperty("kase.baseWorkingDir"),
        "legacy/${mode::class.simpleName}",
      ),
      allWarningsAsErrors = false,
      trackSourceFiles = false,
    ) {

      println(
        """
        ############################################################### messages
        $messages
        ###############################################################
        """.trimIndent(),
      )

      exitCode shouldBe KotlinCompilation.ExitCode.OK

      val pi1 = classLoader.loadClass("com.squareup.test.ParentInterface1")
      val pi2 = classLoader.loadClass("com.squareup.test.ParentInterface2")

      val ci = classLoader.loadClass("com.squareup.test.ContributingInterface")

      println(
        """
        |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        |   pi1: $pi1
        |   pi2: $pi2
        |    ci: $ci
        |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        """.trimMargin(),
      )

      val bindingModules = contributingInterface.generatedBindingModules()
        .associate { clazz ->
          val bindingMarker = clazz.getAnnotation(InternalBindingMarker::class.java)
          clazz.simpleName to bindingMarker.priority
        }
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface1ToKotlinAnyBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_NORMAL)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface2ToKotlinAnyBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_NORMAL)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface1ToKotlinUnitBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_HIGH)
      assertThat(
        bindingModules["ContributingInterfaceAsComSquareupTestParentInterface2ToKotlinUnitBindingModule"],
      ).isEqualTo(ContributesBinding.PRIORITY_HIGHEST)
    }
  }
}
