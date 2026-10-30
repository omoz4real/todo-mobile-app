package com.todo.ui;

import com.todo.model.Todo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class TaskDialog {

    public static Optional<Todo> show(Todo existingTodo) {
        boolean isEdit = existingTodo != null;
        Dialog<Todo> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Task" : "Create New Task");
        dialog.setHeaderText(isEdit ? "Update your task details" : "Add a new task to your list");

        // Set button types
        ButtonType saveButtonType = new ButtonType(isEdit ? "Save Changes" : "Create Task", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Finish quarterly report");
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Add extra details or notes (optional)...");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.setStyle("-fx-control-inner-background: #1e293b; -fx-text-fill: #f8fafc; -fx-prompt-text-fill: #64748b;");
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Medium", "High");
        priorityBox.setValue("Medium");
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        priorityBox.getStyleClass().add("custom-combo-box");
        priorityBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #f8fafc; -fx-background-color: #1e293b; -fx-padding: 8 12;");
            }
        });
        priorityBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #f8fafc; -fx-background-color: transparent;");
            }
        });
        GridPane.setHgrow(priorityBox, Priority.ALWAYS);

        DatePicker dueDatePicker = new DatePicker();
        dueDatePicker.setPromptText("Select due date");
        dueDatePicker.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(dueDatePicker, Priority.ALWAYS);

        // Prefill if editing
        if (isEdit) {
            titleField.setText(existingTodo.getTitle());
            descriptionArea.setText(existingTodo.getDescription() != null ? existingTodo.getDescription() : "");
            if (existingTodo.getPriority() != null) {
                priorityBox.setValue(existingTodo.getPriority());
            }
            if (existingTodo.getDueDate() != null && !existingTodo.getDueDate().isBlank()) {
                try {
                    dueDatePicker.setValue(LocalDate.parse(existingTodo.getDueDate()));
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        grid.add(new Label("Title: *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priorityBox, 1, 2);
        grid.add(new Label("Due Date:"), 0, 3);
        grid.add(dueDatePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Apply styles if present
        try {
            dialog.getDialogPane().getStylesheets().add(
                TaskDialog.class.getResource("/com/todo/styles.css").toExternalForm()
            );
        } catch (Exception ignored) {
        }

        // Disable save button if title is empty
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveBtn.setDisable(isEdit ? titleField.getText().trim().isEmpty() : true);
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            saveBtn.setDisable(newValue.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText().trim();
                String desc = descriptionArea.getText().trim();
                String priority = priorityBox.getValue();
                String dueDate = dueDatePicker.getValue() != null ? dueDatePicker.getValue().toString() : "";

                if (isEdit) {
                    existingTodo.setTitle(title);
                    existingTodo.setDescription(desc);
                    existingTodo.setPriority(priority);
                    existingTodo.setDueDate(dueDate);
                    return existingTodo;
                } else {
                    return new Todo(0, title, desc, priority, dueDate, false, null);
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
