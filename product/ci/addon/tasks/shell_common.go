// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"fmt"
	"runtime"

	pb "github.com/harness/harness-core/product/ci/engine/proto"
)

func isPowershell(shellType pb.ShellType) bool {
	if shellType == pb.ShellType_POWERSHELL || shellType == pb.ShellType_PWSH {
		return true
	}
	return false
}

func isPython(shellType pb.ShellType) bool {
	if shellType == pb.ShellType_PYTHON {
		return true
	}
	return false
}

func getShell(shellType pb.ShellType) (string, string, error) {
	if shellType == pb.ShellType_BASH {
		return "bash", "-c", nil
	} else if shellType == pb.ShellType_SH {
		return "sh", "-c", nil
	} else if shellType == pb.ShellType_POWERSHELL {
		return "powershell", "-Command", nil
	} else if shellType == pb.ShellType_PWSH {
		return "pwsh", "-Command", nil
	} else if shellType == pb.ShellType_PYTHON {
		if runtime.GOOS == "windows" {
			return "python", "-c", nil
		}
		return "python3", "-c", nil
	}
	return "", "", fmt.Errorf("Unknown shell type: %s", shellType)
}
