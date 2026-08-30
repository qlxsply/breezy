package com.corwin.system.task.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing a task definition mapped from an {@code @InternalTask}-annotated method.
 *
 * @author Corwin 2026/3/30
 */
@Entity
@Table(name = "sys_task_definition")
public class TaskDefinition {

  @Id
  @Column(length = 64)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(length = 512)
  private String description;

  @Column(name = "bean_name", nullable = false, length = 128)
  private String beanName;

  @Column(name = "method_name", nullable = false, length = 128)
  private String methodName;

  @Column(name = "is_removed", nullable = false)
  private boolean removed = false;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getBeanName() {
    return beanName;
  }

  public String getMethodName() {
    return methodName;
  }

  public boolean isRemoved() {
    return removed;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Updates the task definition metadata and refreshes the update timestamp.
   *
   * @param name the new task name
   * @param description the new task description
   * @param beanName the new Spring bean name
   * @param methodName the new method name
   */
  public void updateDetails(String name, String description, String beanName, String methodName) {
    this.name = name;
    this.description = description;
    this.beanName = beanName;
    this.methodName = methodName;
    touch();
  }

  /** Marks this task definition as logically removed. */
  public void markRemoved() {
    this.removed = true;
    touch();
  }

  /**
   * Factory method to create a new task definition.
   *
   * @param code the unique task code
   * @param name the task name
   * @param description the task description
   * @param beanName the Spring bean name
   * @param methodName the method name
   * @return a new TaskDefinition instance
   */
  public static TaskDefinition create(
      String code, String name, String description, String beanName, String methodName) {
    TaskDefinition def = new TaskDefinition();
    def.code = code;
    def.name = name;
    def.description = description;
    def.beanName = beanName;
    def.methodName = methodName;
    def.updatedAt = HighDate.mockInstant();
    return def;
  }

  private void touch() {
    this.updatedAt = HighDate.mockInstant();
  }
}
