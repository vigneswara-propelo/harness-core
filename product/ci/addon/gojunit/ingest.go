// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Copyright Josh Komoroske. All rights reserved.
// Use of this source code is governed by the MIT license,
// a copy of which can be found in the LICENSE.txt file.

package junit

import (
	"strconv"
	"strings"
	"time"

	"github.com/harness/ti-client/types"
)

// findSuites performs a depth-first search through the XML document, and
// attempts to ingest any "testsuite" tags that are encountered.
func findSuites(nodes []xmlNode, suites chan Suite, parentFilename, rootSuiteName string) {
	for _, node := range nodes {
		switch node.XMLName.Local {
		case "testsuite":
			suites <- ingestSuite(node, parentFilename)
			if node.Attr("name") == rootSuiteName {
				parentFilename = node.Attr("file")
			}
		default:
			findSuites(node.Nodes, suites, parentFilename, rootSuiteName)
		}
	}
}

func ingestSuite(root xmlNode, parentFilename string) Suite {
	suite := Suite{
		Name:       root.Attr("name"),
		Package:    root.Attr("package"),
		Properties: root.Attrs,
	}

	parentFilename = getFilename(root.Attr("file"), parentFilename)

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "testsuite":
			testsuite := ingestSuite(node, parentFilename)
			suite.Suites = append(suite.Suites, testsuite)
		case "testcase":
			testcase := ingestTestcase(node, parentFilename)
			suite.Tests = append(suite.Tests, testcase)
		case "properties":
			props := ingestProperties(node)
			suite.Properties = props
		case "system-out":
			suite.SystemOut = string(node.Content)
		case "system-err":
			suite.SystemErr = string(node.Content)
		}
	}

	suite.Aggregate()
	return suite
}

func ingestProperties(root xmlNode) map[string]string {
	props := make(map[string]string, len(root.Nodes))

	for _, node := range root.Nodes {
		if node.XMLName.Local == "property" {
			name := node.Attr("name")
			value := node.Attr("value")
			props[name] = value
		}
	}

	return props
}

func ingestTestcase(root xmlNode, parentFilename string) Test {
	test := Test{
		Name:       root.Attr("name"),
		Classname:  root.Attr("classname"),
		Filename:   getFilename(root.Attr("file"), parentFilename),
		DurationMs: duration(root.Attr("time")).Milliseconds(),
		Result:     types.Result{Status: types.StatusPassed},
		Properties: root.Attrs,
	}

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "skipped":
			test.Result.Status = types.StatusSkipped
			test.Result.Message = node.Attr("message")
			test.Result.Desc = string(node.Content)
		case "failure":
			test.Result.Status = types.StatusFailed
			test.Result.Message = node.Attr("message")
			test.Result.Type = node.Attr("type")
			test.Result.Desc = string(node.Content)
		case "error":
			test.Result.Status = types.StatusError
			test.Result.Message = node.Attr("message")
			test.Result.Type = node.Attr("type")
			test.Result.Desc = string(node.Content)
		case "system-out":
			test.SystemOut = string(node.Content)
		case "system-err":
			test.SystemErr = string(node.Content)
		}
	}

	return test
}

func duration(t string) time.Duration {
	// Remove commas for larger durations
	t = strings.ReplaceAll(t, ",", "")

	// Check if there was a valid decimal value
	if s, err := strconv.ParseFloat(t, 64); err == nil {
		return time.Duration(s*1000000) * time.Microsecond
	}

	// Check if there was a valid duration string
	if d, err := time.ParseDuration(t); err == nil {
		return d
	}

	return 0
}

// getFilename returns the filename, using the parent filename if the current filename is empty
func getFilename(currentFilename, parentFilename string) string {
	if currentFilename != "" {
		return currentFilename
	}
	return parentFilename
}
