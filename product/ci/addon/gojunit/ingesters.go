// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Copyright Josh Komoroske. All rights reserved.
// Use of this source code is governed by the MIT license,
// a copy of which can be found in the LICENSE.txt file.

package junit

import (
	"bytes"
	"io"
	"os"
)

// IngestFile will parse the given XML file and return a slice of all contained
// JUnit test suite definitions.
func IngestFile(filename, rootSuiteName string) ([]Suite, error) {
	file, err := os.Open(filename)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	return IngestReader(file, rootSuiteName)
}

// IngestReader will parse the given XML reader and return a slice of all
// contained JUnit test suite definitions.
func IngestReader(reader io.Reader, rootSuiteName string) ([]Suite, error) {
	var (
		suiteChan = make(chan Suite)
		suites    = make([]Suite, 0)
	)

	nodes, err := parse(reader)
	if err != nil {
		return nil, err
	}

	go func() {
		findSuites(nodes, suiteChan, "", rootSuiteName)
		close(suiteChan)
	}()

	for suite := range suiteChan {
		suites = append(suites, suite)
	}

	return suites, nil
}

// Ingest will parse the given XML data and return a slice of all contained
// JUnit test suite definitions.
func Ingest(data []byte, rootSuiteName string) ([]Suite, error) {
	return IngestReader(bytes.NewReader(data), rootSuiteName)
}
