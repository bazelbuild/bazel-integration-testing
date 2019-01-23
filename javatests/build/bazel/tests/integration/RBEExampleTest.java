package build.bazel.tests.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RBEExampleTest extends BazelBaseTestCase {

  @Test
  public void testRunningWithEnvironment() throws Exception {
    String testName = "test_me";
    driver.scratchFile("BUILD.bazel", shTest(testName));
    driver.scratchExecutableFile(testName + ".sh", helloWorldShellScript());

    writeToolchainsPlatformTarget();
    writeWorkspaceFileWithBazelToolchains();
    remoteCompatibleBazelRcFile();

    driver
        .bazel("test", "//:" + testName)
        .mustRunSuccessfully();
  }

  private void writeWorkspaceFileWithBazelToolchains() throws IOException {
    driver.scratchFile("WORKSPACE",
        "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_archive\")",
      "http_archive(",
      "             name = \"bazel_toolchains\",",
      "             urls = [",
      "               \"https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/be10bee3010494721f08a0fccd7f57411a1e773e.tar.gz\"",
      "             ],",
      "             strip_prefix = \"bazel-toolchains-be10bee3010494721f08a0fccd7f57411a1e773e\",",
      "             sha256 = \"5962fe677a43226c409316fcb321d668fc4b7fa97cb1f9ef45e7dc2676097b26\",",
      ")"
        );
  }

  private void writeToolchainsPlatformTarget() throws IOException {
    driver.scratchFile("toolchains/BUILD.bazel",
      "package(default_visibility = [\"//visibility:public\"])",
        "platform(",
        "    name = \"rbe_ubuntu1604\",",
        "    constraint_values = [",
        "        \"@bazel_tools//platforms:x86_64\",",
        "        \"@bazel_tools//platforms:linux\",",
        "        \"@bazel_tools//tools/cpp:clang\",",
        "        \"@bazel_toolchains//constraints:xenial\",",
        "        \"@bazel_toolchains//constraints/sanitizers:support_msan\",",
        "    ],",
        ")");
  }

  private void remoteCompatibleBazelRcFile() throws IOException {
    driver.scratchFile(".bazelrc",
        remoteCompatibleToolchainFlags());
  }

  private static List<String> remoteCompatibleToolchainFlags() {
     return Arrays.asList(
         "build --crosstool_top=@bazel_toolchains//configs/ubuntu16_04_clang/1.1/bazel_0.22.0/default:toolchain",
         "build --action_env=BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1",
         "build --extra_toolchains=@bazel_toolchains//configs/ubuntu16_04_clang/1.1/bazel_0.22.0/cpp:cc-toolchain-clang-x86_64-default",
         "build --extra_execution_platforms=//toolchains:rbe_ubuntu1604"
     );
  }

  private List<String> helloWorldShellScript() {
    return Arrays.asList("#!/bin/bash", "echo 'hello world'", "exit 0", "");
  }

  private static List<String> shTest(String name) {
    return Arrays.asList(
        "sh_test(", "  name = '" + name + "',", "  srcs = ['" + name + ".sh'],", ")");
  }

}
