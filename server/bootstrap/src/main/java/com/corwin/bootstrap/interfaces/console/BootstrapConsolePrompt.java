package com.corwin.bootstrap.interfaces.console;

import java.util.Scanner;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/4/28
 */
@Component
public class BootstrapConsolePrompt {

  private final Scanner scanner = new Scanner(System.in);

  public synchronized boolean confirm(String prompt, boolean defaultValue) {
    String suffix = defaultValue ? " [Y/n]: " : " [y/N]: ";
    System.out.print(prompt + suffix);
    String value = scanner.nextLine();
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    String normalized = value.trim().toLowerCase();
    return "y".equals(normalized) || "yes".equals(normalized);
  }
}
