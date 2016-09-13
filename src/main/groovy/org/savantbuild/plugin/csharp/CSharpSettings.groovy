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

/**
 * Settings class that defines the settings used by the Java plugin.
 */
class CSharpSettings {
  /**
   * The name of the compiler executable. This defaults to <code>mcs</code> because we assume you are using Mono.
   */
  String compilerExecutable = "mcs"

  /**
   * Additional compiler arguments. This are included when javac is invoked. Defaults to {@code "-g"}.
   */
  String compilerArguments = "-g"

  /**
   * The type parameter that is passed to the compiler.
   */
  String compilerType = "library"

  /**
   * Additional JavaDoc arguments. This are included when javadoc is invoked. Defaults to {@code ""}.
   */
  String docArguments = ""

  /**
   * Configures the C# language version to use for compilation. This will depend on the compiler you are using, but for
   * mcs, this can be ISO-1, ISO-2, 3, or 4.
   */
  String languageVersion

  /**
   * Additional directories that contain JAR files to include in the compilation classpath. Defaults to {@code []}.
   */
  List<Object> libraryDirectories = []

  /**
   * The list of dependencies to include on the classpath when javac is called to compile the main Java source files.
   * This defaults to:
   * <p>
   * <pre>
   *   [
   *     [group: "compile", transitive: false, fetchSource: false],
   *     [group: "provided", transitive: false, fetchSource: false]
   *   ]
   * </pre>
   */
  List<Map<String, Object>> mainDependencies = [
      [group: "compile", transitive: false, fetchSource: false],
      [group: "provided", transitive: false, fetchSource: false]
  ]

  /**
   * Configures the SDK/Mono/.Net version to use for compilation. This version must be defined in the
   * ~/.savant/plugins/org.savantbuild.plugin.csharp.properties file.
   */
  String sdkVersion

  /**
   * The list of dependencies to include on the classpath when java is called to compile the test Java source files.
   * This defaults to:
   * <p>
   * <pre>
   *   [
   *     [group: "compile", transitive: false, fetchSource: false],
   *     [group: "test-compile", transitive: false, fetchSource: false],
   *     [group: "provided", transitive: false, fetchSource: false]
   *   ]
   * </pre>
   */
  List<Map<String, Object>> testDependencies = [
      [group: "compile", transitive: false, fetchSource: false],
      [group: "test-compile", transitive: false, fetchSource: false],
      [group: "provided", transitive: false, fetchSource: false]
  ]
}
