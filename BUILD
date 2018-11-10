load("@bazel_gazelle//:def.bzl", "gazelle")

package_group(
    name = "internal",
    packages = ["//..."],
)

filegroup(
    name = "root_bzl",
    srcs = glob(["*.bzl"]),
    visibility = ["//:internal"],
)

filegroup(
    name = "all_bzl",
    srcs = [
        ":root_bzl",
        "//go:bzl",
        "//tools",
    ],
    visibility = ["//:internal"],
)

gazelle(
    name = "gazelle",
    prefix = "github.com/bazelbuild/bazel-integration-testing",
)
