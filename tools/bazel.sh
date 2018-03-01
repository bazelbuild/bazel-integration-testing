#!/bin/sh -e

# Get the path to the install base extracted during repository fetching.
MYSELF="$0"
while [ -h "${MYSELF}" ]; do
  MYSELF="$(readlink "${MYSELF}")"
done
BASEDIR="$(dirname "${MYSELF}")"

# Create a new install base symlinked to the one created during repository
# fetching. This way, Bazel can set the timestamp on this install base but
# we do not have to extact Bazel itself.
INSTALL_BASE="${TEST_TMPDIR:-${TMP:/tmp}}/bazel_install_base"
if [ ! -d "${INSTALL_BASE}" ]; then
  mkdir -p "${INSTALL_BASE}"
  for f in "${BASEDIR}/install_base"/*; do
    ln -s "$f" "${INSTALL_BASE}/$(basename "$f")"
  done
fi

export BAZEL_REAL="${BASEDIR}/bin/bazel-real"


WORKSPACE_DIR="${PWD}"
while [[ "${WORKSPACE_DIR}" != / ]]; do
    if [[ -e "${WORKSPACE_DIR}/WORKSPACE" ]]; then
      break;
    fi
    WORKSPACE_DIR="$(dirname "${WORKSPACE_DIR}")"
done
readonly WORKSPACE_DIR


if [[ -e "${WORKSPACE_DIR}/WORKSPACE" ]]; then
  readonly WRAPPER="${WORKSPACE_DIR}/tools/bazel"

  if [[ -x "${WRAPPER}" ]]; then
  	export INSTALL_BASE="${INSTALL_BASE}"
    "${WRAPPER}" "$@"
  else
  	"${BASEDIR}/bin/bazel-real" --install_base="${INSTALL_BASE}" "$@"
  fi

fi
