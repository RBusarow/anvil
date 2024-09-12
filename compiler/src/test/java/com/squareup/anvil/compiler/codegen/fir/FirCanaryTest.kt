package com.squareup.anvil.compiler.codegen.fir

import com.squareup.anvil.compiler.internal.testing.AnvilCompilationMode
import com.squareup.anvil.compiler.testing.AnvilCompilationModeTest
import org.junit.jupiter.api.TestFactory

class FirCanaryTest : AnvilCompilationModeTest(AnvilCompilationMode.Embedded()) {
  @TestFactory
  fun `this is a test`() = testFactory {
    compile(
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
          // val b: B
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
      """,
      enableDaggerAnnotationProcessor = true,
      allWarningsAsErrors = false,
      useK2 = true,
    ) {
    }
  }
}
