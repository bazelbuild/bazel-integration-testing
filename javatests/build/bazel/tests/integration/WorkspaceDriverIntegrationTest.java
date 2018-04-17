package build.bazel.tests.integration;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class WorkspaceDriverIntegrationTest extends BazelBaseTestCase {
    @Test
    public void testWorkspaceWithBazelRcFile() throws Exception {
        String testName = "TestMe";

        addExternalRepositoryFor("org_junit", "junit-4.11.jar");
        writeWorkspaceFileWithRepositories("org_junit");

        driver.scratchFile("BUILD.bazel", testLabelNamed(testName));
        driver.scratchFile("TestMe.java", passingTestNamed(testName));

        driver.scratchFile(".bazelrc", "test --javacopt=\"-InvalidOpt\"");

        Command cmd = driver.bazelCommand()
                .withBazelrcFile(Paths.get(".bazelrc"))
                .withArguments("test", "//:TestMe").build();

        int returnCode = cmd.run();

        assertNotEquals("bazel test return code", 0, returnCode);
        assertTrue("stderr contains InvalidOpt failure", cmd.getErrorLines().stream().anyMatch(x -> x.contains("-InvalidOpt")));
    }

    @Test
    public void testRunningWithEnvironment() throws Exception {
        String testName = "test_me";
        String key = "SOME_KEY";
        String val = "some_value";
        driver.scratchFile("BUILD.bazel", shTest(testName));
        driver.scratchExecutableFile(testName+".sh", shellTestingEnvironmentVariable(key,val));
        Command cmd = driver.bazelCommand()
                //.withEnvironmentVariable(key,val)
                .withArguments(
                        "test",
                        "--test_env="+key,
                        "//:"+testName).build();

        int returnCode = cmd.run();

        assertEquals("bazel test environemnt variable return code", 0, returnCode);
    }

    private List<String> shellTestingEnvironmentVariable(String key, String val) {
        return Arrays.asList(
                "#!/bin/bash",
                "test \"$"+key+"\" = \""+val+"\"","");
    }

    private void writeWorkspaceFileWithRepositories(String... repos) throws IOException {
        Stream<String> reposDec = Arrays.stream(repos).map(WorkspaceDriverIntegrationTest::repositoryDeclarationFor);

        driver.scratchFile("./WORKSPACE",
                "workspace(name = 'driver_integration_tests')",
                reposDec.reduce("", (acc, cur) -> acc + cur)
        );
    }

    private void addExternalRepositoryFor(final String repoName, final String repoJarName) throws IOException {
        driver.copyFromRunfiles("build_bazel_integration_testing/external/" + repoName + "/jar/" + repoJarName,
                "external/" + repoName + "/jar/" + repoJarName);
        driver.scratchFile("external/" + repoName + "/WORKSPACE", "");
        driver.scratchFile("external/" + repoName + "/jar/BUILD.bazel", "java_import(\n" +
                "    name = 'jar',\n" +
                "    jars = ['" + repoJarName + "'],\n" +
                "    visibility = ['//visibility:public']\n" +
                ")\n");
    }

    private static List<String> testLabelNamed(String name) {
        return Arrays.asList(
                "java_test(",
                "  name = '" + name + "',",
                "  srcs = ['" + name + ".java'],",
                "  test_class = 'build.bazel.tests.integration." + name + "',",
                "  deps = ['@org_junit//jar'],",
                ")"
        );
    }

    private static List<String> shTest(String name) {
        return Arrays.asList(
                "sh_test(",
                "  name = '" + name + "',",
                "  srcs = ['" + name + ".sh'],",
                ")"
        );
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
        return "local_repository(\n" +
                "    name = \"" + repoName + "\",\n" +
                "    path = \"./external/" + repoName + "\"\n" +
                ")\n";
    }
}
