package(default_visibility = ["//visibility:public"])

py_binary(
    name = "updater",
    srcs = ["updater.py"],
    deps = [
        "@io_abseil_py//absl:app",
        "@io_abseil_py//absl/flags",
    ],
)

filegroup(
    name = "tools",
    srcs = glob(["*"]),
    visibility = ["//:internal"],
)
