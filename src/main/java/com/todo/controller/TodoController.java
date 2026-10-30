package com.todo.controller;

import com.todo.dao.TodoDao;
import com.todo.model.Todo;
import com.todo.ui.TaskDialog;
import com.todo.ui.TodoCell;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Optional;

public class TodoController {

    @FXML private Label totalTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label progressPercentLabel;
    @FXML private ProgressBar completionProgressBar;

    @FXML private TextField searchField;
    @FXML private ToggleButton filterAllBtn;
    @FXML private ToggleButton filterActiveBtn;
    @FXML private ToggleButton filterCompletedBtn;
    @FXML private ComboBox<String> priorityFilterBox;
    @FXML private ComboBox<String> sortBox;

    @FXML private ListView<Todo> todoListView;
    @FXML private VBox emptyStateBox;

    private final TodoDao todoDao = new TodoDao();
    private final ObservableList<Todo> masterTodoList = FXCollections.observableArrayList();
    private FilteredList<Todo> filteredTodoList;
    private SortedList<Todo> sortedTodoList;
    private ToggleGroup statusToggleGroup;

    @FXML
    public void initialize() {
        // Status toggle group
        statusToggleGroup = new ToggleGroup();
        filterAllBtn.setToggleGroup(statusToggleGroup);
        filterActiveBtn.setToggleGroup(statusToggleGroup);
        filterCompletedBtn.setToggleGroup(statusToggleGroup);

        // Prevent unselecting all toggles
        statusToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                applyFilters();
            }
        });

        // Priority filter options
        priorityFilterBox.setItems(FXCollections.observableArrayList(
            "All Priorities", "High", "Medium", "Low"
        ));
        priorityFilterBox.setValue("All Priorities");
        setupComboBoxStyling(priorityFilterBox);
        priorityFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Sort options
        sortBox.setItems(FXCollections.observableArrayList(
            "Default", "Due Date", "Priority", "Title"
        ));
        sortBox.setValue("Default");
        setupComboBoxStyling(sortBox);
        sortBox.valueProperty().addListener((obs, oldVal, newVal) -> updateSortComparator());

        // Setup custom cell factory
        todoListView.setCellFactory(param -> new TodoCell(
            this::handleToggleComplete,
            this::handleEditTask,
            this::handleDeleteTask
        ));

        // Filtered & Sorted lists
        filteredTodoList = new FilteredList<>(masterTodoList, p -> true);
        sortedTodoList = new SortedList<>(filteredTodoList);
        updateSortComparator();
        todoListView.setItems(sortedTodoList);

        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Empty state visibility
        emptyStateBox.visibleProperty().bind(Bindings.isEmpty(filteredTodoList));

        // Load data from DB
        loadTodosFromDatabase();
    }

    private void loadTodosFromDatabase() {
        masterTodoList.setAll(todoDao.getAllTodos());
        updateStats();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String priorityFilter = priorityFilterBox.getValue();
        Toggle selectedStatus = statusToggleGroup.getSelectedToggle();

        filteredTodoList.setPredicate(todo -> {
            if (todo == null) return false;

            // 1. Status Filter
            if (selectedStatus == filterActiveBtn && todo.isCompleted()) {
                return false;
            }
            if (selectedStatus == filterCompletedBtn && !todo.isCompleted()) {
                return false;
            }

            // 2. Priority Filter
            if (priorityFilter != null && !priorityFilter.equals("All Priorities")) {
                if (todo.getPriority() == null || !todo.getPriority().equalsIgnoreCase(priorityFilter)) {
                    return false;
                }
            }

            // 3. Search Filter
            if (!searchText.isEmpty()) {
                boolean matchesTitle = todo.getTitle() != null && todo.getTitle().toLowerCase().contains(searchText);
                boolean matchesDesc = todo.getDescription() != null && todo.getDescription().toLowerCase().contains(searchText);
                return matchesTitle || matchesDesc;
            }

            return true;
        });
    }

    private void updateSortComparator() {
        String sortType = sortBox.getValue() != null ? sortBox.getValue() : "Default";

        Comparator<Todo> comparator = switch (sortType) {
            case "Due Date" -> Comparator.comparing((Todo t) -> {
                if (t.getDueDate() == null || t.getDueDate().isBlank()) return "9999-99-99";
                return t.getDueDate();
            });
            case "Priority" -> Comparator.comparingInt(t -> {
                String p = t.getPriority() != null ? t.getPriority().toLowerCase() : "";
                return switch (p) {
                    case "high" -> 1;
                    case "medium" -> 2;
                    case "low" -> 3;
                    default -> 4;
                };
            });
            case "Title" -> Comparator.comparing(t -> t.getTitle() != null ? t.getTitle().toLowerCase() : "");
            default -> Comparator.comparing(Todo::isCompleted)
                .thenComparing((Todo t) -> {
                    if (t.getDueDate() == null || t.getDueDate().isBlank()) return "9999-99-99";
                    return t.getDueDate();
                })
                .thenComparing(Comparator.comparingInt(Todo::getId).reversed());
        };

        sortedTodoList.setComparator(comparator);
    }

    private void updateStats() {
        int total = masterTodoList.size();
        int completed = (int) masterTodoList.stream().filter(Todo::isCompleted).count();
        int pending = total - completed;
        double progress = total > 0 ? (double) completed / total : 0.0;

        totalTasksLabel.setText(String.valueOf(total));
        pendingTasksLabel.setText(String.valueOf(pending));
        completedTasksLabel.setText(String.valueOf(completed));
        completionProgressBar.setProgress(progress);
        progressPercentLabel.setText((int) Math.round(progress * 100) + "%");
    }

    @FXML
    private void handleNewTask() {
        Optional<Todo> result = TaskDialog.show(null);
        result.ifPresent(newTodo -> {
            Todo inserted = todoDao.insert(newTodo);
            if (inserted != null) {
                masterTodoList.add(0, inserted);
                updateStats();
                applyFilters();
            }
        });
    }

    private void handleEditTask(Todo todo) {
        Optional<Todo> result = TaskDialog.show(todo);
        result.ifPresent(updatedTodo -> {
            boolean success = todoDao.update(updatedTodo);
            if (success) {
                // Refresh list item
                int index = masterTodoList.indexOf(todo);
                if (index >= 0) {
                    masterTodoList.set(index, updatedTodo);
                }
                updateStats();
                applyFilters();
            }
        });
    }

    private void handleDeleteTask(Todo todo) {
        Alert confirmAlert = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete \"" + todo.getTitle() + "\"?",
            ButtonType.YES, ButtonType.NO
        );
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText(null);

        Optional<ButtonType> response = confirmAlert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.YES) {
            boolean success = todoDao.delete(todo.getId());
            if (success) {
                masterTodoList.remove(todo);
                updateStats();
                applyFilters();
            }
        }
    }

    private void handleToggleComplete(Todo todo, boolean isCompleted) {
        todo.setCompleted(isCompleted);
        todoDao.toggleCompleted(todo.getId(), isCompleted);

        // Trigger updates in list and stats
        int index = masterTodoList.indexOf(todo);
        if (index >= 0) {
            masterTodoList.set(index, todo);
        }
        updateStats();
        applyFilters();
    }

    private void setupComboBoxStyling(ComboBox<String> comboBox) {
        comboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #f8fafc; -fx-background-color: #1e293b; -fx-padding: 8 12;");
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #f8fafc; -fx-background-color: transparent;");
            }
        });
    }
}
