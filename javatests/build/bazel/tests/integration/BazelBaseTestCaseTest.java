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

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

/** {@link BazelBaseTestCase}Test */
// suppress since same parameter value is ok for tests readability, tests should encapsulate and not
// hide
@SuppressWarnings("SameParameterValue")
public final class BazelBaseTestCaseTest extends BazelBaseTestCase {

  private static final String WORKSPACE_NAME =
      "workspace(name = 'build_bazel_integration_testing')";

  @Test
  public void testVersion() throws Exception {
    Command cmd = driver.bazelCommand().withArguments("info", "release").build();
    assertEquals(0, cmd.run());
    assertThat(cmd.getOutputLines()).contains("release " + System.getProperty("bazel.version"));
  }

  @Test
  public void testRunningBazelInGivenRelativeDirectory() throws Exception {
    driver.scratchFile("foo/BUILD", "sh_test(name = \"bar\",\n" + "srcs = [\"bar.sh\"])");
    driver.scratchExecutableFile("foo/bar.sh", "echo \"in bar\"");

    Command cmd = driver.bazelCommand().withWorkingDirectory(Paths.get("foo")).withArguments( "run", "bar").build();

    assertEquals(0, cmd.run());
    assertThat(cmd.getOutputLines()).contains("in bar");
  }

  @Test
  public void testTestSuiteExists() throws Exception {
    loadIntegrationTestRuleIntoWorkspace();
    setupPassingTest("IntegrationTestSuiteTest");

    Command cmd = driver.bazelCommand().withArguments("test", "//:IntegrationTestSuiteTest").build();
    final int exitCode = cmd.run();

    org.hamcrest.MatcherAssert.assertThat(exitCode, is(successfulExitCode(cmd)));
  }

  private TypeSafeDiagnosingMatcher<Integer> successfulExitCode(
      final Command cmd) {
    return new TypeSafeDiagnosingMatcher<Integer>() {
      @Override
      protected boolean matchesSafely(
          final Integer exitCode, final Description mismatchDescription) {
        if (exitCode != 0) {
          mismatchDescription
              .appendText(" exit code was ")
              .appendValue(exitCode)
              .appendText("\n")
              .appendText("Workspace contents: \n")
              .appendValueList("", "\n", "\n", driver.workspaceDirectoryContents())
              .appendDescriptionOf(commandDescription(cmd));
          return false;
        }
        return true;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("successful exit code (0)");
      }
    };
  }

  private SelfDescribing commandDescription(final Command cmd) {
    return description -> {
      final String newLine = System.getProperty("line.separator");
      final List<String> logContents =
          logsOfInternalTests(cmd.getErrorLines()).collect(Collectors.toList());
      description
          .appendText("std-error:\n")
          .appendValueList("", newLine, newLine, cmd.getErrorLines());
      if (!logContents.isEmpty()) {
        description
            .appendText("Contents of internal test logs:\n")
            .appendText("*******************************\n")
            .appendValueList(newLine, newLine, newLine, logContents);
      }
    };
  }

  private Stream<String> logsOfInternalTests(final List<String> errorLines) {
    return errorLines
        .stream()
        .filter(line -> line.contains("(see "))
        .map(line -> line.split("see ")[1].replace(")", ""))
        .map(Paths::get)
        .map(
            logPath -> {
              try {
                LinkedList<String> logContents = new LinkedList<>(Files.readAllLines(logPath));
                logContents.addFirst("Log contents:");
                logContents.addFirst(logPath.toString());
                logContents.addFirst("Log path:");
                return logContents;
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .flatMap(Collection::stream);
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
    driver.copyFromRunfiles("build_bazel_integration_testing/java/build/bazel/tests/integration/libworkspace_driver.jar",
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
    driver.copyDirectoryFromRunfiles("build_bazel_integration_testing/tools", "build_bazel_integration_testing");
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
