# Copied from https://github.com/tensorflow/tensorflow/blob/dfa7d2d2953c3d33d2e8deb898240be008de5be6/tensorflow/workspace.bzl

# Parse the bazel version string from `native.bazel_version`.
def _parse_bazel_version(bazel_version):
  # Remove commit from version.
  version = bazel_version.split(" ", 1)[0]

  # Split into (release, date) parts and only return the release
  # as a tuple of integers.
  parts = version.split('-', 1)

  # Turn "release" into a tuple of strings
  version_tuple = ()
  for number in parts[0].split('.'):
    version_tuple += (str(number),)
  return version_tuple

def check_bazel_version(minimum_bazel_version, maximum_bazel_version=None):
  """Check that a specific bazel version is being used.

  Args:
     minimum_bazel_version: minimum version of Bazel expected
     maximum_bazel_version: maximum version of Bazel expected
  """
  if "bazel_version" not in dir(native):
    fail("\nCurrent Bazel version is lower than 0.2.1, expected at least %s\n" % minimum_bazel_version)
  elif not native.bazel_version:
    print("\nCurrent Bazel is not a release version, cannot check for compatibility.")
    print("Make sure that you are running at least Bazel %s.\n" % minimum_bazel_version)
  else:
    current_bazel_version = _parse_bazel_version(native.bazel_version)
    min_bazel_version = _parse_bazel_version(minimum_bazel_version)
    if min_bazel_version > current_bazel_version:
      fail("\nCurrent Bazel version is {}, expected at least {}\n".format(
          native.bazel_version, minimum_bazel_version))
    if maximum_bazel_version:
      max_bazel_version = _parse_bazel_version(maximum_bazel_version)
      if max_bazel_version < current_bazel_version:
        fail("\nCurrent Bazel version is {}, expected at most {}\n".format(
            native.bazel_version, maximum_bazel_version))

  pass
