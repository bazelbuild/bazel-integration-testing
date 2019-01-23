## Usage
### BUILD.bazel
```python
load("//:bazel_integration_test.bzl", "bazel_java_integration_test")
bazel_java_integration_test(
    name = "MyTest",
    srcs = ["MyTest.java"],
)
```
If you want your test to be in a different JVM language you can use the following:
```python
bazel_java_integration_test(
    name = "MyScalaTest",
    test_class = "com.example.tests.integration.MyTestFromScala",
    runtime_deps = [":MyScalaTestSrc"],
)
```
Where `MyScalaTestSrc` is a `scala_library` you add yourself and has `com.example.tests.integration.MyTestFromScala` class.       
### Test
**Note** your test class must be run by JUnit as its run by `java_test`.  
Very simplistic example:
```java
public class ExampleTest {
  private WorkspaceDriver driver = new WorkspaceDriver();

  @BeforeClass
  public static void setUpClass() throws IOException {
    WorkspaceDriver.setUpClass();
  }

  @Before
  public void setUp() throws Exception {
    driver.setUp();
  }

  @Test
  public void bazelFailure() throws Exception {
    driver.scratchFile("foo/BUILD", "sh_test(name = \"bar\",\n" + "srcs = [\"bar.sh\"])");
    driver.scratchExecutableFile("foo/bar.sh", "echo \"boom\"", "exit -1");

    Command cmd = driver.bazelCommand("test", "//foo:bar").build();

    int returnCode = cmd.run();

    assertNotEquals("bazel test return code", 0, returnCode);
    assertTrue(
        "stderr contains boom failure",
        cmd.getErrorLines().stream().anyMatch(x -> x.contains("boom")));
  }
}
```  
In your test you should work with the `WorkspaceDriver` which has many useful utility methods for working with scratch workspaces.  
These include, but are not limited to, `copyFromRunfiles`/`scratchFile`/`workspaceDirectoryContents`(useful for verbose error messages) and of course `bazelCommand` which allows you to run `bazel build` / `bazel test` / `bazel query` and more.  
The [WorkspaceDriverIntegrationTest](https://github.com/bazelbuild/bazel-integration-testing/blob/master/javatests/build/bazel/tests/integration/WorkspaceDriverIntegrationTest.java) has a few examples of realistic tests which sets up the environment and asserts Bazel's outputs (exit code, stderr, etc).  
[WorkspaceDriverTest](https://github.com/bazelbuild/bazel-integration-testing/blob/master/javatests/build/bazel/tests/integration/WorkspaceDriverTest.java) and [BazelBaseTestCaseTest](https://github.com/bazelbuild/bazel-integration-testing/blob/master/javatests/build/bazel/tests/integration/BazelBaseTestCaseTest.java) show some more of the features.
