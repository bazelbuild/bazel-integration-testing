package build.bazel.tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    BazelCommand cmd =
        driver.bazel("test", "//:TestMe").withBazelrcFile(Paths.get(".bazelrc")).run();

    assertNotEquals(0, cmd.exitCode());
    assertTrue(
        "stderr contains InvalidOpt failure",
        cmd.errorLines().stream().anyMatch(x -> x.contains("-InvalidOpt")));
  }

  @Test
  public void testRunningWithEnvironment() throws Exception {
    String testName = "test_me";
    String key = "SOME_KEY";
    String val = "some_value";
    driver.scratchFile("BUILD.bazel", shTest(testName));
    driver.scratchExecutableFile(testName + ".sh", shellTestingEnvironmentVariable(key, val));
    driver
        .bazel("test", "--test_env=" + key, "//:" + testName)
        .withEnvironmentVariable(key, val)
        .mustRunSuccessfully();
  }

  @Test
  public void testRunWithArguments() throws Exception {
    driver.scratchFile("BUILD.bazel", shBinary("test_me"));
    driver.scratchExecutableFile("test_me.sh", shellTestingArguments("hello", "world"));
    driver.bazel("run", "//:test_me", "--", "hello", "world").mustRunSuccessfully();
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
        testLabelNamed("TestMe", "@com_beust_jcommander//jar", "@javax_inject//jar"));

    // We import some classes to show that the files cached with
    // `bazel_external_dependency_archive` are jars that contain expected classes.
    driver.scratchFile(
        "TestMe.java",
        passingTestNamed(
            "TestMe", "import com.beust.jcommander.JCommander;", "import javax.inject.Singleton;"));

    driver.bazel("test", "//:TestMe").mustRunSuccessfully();
  }

  /** Try (unsuccessfully) to download a dependency that was not specified as an external dep. */
  @Test
  public void testRepositoryCacheIsFrozen() throws Exception {
    //freezing of repository cache isn't supported on windows
    if (OS.getCurrent() != OS.WINDOWS) {
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

      BazelCommand cmd = driver.bazel("build", "@net_sf_jopt_simple//jar").run();

      assertEquals(1, cmd.exitCode());
      String err = String.join(",", cmd.errorLines());
      assertTrue(
          err.contains("(Permission denied)") // The repository cache was frozen (Mac OSX)
              || err.contains("Unknown host")); // The block-network tag works (Linux)
    }
  }

  @Test
  public void testRunningBazelQuery() throws Exception {
    driver.scratchFile("BUILD.bazel", shTest("foo"));
    driver.bazelWithoutJavaBaseConfig("query","//:foo").mustRunSuccessfully();
  }

  @Test
  public void testExecutable() throws Exception {
    driver
        .executable("foo/bar", "qux")
        .manifest(
            "build_bazel_integration_testing/testdata/java_executable_with_runfiles.manifest.txt")
        .scratch();

    Command cmd = driver.bazelCommand("run", "//foo/bar:qux", "--", "hello", "world").build();

    int returnCode = cmd.run();
    assertEquals(0, returnCode);
  }

  private List<String> shellTestingEnvironmentVariable(String key, String val) {
    return Arrays.asList("#!/bin/bash", "test \"$" + key + "\" = \"" + val + "\"", "");
  }

  private List<String> shellTestingArguments(String... arguments) {
    return Stream.concat(
            Stream.of("#!/bin/bash"),
            IntStream.range(0, arguments.length)
                .mapToObj(i -> "test \"$" + (i + 1) + "\" = \"" + arguments[i] + "\""))
        .collect(Collectors.toList());
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
        "build_bazel_integration_testing/external/" + repoName + "/" + repoJarName,
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

  private static List<String> testLabelNamed(String name, String... additionalDeps) {
    return Arrays.asList(
        "java_test(",
        "  name = '" + name + "',",
        "  srcs = ['" + name + ".java'],",
        "  test_class = 'build.bazel.tests.integration." + name + "',",
        "  deps = ['@org_junit//jar'" + quoteAndAddDeps(additionalDeps) + "],",
        ")");
  }

  private static String quoteAndAddDeps(String[] additionalDeps) {
    return Arrays.stream(additionalDeps)
        .map(dep -> "'" + dep + "'")
        .reduce("", (acc, cur) -> acc + "," + cur);
  }

  private static List<String> shTest(String name) {
    return Arrays.asList(
        "sh_test(", "  name = '" + name + "',", "  srcs = ['" + name + ".sh'],", ")");
  }

  private static List<String> shBinary(String name) {
    return Arrays.asList(
        "sh_binary(", "  name = '" + name + "',", "  srcs = ['" + name + ".sh'],", ")");
  }

  private static List<String> passingTestNamed(String name, String... additionalImports) {
    List<String> prefix = Collections.singletonList("package build.bazel.tests.integration;");
    List<String> additionalDepsList = Arrays.asList(additionalImports);
    List<String> suffix =
        Arrays.asList(
            "import org.junit.Test;",
            "public class " + name + " {",
            "  @Test",
            "  public void testSuccess() {",
            "  }",
            "}");
    return Stream.of(prefix, additionalDepsList, suffix)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
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
