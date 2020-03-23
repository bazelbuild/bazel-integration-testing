load("//tools:common.bzl", "BAZEL_VERSIONS")

java_library(
    name = "workspace_driver",
    testonly = True,
    srcs = [
        "BazelCommand.java",
        "Command.java",
        "ExecutableBuilder.java",
        "RepositoryCache.java",
        "WorkspaceDriver.java",
        "OS.java",
    ],
    visibility = ["//visibility:public"],
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
