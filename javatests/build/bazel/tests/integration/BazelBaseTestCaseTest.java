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

  private static final String WORKSPACE_NAME =
      "workspace(name = 'build_bazel_integration_testing')";

  @Test
  public void testVersion() throws Exception {
    BazelCommand cmd = driver.bazel("info", "release").mustRunSuccessfully();
    assertThat(cmd.outputLines())
        .contains("release " + properties.getProperty("bazel.version"));
  }

  @Test
  public void testRunningBazelInGivenRelativeDirectory() throws Exception {
    driver.scratchFile("foo/BUILD", "sh_test(name = \"bar\",\n" + "srcs = [\"bar.sh\"])");
    driver.scratchExecutableFile("foo/bar.sh", "echo \"in bar\"");

    // --enable_runfiles has no effect on Linux and macOS, but it enables runfiles symlink tree on Windows.
    BazelCommand cmd =
        driver.bazel("run", "bar", "--enable_runfiles").inWorkingDirectory(Paths.get("foo")).mustRunSuccessfully();
    assertThat(cmd.outputLines()).contains("in bar");
  }

  @Test
  public void testTestSuiteExists() throws Exception {
    loadIntegrationTestRuleIntoWorkspace();
    setupPassingTest("IntegrationTestSuiteTest");
    // --enable_runfiles has no effect on Linux and macOS, but it enables runfiles symlink tree on Windows.
    driver.bazel("test", "//:IntegrationTestSuiteTest", "--enable_runfiles").mustRunSuccessfully();
  }

  @Test
  public void testJvmFlags() {
    org.hamcrest.MatcherAssert.assertThat(System.getProperty("foo.bar"), is("true"));
  }

  private void setupPassingTest(final String testName) throws IOException {
    writePassingTestJavaSource(testName);
    writeTestBuildFile(testName);
  }

  private void writeTestBuildFile(final String testName) throws IOException {
    driver.scratchFile(
        "BUILD",
        "load('//:bazel_integration_test.bzl', 'bazel_java_integration_test')",
        "",
        "bazel_java_integration_test(",
        "    name = '" + testName + "',",
        "    test_class = '" + testName + "',",
        "    srcs = ['" + testName + ".java'],",
        // inside the sandbox we don't have access to full bazel
        // and we don't need it since it's prepared in advance for us
        "    add_bazel_data_dependency = False,",
        ")");
  }

  private void loadIntegrationTestRuleIntoWorkspace() throws IOException {
    setupRuleSkylarkFiles();
    setupRuleCode();
    driver.scratchFile("./WORKSPACE", WORKSPACE_NAME);
  }

  private void setupRuleCode() throws IOException {
    driver.copyFromRunfiles(
        "build_bazel_integration_testing/java/build/bazel/tests/integration/libworkspace_driver.jar",
        "java/build/bazel/tests/integration/libworkspace_driver.jar");
    driver.scratchFile(
        "java/build/bazel/tests/integration/BUILD.bazel",
        "java_import(",
        "    name = 'workspace_driver',",
        "    jars = ['libworkspace_driver.jar'],",
        "    visibility = ['//visibility:public']",
        ")");
  }

  private void setupRuleSkylarkFiles() throws IOException {
    driver.copyFromRunfiles(
        "build_bazel_integration_testing/bazel_integration_test.bzl", "bazel_integration_test.bzl");
    driver.copyDirectoryFromRunfiles(
        "build_bazel_integration_testing/tools", "build_bazel_integration_testing");
    driver.scratchFile(
        "go/bazel_integration_test.bzl",
        "RULES_GO_COMPATIBLE_BAZEL_VERSION = []\n"
            + "def bazel_go_integration_test(name, srcs, deps=[], versions=RULES_GO_COMPATIBLE_BAZEL_VERSION, **kwargs):\n"
            + "  pass");
    // In order to make //go a package it must have a build file (even if it's empty).
    driver.scratchFile("go/BUILD.bazel", "");
  }

  private void writePassingTestJavaSource(final String testName) throws IOException {
    driver.scratchFile("" + testName + ".java", somePassingTestNamed(testName));
  }

  private List<String> somePassingTestNamed(final String testName) {
    return Arrays.asList(
        "import org.junit.Test;",
        "public class " + testName + " {",
        " @Test",
        " public void testSuccess() {",
        "  }",
        "}");
  }
}
