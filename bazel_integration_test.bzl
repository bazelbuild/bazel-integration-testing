# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Integration test framework for using Bazel
load(
    "//tools:common.bzl",
    BAZEL_VERSIONS_ = "BAZEL_VERSIONS",
)
load(
    "//tools:repositories.bzl",
    bazel_binaries_ = "bazel_binaries",
    bazel_binary_ = "bazel_binary",
)
load(
    "//tools:bazel_java_integration_test.bzl",
    bazel_java_integration_test_ = "bazel_java_integration_test",
    bazel_java_integration_test_deps_ = "bazel_java_integration_test_deps",
)
load(
    "//tools:bazel_py_integration_test.bzl",
    bazel_py_integration_test_ = "bazel_py_integration_test",
)
load(
    "//tools:import.bzl",
    bazel_external_dependency_archive_ = "bazel_external_dependency_archive",
)
load(
    "//go:bazel_integration_test.bzl",
    bazel_go_integration_test_ = "bazel_go_integration_test",
)

print(
    "WARNING: bazel_integration_test.bzl is deprecated and will go away in " +
    "the future, please directly load the bzl file(s) of the module(s) " +
    "needed as it is more efficient.",
)

BAZEL_VERSIONS = BAZEL_VERSIONS_

bazel_binary = bazel_binary_

bazel_binaries = bazel_binaries_

bazel_java_integration_test = bazel_java_integration_test_

bazel_java_integration_test_deps = bazel_java_integration_test_deps_

bazel_py_integration_test = bazel_py_integration_test_

bazel_external_dependency_archive = bazel_external_dependency_archive_

bazel_go_integration_test = bazel_go_integration_test_
