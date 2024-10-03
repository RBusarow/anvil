package com.squareup.anvil.compiler.fir.internal

import com.google.auto.service.AutoService
import com.squareup.anvil.compiler.fir.AnvilCompilerPluginRegistrar
import com.squareup.anvil.compiler.fir.AnvilFirExtensionRegistrar
import com.squareup.anvil.compiler.fir.CanaryIrMerger
import dagger.internal.codegen.ComponentProcessor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.computeKotlinPaths
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.PlainTextMessageRenderer
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileModulesUsingFrontendIrAndLightTree
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.cli.jvm.configureContentRootsFromClassPath
import org.jetbrains.kotlin.cli.jvm.configureJavaModulesContentRoots
import org.jetbrains.kotlin.cli.jvm.configureJdkHome
import org.jetbrains.kotlin.cli.jvm.configureKlibPaths
import org.jetbrains.kotlin.cli.jvm.configureModuleChunk
import org.jetbrains.kotlin.cli.jvm.configureStandardLibs
import org.jetbrains.kotlin.cli.jvm.setupJvmSpecificArguments
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.kapt3.KAPT_OPTIONS
import org.jetbrains.kotlin.kapt3.base.AptMode
import org.jetbrains.kotlin.kapt3.base.DetectMemoryLeaksMode
import org.jetbrains.kotlin.kapt3.base.KaptFlag
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt4.Kapt4CompilerPluginRegistrar
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import java.io.File

internal fun compile2(
  workingDir: File,
  sourceFiles: List<File>,
): Boolean {

  val k2JvmArgs = K2JVMCompilerArguments().also { args ->

    args.useKapt4 = true
    args.reportPerf = true

    args.moduleName = "root"
    args.additionalJavaModules = emptyArray()

    // args.enableDebugMode = true
    // args.noJdk = args.jdkHome == null
    // args.reportAllWarnings = true
    // args.reportPerf = true

    // args.compileJava = true
    args.jdkHome = javaHome().absolutePath
    args.jvmTarget = "17"
    args.languageVersion = "2.0"
    // args.reportOutputFiles = true
    // args.suppressMissingBuiltinsError = true
    args.verbose = true
    // args.version = true

    // Arguments with always default values when used from build tools
    args.noStdlib = true
    args.noReflect = true
    args.allowNoSourceFiles = true

    args.destination = workingDir.resolve("kotlin/classes").absolutePath

    args.freeArgs += "-Xuse-kapt4"

    args.freeArgs += sourceFiles.pathStrings()

    args.pluginOptions = kaptOptions(workingDir).toPluginOptions()
      .plus("plugin:org.jetbrains.kotlin.kapt3:useK2=true")
      .toTypedArray()

    args.classpath = listOf(
      // File(
      //   "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util/242.8057/5769ebb247c6e8c8549f7f900e8c81cc31504e21/util-242.8057.jar",
      // ),
      HostEnvironment.javaxInject,
      HostEnvironment.dagger,
      HostEnvironment.kotlinStdLib,
      HostEnvironment.jetbrainsAnnotations,
      HostEnvironment.anvilCompiler,
      // workingDir.resolve("---kapt-stuff"),
      // HostEnvironment.anvilCompiler,
      // HostEnvironment.anvilCompilerApi,
      // HostEnvironment.anvilCompilerUtils,
      HostEnvironment.anvilAnnotations,

      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.20/244b603e8c729f98baaf9088d90b5b9350c68af8/kotlin-compiler-embeddable-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.20/244b603e8c729f98baaf9088d90b5b9350c68af8/kotlin-compiler-embeddable-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-annotation-processing-embeddable/2.0.20/bf63a1646a0cc679248a8a1f9335835f9085a40e/kotlin-annotation-processing-embeddable-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util/242.8057/5769ebb247c6e8c8549f7f900e8c81cc31504e21/util-242.8057.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-annotation-processing/2.0.20/caebad5f8cfd9e81c677e7b5438264c428b9915c/kotlin-annotation-processing-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.20/7388d355f7cceb002cd387ccb7ab3850e4e0a07f/kotlin-stdlib-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar",
    )
      .pathStrings()
      .plus(
        "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/jakarta.inject/jakarta.inject-api/2.0.1/4c28afe1991a941d7702fe1362c365f0a8641d1e/jakarta.inject-api-2.0.1.jar",
      )
      .joinToString(File.pathSeparator)

    args.pluginClasspaths = listOf(
      HostEnvironment.javaxInject,
      HostEnvironment.daggerCompiler,
      HostEnvironment.anvilCompiler,
      HostEnvironment.anvilCompilerApi,
      HostEnvironment.anvilCompilerUtils,
      HostEnvironment.anvilAnnotations,
    )
      .pathStrings()
      .plus(cp)
      .toTypedArray()

    // args.suppressMissingBuiltinsError = false
    // args.disableStandardScript = true
  }
    .let { parseCommandLineArguments<K2JVMCompilerArguments>(it.toArgumentStrings()) }

  K2JVMCompiler().exec(System.err, args = k2JvmArgs.toArgumentStrings().toTypedArray())
  return true

  val messageRenderer = object : PlainTextMessageRenderer(true) {
    private val cwd by lazy(LazyThreadSafetyMode.NONE) { File(".").absoluteFile }

    override fun getName(): String = "ColorizedRelativePath"

    override fun getPath(location: CompilerMessageSourceLocation): String {
      return File(location.path).toRelativeString(cwd)
    }
  }

  val messageCollector = AnvilMessageCollector(messageRenderer)

  val configuration = createCompilerConfiguration(
    workingDir = workingDir,
    compilerClasspathFiles = listOf(
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.20/244b603e8c729f98baaf9088d90b5b9350c68af8/kotlin-compiler-embeddable-2.0.20.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.20/244b603e8c729f98baaf9088d90b5b9350c68af8/kotlin-compiler-embeddable-2.0.20.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-annotation-processing-embeddable/2.0.20/bf63a1646a0cc679248a8a1f9335835f9085a40e/kotlin-annotation-processing-embeddable-2.0.20.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.platform/util/242.8057/5769ebb247c6e8c8549f7f900e8c81cc31504e21/util-242.8057.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-annotation-processing/2.0.20/caebad5f8cfd9e81c677e7b5438264c428b9915c/kotlin-annotation-processing-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-annotation-processing-gradle/2.0.20/bf63a1646a0cc679248a8a1f9335835f9085a40e/kotlin-annotation-processing-gradle-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.0.20/4aea042b39014e0a924c2e1d4a21b6fff7e4d35/kotlin-script-runtime-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-common/2.0.20/af6097b8ac359457ec1cf9b34bd9b5313b52eb6/kotlin-scripting-common-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-embeddable/2.0.20/1b4f069810aafc946746a27954b04e6fcac7814b/kotlin-scripting-compiler-embeddable-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-compiler-impl-embeddable/2.0.20/5e90bef162c17275e8583c8d1fda64b48c745250/kotlin-scripting-compiler-impl-embeddable-2.0.20.jar",
      // "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-scripting-jvm/2.0.20/46b8d2d2028448f6596cc3ce0ad7b10b259ec236/kotlin-scripting-jvm-2.0.20.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.0.20/7388d355f7cceb002cd387ccb7ab3850e4e0a07f/kotlin-stdlib-2.0.20.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar",
    ).map(::File),
    sourceFiles = sourceFiles,
    moduleName = "root",
    messageCollector = messageCollector,
    kotlinLanguageVersion = LanguageVersion.KOTLIN_2_0,
    jvmTarget = JvmTarget.JVM_17,
  )

  configuration.setupCommonArguments(k2JvmArgs, ::JvmMetadataVersion)
  configuration.setupJvmSpecificArguments(k2JvmArgs)
  configuration.configureJdkHome(k2JvmArgs)
  configuration.configureJdkClasspathRoots()
  configuration.configureContentRootsFromClassPath(k2JvmArgs)

  val paths = computeKotlinPaths(messageCollector, k2JvmArgs)

  configuration.configureJavaModulesContentRoots(k2JvmArgs)
  configuration.configureStandardLibs(paths, k2JvmArgs)
  configuration.configureAdvancedJvmOptions(k2JvmArgs)
  configuration.configureKlibPaths(k2JvmArgs)

  @Suppress("DEPRECATION")
  fun CompilerConfiguration.addRegistrar(
    registrar: org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar,
  ) {
    add(
      org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS,
      registrar,
    )
  }

  fun CompilerConfiguration.addRegistrar(registrar: CompilerPluginRegistrar) {
    add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, registrar)
  }

  // configuration.addRegistrar(Kapt3ComponentRegistrar())
  configuration.addRegistrar(ScriptingCompilerConfigurationComponentRegistrar())

  configuration.addRegistrar(CanaryCompilerPluginRegistrar())
  configuration.addRegistrar(Kapt4CompilerPluginRegistrar())
  configuration.addRegistrar(AnvilCompilerPluginRegistrar())
  configuration.addRegistrar(ScriptingK2CompilerPluginRegistrar())

  val rootDisposable = Disposer.newDisposable("Disposable for compile2")

  setIdeaIoUseFallback()

  configuration.put(
    JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION,
    k2JvmArgs.disableStandardScript,
  )

  val vfsProjectEnvironment = createProjectEnvironment(
    configuration = configuration,
    parentDisposable = rootDisposable,
    configFiles = JVM_CONFIG_FILES,
    messageCollector = messageCollector,
  )

  val moduleChunk = configuration.configureModuleChunk(k2JvmArgs, null)

  return compileModulesUsingFrontendIrAndLightTree(
    projectEnvironment = vfsProjectEnvironment,
    compilerConfiguration = configuration,
    messageCollector = messageCollector,
    buildFile = null,
    module = moduleChunk.modules.single(),
    targetDescription = "my target",
    checkSourceFiles = true,
    isPrintingVersion = true,
  )
}

private fun kaptOptions(workingDir: File) = KaptOptions.Builder().also { kapt ->
  kapt.detectMemoryLeaks = DetectMemoryLeaksMode.DEFAULT
  kapt.processors.add(ComponentProcessor::class.qualifiedName!!)
  kapt.classesOutputDir = workingDir.resolve("kapt/generated").absoluteFile
  kapt.incrementalDataOutputDir = workingDir.resolve("kapt/generated").absoluteFile
  kapt.processingClasspath.addAll(
    listOf(
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.dagger/dagger-compiler/2.52/ff418f410326eba248f5540a64c4a1e9c45cf570/dagger-compiler-2.52.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.dagger/dagger-spi/2.52/50674112d6ffdf0c77ba383dd789216f8c35a995/dagger-spi-2.52.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.dagger/dagger/2.52/c385ea51a9873b238d183faa22d54c00d65195ac/dagger-2.52.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.googlejavaformat/google-java-format/1.5/fba7f130d29061d2d2ea384b4880c10cae92ef73/google-java-format-1.5.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/33.0.0-jre/161ba27964a62f241533807a46b8711b13c1d94b/guava-33.0.0-jre.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.guava/failureaccess/1.0.2/c4a06a64e650562f30b7bf9aaec1bfed43aca12b/failureaccess-1.0.2.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.squareup/javapoet/1.13.0/d6562d385049f35eb50403fa86bb11cce76b866a/javapoet-1.13.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.squareup/kotlinpoet/1.11.0/5a16322632c6361f7058c948bab1aafa6e7a337f/kotlinpoet-1.11.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/javax.inject/javax.inject/1/6975da39a7040257bd51d21a231b76c915872d38/javax.inject-1.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/net.ltgt.gradle.incap/incap/0.2/c73e3db9bee414d6ee27995d951fcdbee09acad/incap-0.2.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-compat-qual/2.5.5/435dc33e3019c9f019e15f01aa111de9d6b2b79c/checker-compat-qual-2.5.5.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/1.9.0/e000bd084353d84c9e888f6fb341dc1f5b79d948/kotlin-stdlib-jdk8-1.9.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/1.6.10/1cbe9c92c12a94eea200d23c2bbaedaf3daf5132/kotlin-reflect-1.6.10.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/1.9.0/f320478990d05e0cfaadd74f9619fd6027adbf37/kotlin-stdlib-jdk7-1.9.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.9.24/9928532f12c66ad816a625b3f9984f8368ca6d2b/kotlin-stdlib-1.9.24.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/jakarta.inject/jakarta.inject-api/2.0.1/4c28afe1991a941d7702fe1362c365f0a8641d1e/jakarta.inject-api-2.0.1.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.errorprone/javac-shaded/9-dev-r4023-3/72b688efd290280a0afde5f9892b0fde6f362d1d/javac-shaded-9-dev-r4023-3.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/b421526c5f297295adef1c886e5246c39d4ac629/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.checkerframework/checker-qual/3.41.0/8be6df7f1e9bccb19f8f351b3651f0bac2f5e0c/checker-qual-3.41.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/com.google.errorprone/error_prone_annotations/2.23.0/43a27853b6c7d54893e0b1997c2c778c347179eb/error_prone_annotations-2.23.0.jar",
      "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar",
    ).map(::File),
  )
  kapt.sourcesOutputDir = workingDir.resolve("kapt/generated").absoluteFile
  kapt.stubsOutputDir = workingDir.resolve("kapt/stubs").absoluteFile
  kapt.mode = AptMode.STUBS_AND_APT

  /*
    public native int java.lang.Object.hashCode()
    public final native void java.lang.Object.notify()
    public java.lang.String java.lang.Object.toString()
    public final native void java.lang.Object.notifyAll()
    public boolean java.lang.Object.equals(java.lang.Object)
    public final native java.lang.Class java.lang.Object.getClass()
    public final void java.lang.Object.wait() throws java.lang.InterruptedException
    public boolean org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar.getSupportsK2()
    public final void java.lang.Object.wait(long,int) throws java.lang.InterruptedException
    public final native void java.lang.Object.wait(long) throws java.lang.InterruptedException
    public void org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar.registerProjectComponents(com.intellij.mock.MockProject,org.jetbrains.kotlin.config.CompilerConfiguration)
    public abstract void org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar.registerProjectComponents(org.jetbrains.kotlin.com.intellij.mock.MockProject,org.jetbrains.kotlin.config.CompilerConfiguration)
   */

  /*
  Receiver class org.jetbrains.kotlin.kapt3.Kapt3ComponentRegistrar does not define or inherit an implementation of the resolved method
  'abstract void registerProjectComponents(
    org.jetbrains.kotlin.com.intellij.mock.MockProject,
    org.jetbrains.kotlin.config.CompilerConfiguration
  )'
  of interface org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar.
   */

  // error(
  //   Kapt3ComponentRegistrar::class.java.methods.sortedBy {
  //     it.toString().length
  //   }.joinToString("\n"),
  // )

  kapt.flags.addAll(
    listOf(
      KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS,
      KaptFlag.STRIP_METADATA,
      KaptFlag.USE_LIGHT_ANALYSIS,
      // KaptFlag.CORRECT_ERROR_TYPES,
      KaptFlag.INCLUDE_COMPILE_CLASSPATH,
    ),
  )
}

internal fun createCompilerConfiguration(
  workingDir: File,
  compilerClasspathFiles: List<File>,
  sourceFiles: List<File>,
  moduleName: String,
  messageCollector: MessageCollector,
  kotlinLanguageVersion: LanguageVersion,
  jvmTarget: JvmTarget,
): CompilerConfiguration {

  val javaFiles = mutableListOf<File>()
  val kotlinFiles = mutableListOf<String>()

  sourceFiles.forEach { file ->
    when {
      file.isKotlinFile(listOf("kt")) -> kotlinFiles.add(file.absolutePath)
      file.isJavaFile() -> javaFiles.add(file)
    }
  }

  val kaptOptions = kaptOptions(workingDir)

  return CompilerConfiguration().also { config ->
    config.put(KAPT_OPTIONS, kaptOptions)

    config.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    config.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
    config.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

    val languageVersionSettings = LanguageVersionSettingsImpl(
      languageVersion = kotlinLanguageVersion,
      apiVersion = ApiVersion.createByLanguageVersion(kotlinLanguageVersion),
    )
    config.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings)

    config.addJavaSourceRoots(javaFiles)
    config.addKotlinSourceRoots(kotlinFiles)
    config.addJvmClasspathRoots(compilerClasspathFiles)
    config.configureJdkClasspathRoots()
  }
}

@AutoService(CompilerPluginRegistrar::class)
internal class CanaryCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(AnvilFirExtensionRegistrar())

    IrGenerationExtension.registerExtension(CanaryIrMerger())

    println(
      """
      _ _,---._
   ,-','       `-.___
  /-;'               `._
 /\/          ._   _,'o \
( /\       _,--'\,','"`. )
 |\      ,'o     \'    //\
 |      \        /   ,--'""`-.
 :       \_    _/ ,-'         `-._
  \        `--'  /                )
   `.  \`._    ,'     ________,','
     .--`     ,'  ,--` __\___,;'
      \`.,-- ,' ,`_)--'  /`.,'
       \( ;  | | )      (`-/
         `--'| |)       |-/
           | | |        | |
           | | |,.,-.   | |_
           | `./ /   )---`  )
          _|  /    ,',   ,-'
         ,'|_(    /-<._,' |--,
         |    `--'---.     \/ \
         |          / \    /\  \
       ,-^---._     |  \  /  \  \
    ,-'        \----'   \/    \--`.
   /            \              \   \
      """.trimIndent(),
    )

    with(Kapt4CompilerPluginRegistrar()) {
      registerExtensions(configuration)
    }
  }
}

internal fun javaHome(): File = javaHomeOrNull() ?: error("JAVA_HOME and 'java.home' not set")

internal fun javaHomeOrNull(): File? {
  val path = System.getProperty("java.home")
    ?: System.getenv("JAVA_HOME")
    ?: return null

  return File(path).also { check(it.isDirectory) }
}

public fun Iterable<File>.pathStrings(): List<String> = map { it.absolutePath }.distinct()

private val cp = listOf(
  "com.fasterxml/aalto-xml/1.3.2/9f2d26b8fc000c13471b95e54f84da00de214319/aalto-xml-1.3.2.jar",
  "com.google.auto.service/auto-service-annotations/1.1.1/da12a15cd058ba90a0ff55357fb521161af4736d/auto-service-annotations-1.1.1.jar",
  "com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/jsr305-3.0.2.jar",
  "com.google.dagger/dagger-compiler/2.52/ff418f410326eba248f5540a64c4a1e9c45cf570/dagger-compiler-2.52.jar",
  "com.google.dagger/dagger-spi/2.52/50674112d6ffdf0c77ba383dd789216f8c35a995/dagger-spi-2.52.jar",
  "com.google.dagger/dagger/2.52/c385ea51a9873b238d183faa22d54c00d65195ac/dagger-2.52.jar",
  "com.google.devtools.ksp/symbol-processing-api/1.9.24-1.0.20/3d899943719423930b113797921654efdd96d4af/symbol-processing-api-1.9.24-1.0.20.jar",
  "com.google.errorprone/error_prone_annotations/2.26.1/c1fde57694bdc14e8618899aaa6e857d9465d7de/error_prone_annotations-2.26.1.jar",
  "com.google.errorprone/javac-shaded/9-dev-r4023-3/72b688efd290280a0afde5f9892b0fde6f362d1d/javac-shaded-9-dev-r4023-3.jar",
  "com.google.googlejavaformat/google-java-format/1.5/fba7f130d29061d2d2ea384b4880c10cae92ef73/google-java-format-1.5.jar",
  "com.google.guava/failureaccess/1.0.2/c4a06a64e650562f30b7bf9aaec1bfed43aca12b/failureaccess-1.0.2.jar",
  "com.google.guava/guava/33.1.0-jre/9b7ed39143d59e8eabcc6f91ffe4d23db2efe558/guava-33.1.0-jre.jar",
  "com.google.guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/b421526c5f297295adef1c886e5246c39d4ac629/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
  // "com.jetbrains.intellij.platform/core-impl/233.13135.103/ffea553eb0e4eb919b8d1a01277bd6c32354c21e/core-impl-233.13135.103.jar",
  "com.jetbrains.intellij.platform/core/242.8057/b11bd8c9efe68259765b1c0ee0e21467aad13092/core-242.8057.jar",
  "com.jetbrains.intellij.platform/diagnostic/242.8057/42d0e3c11772ccba823798c1ee55cbb04d066b98/diagnostic-242.8057.jar",
  "com.jetbrains.intellij.platform/extensions/242.8057/91522d1a0d43c95f3ba1379114b4199a0e1e8c12/extensions-242.8057.jar",
  "com.jetbrains.intellij.platform/util-base/242.8057/1e452ed4722885852e59cb7d496daf55fc48843f/util-base-242.8057.jar",
  "com.jetbrains.intellij.platform/util-class-loader/242.8057/49dd7cfed32ff38f03ff101f70024e2305030b11/util-class-loader-242.8057.jar",
  "com.jetbrains.intellij.platform/util-coroutines/242.8057/10da973ac996fc3e112e55d2ab2b577f42535b5a/util-coroutines-242.8057.jar",
  "com.jetbrains.intellij.platform/util-jdom/242.8057/e71cc313f32138fcf303593b74ff4e6f8c00d0a/util-jdom-242.8057.jar",
  "com.jetbrains.intellij.platform/util-progress/242.8057/8ad0ca851280be3980af3ad017738394138c750a/util-progress-242.8057.jar",
  "com.jetbrains.intellij.platform/util-rt-java8/242.8057/745f5ced90ed26d08ea7b5da345c0ac3fcb63a26/util-rt-java8-242.8057.jar",
  "com.jetbrains.intellij.platform/util-rt/242.8057/fa998f0181cd8ac3801969c209d25931c1a205fc/util-rt-242.8057.jar",
  "com.jetbrains.intellij.platform/util-xml-dom/242.8057/abd35ef99bf1b304294436561e8be069beb85538/util-xml-dom-242.8057.jar",
  "com.jetbrains.intellij.platform/util/242.8057/5769ebb247c6e8c8549f7f900e8c81cc31504e21/util-242.8057.jar",
  "com.squareup/javapoet/1.13.0/d6562d385049f35eb50403fa86bb11cce76b866a/javapoet-1.13.0.jar",
  "com.squareup/kotlinpoet-jvm/1.18.1/c4143e9d675aac41f47ceb4e7886d801010bd0c8/kotlinpoet-jvm-1.18.1.jar",
  "com.squareup/kotlinpoet-ksp/1.18.1/1c3a1beb44ac42c82ac7ee1b43e8d7611d6cbaaf/kotlinpoet-ksp-1.18.1.jar",
  "io.github.classgraph/classgraph/4.8.174/d86b16ab77abd87118b51e6a2c61e2d44ce6deaa/classgraph-4.8.174.jar",
  "jakarta.inject/jakarta.inject-api/2.0.1/4c28afe1991a941d7702fe1362c365f0a8641d1e/jakarta.inject-api-2.0.1.jar",
  "javax.annotation/jsr250-api/1.0/5025422767732a1ab45d93abfea846513d742dcf/jsr250-api-1.0.jar",
  "javax.inject/javax.inject/1/6975da39a7040257bd51d21a231b76c915872d38/javax.inject-1.jar",
  "jaxen/jaxen/1.2.0/c10535a925bd35129a4329bc75065cc6b5293f2c/jaxen-1.2.0.jar",
  "net.java.dev.jna/jna-platform/5.14.0/28934d48aed814f11e4c584da55c49fa7032b31b/jna-platform-5.14.0.jar",
  "net.java.dev.jna/jna/5.14.0/67bf3eaea4f0718cb376a181a629e5f88fa1c9dd/jna-5.14.0.jar",
  "net.ltgt.gradle.incap/incap/0.2/c73e3db9bee414d6ee27995d951fcdbee09acad/incap-0.2.jar",
  "org.checkerframework/checker-compat-qual/2.5.5/435dc33e3019c9f019e15f01aa111de9d6b2b79c/checker-compat-qual-2.5.5.jar",
  "org.checkerframework/checker-qual/3.42.0/638ec33f363a94d41a4f03c3e7d3dcfba64e402d/checker-qual-3.42.0.jar",
  "org.codehaus.woodstox/stax2-api/4.2/13c2b30926bca0429c704c4b4ca0b5d0432b69cd/stax2-api-4.2.jar",
  "org.jetbrains.intellij.deps.fastutil/intellij-deps-fastutil/8.5.13-jb4/12de686f29a7684ede155b081bdbf0037667594b/intellij-deps-fastutil-8.5.13-jb4.jar",
  "org.jetbrains.intellij.deps/trove4j/1.0.20200330/3afb14d5f9ceb459d724e907a21145e8ff394f02/trove4j-1.0.20200330.jar",
  // "org.jetbrains.kotlin/kotlin-annotation-processing-compiler/2.0.20/de9c3dfa14c0d0fe2381f2f5caaab8b379ba32ca/kotlin-annotation-processing-compiler-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-annotation-processing-embeddable/2.0.20/bf63a1646a0cc679248a8a1f9335835f9085a40e/kotlin-annotation-processing-embeddable-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-annotation-processing-gradle/2.0.20/bf63a1646a0cc679248a8a1f9335835f9085a40e/kotlin-annotation-processing-gradle-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-annotation-processing-runtime/2.0.20/626b0ccfafcff6547461647073f5d3756344d05e/kotlin-annotation-processing-runtime-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-annotation-processing/2.0.20/caebad5f8cfd9e81c677e7b5438264c428b9915c/kotlin-annotation-processing-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.20/244b603e8c729f98baaf9088d90b5b9350c68af8/kotlin-compiler-embeddable-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-daemon-embeddable/2.0.20/9eb02dce62f058efe6a121cf00cf5da9779e2746/kotlin-daemon-embeddable-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-metadata-jvm/2.0.20/5580cb0df67a99fe78e2d0e938eb82f1d2d587b2/kotlin-metadata-jvm-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-reflect/2.0.20/580c610641d75b6448825cbe455a1ded220e148b/kotlin-reflect-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-script-runtime/2.0.20/4aea042b39014e0a924c2e1d4a21b6fff7e4d35/kotlin-script-runtime-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-scripting-common/2.0.20/af6097b8ac359457ec1cf9b34bd9b5313b52eb6/kotlin-scripting-common-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-scripting-compiler-embeddable/2.0.20/1b4f069810aafc946746a27954b04e6fcac7814b/kotlin-scripting-compiler-embeddable-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-scripting-compiler-impl-embeddable/2.0.20/5e90bef162c17275e8583c8d1fda64b48c745250/kotlin-scripting-compiler-impl-embeddable-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-scripting-jvm/2.0.20/46b8d2d2028448f6596cc3ce0ad7b10b259ec236/kotlin-scripting-jvm-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-stdlib-jdk7/2.0.20/d70dafddc862404c4ecdb59eeb4bdfac8019937d/kotlin-stdlib-jdk7-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-stdlib-jdk8/2.0.20/764dd55ea8b14f76a3d66c403cfed212287a23dd/kotlin-stdlib-jdk8-2.0.20.jar",
  "org.jetbrains.kotlin/kotlin-stdlib/2.0.20/7388d355f7cceb002cd387ccb7ab3850e4e0a07f/kotlin-stdlib-2.0.20.jar",
  "org.jetbrains.kotlinx/kotlinx-collections-immutable-jvm/0.3.7/1cd4ff7059bcb437086b76311a9fc4775a887bcc/kotlinx-collections-immutable-jvm-0.3.7.jar",
  "org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0/ac1dc37a30a93150b704022f8d895ee1bd3a36b3/kotlinx-coroutines-core-jvm-1.8.0.jar",
  "org.jetbrains.kotlinx/kotlinx-coroutines-debug/1.8.0/5ae040b057e6b3f52e1d0fd50b381da61c700e29/kotlinx-coroutines-debug-1.8.0.jar",
  "org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.6.3/6b6c17d0312ba7192893adea9d52959941d0119b/kotlinx-serialization-core-jvm-1.6.3.jar",
  "org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.6.3/2241746853abf04073e3ab0dcd9e6729d363b313/kotlinx-serialization-json-jvm-1.6.3.jar",
  "org.jetbrains/annotations-java5/24.0.0/63a023e79ed9257c68077d8d50de87acc154a6e2/annotations-java5-24.0.0.jar",
  "org.jetbrains/annotations/24.0.0/69b8b443c437fdeefa8d20c18d257b94836a92b9/annotations-24.0.0.jar",
  "org.lz4/lz4-java/1.8.0/4b986a99445e49ea5fbf5d149c4b63f6ed6c6780/lz4-java-1.8.0.jar",
  "org.slf4j/log4j-over-slf4j/1.7.36/2a753acda077203a4794f106871bb237501c9a53/log4j-over-slf4j-1.7.36.jar",
  "oro/oro/2.0.8/5592374f834645c4ae250f4c9fbb314c9369d698/oro-2.0.8.jar",
)
  .map { "/Users/rbusarow/.gradle/caches/modules-2/files-2.1/$it" }
