// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/utils"
	pb "github.com/harness/harness-core/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	taskFileEnv = "TASK_DATA_PATH"
)

// ExecuteTask represents interface to execute a CD task
type ExecuteTask interface {
	Run(ctx context.Context) (map[string]string, error)
}

type executeTask struct {
	id                string
	shellType         pb.ShellType
	envVarOutputs     []string
	taskParams        []byte
	command           string
	logMetrics        bool
	log               *zap.SugaredLogger
	cmdContextFactory exec.CmdContextFactory
	addonLogger       *zap.SugaredLogger
	procWriter        io.Writer
	numRetries        int32
	fs                filesystem.FileSystem
	tmpFilePath       string
}

// NewExecuteStep creates a execute step executor
func NewExecuteStep(step *pb.UnitStep, tmpFilePath string, log *zap.SugaredLogger,
	w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) ExecuteTask {
	e := step.GetExecuteTask()
	fs := filesystem.NewOSFileSystem(log)

	return &executeTask{
		id:                step.GetId(),
		taskParams:        e.GetTaskParameters(),
		command:           e.GetExecuteCommand(),
		logMetrics:        logMetrics,
		log:               log,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		addonLogger:       addonLogger,
		procWriter:        w,
		numRetries:        1,
		fs:                fs,
		tmpFilePath:       tmpFilePath,
		envVarOutputs:     e.GetEnvVarOutputs(),
		shellType:         e.GetShellType(),
	}
}

// Run method
func (e *executeTask) Run(ctx context.Context) (map[string]string, error) {
	start := time.Now()
	e.numRetries = 1
	// 1. Write the task parameters to the path if the parameters are not empty
	e.addonLogger.Infow(" Run method for execute task")
	if len(e.taskParams) > 0 {
		if taskfile, ok := os.LookupEnv(taskFileEnv); !ok {
			err := fmt.Errorf("task file path not set.")
			e.log.Warnw(err.Error(), "env", taskFileEnv)
			return nil, err
		} else {
			err := ioutil.WriteFile(taskfile, e.taskParams, 0644)
			if err != nil {
				e.log.Errorw("unable to write task parameters to file")
				return nil, err
			}
		}
	} else {
		e.log.Warnw("Task data input size is 0", "id", e.id)
	}

	e.addonLogger.Infow("Task params written, executing task command now", e.command[0])

	// 2. Append entrypoint and output collection command to the original command
	outputFile := filepath.Join(e.tmpFilePath, fmt.Sprintf("%s%s", e.id, outputEnvSuffix))
	outputVarCmd := getOutputVarCmd(e.envVarOutputs, outputFile, isPowershell(e.shellType), isPython(e.shellType))
	// TODO: add expression evaluation support for secrets
	executeCmd := fmt.Sprintf("%s%s", e.command, outputVarCmd)

	// TODO: get default entrypoint if no command provided.
	cmdArgs, err := e.getEntrypoint(ctx, executeCmd)
	if err != nil {
		return nil, err
	}

	// 3. Run the task script
	cmd := e.cmdContextFactory.CmdContextWithSleep(ctx, time.Duration(0), cmdArgs[0], cmdArgs[1:]...).
		WithStdout(e.procWriter).WithStderr(e.procWriter)
	err = runCmd(ctx, cmd, e.id, cmdArgs, e.numRetries, start, e.logMetrics, e.addonLogger)

	stepOutput := make(map[string]string)
	if len(e.envVarOutputs) != 0 {
		var err error
		outputVars, err := fetchOutputVariables(outputFile, e.fs, e.log)
		if err != nil {
			e.log.Errorw(
				"error encountered while fetching output of run step",
				"arguments", executeCmd,
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(err),
			)
			return nil, err
		}

		stepOutput = outputVars
	}

	e.addonLogger.Infow(
		"Successfully executed run step",
		"arguments", cmdArgs,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)

	if err != nil {
		e.log.Errorw("unable to run the task script", zap.Error(err))
		return stepOutput, err
	}

	return stepOutput, nil
}

func (e *executeTask) getEntrypoint(ctx context.Context, cmdToExecute string) ([]string, error) {
	// give priority to entrypoint
	if len(cmdToExecute) != 0 {
		shell, ep, err := getShell(e.shellType)
		if err != nil {
			return nil, err
		}
		return []string{shell, ep, cmdToExecute}, nil
	}
	return nil, fmt.Errorf("Command to execute is empty, step id: %s", e.id)
}
