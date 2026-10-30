package com.todo.ui;

import com.todo.model.Todo;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TodoCell extends ListCell<Todo> {
    private final HBox container = new HBox();
    private final CheckBox completeCheckBox = new CheckBox();
    private final VBox textContainer = new VBox();
    private final HBox titleAndBadges = new HBox();
    private final Label titleLabel = new Label();
    private final Label priorityBadge = new Label();
    private final Label descriptionLabel = new Label();
    private final Label dateLabel = new Label();
    private final HBox actionBox = new HBox();
    private final Button editBtn = new Button("✎ Edit");
    private final Button deleteBtn = new Button("✕ Delete");

    private final BiConsumer<Todo, Boolean> onToggleComplete;
    private final Consumer<Todo> onEdit;
    private final Consumer<Todo> onDelete;

    public TodoCell(BiConsumer<Todo, Boolean> onToggleComplete, Consumer<Todo> onEdit, Consumer<Todo> onDelete) {
        this.onToggleComplete = onToggleComplete;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        container.getStyleClass().add("task-card");
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(14);

        titleLabel.getStyleClass().add("task-title");
        descriptionLabel.getStyleClass().add("task-description");
        dateLabel.getStyleClass().add("task-date");

        titleAndBadges.setAlignment(Pos.CENTER_LEFT);
        titleAndBadges.setSpacing(10);
        titleAndBadges.getChildren().addAll(titleLabel, priorityBadge);

        textContainer.setSpacing(4);
        textContainer.getChildren().addAll(titleAndBadges, descriptionLabel, dateLabel);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        editBtn.getStyleClass().add("btn-action");
        deleteBtn.getStyleClass().add("btn-danger");

        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setSpacing(6);
        actionBox.getChildren().addAll(editBtn, deleteBtn);

        container.getChildren().addAll(completeCheckBox, textContainer, actionBox);

        completeCheckBox.setOnAction(event -> {
            Todo item = getItem();
            if (item != null && onToggleComplete != null) {
                onToggleComplete.accept(item, completeCheckBox.isSelected());
            }
        });

        editBtn.setOnAction(event -> {
            Todo item = getItem();
            if (item != null && onEdit != null) {
                onEdit.accept(item);
            }
        });

        deleteBtn.setOnAction(event -> {
            Todo item = getItem();
            if (item != null && onDelete != null) {
                onDelete.accept(item);
            }
        });
    }

    @Override
    protected void updateItem(Todo todo, boolean empty) {
        super.updateItem(todo, empty);

        if (empty || todo == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        // Set completed state
        completeCheckBox.setSelected(todo.isCompleted());

        // Update card container styles
        container.getStyleClass().removeAll("task-card-completed");
        titleLabel.getStyleClass().removeAll("task-title-completed");
        if (todo.isCompleted()) {
            container.getStyleClass().add("task-card-completed");
            titleLabel.getStyleClass().add("task-title-completed");
        }

        titleLabel.setText(todo.getTitle());

        // Description
        if (todo.getDescription() != null && !todo.getDescription().isBlank()) {
            descriptionLabel.setText(todo.getDescription());
            descriptionLabel.setVisible(true);
            descriptionLabel.setManaged(true);
        } else {
            descriptionLabel.setText("");
            descriptionLabel.setVisible(false);
            descriptionLabel.setManaged(false);
        }

        // Priority Badge
        priorityBadge.getStyleClass().removeAll("badge", "badge-priority-high", "badge-priority-medium", "badge-priority-low");
        priorityBadge.getStyleClass().add("badge");
        String priority = todo.getPriority() != null ? todo.getPriority() : "Medium";
        priorityBadge.setText(priority);
        switch (priority.toLowerCase()) {
            case "high" -> priorityBadge.getStyleClass().add("badge-priority-high");
            case "low" -> priorityBadge.getStyleClass().add("badge-priority-low");
            default -> priorityBadge.getStyleClass().add("badge-priority-medium");
        }

        // Due Date & Created Date
        dateLabel.getStyleClass().removeAll("task-date-overdue");
        StringBuilder dateText = new StringBuilder();
        if (todo.getDueDate() != null && !todo.getDueDate().isBlank()) {
            dateText.append("📅 Due: ").append(todo.getDueDate());
            try {
                LocalDate dueDate = LocalDate.parse(todo.getDueDate());
                if (!todo.isCompleted() && dueDate.isBefore(LocalDate.now())) {
                    dateLabel.getStyleClass().add("task-date-overdue");
                    dateText.append(" (Overdue!)");
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        if (todo.getCreatedAt() != null && !todo.getCreatedAt().isBlank()) {
            if (!dateText.isEmpty()) dateText.append("  •  ");
            dateText.append("Created: ").append(todo.getCreatedAt().substring(0, Math.min(10, todo.getCreatedAt().length())));
        }
        dateLabel.setText(dateText.toString());

        setText(null);
        setGraphic(container);
    }
}
