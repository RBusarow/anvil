package com.squareup.anvil.compiler.fir

import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import java.io.File

public class FirCodeGenerationExtension : FirAnalysisHandlerExtension() {

  override fun isApplicable(configuration: CompilerConfiguration): Boolean = true
  override fun doAnalysis(configuration: CompilerConfiguration): Boolean {

    configuration.kotlinSourceRoots
      .last()
      .path
      .let(::File)
      .resolveSibling("OtherOtherClass.kt")
      .writeText(
        //language=kotlin
        """
        package com.squareup.test

        import javax.inject.Inject

        class OtherOtherClass @Inject constructor()
        """.trimIndent(),
      )

    return true

    // val srcGen = configuration.commandLineOptions.sourceGenFolder
    //
    // srcGen.resolve("com/squareup/test")
    //   .also { it.mkdirs() }
    //   .resolve("Component.kt")
    //   .writeText(
    //     //language=kotlin
    //     """
    //     package com.squareup.test
    //
    //     fun test() = println("test")
    //     """.trimIndent(),
    //   )
    //
    // return true

    // val projectDisposable = Disposer.newDisposable("FirCodeGenerationExtension.project")
    //
    // val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    //
    // val logger = MessageCollectorBackedAnvilLogger(
    //   isVerbose = true,
    //   isInfoAsWarnings = false,
    //   messageCollector = messageCollector,
    // )
    //
    // val projectEnvironment = createProjectEnvironment(
    //   configuration = configuration,
    //   parentDisposable = projectDisposable,
    //   configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES,
    //   messageCollector = messageCollector,
    // )
    //
    // try {
    //
    //   val standaloneAnalysisAPISession = buildStandaloneAnalysisAPISession(
    //     projectDisposable = projectDisposable,
    //     classLoader = FirCodeGenerationExtension::class.java.classLoader,
    //   ) {
    //     @Suppress("DEPRECATION") // TODO: KT-61319 Kapt: remove usages of deprecated buildKtModuleProviderByCompilerConfiguration
    //     buildKtModuleProviderByCompilerConfiguration(configuration)
    //
    //     registerProjectService(
    //       KtCompilerPluginsProvider::class.java,
    //       object : KtCompilerPluginsProvider() {
    //         private val extensionStorage = CompilerPluginRegistrar.ExtensionStorage().apply {
    //           configuration
    //             .getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
    //             .forEach { registrar ->
    //               with(registrar) { registerExtensions(configuration) }
    //             }
    //         }
    //
    //         override fun <T : Any> getRegisteredExtensions(
    //           module: KtSourceModule,
    //           extensionType: ProjectExtensionDescriptor<T>,
    //         ): List<T> {
    //           @Suppress("UNCHECKED_CAST")
    //           return (extensionStorage.registeredExtensions[extensionType] as? List<T>)
    //             ?: emptyList()
    //         }
    //
    //         override fun isPluginOfTypeRegistered(
    //           module: KtSourceModule,
    //           pluginType: CompilerPluginType,
    //         ): Boolean = false
    //       },
    //     )
    //   }
    //
    //   val (module, psiFiles) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()
    //   val ktFiles = psiFiles.filterIsInstance<KtFile>()
    //
    //   val fileBlob = ktFiles.joinToString("\n") {
    //
    //     listOf(
    //       it.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()?.get(),
    //       it.classes.toList(),
    //       it.virtualFilePath,
    //       it::class.qualifiedName,
    //     )
    //       .joinToString("  --  ")
    //   }
    //
    //   fun String.pretty(): String =
    //     replace("""(\S+:.+?) (?=\S+:)""".toRegex(), "$1\n  ")
    //       // .replace(":( ?(?:true|false)) ".toRegex(), ":$1\n  ")
    //       .replace("] (\\S+)".toRegex(), "]\n$1")
    //       // .replace(":( ?\\d+) ".toRegex(), ":$1\n  ")
    //       // .replace(":( ?\\[.*?]) ".toRegex(), ":$1\n  ")
    //       // .replace(":( ?\\{.*?}) ".toRegex(), ":$1\n  ")
    //       .toStringPretty()
    //
    //   val configurationBLob = configuration.renderAsDataClassToString().pretty()
    //
    //   val moduleBlob = standaloneAnalysisAPISession.modulesWithFiles.entries
    //     .single().key
    //     .renderAsDataClassToString()
    //     .pretty()
    //
    //   val analyzerServicesBlob = module.analyzerServices.renderAsDataClassToString().pretty()
    //
    //   throw AnvilCompilationException(
    //     """
    //       |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //       |module: $module
    //       |
    //       | -- kt files
    //       |$fileBlob
    //       |
    //       | -- module
    //       |$moduleBlob
    //       |
    //       | -- analyzer services blob
    //       |$analyzerServicesBlob
    //       |
    //       | -- configuration
    //       |$configurationBLob
    //       |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //     """.trimMargin(),
    //   )
    // } finally {
    //   projectDisposable.dispose()
    // }

    // val projectDisposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
    return true
  }
}
