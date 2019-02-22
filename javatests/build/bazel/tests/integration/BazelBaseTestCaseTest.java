// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.bazel.tests.integration;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** {@link BazelBaseTestCase}Test */
// suppress since same parameter value is ok for tests readability, tests should encapsulate and not
// hide
@SuppressWarnings("SameParameterValue")
public final class BazelBaseTestCaseTest extends BazelBaseTestCase {


  @Test
  public void testVersion() throws Exception {
    BazelCommand cmd = driver.bazel("info", "release").mustRunSuccessfully();
    assertThat(cmd.outputLines())
        .contains("release " + WorkspaceDriver.properties.getProperty("bazel.version"));
  }

  @Test
  public void testRunningBazelInGivenRelativeDirectory() throws Exception {
    driver.scratchFile("foo/BUILD", "sh_test(name = \"bar\",\n" + "srcs = [\"bar.sh\"])");
    driver.scratchExecutableFile("foo/bar.sh", "echo \"in bar\"");

    BazelCommand cmd =
        driver.bazel("run", "bar").inWorkingDirectory(Paths.get("foo")).mustRunSuccessfully();
    assertThat(cmd.outputLines()).contains("in bar");
  }

  /**
   * Write out a BUILD file that loads the `bazel_java_integration_test` rule.
   *
   * This is a demo of how to load a .bzl file that is included in the parent repo.
   * The test itself doesn't invoke Bazel, it's just a wrapper.
   */
  @Test
  public void testTestSuiteExists() throws Exception {
    loadIntegrationTestRuleIntoWorkspace();
    final String testName = "IntegrationTestSuiteTest";
    driver.scratchFile(testName + ".java", Arrays.asList(
        "import org.junit.Test;",
        "public class " + testName + " {",
        " @Test",
        " public void testSuccess() {",
        "  }",
        "}"));
    driver.scratchFile(
        "BUILD",
        "load('@build_bazel_integration_testing//java:java.bzl', 'bazel_java_integration_test')",
        "",
        "bazel_java_integration_test(",
        "    name = '" + testName + "',",
        "    test_class = '" + testName + "',",
        "    srcs = ['" + testName + ".java'],",
        // inside the sandbox we don't have access to full bazel
        // and we don't need it since it's prepared in advance for us
        "    add_bazel_data_dependency = False,",
        ")");

    driver.bazel("test", "//:IntegrationTestSuiteTest").mustRunSuccessfully();
  }

  @Test
  public void testJvmFlags() {
    org.hamcrest.MatcherAssert.assertThat(System.getProperty("foo.bar"), is("true"));
  }

  private void loadIntegrationTestRuleIntoWorkspace() throws IOException {
    driver.scratchFile("./WORKSPACE",
      "workspace(name = 'integration_test')",
      "",
      "local_repository(",
      "    name = 'build_bazel_integration_testing',",
      "    path = './build_bazel_integration_testing',",
      ")");

    // Now copy in the build_bazel_integration_testing, which is included from
    // the "data" attribute. By depending on the bzl_library rule we have
    // access to the //java and //tools sections of the bzl tree. Note that
    // this only imports files that are EXPLICITLY listed in the "data"
    // attribute or are a compiled dependency of this library. Anything outside
    // of that (BUILD files, WORKSPACE files, or source files) are not
    // included.
    driver.copyDirectoryFromRunfiles(
        "build_bazel_integration_testing/", "");

    // When using local_repository, the local repo must have a WORKSPACE file.
    // Make one since we don't get one for free (it isn't in the "data"
    // attribute").
    driver.scratchFile("build_bazel_integration_testing/WORKSPACE",
        "workspace(name = 'build_bazel_integration_testing')");

    // In order to make //java and //tools a package it must have a build file
    // (even if it's empty).
    // Make one since we don't get one for free (it isn't in the "data"
    // attribute").
    driver.scratchFile("build_bazel_integration_testing/java/BUILD", "");
    driver.scratchFile("build_bazel_integration_testing/tools/BUILD", "");

    // Create the java/build/bazel/tests/integration package with a java_import
    // that loads the java compiled resource that the sub-test can import the
    // workspace driver (even though it doesn't use it).
    driver.scratchFile(
        "build_bazel_integration_testing/java/build/bazel/tests/integration/BUILD.bazel",
        "java_import(",
        "    name = 'workspace_driver',",
        "    jars = ['libworkspace_driver.jar'],",
        "    visibility = ['//visibility:public']",
        ")");
  }
}
