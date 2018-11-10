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

load("//tools:bazel_version_dict.bzl", "BAZEL_VERSION_DICT")

BAZEL_VERSIONS = BAZEL_VERSION_DICT.keys()

def _zfill(v, l = 5):
    """zfill a string by padding 0s to the left of the string till it is the link
    specified by l.
    """
    return "0" * (l - len(v)) + v

def _unfill(v, l = 5):
    """unfill takes a zfilled string and returns it to the original value"""
    return [
        int(v[l * i:l * (i + 1)])
        for i in range(len(v) / l)
    ]

def GET_LATEST_BAZEL_VERSIONS(count = 3):
    """GET_LATEST_BAZEL_VERSIONS count and returns a list
    of the latest $count minor versions at their latest patch.
  
    Example:
  
    ["0.1.0", "0.1.1", "0.2.0", "0.3.0"] => ["0.1.1", "0.2.0", "0.3.0"]
  
    Arguments:
      count: The number of versions to return.
    """
    version_tuple_list = []
    for v in BAZEL_VERSIONS:
        version_tuple_list.append("".join([_zfill(x, 5) for x in v.split(".")]))

    already_handled_major_minors = []
    toReturn = []

    # By padding everything with a consistent number of 0s we can sort using
    # string sort and get a list in order.
    for v in reversed(sorted(version_tuple_list)):
        if len(toReturn) >= count:
            break

        major_minor = v[0:10]

        if major_minor in already_handled_major_minors:
            continue

        already_handled_major_minors.append(major_minor)

        toReturn.append(_unfill(v))

    return [".".join(v) for v in toReturn]
