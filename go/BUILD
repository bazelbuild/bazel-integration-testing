# gazelle:exclude bazel_test.go

load("//:bazel_integration_test.bzl", "bazel_go_integration_test")
load("@io_bazel_rules_go//go:def.bzl", "go_library")

filegroup(
    name = "bzl",
    srcs = [
        "bazel_integration_test.bzl",
    ],
    visibility = ["//:internal"],
)

bazel_go_integration_test(
    name = "bazel_test",
    srcs = [
        "bazel_test.go",  # keep
    ],
    importpath = "github.com/bazelbuild/bazel-integration-testing/go_external_test",
    deps = [
        ":go_default_library",
    ],
)

go_library(
    name = "go_default_library",
    srcs = ["bazel.go"],
    importpath = "github.com/bazelbuild/bazel-integration-testing/go",
    visibility = ["//visibility:public"],
    deps = [
        "@com_github_bazelbuild_rules_go//go/tools/bazel:go_default_library",
    ],
)
