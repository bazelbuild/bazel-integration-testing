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

print(
    "WARNING: tools/import.bzl has been moved. Please replace your load " +
    "statement with:\n\n" +
    "load(\"@build_bazel_integration_testing//import.bzl\", \"bazel_external_dependency_archive\")",
)

load(
    "//:import.bzl",
    bazel_external_dependency_archive_ = "bazel_external_dependency_archive",
)

bazel_external_dependency_archive = bazel_external_dependency_archive_
