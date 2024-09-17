package ceyal;

import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class ProcessFlowAppWithEventLog extends Application {

    private Map<String, Task> taskMap = new HashMap<>();
    private Map<String, Gateway> gatewayMap = new HashMap<>();
    private Pane processPane;
    private VBox performanceDashboard;
    private int availableResources = 5; // Total number of resources
    private Label resourceLabel;
    private XYChart.Series<Number, Number> performanceSeries;

    private List<Task> customFlow = new ArrayList<>(); // For custom task flows
    private Map<String, Integer> taskPrioritization = new HashMap<>(); // Task priority map

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Create the main menu
        MenuBar menuBar = createMainMenu(primaryStage);
        root.setTop(menuBar);

        // Create the task settings panel
        VBox taskSettingsPanel = createTaskSettingsPanel();
        root.setRight(taskSettingsPanel);

        // Create the event log panel
        TextArea eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        root.setBottom(eventLogArea);

        // Set the process pane in the center
        processPane = new Pane();
        root.setCenter(processPane);

        // Create performance dashboard
        performanceDashboard = createPerformanceDashboard();
        root.setLeft(performanceDashboard);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setTitle("Advanced Process Flow with Resource Management and Custom Flow");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMainMenu(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // File menu with Save and Load options
        Menu fileMenu = new Menu("File");
        MenuItem saveProcessItem = new MenuItem("Save Process");
        MenuItem loadProcessItem = new MenuItem("Load Process");
        saveProcessItem.setOnAction(e -> saveProcess(primaryStage));
        loadProcessItem.setOnAction(e -> loadProcess(primaryStage));
        fileMenu.getItems().addAll(saveProcessItem, loadProcessItem);

        // Simulate menu for process simulation
        Menu simulateMenu = new Menu("Simulate");
        MenuItem simulateProcessItem = new MenuItem("Simulate Process");
        simulateProcessItem.setOnAction(e -> simulateProcess(5));  // Simulate 5 instances concurrently
        simulateMenu.getItems().add(simulateProcessItem);

        // Load event log
        MenuItem uploadLogItem = new MenuItem("Upload Event Log");
        uploadLogItem.setOnAction(e -> uploadEventLog(primaryStage));
        fileMenu.getItems().add(uploadLogItem);

        // Scenario Comparison
        Menu scenarioMenu = new Menu("Scenario");
        MenuItem compareScenarioItem = new MenuItem("Compare Scenarios");
        compareScenarioItem.setOnAction(e -> compareScenarios());
        scenarioMenu.getItems().add(compareScenarioItem);

        menuBar.getMenus().addAll(fileMenu, simulateMenu, scenarioMenu);
        return menuBar;
    }

    private VBox createTaskSettingsPanel() {
        VBox taskSettingsPanel = new VBox(10);
        taskSettingsPanel.setPadding(new Insets(10));
        Label taskNameLabel = new Label("Task Name:");
        TextField taskNameField = new TextField();
        Label durationLabel = new Label("Duration:");
        TextField durationField = new TextField();
        Label probabilityLabel = new Label("Probability:");
        TextField probabilityField = new TextField();
        Label priorityLabel = new Label("Priority:");
        TextField priorityField = new TextField();
        Button saveButton = new Button("Save Task");

        saveButton.setOnAction(e -> {
            String taskName = taskNameField.getText();
            int duration = Integer.parseInt(durationField.getText());
            int probability = Integer.parseInt(probabilityField.getText());
            int priority = Integer.parseInt(priorityField.getText());
            createTask(taskName, duration, probability, priority);
        });

        taskSettingsPanel.getChildren().addAll(taskNameLabel, taskNameField, durationLabel, durationField, probabilityLabel, probabilityField, priorityLabel, priorityField, saveButton);
        return taskSettingsPanel;
    }

    private VBox createPerformanceDashboard() {
        VBox dashboard = new VBox(10);
        dashboard.setPadding(new Insets(10));
        Label dashboardLabel = new Label("Performance Dashboard");

        // Performance Chart
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Tasks Completed");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Task Throughput");

        performanceSeries = new XYChart.Series<>();
        lineChart.getData().add(performanceSeries);

        // Resource Availability
        resourceLabel = new Label("Available Resources: " + availableResources);

        dashboard.getChildren().addAll(dashboardLabel, lineChart, resourceLabel);
        return dashboard;
    }

    private void createTask(String name, int duration, int probability, int priority) {
        Task task = new Task(name, duration, probability);
        taskMap.put(name, task);
        processPane.getChildren().add(task);
        taskPrioritization.put(name, priority);

        // Add drag-and-drop functionality for custom flow
        task.setOnDragDetected(event -> {
            Dragboard db = task.startDragAndDrop(TransferMode.ANY);
            ClipboardContent content = new ClipboardContent();
            content.putString(task.getName());
            db.setContent(content);
            event.consume();
        });

        task.setOnDragOver(event -> {
            if (event.getGestureSource() != task && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        task.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String taskName = db.getString();
                if (!taskName.equals(task.getName())) {
                    connectTasks(taskMap.get(taskName), task);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void connectTasks(Task sourceTask, Task targetTask) {
        Line connectionLine = new Line(
                sourceTask.getTranslateX() + sourceTask.getWidth(),
                sourceTask.getTranslateY() + sourceTask.getHeight() / 2,
                targetTask.getTranslateX(),
                targetTask.getTranslateY() + targetTask.getHeight() / 2
        );
        processPane.getChildren().add(connectionLine);
        customFlow.add(sourceTask);
        customFlow.add(targetTask);
    }

    private void simulateProcess(int numInstances) {
        if (customFlow.isEmpty()) {
            showAlert("Simulation Error", "No tasks available for simulation. Please create and connect tasks.");
            return;
        }

        // Simulate multiple process instances concurrently
        for (int i = 0; i < numInstances; i++) {
            Circle token = new Circle(10, Color.RED);
            processPane.getChildren().add(token);

            // Sort tasks by priority if needed
            customFlow.sort(Comparator.comparingInt(t -> taskPrioritization.get(t.getName())));

            // Check if customFlow has elements
            Task firstTask = customFlow.get(0); // Assuming first task in custom flow is the start
            if (firstTask != null) {
                moveTokenToTask(token, firstTask);
            }
        }
    }


    private void moveTokenToTask(Circle token, Task task) {
        if (availableResources > 0) {
            availableResources--;
            updateResourceLabel();

            PathTransition transition = new PathTransition();
            transition.setNode(token);
            transition.setPath(new Line(token.getTranslateX(), token.getTranslateY(), task.getTranslateX(), task.getTranslateY()));
            transition.setDuration(Duration.seconds(task.getDuration()));
            transition.setOnFinished(e -> {
                // Continue to next task after the transition
                availableResources++; // Release resource
                updateResourceLabel();

                String nextTaskName = getNextTaskName(task.getName());
                if (nextTaskName != null) {
                    Task nextTask = taskMap.get(nextTaskName);
                    if (nextTask != null) {
                        moveTokenToTask(token, nextTask);
                    }
                }
                updatePerformanceMetrics(task);
            });
            transition.play();
        } else {
            System.out.println("No available resources for task: " + task.getName());
        }
    }

    private String getNextTaskName(String currentTaskName) {
        int currentIndex = customFlow.indexOf(taskMap.get(currentTaskName));
        return currentIndex + 1 < customFlow.size() ? customFlow.get(currentIndex + 1).getName() : null;
    }

    private void updatePerformanceMetrics(Task task) {
        // Update task performance in the dashboard
        performanceSeries.getData().add(new XYChart.Data<>(System.currentTimeMillis() / 1000, customFlow.indexOf(task) + 1));
        System.out.println("Task " + task.getName() + " completed.");
    }

    private void updateResourceLabel() {
        resourceLabel.setText("Available Resources: " + availableResources);
    }

    private void compareScenarios() {
        System.out.println("Comparing scenarios..."); // Placeholder for scenario comparison logic
        // Implement scenario saving/loading and performance comparison logic here
    }

    private void uploadEventLog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Event Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            parseEventLog(file);
        }
    }

    private void parseEventLog(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Example line format: CaseID, TaskName, Duration, Probability, Priority
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    String taskName = parts[1].trim();
                    int duration = Integer.parseInt(parts[2].trim());
                    int probability = Integer.parseInt(parts[3].trim());
                    int priority = Integer.parseInt(parts[4].trim());
                    createTask(taskName, duration, probability, priority);
                }
            }
        } catch (IOException | NumberFormatException e) {
            showAlert("Error", "Error parsing event log.");
        }
    }

    private void saveProcess(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Process");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                for (Task task : taskMap.values()) {
                    writer.println(task.getName() + "," + task.getDuration() + "," + task.getProbability() + "," + taskPrioritization.get(task.getName()));
                }
            } catch (IOException e) {
                showAlert("Error", "Failed to save the process.");
            }
        }
    }

    private void loadProcess(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Process");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            taskMap.clear();
            processPane.getChildren().clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 4) {
                        String taskName = parts[0].trim();
                        int duration = Integer.parseInt(parts[1].trim());
                        int probability = Integer.parseInt(parts[2].trim());
                        int priority = Integer.parseInt(parts[3].trim());
                        createTask(taskName, duration, probability, priority);
                    }
                }
            } catch (IOException e) {
                showAlert("Error", "Failed to load the process.");
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private class Task extends Rectangle {
        private StringProperty nameProperty = new SimpleStringProperty();
        private int duration;
        private int probability;

        public Task(String name, int duration, int probability) {
            super(100, 50);
            setName(name);
            this.duration = duration;
            this.probability = probability;
            setFill(Color.LIGHTBLUE);
            setStroke(Color.BLACK);
            setStrokeWidth(2);
            setTranslateX(50); // Set initial position
            setTranslateY(50 + (taskMap.size() - 1) * 60); // Stack tasks vertically
            setOnMouseClicked(event -> showTaskDetails());
        }

        public String getName() {
            return nameProperty.get();
        }

        public void setName(String name) {
            nameProperty.set(name);
        }

        public StringProperty nameProperty() {
            return nameProperty;
        }

        public int getDuration() {
            return duration;
        }

        public int getProbability() {
            return probability;
        }

        private void showTaskDetails() {
            showAlert("Task Details", "Name: " + getName() + "\nDuration: " + duration + "\nProbability: " + probability);
        }
    }

    private class Gateway extends Polyline {
        private String type;

        public Gateway(String type) {
            super(0, 0, 50, 100, 100, 0); // Diamond shape for BPMN-style gateway
            this.type = type;
            setFill(Color.LIGHTGREEN);
            setStroke(Color.BLACK);
            setStrokeWidth(2);
        }

        public String getType() {
            return type;
        }
    }
}
