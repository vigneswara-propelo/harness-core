/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.MemoryPerformanceUtils.memoryUsage;

import io.harness.data.structure.EmptyPredicate;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
public class LogPerformanceImpl {
  private static final String TERM_ENV_VARIABLE = "TERM";
  private static final String DEFAULT_TERM_ENV_VALUE = "xterm";
  private static final int NOS_OF_TOP_PROCESS_LINES_TO_READ = 15;
  private static final String CPU_PERCENTAGE_KEY = "%cpu(s):";
  private static final String CPU_KEY = "CPU";
  private static final String MEM_KEY = "Mem";
  public Map<String, String> obtainDelegateCpuMemoryPerformance(ImmutableMap.Builder<String, String> builder) {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    builder.put("cpu-process",
        BigDecimal.valueOf(osBean.getProcessCpuLoad() * 100).setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    builder.put("cpu-system",
        BigDecimal.valueOf(osBean.getSystemCpuLoad() * 100).setScale(2, BigDecimal.ROUND_HALF_UP).toString());

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    memoryUsage(builder, "heap-", memoryMXBean.getHeapMemoryUsage());

    memoryUsage(builder, "non-heap-", memoryMXBean.getNonHeapMemoryUsage());

    return builder.build();
  }

  public void logTopCpuMemoryProcesses() {
    // Get the operating system management bean
    java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    // Ensure the operating system supports CPU usage monitoring
    if (osBean.getSystemLoadAverage() == -1) {
      log.info("Operating system does not support CPU usage monitoring.");
      return;
    }

    logTopProcessesByCpuAndMemory();
    log.info("The delegate process ID {}", getCurrentProcessId());
  }

  private static long getCurrentProcessId() {
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    String processName = runtimeBean.getName();
    return Long.parseLong(processName.split("@")[0]);
  }

  private void logTopProcessesByCpuAndMemory() {
    try {
      int totalLinesToRead = 0;

      String termEnv = System.getenv(TERM_ENV_VARIABLE);
      if (StringUtils.isEmpty(termEnv)) {
        termEnv = DEFAULT_TERM_ENV_VALUE;
      }

      // ProcessBuilder is used to spawn a child process to run the given command
      // ProcessBuild allows the process to be killed through manually
      ProcessBuilder cpuProcessBuilder = new ProcessBuilder("top", "-b", "-n", "1");
      cpuProcessBuilder.environment().put(TERM_ENV_VARIABLE, termEnv);

      Process cpuProcess = cpuProcessBuilder.start();

      try (BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuProcess.getInputStream()))) {
        String cpuLine;
        log.info("Top processes in the system :");
        while ((cpuLine = cpuReader.readLine()) != null) {
          log.info(cpuLine);
          totalLinesToRead++;
          if (totalLinesToRead >= NOS_OF_TOP_PROCESS_LINES_TO_READ) {
            // Close the input stream and kill the process.
            cpuProcess.getInputStream().close();
            cpuProcess.destroy();
            break;
          }
        }
      }

      // Ensure that the cpuProcess is terminated
      int exitCode = cpuProcess.waitFor();
      log.info("The process to dump Top processes exited with code {}", exitCode);

    } catch (IOException e) {
      log.error(e.toString());
    } catch (InterruptedException e) {
      log.error(e.toString());
    } catch (Exception e) {
      log.error(e.toString());
    }
  }

  public Map<String, Double> getDelegateMemoryCpuUsage() {
    Map<String, Double> resourceUsuageMap = new HashMap<>();
    try {
      int totalLinesToRead = 0;
      String termEnv = System.getenv(TERM_ENV_VARIABLE);
      if (StringUtils.isEmpty(termEnv)) {
        termEnv = DEFAULT_TERM_ENV_VALUE;
      }
      ProcessBuilder cpuProcessBuilder = new ProcessBuilder("top", "-b", "-n", "1", "-o", "%CPU");
      cpuProcessBuilder.environment().put(TERM_ENV_VARIABLE, termEnv);
      Process process = cpuProcessBuilder.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String[] processInfo = line.split("\\s+");
          // Look for line, %Cpu(s):  0.0 us,  0.0 sy,  0.0 ni,100.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
          if (isNotEmpty(processInfo[0]) && processInfo[0].equalsIgnoreCase(CPU_PERCENTAGE_KEY)) {
            List<String> cpuLine = new ArrayList<>();
            cpuLine.addAll(Arrays.stream(processInfo).filter(EmptyPredicate::isNotEmpty).collect(Collectors.toList()));
            if (isNotEmpty(cpuLine) && isNotEmpty(cpuLine.get(1)) && !resourceUsuageMap.containsKey(CPU_KEY)) {
              resourceUsuageMap.put(CPU_KEY, Double.parseDouble(cpuLine.get(1)));
            }
          }
          // Look for line, MiB Mem : 7818.5 total, 4232.0 free, 1572.2 used, 2014.3 buff/cache
          // OR KiB Mem :  8092456 total,  2345672 free,  3598140 used,  2143644 buff/cache
          if (isNotEmpty(processInfo[0])
              && (processInfo[0].equalsIgnoreCase("MiB") || processInfo[0].equalsIgnoreCase("KiB"))
              && isNotEmpty(processInfo[1]) && processInfo[1].equalsIgnoreCase(MEM_KEY)) {
            double memoryUsed = extractMemoryUsage(line);
            String key = processInfo[0] + MEM_KEY;
            if (!resourceUsuageMap.containsKey(key)) {
              resourceUsuageMap.put(key, memoryUsed);
            }
          }

          totalLinesToRead++;
          if (totalLinesToRead >= NOS_OF_TOP_PROCESS_LINES_TO_READ) {
            // Close the input stream and kill the process.
            process.getInputStream().close();
            process.destroy();
            break;
          }
        }
      }
      process.destroy();
    } catch (IOException e) {
      log.error("IOException occurred: {}", e.toString());
    } catch (Exception ex) {
      log.error("Unhandled exception: {}", ex.toString());
    }
    return resourceUsuageMap;
  }

  private static double extractMemoryUsage(String line) {
    double totalMemory = 0.0;
    double usedMemory = 0.0;
    // Extract Memory usage information from the line
    // Example: MiB Mem : 7818.5 total, 4232.0 free, 1572.2 used, 2014.3 buff/cache
    String[] values = line.trim().split(",");
    for (String v : values) {
      String[] parts = v.split(" ");
      int labelIndex = parts.length - 1;
      if (labelIndex <= 0) {
        continue;
      }
      if (parts[labelIndex].equals("total")) {
        totalMemory = Double.parseDouble(parts[labelIndex - 1]);
      }
      if (parts[labelIndex].equals("used")) {
        usedMemory = Double.parseDouble(parts[labelIndex - 1]);
      }
    }
    return (usedMemory / totalMemory) * 100;
  }
}
