/*
 * Copyright (c) 2016, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.csharp

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Predicate
import java.util.stream.Collectors

import org.savantbuild.dep.domain.ArtifactID
import org.savantbuild.domain.Project
import org.savantbuild.io.FileSet
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.plugin.dep.DependencyPlugin
import org.savantbuild.plugin.file.FilePlugin
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

/**
 * The CSharp plugin. The public methods on this class define the features of the plugin.
 */
class CSharpPlugin extends BaseGroovyPlugin {
  public static
  final String ERROR_MESSAGE = "You must create the file [~/.savant/plugins/org.savantbuild.plugin.csharp.properties] " +
      "that contains the system configuration for the Mono/C# system. This file should include the location of the C# compiler " +
      "(msc or csc) by Language version. These properties look like this:\n\n" +
      "  2.0=/Library/Frameworks/Mono.framework/Versions/4.4.2\n"

  CSharpLayout layout = new CSharpLayout()

  CSharpSettings settings = new CSharpSettings()

  Properties properties

  Path compilerPath

  FilePlugin filePlugin

  DependencyPlugin dependencyPlugin

  CSharpPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
    filePlugin = new FilePlugin(project, runtimeConfiguration, output)
    dependencyPlugin = new DependencyPlugin(project, runtimeConfiguration, output)
    properties = loadConfiguration(new ArtifactID("org.savantbuild.plugin", "csharp", "csharp", "dll"), ERROR_MESSAGE)
  }

  /**
   * Cleans the build directory by completely deleting it.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   csharp.clean()
   * </pre>
   */
  void clean() {
    Path buildDir = project.directory.resolve(layout.buildDirectory)
    output.infoln "Cleaning [${buildDir}]"
    FileTools.prune(buildDir)
  }

  /**
   * Compiles the main and test C# files (src/main/csharp and src/test/csharp). This outputs 2 DLLs to the DLL output
   * directory.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   csharp.compile()
   * </pre>
   */
  void compile() {
    compileMain()
    compileTest()
  }

  /**
   * Compiles the main C# files (src/main/csharp by default).
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   csharp.compileMain()
   * </pre>
   */
  void compileMain() {
    compileInternal(layout.mainSourceDirectory, layout.dllOutputDirectory, layout.mainResourceDirectory, project.name + ".dll", settings.mainDependencies)
  }

  /**
   * Compiles the test C# files (src/test/csharp by default).
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   csharp.compileTest()
   * </pre>
   */
  void compileTest() {
    compileInternal(layout.testSourceDirectory, layout.dllOutputDirectory, layout.testResourceDirectory, project.name + "-test.dll", settings.testDependencies, layout.dllOutputDirectory.resolve("${project.name}.dll"))
  }

  /**
   * Creates the project's Doc. This executes the C# compiler command and outputs the docs to the {@code layout.docDirectory}
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   csharp.document()
   * </pre>
   */
  void document() {
    initialize()

    output.infoln("Generating JavaDoc to [%s]", layout.docDirectory)

    FileSet fileSet = new FileSet(project.directory.resolve(layout.mainSourceDirectory))
    Set<String> packages = fileSet.toFileInfos()
        .stream()
        .map({ info -> info.relative.getParent().toString().replace("/", ".") })
        .collect(Collectors.toSet())

    String command = "${javaDocPath} ${classpath(settings.mainDependencies, settings.libraryDirectories)} ${settings.docArguments} -sourcepath ${layout.mainSourceDirectory} -d ${layout.docDirectory} ${packages.join(" ")}"
    output.debugln("Executing JavaDoc command [%s]", command)

    Process process = command.execute([], project.directory.toFile())
    process.consumeProcessOutput((Appendable) System.out, System.err)
    process.waitFor()

    int exitCode = process.exitValue()
    if (exitCode != 0) {
      fail("JavaDoc failed")
    }
  }

  /**
   * Compiles an arbitrary source directory to an arbitrary build directory.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   java.compile(Paths.get("src/foo"), Paths.get("build/bar"), [[group: "compile", transitive: false, fetchSource: false]], Paths.get("additionalClasspathDirectory"))
   * </pre>
   *
   * @param sourceDirectory The source directory that contains the Java source files.
   * @param dllDirectory The build directory that the DLLs are built into.
   * @param dependencies The dependencies to resolve and include on the compile classpath.
   */
  private void compileInternal(Path sourceDirectory, Path dllDirectory, Path resourceDirectory, String dllName, List<Map<String, Object>> dependencies, Path... additionalLibraries) {
    initialize()

    Path resolvedSourceDir = project.directory.resolve(sourceDirectory)
    Path resolvedDLLDir = project.directory.resolve(dllDirectory)
    Path resolvedDLL = resolvedDLLDir.resolve(dllName)

    output.debugln("Looking for all files to compile in [%s]", resolvedSourceDir)

    if (Files.isRegularFile(resolvedDLL)) {
      Predicate<Path> filter = FileTools.extensionFilter(".cs")
      long filesToCompile = Files.walk(resolvedSourceDir)
          .filter(filter)
          .filter({ p -> Files.getLastModifiedTime(p) > Files.getLastModifiedTime(resolvedDLL) })
          .count()
      if (filesToCompile == 0) {
        output.infoln("Skipping compile for source directory [%s]. No files need compiling", sourceDirectory)
        return
      }

      output.infoln("Compiling [${filesToCompile}] C# classes from [${sourceDirectory}]")
    }

    String command = "${compilerPath} ${settings.additionalReferences.empty ? "" : "-r:${settings.additionalReferences.join(" -r:")}"} ${settings.languageVersion != null ? "-langversion:${settings.languageVersion}" : ""} ${settings.compilerArguments} ${libraryArguments(dependencies, settings.libraryDirectories, additionalLibraries)} ${resourceArguments(resourceDirectory)} -t:${settings.compilerType} -out:${dllDirectory.resolve(dllName)} -recurse:${sourceDirectory}/*.cs"
    output.debugln("Executing compiler command [%s]", command)

    Files.createDirectories(resolvedDLLDir)
    Process process = command.execute(null, project.directory.toFile())
    process.consumeProcessOutput((Appendable) System.out, System.err)
    process.waitFor()

    int exitCode = process.exitValue()
    if (exitCode != 0) {
      fail("Compilation failed")
    }
  }

  private String libraryArguments(List<Map<String, Object>> dependenciesList, List<Object> libraryDirectories, Path... additionalPaths) {
    List<Path> additionalDLLs = new ArrayList<>()
    if (libraryDirectories != null) {
      libraryDirectories.each { path ->
        Path dir = project.directory.resolve(FileTools.toPath(path))
        if (!Files.isDirectory(dir)) {
          return
        }

        Files.list(dir).filter(FileTools.extensionFilter(".dll")).forEach { file -> additionalDLLs.add(file.toAbsolutePath()) }
      }
    }

    Classpath classpath = dependencyPlugin.classpath {
      dependenciesList.each { deps -> dependencies(deps) }
      additionalPaths.each { additionalPath -> path(location: additionalPath) }
      additionalDLLs.each { additionalDLL -> path(location: additionalDLL) }
    }

    StringBuilder arguments = new StringBuilder()
    classpath.paths.each { p -> arguments.append(" -r:").append(p) }
    return arguments;
  }

  private String resourceArguments(Path resouceDirectory) {
    if (resouceDirectory == null) {
      return ""
    }

    Path dir = project.directory.resolve(FileTools.toPath(resouceDirectory))
    if (!Files.isDirectory(dir)) {
      return ""
    }

    StringBuilder arguments = new StringBuilder()
    Files.list(dir).forEach { file -> arguments.append(" -resource:").append(file.toAbsolutePath()) }
    return arguments;
  }

  private void initialize() {
    if (compilerPath) {
      return
    }

    if (!settings.sdkVersion) {
      fail("You must configure the SDK/Mono/.Net version to use with the settings object. It will look something like this:\n\n" +
          "  csharp.settings.sdkVersion=\"2.6\"")
    }

    String csharpHome = properties.getProperty(settings.sdkVersion)
    if (!csharpHome) {
      fail("No SDK/Mono/.Net platform is configured for version [%s].\n\n[%s]", settings.sdkVersion, ERROR_MESSAGE)
    }

    compilerPath = Paths.get(csharpHome, "bin/${settings.compilerExecutable}")
    if (!Files.isRegularFile(compilerPath)) {
      fail("The ${settings.compilerExecutable} compiler [%s] does not exist.", compilerPath.toAbsolutePath())
    }
    if (!Files.isExecutable(compilerPath)) {
      fail("The ${settings.compilerExecutable} compiler [%s] is not executable.", compilerPath.toAbsolutePath())
    }
  }
}
