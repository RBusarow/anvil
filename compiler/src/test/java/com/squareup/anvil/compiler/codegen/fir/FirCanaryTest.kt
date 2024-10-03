package com.squareup.anvil.compiler.codegen.fir

import com.rickbusarow.kase.stdlib.createSafely
import com.squareup.anvil.compiler.fir.internal.compile2
import com.squareup.anvil.compiler.internal.testing.AnvilCompilationMode
import com.squareup.anvil.compiler.testing.AnvilCompilationModeTest
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestFactory

class FirCanaryTest : AnvilCompilationModeTest(AnvilCompilationMode.Embedded()) {

  val targets = //language=kotlin
    """
    package foo

    import dagger.Binds
    import dagger.Component
    import dagger.Module
    import dagger.Subcomponent
    import kotlin.reflect.KClass
    import javax.inject.Inject

    @MergeComponentFir
    @Component(
      modules = [ABindingModule::class]
    )
    interface TestComponent

    interface ComponentBase {
      val b: B
    }

    @Module
    interface ABindingModule {
      @Binds
      fun bindAImpl(aImpl: AImpl): A
    }

    @Module
    interface EmptyModule {
      @Binds
      fun bindBImpl(bImpl: BImpl): B
    }

    class InjectClass @Freddy constructor()
    
    class OtherClass @Inject constructor()
    
    interface A
    class AImpl @Inject constructor() : A
    
    interface B
    class BImpl @Inject constructor(val a: A) : B

    annotation class Freddy
    annotation class MergeComponentFir
    annotation class ComponentKotlin(val modules: Array<KClass<*>>)
    """.trimIndent()

  @TestFactory
  fun `compile2 version canary`() = testFactory {

    compile2(
      workingDir,
      listOf(
        workingDir.resolve("foo/targets.kt").createSafely(targets),
      ),
    ) shouldBe true
  }

  @TestFactory
  fun `kct version canary`() = testFactory {

    compile(
      targets,
      enableDaggerAnnotationProcessor = true,
      allWarningsAsErrors = false,
      useK2 = true,
    ) {

      exitCode shouldBe KotlinCompilation.ExitCode.OK
    }
  }
}
