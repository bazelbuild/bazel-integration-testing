load("//:bazel_integration_test.bzl", "bazel_java_integration_test")

bazel_java_integration_test(
    name = "BazelBaseTestCaseTest",
    srcs = ["BazelBaseTestCaseTest.java"],
    deps = ["@com_google_truth//jar"],
    data = ["//:root_bzl", "//tools:tools"]
)

java_library(
    name = "BazelIntegrationTestNoSourcesSrc",
    srcs = ["BazelIntegrationTestNoSourcesTest.java"],
    deps = ["@org_junit//jar"] 
)

bazel_java_integration_test(
    name = "BazelIntegrationTestNoSources",
    runtime_deps = [":BazelIntegrationTestNoSourcesSrc"],
    test_class = "build.bazel.tests.integration.BazelIntegrationTestNoSourcesTest",
)
