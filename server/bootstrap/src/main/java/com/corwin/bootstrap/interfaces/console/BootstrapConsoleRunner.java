package com.corwin.bootstrap.interfaces.console;

import com.corwin.bootstrap.application.BootstrapTaskKey;
import com.corwin.bootstrap.application.BootstrapTaskReport;
import com.corwin.bootstrap.application.service.BootstrapOrchestrator;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/5/5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapConsoleRunner implements ApplicationRunner {

  private static final String ALL_COMMAND = "all";

  private final BootstrapOrchestrator orchestrator;
  private final ConfigurableApplicationContext context;

  private final Scanner scanner = new Scanner(System.in);

  @Override
  public void run(ApplicationArguments args) {
    int exitCode = 0;
    try {
      exitCode = runBootstrap(args);
    } catch (Exception ex) {
      exitCode = 1;
      log.error("bootstrap execution failed", ex);
      System.err.println("Bootstrap execution failed: " + ex.getMessage());
    } finally {
      context.close();
    }
    System.exit(exitCode);
  }

  private int runBootstrap(ApplicationArguments args) {
    boolean dryRun = args.containsOption("bootstrap.dry-run");
    List<BootstrapTaskKey> selectedTasks = resolveTasks(args);
    List<BootstrapTaskReport> reports = new ArrayList<>();
    for (BootstrapTaskKey task : selectedTasks) {
      BootstrapTaskReport report = orchestrator.runTask(task, dryRun);
      reports.add(report);
      if (!report.success()) {
        break;
      }
    }
    printReports(reports);
    return reports.stream().allMatch(BootstrapTaskReport::success) ? 0 : 1;
  }

  private List<BootstrapTaskKey> resolveTasks(ApplicationArguments args) {
    if (args.containsOption("bootstrap.run")) {
      return resolveTasksFromArgs(args);
    }
    return resolveTasksFromConsole();
  }

  private List<BootstrapTaskKey> resolveTasksFromArgs(ApplicationArguments args) {
    List<String> values = args.getOptionValues("bootstrap.run");
    if (values == null
        || values.isEmpty()
        || values.getFirst() == null
        || values.getFirst().isBlank()) {
      return List.of(BootstrapTaskKey.values());
    }

    String command = values.getFirst().trim().toLowerCase();
    if (ALL_COMMAND.equals(command)) {
      return List.of(BootstrapTaskKey.values());
    }

    BootstrapTaskKey task = BootstrapTaskKey.fromCliValue(command);
    if (task == null) {
      throw new IllegalArgumentException(
          "Unsupported bootstrap command: "
              + command
              + ". Supported: schema, dict, api, resource, users, files, all");
    }
    return List.of(task);
  }

  private List<BootstrapTaskKey> resolveTasksFromConsole() {
    BootstrapTaskKey[] tasks = BootstrapTaskKey.values();

    System.out.println();
    System.out.println("请选择要执行的初始化项：");
    for (int index = 0; index < tasks.length; index++) {
      System.out.println(
          (index + 1) + ". " + displayName(tasks[index]) + " (" + tasks[index].cliValue() + ")");
    }

    int allIndex = tasks.length + 1;
    System.out.println(allIndex + ". 全部");
    System.out.println();
    System.out.print("请输入序号，多个序号使用英文逗号分隔，执行顺序以输入顺序为准，例如 1,3,2：");

    String input = scanner.nextLine();
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("未选择任何初始化项");
    }

    String[] parts = input.split(",");
    List<BootstrapTaskKey> selectedTasks = new ArrayList<>();
    for (String part : parts) {
      String value = part.trim();
      if (value.isBlank()) {
        continue;
      }

      int index;
      try {
        index = Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("非法序号：" + value);
      }

      if (index == allIndex) {
        if (parts.length > 1) {
          throw new IllegalArgumentException("“全部”选项不能和其他选项同时选择");
        }
        return List.of(tasks);
      }
      if (index < 1 || index > tasks.length) {
        throw new IllegalArgumentException("序号超出范围：" + index);
      }
      selectedTasks.add(tasks[index - 1]);
    }

    if (selectedTasks.isEmpty()) {
      throw new IllegalArgumentException("未选择任何初始化项");
    }
    return selectedTasks;
  }

  private String displayName(BootstrapTaskKey task) {
    return switch (task) {
      case SCHEMA_SYNC -> "数据库结构同步";
      case API_SYNC -> "API 同步";
      case RESOURCE_SYNC -> "资源同步";
      case DICTIONARY_SYNC -> "字典同步";
      case DEFAULT_USER_SYNC -> "默认用户同步";
      case SYSTEM_FILE_SYNC -> "系统文件同步";
    };
  }

  private void printReports(List<BootstrapTaskReport> reports) {
    for (BootstrapTaskReport report : reports) {
      System.out.println();
      System.out.println(
          "["
              + report.task().name()
              + "] "
              + (report.success() ? "SUCCESS" : "FAILED")
              + (report.dryRun() ? " (dry-run)" : ""));
      System.out.println("durationMs: " + report.durationMs());
      System.out.println("message: " + report.message());
    }
  }
}
