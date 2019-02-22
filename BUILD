load("@bazel_gazelle//:def.bzl", "gazelle")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

package_group(
    name = "internal",
    packages = ["//..."],
)

bzl_library(
    name = "root_bzl",
    srcs = glob(["*.bzl"]),
    visibility = ["//:internal"],
)

bzl_library(
    name = "all_bzl",
    srcs = [
        ":root_bzl",
        "//go:bzl",
        "//java:bzl",
        "//python:bzl",
        "//tools:bzl",
    ],
    visibility = ["//:internal"],
)

# gazelle:prefix github.com/bazelbuild/bazel-integration-testing
gazelle(name = "gazelle")
