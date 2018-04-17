load("//:bazel_integration_test.bzl", "bazel_java_integration_test")

bazel_java_integration_test(
    name = "BazelBaseTestCaseTest",
    srcs = ["BazelBaseTestCaseTest.java"],
    data = [
        "//:all_bzl",
    ],
    deps = ["//java/build/bazel/tests/integration",
            "@com_google_truth//jar",],
)

bazel_java_integration_test(
    name = "WorkspaceDriverTest",
    srcs = ["WorkspaceDriverTest.java"],
    deps = ["@com_spotify_hamcrest_optional//jar"],
)

java_library(
    name = "BazelIntegrationTestNoSourcesSrc",
    srcs = ["BazelIntegrationTestNoSourcesTest.java"],
    deps = ["@org_junit//jar"],
)

# The below uses test_class instead of renaming the BazelIntegrationTestNoSourcesSrc target.
# This demonstrates a usage of test_class attribute.
bazel_java_integration_test(
    name = "BazelIntegrationTestNoSources",
    test_class = "build.bazel.tests.integration.BazelIntegrationTestNoSourcesTest",
    runtime_deps = [":BazelIntegrationTestNoSourcesSrc"],
)

bazel_java_integration_test(
    name = "WorkspaceDriverIntegrationTest",
    srcs = ["WorkspaceDriverIntegrationTest.java"],
    deps = ["//java/build/bazel/tests/integration"],
)

java_library(
    name = "CommandTestSrc",
    srcs = ["CommandTest.java"],
    deps = [
        "//java/build/bazel/tests/integration:workspace_driver",
        "@org_junit//jar",
        "@org_hamcrest_core//jar",
        ],
)

java_test(
    name = "CommandTest",

    runtime_deps = [":CommandTestSrc"],
)