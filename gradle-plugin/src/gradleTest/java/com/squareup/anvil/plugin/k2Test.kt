package com.squareup.anvil.plugin

import com.rickbusarow.kase.gradle.dsl.buildFile
import com.squareup.anvil.plugin.testing.BaseGradleTest
import io.kotest.matchers.file.shouldExist
import org.junit.jupiter.api.TestFactory

class k2Test : BaseGradleTest() {

  @TestFactory
  fun `canary test`() = testFactory {

    rootProject {

      buildFile {
        plugins {
          kotlin("jvm")
          id("com.squareup.anvil")
          // kotlin("kapt")
        }

        // anvil {
        //   generateDaggerFactories.set(true)
        // }

        dependencies {
          compileOnly(libs.inject)
          api(libs.dagger2.annotations)
          // kapt(libs.dagger2.compiler)
        }
      }

      dir("src/main/java") {
        injectClass()

        kotlinFile(
          "foo/Freddy.kt",
          """
            package foo
            
            annotation class Freddy

            abstract class Ball
          """.trimIndent(),
        )

        kotlinFile(
          "com/squareup/test/OtherClass.kt",
          """
            package com.squareup.test
            
            import javax.inject.Inject
            import foo.Freddy
            
            @Freddy
            class OtherClass @Inject constructor()
          """.trimIndent(),
        )

        javaFile(
          "com/squareup/test/InjectClass_Factory.java",
          """
            package com.squareup.test;
            
            import dagger.internal.DaggerGenerated;
            import dagger.internal.Factory;
            import dagger.internal.QualifierMetadata;
            import dagger.internal.ScopeMetadata;
            import javax.annotation.processing.Generated;
            
            @ScopeMetadata
            @QualifierMetadata
            @DaggerGenerated
            @Generated(
                value = "dagger.internal.codegen.ComponentProcessor",
                comments = "https://dagger.dev"
            )
            @SuppressWarnings({
                "unchecked",
                "rawtypes",
                "KotlinInternal",
                "KotlinInternalInJava",
                "cast"
            })
            public final class InjectClass_Factory implements Factory<InjectClass> {
              @Override
              public InjectClass get() {
                return newInstance();
              }
            
              public static InjectClass_Factory create() {
                return InstanceHolder.INSTANCE;
              }
            
              public static InjectClass newInstance() {
                return new InjectClass();
              }
            
              private static final class InstanceHolder {
                private static final InjectClass_Factory INSTANCE = new InjectClass_Factory();
              }
            }
          """.trimIndent(),
        )
      }
      gradlePropertiesFile(
        """
          org.gradle.caching=false
          com.squareup.anvil.trackSourceFiles=true
          kapt.use.k2=true

          # Enable compilation of project using FIR compiler
          kotlin.build.useFir=true
          # Enable FIR compiler for kotlin-stdlib, kotlin-reflect, kotlin-test.
          kotlin.build.useFirForLibraries=true
        """.trimIndent(),
      )
    }

    shouldSucceed("jar") {

      rootProject.generatedDir(useKsp = false).injectClassFactory.shouldExist()
    }

    workingDir.resolve("build").deleteRecursivelyOrFail()
  }
}
