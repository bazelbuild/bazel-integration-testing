workspace(name = "build_bazel_integration_testing")

## Sanity checks

load("//:bazel_version.bzl", "check_bazel_version")

check_bazel_version("0.5.0")

#### Fetch remote resources

## Python

new_http_archive(
    name = "com_google_python_gflags",
    build_file_content = """
py_library(
    name = "gflags",
    srcs = [
        "gflags.py",
        "gflags_validators.py",
    ],
    visibility = ["//visibility:public"],
)
""",
    sha256 = "344990e63d49b9b7a829aec37d5981d558fea12879f673ee7d25d2a109eb30ce",
    strip_prefix = "python-gflags-python-gflags-2.0",
    url = "https://github.com/google/python-gflags/archive/python-gflags-2.0.zip",
)

## Java

maven_jar(
    name = "com_google_truth",
    artifact = "com.google.truth:truth:jar:0.31",
)

## golang

http_archive(
    name = "io_bazel_rules_go",
    sha256 = "91fca9cf860a1476abdc185a5f675b641b60d3acf0596679a27b580af60bf19c",
    url = "https://github.com/bazelbuild/rules_go/releases/download/0.7.0/rules_go-0.7.0.tar.gz",
)

#### Use remote resources

## java

load("//:bazel_integration_test.bzl", "bazel_java_integration_test_deps")

bazel_java_integration_test_deps()

## golang

load("@io_bazel_rules_go//go:def.bzl", "go_rules_dependencies", "go_register_toolchains", "go_repository")

go_rules_dependencies()

go_register_toolchains()

go_repository(
    name = "com_github_bazelbuild_rules_go",
    commit = "0bd97fc6ae48d4124e1a8506bcb397be766f8b83",
    importpath = "github.com/bazelbuild/rules_go",
)

## Your new language here!
