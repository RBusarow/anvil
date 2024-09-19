package com.squareup.anvil.compiler.fir.internal

import com.google.auto.service.AutoService
import com.squareup.anvil.compiler.fir.AnvilFirExtensionRegistrar
import dagger.internal.codegen.ComponentProcessor
import org.jetbrains.kotlin.analysis.utils.relfection.renderAsDataClassToString
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.report
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
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
import org.jetbrains.kotlin.kapt3.base.DetectMemoryLeaksMode
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt4.Kapt4CompilerPluginRegistrar
import org.jetbrains.kotlin.util.ServiceLoaderLite
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

internal fun compile2(sourceFiles: List<File>): Boolean {

  // val messageCollector = PrintingMessageCollector(
  //   System.out,
  //   MessageRenderer.PLAIN_RELATIVE_PATHS,
  //   true,
  // )

  // val compilerConfiguration = createCompilerConfiguration(
  //   classpathFiles = HostEnvironment.inheritedClasspath,
  //   sourceFiles = sourceFiles,
  //   moduleName = "my-module",
  //   messageCollector = messageCollector,
  //   kotlinLanguageVersion = LanguageVersion.KOTLIN_2_0,
  //   jvmTarget = JvmTarget.JVM_17,
  // )

  val wd = sourceFiles.first().parentFile.parentFile

  val k2JvmArgs = K2JVMCompilerArguments().also { args ->
    args.compileJava = true
    args.useKapt4 = true

    args.additionalJavaModules = emptyArray()

    args.apiVersion = "2.0"
    args.languageVersion = "2.0"
    args.verbose = true

    args.classpath = HostEnvironment.inheritedClasspath
      .filterNot { it.path.contains("kotlin-compile-testing") }
      .joinToString(File.pathSeparator) { it.absolutePath }

    args.pluginClasspaths = HostEnvironment.inheritedClasspath
      .map { it.absolutePath }
      .plus(getResourcesPath())
      .filterNot { it.contains("kotlin-compile-testing") }
      .toTypedArray()

    args.noReflect = true
    args.noStdlib = true

    args.compileJava = false
    args.jdkHome = System.getProperty("java.home")
    args.jvmTarget = "1.8"

    args.reportOutputFiles = true
    args.suppressMissingBuiltinsError = true

    // classes directory
    args.destination = wd.resolve("kotlin/classes").absolutePath

    args.suppressMissingBuiltinsError = false
    args.disableStandardScript = true
  }

  fun K2JVMCompilerArguments.debugString(): String {
    val additionalJavaModules = additionalJavaModules?.toList()
    return renderAsDataClassToString()
      .replace(":/", "\n  ")
      .replace(", ", "\n  ")
      .replace(
        "additionalJavaModules.*".toRegex(),
        "additionalJavaModules: $additionalJavaModules",
      )
  }

  // error(k2JvmArgs.debugString())
  /* Work around for warning that sometimes happens:
  "Failed to initialize native filesystem for Windows
  java.lang.RuntimeException: Could not find installation home path.
  Please make sure bin/idea.properties is present in the installation directory"
  See: https://github.com/arturbosch/detekt/issues/630
   */
  withSystemProperty("idea.use.native.fs.for.win", "false") {
    K2JVMCompiler().exec(
      System.err,
      args = k2JvmArgs.toArgumentStrings().toTypedArray(),
    )
  }

  return true

  // val projectEnvironment = createProjectEnvironment(
  //   configuration = compilerConfiguration,
  //   parentDisposable = { println("yay disposing!") },
  //   configFiles = JVM_CONFIG_FILES,
  //   messageCollector = messageCollector,
  // )
  //
  // val moduleChunk = compilerConfiguration.configureModuleChunk(k2JvmArgs, null)
  //
  // return compileModulesUsingFrontendIrAndLightTree(
  //   projectEnvironment = projectEnvironment,
  //   compilerConfiguration = compilerConfiguration,
  //   messageCollector = messageCollector,
  //   buildFile = null,
  //   module = moduleChunk.modules.single(),
  //   targetDescription = "my target",
  //   checkSourceFiles = true,
  //   isPrintingVersion = true,
  // )
}

internal inline fun <T> withSystemProperty(key: String, value: String, f: () -> T): T =
  withSystemProperties(mapOf(key to value), f)

internal inline fun <T> withSystemProperties(properties: Map<String, String>, f: () -> T): T {
  val previousProperties = mutableMapOf<String, String?>()

  for ((key, value) in properties) {
    previousProperties[key] = System.getProperty(key)
    System.setProperty(key, value)
  }

  try {
    return f()
  } finally {
    for ((key, value) in previousProperties) {
      if (value != null) {
        System.setProperty(key, value)
      }
    }
  }
}

private val resourceName = "META-INF/services/${CompilerPluginRegistrar::class.java.name}"

private fun getResourcesPath(): String =
  AnvilPredicates::class.java.classLoader
    .getResources(resourceName)
    .asSequence()
    .mapNotNull { url -> urlToResourcePath(url) }
    .find { resourcesPath ->
      ServiceLoaderLite
        .findImplementations(CompilerPluginRegistrar::class.java, listOf(resourcesPath.toFile()))
        .any { implementation -> implementation == MainCompilerPluginRegistrar::class.java.name }
    }?.toString()
    ?: throw AssertionError("Could not get path to CompilerPluginRegistrar service from META-INF")

/** Maps a URL resource for a class from a JAR or file to an absolute Path on disk  */
internal fun urlToResourcePath(url: URL): Path? {
  val uri = url.toURI()
  val uriPath =
    when (uri.scheme) {
      "jar" -> uri.rawSchemeSpecificPart.removeSuffix("!/$resourceName")
      "file" -> uri.toString().removeSuffix("/$resourceName")
      else -> return null
    }
  return Paths.get(URI.create(uriPath)).toAbsolutePath()
}

internal fun createCompilerConfiguration(
  classpathFiles: List<File>,
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

  val wd = sourceFiles.first().parentFile.parentFile

  val kaptOptions = KaptOptions.Builder().also { kapt ->
    kapt.detectMemoryLeaks = DetectMemoryLeaksMode.NONE
    kapt.processors.add(ComponentProcessor::class.qualifiedName!!)
    kapt.sourcesOutputDir = wd.resolve("kapt/generated").absoluteFile
  }

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
    config.addJvmClasspathRoots(classpathFiles)
    config.configureJdkClasspathRoots()
  }
}

private fun CompilerConfiguration.configureJdkClasspathRoots() {
  if (getBoolean(JVMConfigurationKeys.NO_JDK)) return

  val jdkHome = get(JVMConfigurationKeys.JDK_HOME)
  val (javaRoot, classesRoots) = if (jdkHome == null) {
    val javaHome = File(System.getProperty("java.home"))
    put(JVMConfigurationKeys.JDK_HOME, javaHome)

    javaHome to PathUtil.getJdkClassesRootsFromCurrentJre()
  } else {
    jdkHome to PathUtil.getJdkClassesRoots(jdkHome)
  }

  if (!CoreJrtFileSystem.isModularJdk(javaRoot)) {
    if (classesRoots.isEmpty()) {
      report(
        CompilerMessageSeverity.ERROR,
        "No class roots are found in the JDK path: $javaRoot",
      )
    } else {
      addJvmSdkRoots(classesRoots)
    }
  }
}

private fun createKotlinCoreEnvironment(
  configuration: CompilerConfiguration,
): KotlinCoreEnvironment {
  // https://github.com/JetBrains/kotlin/commit/2568804eaa2c8f6b10b735777218c81af62919c1
  setIdeaIoUseFallback()

  val d = Disposable { println("########### disposing!") }

  return KotlinCoreEnvironment.createForProduction(
    projectDisposable = d,
    configuration = configuration,
    configFiles = JVM_CONFIG_FILES,
  )
}

@AutoService(CompilerPluginRegistrar::class)
internal class MainCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    FirExtensionRegistrarAdapter.registerExtension(AnvilFirExtensionRegistrar())

    with(Kapt4CompilerPluginRegistrar()) {
      registerExtensions(configuration)
    }
  }
}
