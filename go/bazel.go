/**
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bazel_go

import (
	"bytes"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"
	"syscall"

	rules_go_bazel "github.com/bazelbuild/rules_go/go/tools/bazel"
)

var _ = fmt.Printf

var BazelVersion = "bazel_version was not set. This is an error."

type TestingBazel struct {
	// Path to the Bazel binary.
	bazel string

	// Path to the testing temp directory.
	tmpDir string
}

// New instance of a TestingBazel will be created and the program will cd into
// the appropriate working dir for that bazel client.
func New() (*TestingBazel, error) {
	// Make a temporary dir in the OS specific tmpdir.
	dir, err := ioutil.TempDir("", "testing")
	if err != nil {
		return nil, errors.New("Unable to make tempdir for bazel")
	}

	err = os.Chdir(dir)
	if err != nil {
		return nil, errors.New("Unable to make tempdir for bazel")
	}

	b := &TestingBazel{
		tmpDir: dir,
	}
	if !b.setAndUnpackBazel(fmt.Sprintf("build_bazel_bazel_%s/bazel", strings.Replace(BazelVersion, ".", "_", -1))) {
		return nil, errors.New("Unable to find Bazel")
	}

	return b, nil
}

func (b *TestingBazel) setAndUnpackBazel(bazel string) bool {
	bazel, err := b.Rlocation(bazel)
	if err != nil {
		return false
	}
	if _, err := os.Stat(bazel); !os.IsNotExist(err) {
		b.bazel = bazel
		// Unpack Bazel by running a command on it.
		b.RunBazel([]string{"help"})
		return true
	}
	return false
}

// ScratchFile creates a new file at the path you provided.
func (b *TestingBazel) ScratchDir(p string) error {
	return os.MkdirAll(p, 0755)
}

// ScratchFile creates a new file at the path you provided. If the file is in a
// directory that may not exist, use ScratchDir to make the directory. To set a
// mode on the file use ScratchFileWithMode.
func (b *TestingBazel) ScratchFile(p, content string) error {
	return b.ScratchFileWithMode(p, content, 0644)
}

// ScratchFile creates a new file at the path you provided with a specified
// mode (useful for setting executable bit). If the file is in a directory that
// may not exist, use ScratchDir to make the directory.
func (b *TestingBazel) ScratchFileWithMode(p, content string, mode os.FileMode) error {
	if err := ioutil.WriteFile(p, []byte(content), mode); err != nil {
		return err
	}
	return nil
}

// GetBazel returns the path to the Bazel binary that is available.
func (b *TestingBazel) GetBazel() string {
	return b.bazel
}

func (b *TestingBazel) RunBazel(args []string) (int, string, string) {
	cmd := exec.Command(b.bazel, args...)

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	if err := cmd.Run(); err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			status := exitErr.Sys().(syscall.WaitStatus)
			return status.ExitStatus(), stdout.String(), stderr.String()
		}
	}

	// When there isn't an error we know it exited successfully
	return 0, stdout.String(), stderr.String()
}

// Rlocation Returns the absolute path to a runfile.
func (b *TestingBazel) Rlocation(runfile string) (string, error) {
	return rules_go_bazel.Runfile(runfile)
}
