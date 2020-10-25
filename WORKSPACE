workspace(name = "build_bazel_integration_testing")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Remote execution infra
# Required configuration for remote build execution
bazel_toolchains_version = "2.0.2"

bazel_toolchains_sha256 = "a653c9d318e42b14c0ccd7ac50c4a2a276c0db1e39743ab88b5aa2f0bc9cf607"

http_archive(
    name = "bazel_toolchains",
    sha256 = bazel_toolchains_sha256,
    strip_prefix = "bazel-toolchains-%s" % bazel_toolchains_version,
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % bazel_toolchains_version,
        "https://github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz" % bazel_toolchains_version,
    ],
)

load("@bazel_toolchains//rules:rbe_repo.bzl", "rbe_autoconfig")

# Creates toolchain configuration for remote execution with BuildKite CI
# for rbe_ubuntu1604
rbe_autoconfig(
    name = "buildkite_config",
)

## Sanity checks

git_repository(
    name = "bazel_skylib",
    remote = "https://github.com/bazelbuild/bazel-skylib",
    tag = "1.0.3",
)

load("@bazel_skylib//lib:versions.bzl", "versions")

versions.check("0.6.0")

## Linting

load("//private:format.bzl", "format_repositories")

format_repositories()

#### Fetch remote resources

## Python

http_archive(
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

load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")

jvm_maven_import_external(
    name = "com_google_truth",
    artifact = "com.google.truth:truth:jar:0.31",
    artifact_sha256 = "abf21a12d26fbed5a1468f7f47699cc70c3f3832a7cc728b402880a3e5911963",
    server_urls = [
        "https://jcenter.bintray.com/",
        "https://repo1.maven.org/maven2",
    ],
)

jvm_maven_import_external(
    name = "com_spotify_hamcrest_optional",
    artifact = "com.spotify:hamcrest-optional:jar:1.1.1",
    artifact_sha256 = "8362a0a818c4fe41563841d3ef9411475e07dd43e65d1b89063eeefa237256ea",
    server_urls = [
        "https://jcenter.bintray.com/",
        "https://repo1.maven.org/maven2",
    ],
)

#### Use remote resources

## java

load("//:bazel_integration_test.bzl", "bazel_java_integration_test_deps")

bazel_java_integration_test_deps()

load("//tools:import.bzl", "bazel_external_dependency_archive")

bazel_external_dependency_archive(
    name = "test_archive",
    srcs = {
        "90a8e1603eeca48e7e879f3afbc9560715322985f39a274f6f6070b43f9d06fe": [
            "https://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar",
        ],
        "e0de160b129b2414087e01fe845609cd55caec6820cfd4d0c90fabcc7bdb8c1e": [
            "https://repo1.maven.org/maven2/com/beust/jcommander/1.72/jcommander-1.72.jar",
        ],
    },
)

bazel_external_dependency_archive(
    name = "test_archive2",
    srcs = {
        "91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff": [
            "https://repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar",
        ],
    },
)

bazel_external_dependency_archive(
    name = "bazel_toolchains_test",
    srcs = {
        "c6159396a571280c71d072a38147d43dcb44f78fc15976d0d47e6d0bf015458d": [
            "https://github.com/bazelbuild/bazel-toolchains/archive/0.26.1.tar.gz",
        ],
    },
)

## Your new language here!
