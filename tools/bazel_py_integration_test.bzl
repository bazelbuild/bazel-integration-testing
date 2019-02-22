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

# TODO: This file will be deleted March 22, 2019.
print(
    "WARNING: tools/bazel_py_integration_test.bzl has been moved. Please " +
    "replace your load statement with:\n\n" +
    "load(\"@build_bazel_integration_testing//python:python.bzl\", \"bazel_py_integration_test\")",
)

load(
    "//python:python.bzl",
    bazel_py_integration_test_ = "bazel_py_integration_test",
)

# Reexport the python integration test macros.
bazel_py_integration_test = bazel_py_integration_test_
