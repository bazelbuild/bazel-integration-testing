load("//:bazel_integration_test.bzl", "bazel_java_integration_test")

bazel_java_integration_test(
    name = "BazelBaseTestCaseTest",
    srcs = ["BazelBaseTestCaseTest.java"],
    deps = ["@com_google_truth//jar"],
    data = ["//:root_bzl", "//tools:tools"]
)
