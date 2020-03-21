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
        "//tools",
    ],
    visibility = ["//:internal"],
)
