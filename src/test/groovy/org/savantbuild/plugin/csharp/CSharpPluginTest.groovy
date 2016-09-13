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

import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Dependencies
import org.savantbuild.dep.domain.DependencyGroup
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Version
import org.savantbuild.dep.workflow.FetchWorkflow
import org.savantbuild.dep.workflow.PublishWorkflow
import org.savantbuild.dep.workflow.Workflow
import org.savantbuild.dep.workflow.process.CacheProcess
import org.savantbuild.dep.workflow.process.URLProcess
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertTrue

/**
 * Tests the CSharp plugin.
 *
 * @author Brian Pontarelli
 */
class CSharpPluginTest {
  public static Path projectDir

  @BeforeSuite
  public void beforeSuite() {
    println "Setup"
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../csharp-plugin")
    }
  }

  @Test
  public void all() throws Exception {
    println "Start"
    FileTools.prune(projectDir.resolve("build/cache"))

    Output output = new SystemOutOutput(true)
    output.enableDebug()

    Project project = new Project(projectDir.resolve("test-project"), output)
    project.group = "org.savantbuild.test"
    project.name = "test-project"
    project.version = new Version("1.0")
    project.licenses.put(License.ApacheV2_0, null)

    project.dependencies = new Dependencies(new DependencyGroup("test-compile", false, new Artifact("org.nunit:nunit.framework:2.6.3:dll", false)))
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, projectDir.resolve("build/cache").toString()),
            new URLProcess(output, "http://savant.inversoft.org", null, null)
        ),
        new PublishWorkflow(
            new CacheProcess(output, projectDir.resolve("build/cache").toString())
        )
    )

    try {
      CSharpPlugin plugin = new CSharpPlugin(project, new RuntimeConfiguration(), output)
      plugin.settings.sdkVersion = "2.6"
      plugin.settings.libraryDirectories.add("lib")
      plugin.settings.additionalReferences << "System.Web"
      plugin.settings.compilerExecutable = "gmcs"

      println "Clean"
      plugin.clean()
      assertFalse(Files.isDirectory(projectDir.resolve("test-project/build")))

      println "Compile main"
      plugin.compileMain()
      assertTrue(Files.isRegularFile(projectDir.resolve("test-project/build/dlls/test-project.dll")))

      println "Compile test"
      plugin.compileTest()
      assertTrue(Files.isRegularFile(projectDir.resolve("test-project/build/dlls/test-project-test.dll")))
    } catch (e) {
      e.printStackTrace()
    }

//    plugin.document()
//    assertTrue(Files.isRegularFile(projectDir.resolve("test-project/build/doc/index.html")))
  }
}
