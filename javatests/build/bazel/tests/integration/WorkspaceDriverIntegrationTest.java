package build.bazel.tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

public class WorkspaceDriverIntegrationTest extends BazelBaseTestCase {
  @Test
  public void testWorkspaceWithBazelRcFile() throws Exception {
    String testName = "TestMe";

    addExternalRepositoryFor("org_junit", "junit-4.11.jar");
    writeWorkspaceFileWithRepositories("org_junit");

    driver.scratchFile("BUILD.bazel", testLabelNamed(testName));
    driver.scratchFile("TestMe.java", passingTestNamed(testName));

    driver.scratchFile(".bazelrc", "test --javacopt=\"-InvalidOpt\"");

    Command cmd =
        driver.bazelCommand("test", "//:TestMe").withBazelrcFile(Paths.get(".bazelrc")).build();

    int returnCode = cmd.run();

    assertNotEquals("bazel test return code", 0, returnCode);
    assertTrue(
        "stderr contains InvalidOpt failure",
        cmd.getErrorLines().stream().anyMatch(x -> x.contains("-InvalidOpt")));
  }

  @Test
  public void testRunningWithEnvironment() throws Exception {
    String testName = "test_me";
    String key = "SOME_KEY";
    String val = "some_value";
    driver.scratchFile("BUILD.bazel", shTest(testName));
    driver.scratchExecutableFile(testName + ".sh", shellTestingEnvironmentVariable(key, val));
    Command cmd =
        driver
            .bazelCommand("test", "--test_env=" + key, "//:" + testName)
            .withEnvironmentVariable(key, val)
            .build();

    int returnCode = cmd.run();

    assertEquals("bazel test environment variable return code", 0, returnCode);
  }

  /**
   * Test that external dependencies specified with {@code bazel_external_dependency_archive} can be
   * used with {@code java_import_external} without any download taking place. Indeed, because the
   * cache is frozen (see {@link RepositoryCache#freeze()}), it is not possible to add any other
   * dependency.
   */
  @Test
  public void testExternalDep() throws Exception {
    // The three following dependencies are arbitrary and there to show that multiple external
    // dependencies can be used without downloading, either pertaining to the same
    // `bazel_external_dependency_archive` or to two different `bazel_external_dependency_archive`.
    driver.scratchFile(
        "WORKSPACE",
        "load(\"@bazel_tools//tools/build_defs/repo:java.bzl\", \"java_import_external\")",
        "java_import_external(",
        "    name = \"org_junit\",",
        "    licenses = [\"restricted\"],  # Eclipse Public License 1.0",
        "    jar_sha256 = \"90a8e1603eeca48e7e879f3afbc9560715322985f39a274f6f6070b43f9d06fe\",",
        "    jar_urls = [",
        "        \"http://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar\",",
        "        \"http://maven.ibiblio.org/maven2/junit/junit/4.11/junit-4.11.jar\",",
        "    ],",
        ")",
        "java_import_external(",
        "    name = \"com_beust_jcommander\",",
        "    licenses = [\"notice\"],  # Apache 2.0",
        "    jar_sha256 = \"e0de160b129b2414087e01fe845609cd55caec6820cfd4d0c90fabcc7bdb8c1e\",",
        "    jar_urls = [",
        "        \"http://repo1.maven.org/maven2/com/beust/jcommander/1.72/jcommander-1.72.jar\",",
        "        \"http://maven.ibiblio.org/maven2/com/beust/jcommander/1.72/jcommander-1.72.jar\",",
        "    ],",
        ")",
        "java_import_external(",
        "    name = \"javax_inject\",",
        "    licenses = [\"notice\"],  # The Apache Software License, Version 2.0",
        "    jar_sha256 = \"91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff\",",
        "    jar_urls = [",
        "        \"http://repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar\",",
        "        \"http://maven.ibiblio.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar\",",
        "    ],",
        ")");

    driver.scratchFile(
        "BUILD.bazel",
        "java_test(",
        "    name = \"TestMe\",",
        "    srcs = [\"TestMe.java\"],",
        "    test_class = \"build.bazel.tests.integration.TestMe\",",
        "    deps = [\"@org_junit//jar\", \"@com_beust_jcommander//jar\", \"@javax_inject//jar\"],",
        ")");
    // We import some classes to show that the files cached with
    // `bazel_external_dependency_archive` are jars that contain expected classes.
    driver.scratchFile(
        "TestMe.java",
        "package build.bazel.tests.integration;",
        "import com.beust.jcommander.JCommander;",
        "import javax.inject.Singleton;",
        "import org.junit.Test;",
        "public class TestMe {",
        "  @Test",
        "  public void testSuccess() {",
        "  }",
        "}");

    Command cmd = driver.bazelCommand("test", "//:TestMe").build();

    int returnCode = cmd.run();
    assertEquals(0, returnCode);
  }

  /** Try (unsuccessfully) to download a dependency that was not specified as an external dep. */
  @Test
  public void testRepositoryCacheIsFrozen() throws Exception {
    // net_sf_jopt_simple has a sha256 checksum that has not been imported through
    // bazel_external_dependency_archive and so is unavailable.
    driver.scratchFile(
        "WORKSPACE",
        "load(\"@bazel_tools//tools/build_defs/repo:java.bzl\", \"java_import_external\")",
        "java_import_external(",
        "    name = \"net_sf_jopt_simple\",",
        "    licenses = [\"notice\"],  # The MIT License",
        "    jar_sha256 = \"457877c79e038f390557db5f8e92c4436fb4f4b3ba63f28bc228500fee080193\",",
        "    jar_urls = [",
        "        \"http://maven.ibiblio.org/maven2/net/sf/jopt-simple/jopt-simple/5.0.2/jopt-simple-5.0.2.jar\",",
        "        \"http://repo1.maven.org/maven2/net/sf/jopt-simple/jopt-simple/5.0.2/jopt-simple-5.0.2.jar\",",
        "    ],",
        ")");

    Command cmd = driver.bazelCommand("build", "@net_sf_jopt_simple//jar").build();

    int returnCode = cmd.run();
    assertEquals(1, returnCode);
    String err = String.join("\n", cmd.getErrorLines());
    assertTrue(
        err.contains("(Permission denied)") // The repository cache was frozen (Mac OSX)
            || err.contains("Unknown host")); // The block-network tag works (Linux)
  }

  private List<String> shellTestingEnvironmentVariable(String key, String val) {
    return Arrays.asList("#!/bin/bash", "test \"$" + key + "\" = \"" + val + "\"", "");
  }

  private void writeWorkspaceFileWithRepositories(String... repos) throws IOException {
    Stream<String> reposDec =
        Arrays.stream(repos).map(WorkspaceDriverIntegrationTest::repositoryDeclarationFor);

    driver.scratchFile(
        "./WORKSPACE",
        "workspace(name = 'driver_integration_tests')",
        reposDec.reduce("", (acc, cur) -> acc + cur));
  }

  private void addExternalRepositoryFor(final String repoName, final String repoJarName)
      throws IOException {
    driver.copyFromRunfiles(
        "build_bazel_integration_testing/external/" + repoName + "/jar/" + repoJarName,
        "external/" + repoName + "/jar/" + repoJarName);
    driver.scratchFile("external/" + repoName + "/WORKSPACE", "");
    driver.scratchFile(
        "external/" + repoName + "/jar/BUILD.bazel",
        "java_import(\n"
            + "    name = 'jar',\n"
            + "    jars = ['"
            + repoJarName
            + "'],\n"
            + "    visibility = ['//visibility:public']\n"
            + ")\n");
  }

  private static List<String> testLabelNamed(String name) {
    return Arrays.asList(
        "java_test(",
        "  name = '" + name + "',",
        "  srcs = ['" + name + ".java'],",
        "  test_class = 'build.bazel.tests.integration." + name + "',",
        "  deps = ['@org_junit//jar'],",
        ")");
  }

  private static List<String> shTest(String name) {
    return Arrays.asList(
        "sh_test(", "  name = '" + name + "',", "  srcs = ['" + name + ".sh'],", ")");
  }

  private static List<String> passingTestNamed(String name) {
    return Arrays.asList(
        "package build.bazel.tests.integration;",
        "import org.junit.Test;",
        "public class " + name + " {",
        "  @Test",
        "  public void testSuccess() {",
        "  }",
        "}");
  }

  private static String repositoryDeclarationFor(final String repoName) {
    return "local_repository(\n"
        + "    name = \""
        + repoName
        + "\",\n"
        + "    path = \"./external/"
        + repoName
        + "\"\n"
        + ")\n";
  }
}
