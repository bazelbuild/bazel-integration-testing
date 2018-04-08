package build.bazel.tests.integration;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class WorkspaceDriverIntegrationTest extends BazelBaseTestCase {
    @Test
    public void testWorkspaceWithBazelRcFile() throws Exception {
        String testName = "TestMe";

        driver.scratchFile("WORKSPACE", workspaceWithJunit());
        driver.scratchFile("BUILD.bazel", testLabelNamed(testName));
        driver.scratchFile("TestMe.java", passingTestNamed(testName));

        driver.scratchFile(".bazelrc", "test --javacopt=\"-InvalidOpt\"");

        Command cmd = driver.bazel(Optional.of(Paths.get(".bazelrc")), "test", "//:TestMe");
        int returnCode = cmd.run();

        assertNotEquals("bazel test return code", 0, returnCode);
        assertTrue("stderr contains InvalidOpt failure", cmd.getErrorLines().stream().anyMatch(x -> x.contains("-InvalidOpt")));
    }

    private List<String> testLabelNamed(String name) {
        return Arrays.asList(
                "java_test(",
                "  name = '" + name + "',",
                "  srcs = ['"+ name +".java'],",
                "  test_class = 'build.bazel.tests.integration." + name + "',",
                "  deps = ['@org_junit//jar'],",
                ")"
        );
    }

    private List<String> passingTestNamed(String name) {
        return Arrays.asList(
                "package build.bazel.tests.integration;",
                "import org.junit.Test;",
                "public class " + name + " {",
                "  @Test",
                "  public void testSuccess() {",
                "  }",
                "}");
    }

    private List<String> workspaceWithJunit() {
        return Arrays.asList(
                "workspace(name = 'driver_integration_tests')",
                "maven_jar(",
                "  name = 'org_junit',",
                "  artifact = 'junit:junit:jar:4.11',",
                ")"
        );
    }
}
