// package com.squareup.anvil.compiler.fir.internal
//
//  import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
//  import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
//  import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
//  import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
//  import org.jetbrains.kotlin.cli.common.messages.MessageCollector
//  import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
//  import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
//  import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
//  import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles.JVM_CONFIG_FILES
//  import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
//  import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileModulesUsingFrontendIrAndLightTree
//  import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
//  import org.jetbrains.kotlin.cli.jvm.compiler.report
//  import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
//  import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
//  import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
//  import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
//  import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
//  import org.jetbrains.kotlin.com.intellij.openapi.Disposable
//  import org.jetbrains.kotlin.config.ApiVersion
//  import org.jetbrains.kotlin.config.CommonConfigurationKeys
//  import org.jetbrains.kotlin.config.CompilerConfiguration
//  import org.jetbrains.kotlin.config.JVMConfigurationKeys
//  import org.jetbrains.kotlin.config.JvmTarget
//  import org.jetbrains.kotlin.config.LanguageVersion
//  import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
//  import org.jetbrains.kotlin.incremental.isJavaFile
//  import org.jetbrains.kotlin.incremental.isKotlinFile
//  import org.jetbrains.kotlin.modules.Module
//  import org.jetbrains.kotlin.utils.PathUtil
//  import java.io.File
//
//

// private fun compile(sourceFiles: List<File>): Boolean {
//
//   K2JVMCompiler()
//
//   val messageCollector =
//     PrintingMessageCollector(System.out, MessageRenderer.PLAIN_RELATIVE_PATHS, true)
//
//   val compilerConfiguration = createCompilerConfiguration(
//     classpathFiles = emptyList(),
//     sourceFiles = sourceFiles,
//     moduleName = "my-module",
//     messageCollector = messageCollector,
//     kotlinLanguageVersion = LanguageVersion.KOTLIN_2_0,
//     jvmTarget = JvmTarget.JVM_17,
//   )
//   val projectEnvironment = createProjectEnvironment(
//     configuration = compilerConfiguration,
//     parentDisposable = DisposableResetManager(),
//     configFiles = JVM_CONFIG_FILES,
//     messageCollector = messageCollector,
//   )
//
//   return compileModulesUsingFrontendIrAndLightTree(
//     projectEnvironment = projectEnvironment,
//     compilerConfiguration = compilerConfiguration,
//     messageCollector = messageCollector,
//     buildFile = null,
//     module = Module,
//     targetDescription = "my target",
//     checkSourceFiles = true,
//     isPrintingVersion = true,
//   )
// }
//
// internal fun createCompilerConfiguration(
//   classpathFiles: List<File>,
//   sourceFiles: List<File>,
//   moduleName: String,
//   messageCollector: MessageCollector,
//   kotlinLanguageVersion: LanguageVersion,
//   jvmTarget: JvmTarget,
// ): CompilerConfiguration {
//
//   val javaFiles = mutableListOf<File>()
//   val kotlinFiles = mutableListOf<String>()
//
//   sourceFiles.forEach { file ->
//     when {
//       file.isKotlinFile(listOf("kt")) -> kotlinFiles.add(file.absolutePath)
//       file.isJavaFile() -> javaFiles.add(file)
//     }
//   }
//
//   return CompilerConfiguration().apply {
//
//     put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
//     put(JVMConfigurationKeys.JVM_TARGET, jvmTarget)
//     put(CommonConfigurationKeys.MODULE_NAME, moduleName)
//
//     val languageVersionSettings = LanguageVersionSettingsImpl(
//       languageVersion = kotlinLanguageVersion,
//       apiVersion = ApiVersion.createByLanguageVersion(kotlinLanguageVersion),
//     )
//     put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings)
//
//     addJavaSourceRoots(javaFiles)
//     addKotlinSourceRoots(kotlinFiles)
//     addJvmClasspathRoots(classpathFiles)
//     configureJdkClasspathRoots()
//   }
// }
//
// private fun CompilerConfiguration.configureJdkClasspathRoots() {
//   if (getBoolean(JVMConfigurationKeys.NO_JDK)) return
//
//   val jdkHome = get(JVMConfigurationKeys.JDK_HOME)
//   val (javaRoot, classesRoots) = if (jdkHome == null) {
//     val javaHome = File(System.getProperty("java.home"))
//     put(JVMConfigurationKeys.JDK_HOME, javaHome)
//
//     javaHome to PathUtil.getJdkClassesRootsFromCurrentJre()
//   } else {
//     jdkHome to PathUtil.getJdkClassesRoots(jdkHome)
//   }
//
//   if (!CoreJrtFileSystem.isModularJdk(javaRoot)) {
//     if (classesRoots.isEmpty()) {
//       report(CompilerMessageSeverity.ERROR, "No class roots are found in the JDK path: $javaRoot")
//     } else {
//       addJvmSdkRoots(classesRoots)
//     }
//   }
// }
//
// private fun createKotlinCoreEnvironment(
//   configuration: CompilerConfiguration,
// ): KotlinCoreEnvironment {
//   // https://github.com/JetBrains/kotlin/commit/2568804eaa2c8f6b10b735777218c81af62919c1
//   setIdeaIoUseFallback()
//
//   return KotlinCoreEnvironment.createForProduction(
//     parentDisposable = DisposableResetManager(),
//     configuration = configuration,
//     configFiles = JVM_CONFIG_FILES,
//   )
// }
//
// private class DisposableResetManager() : org.jetbrains.kotlin.com.intellij.openapi.Disposable {
// // private class DisposableResetManager() : com.intellij.openapi.Disposable {
//
//   override fun dispose() {
//     println("########### disposing!")
//   }
// }
