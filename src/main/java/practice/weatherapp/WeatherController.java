package practice.weatherapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class WeatherController {

    private static final String API_KEY = "b258f6e31ef04dd4a4581201252008";
    private static final String BASE_URL = "http://api.weatherapi.com/v1";

    private HttpClient httpClient = HttpClient.newHttpClient();
    private Gson gson = new Gson();

    // FXML injected UI components
    @FXML
    private TextField cityInput;
    @FXML
    private Button searchButton;
    @FXML
    private Label cityLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label currentTempLabel;
    @FXML
    private Label feelsLikeLabel;
    @FXML
    private Label humidityLabel;
    @FXML
    private Label cloudCoverageLabel;
    @FXML
    private Label precipitationLabel;
    @FXML
    private Label aqiLabel;
    @FXML
    private VBox forecastContainer;
    @FXML
    private VBox historyContainer;
    @FXML
    private ProgressIndicator loadingIndicator;

    public void initialize() {
        // Set up event handlers
        searchButton.setOnAction(e -> handleSearch());
        cityInput.setOnAction(e -> handleSearch());

        // Load default city weather
        loadWeatherData("Dhaka");
    }

    @FXML
    private void handleSearch() {
        String city = cityInput.getText().trim();
        if (!city.isEmpty()) {
            loadWeatherData(city);
        }
    }

    private void loadWeatherData(String city) {
        loadingIndicator.setVisible(true);

        // Clear previous history data
        historyContainer.getChildren().clear();

        // Load current weather + forecast
        CompletableFuture<Void> currentAndForecast = loadCurrentWeatherAndForecast(city);

        // Load history
        CompletableFuture<Void> history = loadWeatherHistory(city);

        CompletableFuture.allOf(currentAndForecast, history)
                .thenRun(() -> Platform.runLater(() -> loadingIndicator.setVisible(false)))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        showError("Failed to load weather data: " + throwable.getMessage());
                    });
                    return null;
                });
        forecastContainer.getChildren().clear();
        historyContainer.getChildren().clear();
    }

    private CompletableFuture<Void> loadCurrentWeatherAndForecast(String city) {
        String url = BASE_URL + "/forecast.json?key=" + API_KEY + "&q=" + city + "&days=3&aqi=yes";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject data = gson.fromJson(response.body(), JsonObject.class);
                        Platform.runLater(() -> updateCurrentWeatherUI(data));
                        Platform.runLater(() -> updateForecastUI(data));
                    } else {
                        Platform.runLater(() -> showError("City not found or API error"));
                    }
                });
    }

    private CompletableFuture<Void> loadWeatherHistory(String city) {
        // Load last 7 days
        CompletableFuture<Void>[] historyTasks = new CompletableFuture[7];

        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url = BASE_URL + "/history.json?key=" + API_KEY + "&q=" + city + "&dt=" + dateStr;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            final int dayIndex = i;
            historyTasks[i-1] = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            JsonObject data = gson.fromJson(response.body(), JsonObject.class);
                            Platform.runLater(() -> addHistoryDay(data, dayIndex));
                        }
                    });
        }

        return CompletableFuture.allOf(historyTasks)
                .thenRun(() -> Platform.runLater(this::sortHistoryByDate));
    }

    private void updateCurrentWeatherUI(JsonObject data) {
        JsonObject location = data.getAsJsonObject("location");
        JsonObject current = data.getAsJsonObject("current");
        JsonObject airQuality = current.has("air_quality") ? current.getAsJsonObject("air_quality") : null;

        cityLabel.setText("City: " + location.get("name").getAsString() + ", " + location.get("country").getAsString());
        timeLabel.setText("Local Time: " + location.get("localtime").getAsString());
        currentTempLabel.setText("Temperature: " + current.get("temp_c").getAsString() + "°C");
        feelsLikeLabel.setText("Feels like: " + current.get("feelslike_c").getAsString() + "°C");
        humidityLabel.setText("Humidity: " + current.get("humidity").getAsString() + "%");
        cloudCoverageLabel.setText("Cloud coverage: " + current.get("cloud").getAsString() + "%");
        precipitationLabel.setText("Precipitation: " + current.get("precip_mm").getAsString() + " mm");

        if (airQuality != null && airQuality.has("us-epa-index")) {
            int aqiValue = airQuality.get("us-epa-index").getAsInt();
            String aqiText = getAQIText(aqiValue);
            aqiLabel.setText("Air Quality Index: " + aqiValue + " (" + aqiText + ")");
        } else {
            aqiLabel.setText("Air Quality Index: Not available");
        }
    }

    private void updateForecastUI(JsonObject data) {
        forecastContainer.getChildren().clear();

        JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");

        for (JsonElement dayElement : forecastDays) {
            JsonObject day = dayElement.getAsJsonObject();
            JsonObject dayData = day.getAsJsonObject("day");

            HBox dayRow = new HBox(10);
            dayRow.setPadding(new Insets(5));
            dayRow.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 5;");
            dayRow.setAlignment(Pos.CENTER_LEFT);

            Label dateLabel = new Label(day.get("date").getAsString());
            dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            dateLabel.setPrefWidth(80);

            Label tempLabel = new Label(dayData.get("maxtemp_c").getAsString() + "°/" +
                    dayData.get("mintemp_c").getAsString() + "°C");
            tempLabel.setFont(Font.font("Arial", 12));
            tempLabel.setPrefWidth(80);

            Label conditionLabel = new Label(dayData.getAsJsonObject("condition").get("text").getAsString());
            conditionLabel.setFont(Font.font("Arial", 12));

            dayRow.getChildren().addAll(dateLabel, tempLabel, conditionLabel);
            forecastContainer.getChildren().add(dayRow);
        }
    }

    private void addHistoryDay(JsonObject data, int daysAgo) {
        JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");

        if (forecastDays.size() > 0) {
            JsonObject day = forecastDays.get(0).getAsJsonObject();
            JsonObject dayData = day.getAsJsonObject("day");
            String dateStr = day.get("date").getAsString();

            // Check if this date is already present → skip
            boolean exists = historyContainer.getChildren().stream()
                    .anyMatch(node -> {
                        if (node instanceof HBox) {
                            Label lbl = (Label) ((HBox) node).getChildren().get(0);
                            return lbl.getText().equals(dateStr);
                        }
                        return false;
                    });
            if (exists) return;

            HBox dayRow = new HBox(10);
            dayRow.setPadding(new Insets(5));
            dayRow.setStyle("-fx-background-color: #fff8dc; -fx-background-radius: 5;");
            dayRow.setAlignment(Pos.CENTER_LEFT);

            Label dateLabel = new Label(dateStr);
            dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            dateLabel.setPrefWidth(80);

            Label tempLabel = new Label(dayData.get("maxtemp_c").getAsString() + "°/" +
                    dayData.get("mintemp_c").getAsString() + "°C");
            tempLabel.setFont(Font.font("Arial", 12));
            tempLabel.setPrefWidth(80);

            Label conditionLabel = new Label(dayData.getAsJsonObject("condition").get("text").getAsString());
            conditionLabel.setFont(Font.font("Arial", 12));

            // Store days ago for sorting
            dayRow.setUserData(daysAgo);
            dayRow.getChildren().addAll(dateLabel, tempLabel, conditionLabel);

            historyContainer.getChildren().add(dayRow);
        }
    }


    private void sortHistoryByDate() {
        historyContainer.getChildren().sort((node1, node2) -> {
            Integer days1 = (Integer) node1.getUserData();
            Integer days2 = (Integer) node2.getUserData();
            return days2.compareTo(days1); // Most recent first (smallest daysAgo value)
        });
    }

    private String getAQIText(int aqi) {
        switch (aqi) {
            case 1: return "Good";
            case 2: return "Moderate";
            case 3: return "Unhealthy for sensitive group";
            case 4: return "Unhealthy";
            case 5: return "Very Unhealthy";
            case 6: return "Hazardous";
            default: return "Unknown";
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}