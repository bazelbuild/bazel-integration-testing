load("//tools:common.bzl", "BAZEL_VERSIONS")

java_library(
    name = "workspace_driver",
    testonly = True,
    srcs = [
        "BazelCommand.java",
        "Command.java",
        "RepositoryCache.java",
        "TestResult.java",
        "WorkspaceDriver.java",
        "OS.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@io_bazel_bep//jar",
    ],
)

java_library(
    name = "integration",
    testonly = True,
    srcs = [
        "BazelBaseTestCase.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":workspace_driver",
        "@com_google_guava//jar",
        "@org_junit//jar",
    ],
)
