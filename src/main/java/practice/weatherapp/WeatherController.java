//package practice.weatherapp;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Font;
//import javafx.scene.text.FontWeight;
//
//import java.net.URLEncoder;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.net.URI;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.Arrays;
//import java.util.concurrent.CompletableFuture;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonParser;
//
//public class WeatherController {
//
//    private static final String API_KEY = "b258f6e31ef04dd4a4581201252008";
//    private static final String BASE_URL = "http://api.weatherapi.com/v1";
//
//    private HttpClient httpClient = HttpClient.newHttpClient();
//    private Gson gson = new Gson();
//    private String currentWorkingCity = ""; // Store the working city format
//
//    // FXML injected UI components
//    @FXML
//    private TextField cityInput;
//    @FXML
//    private Button searchButton;
//    @FXML
//    private Label cityLabel;
//    @FXML
//    private Label timeLabel;
//    @FXML
//    private Label currentTempLabel;
//    @FXML
//    private Label feelsLikeLabel;
//    @FXML
//    private Label humidityLabel;
//    @FXML
//    private Label cloudCoverageLabel;
//    @FXML
//    private Label precipitationLabel;
//    @FXML
//    private Label aqiLabel;
//    @FXML
//    private VBox forecastContainer;
//    @FXML
//    private VBox historyContainer;
//    @FXML
//    private ProgressIndicator loadingIndicator;
//
//    public void initialize() {
//        // Set up event handlers
//        searchButton.setOnAction(e -> handleSearch());
//        cityInput.setOnAction(e -> handleSearch());
//
//        // Load default city weather - try different formats for Dhaka
//        loadWeatherData("Dhaka");
//    }
//
//    @FXML
//    private void handleSearch() {
//        String city = cityInput.getText().trim();
//        if (!city.isEmpty()) {
//            loadWeatherData(city);
//        } else {
//            showError("Please enter a city name");
//        }
//    }
//
//    private void loadWeatherData(String city) {
//        loadingIndicator.setVisible(true);
//
//        // Clear previous containers before async tasks
//        Platform.runLater(() -> {
//            forecastContainer.getChildren().clear();
//            historyContainer.getChildren().clear();
//        });
//
//        // Try multiple location formats if the first one fails
//        tryMultipleLocationFormats(city)
//                .thenCompose(success -> {
//                    if (success) {
//                        // Only load history if current weather was successful
//                        return loadWeatherHistory(currentWorkingCity);
//                    } else {
//                        return CompletableFuture.completedFuture(false);
//                    }
//                })
//                .thenRun(() -> Platform.runLater(() -> loadingIndicator.setVisible(false)))
//                .exceptionally(throwable -> {
//                    Platform.runLater(() -> {
//                        loadingIndicator.setVisible(false);
//                        showError("Failed to load weather data: " + throwable.getMessage());
//                        System.err.println("Error details: " + throwable);
//                        throwable.printStackTrace();
//                    });
//                    return null;
//                });
//    }
//
//    private CompletableFuture<Boolean> tryMultipleLocationFormats(String city) {
//        // Create different possible formats for the city
//        String[] cityFormats = generateCityFormats(city);
//
//        System.out.println("Trying city formats: " + Arrays.toString(cityFormats));
//
//        return tryLocationFormatsSequentially(cityFormats, 0);
//    }
//
//    private CompletableFuture<Boolean> tryLocationFormatsSequentially(String[] cityFormats, int index) {
//        if (index >= cityFormats.length) {
//            // All formats failed
//            Platform.runLater(() -> showError("Location not found. Please try:\n" +
//                    "• City name: 'London'\n" +
//                    "• City, Country: 'Dhaka, Bangladesh'\n" +
//                    "• City, Country Code: 'Dhaka, BD'\n" +
//                    "• ZIP/Postal Code: '10001'\n" +
//                    "• Coordinates: '23.8103,90.4125'"));
//            return CompletableFuture.completedFuture(false);
//        }
//
//        return tryLocationFormat(cityFormats[index])
//                .thenCompose(success -> {
//                    if (success) {
//                        currentWorkingCity = cityFormats[index];
//                        return CompletableFuture.completedFuture(true);
//                    } else {
//                        return tryLocationFormatsSequentially(cityFormats, index + 1);
//                    }
//                });
//    }
//
//    private String[] generateCityFormats(String city) {
//        city = city.trim();
//
//        // Special handling for common cities
//        if (city.equalsIgnoreCase("Dhaka")) {
//            return new String[]{
//                    "Dhaka,Bangladesh",
//                    "Dhaka,BD",
//                    "23.8103,90.4125", // Dhaka coordinates
//                    "Dhaka"
//            };
//        } else if (city.equalsIgnoreCase("Chittagong")) {
//            return new String[]{
//                    "Chittagong,Bangladesh",
//                    "Chittagong,BD",
//                    "22.3569,91.7832",
//                    "Chittagong"
//            };
//        } else if (city.equalsIgnoreCase("Sylhet")) {
//            return new String[]{
//                    "Sylhet,Bangladesh",
//                    "Sylhet,BD",
//                    "24.8949,91.8687",
//                    "Sylhet"
//            };
//        } else if (city.toLowerCase().contains("dhaka")) {
//            return new String[]{
//                    "Dhaka,Bangladesh",
//                    "Dhaka,BD",
//                    "23.8103,90.4125",
//                    city
//            };
//        } else {
//            // General format attempts
//            return new String[]{
//                    city,
//                    city + ",BD",
//                    city + ",Bangladesh"
//            };
//        }
//    }
//
//    private CompletableFuture<Boolean> tryLocationFormat(String cityFormat) {
//        return loadCurrentWeatherAndForecast(cityFormat);
//    }
//
//    private CompletableFuture<Boolean> loadCurrentWeatherAndForecast(String city) {
//        String url = BASE_URL + "/forecast.json?key=" + API_KEY + "&q=" +
//                URLEncoder.encode(city, StandardCharsets.UTF_16) +
//                "&days=3&aqi=yes";
//
//        System.out.println("Making request to: " + url);
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .header("User-Agent", "WeatherApp/1.0")
//                .timeout(java.time.Duration.ofSeconds(30))
//                .build();
//
//        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
//                .thenApply(response -> {
//                    System.out.println("Response status: " + response.statusCode());
//                    System.out.println("Response body: " + response.body().substring(0, Math.min(500, response.body().length())));
//
//                    if (response.statusCode() == 200) {
//                        try {
//                            JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject();
//                            Platform.runLater(() -> {
//                                updateCurrentWeatherUI(data);
//                                updateForecastUI(data);
//                            });
//                            return true;
//                        } catch (Exception e) {
//                            Platform.runLater(() -> showError("Failed to parse weather data: " + e.getMessage()));
//                            e.printStackTrace();
//                            return false;
//                        }
//                    } else if (response.statusCode() == 400) {
//                        // Parse error response but don't show error yet (we might try other formats)
//                        try {
//                            JsonObject errorData = JsonParser.parseString(response.body()).getAsJsonObject();
//                            JsonObject error = errorData.getAsJsonObject("error");
//                            String errorMessage = error.get("message").getAsString();
//                            System.out.println("Location format failed: " + city + " - " + errorMessage);
//                        } catch (Exception e) {
//                            System.out.println("Location format failed: " + city);
//                        }
//                        return false;
//                    } else if (response.statusCode() == 401) {
//                        Platform.runLater(() -> showError("API Key is invalid or expired"));
//                        return false;
//                    } else if (response.statusCode() == 403) {
//                        Platform.runLater(() -> showError("API Key doesn't have permission for this request"));
//                        return false;
//                    } else {
//                        Platform.runLater(() -> showError("API Error: HTTP " + response.statusCode()));
//                        return false;
//                    }
//                })
//                .exceptionally(throwable -> {
//                    System.err.println("Network error for location: " + city + " - " + throwable.getMessage());
//                    return false;
//                });
//    }
//
//    private CompletableFuture<Boolean> loadWeatherHistory(String city) {
//        CompletableFuture<Void>[] historyTasks = new CompletableFuture[7];
//
//        for (int i = 1; i <= 7; i++) {
//            LocalDate date = LocalDate.now().minusDays(i);
//            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//            String url = BASE_URL + "/history.json?key=" + API_KEY + "&q=" +
//                    URLEncoder.encode(city, StandardCharsets.UTF_8) + "&dt=" + dateStr;
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .header("User-Agent", "WeatherApp/1.0")
//                    .timeout(java.time.Duration.ofSeconds(30))
//                    .build();
//
//            final int dayIndex = i;
//            historyTasks[i-1] = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
//                    .thenAccept(response -> {
//                        if (response.statusCode() == 200) {
//                            try {
//                                JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject();
//                                Platform.runLater(() -> addHistoryDay(data, dayIndex));
//                            } catch (Exception e) {
//                                System.err.println("Failed to parse history data for day " + dayIndex + ": " + e.getMessage());
//                            }
//                        } else {
//                            System.err.println("Failed to load history for day " + dayIndex + ": HTTP " + response.statusCode());
//                        }
//                    })
//                    .exceptionally(throwable -> {
//                        System.err.println("Network error for history day " + dayIndex + ": " + throwable.getMessage());
//                        return null;
//                    });
//        }
//
//        return CompletableFuture.allOf(historyTasks)
//                .thenRun(() -> Platform.runLater(this::sortHistoryByDate))
//                .thenApply(v -> true);
//    }
//
//    private void updateCurrentWeatherUI(JsonObject data) {
//        try {
//            JsonObject location = data.getAsJsonObject("location");
//            JsonObject current = data.getAsJsonObject("current");
//            JsonObject airQuality = current.has("air_quality") ? current.getAsJsonObject("air_quality") : null;
//
//            cityLabel.setText("City: " + location.get("name").getAsString() + ", " + location.get("country").getAsString());
//            timeLabel.setText("Local Time: " + location.get("localtime").getAsString());
//            currentTempLabel.setText("Temperature: " + current.get("temp_c").getAsString() + "°C");
//            feelsLikeLabel.setText("Feels like: " + current.get("feelslike_c").getAsString() + "°C");
//            humidityLabel.setText("Humidity: " + current.get("humidity").getAsString() + "%");
//            cloudCoverageLabel.setText("Cloud coverage: " + current.get("cloud").getAsString() + "%");
//            precipitationLabel.setText("Precipitation: " + current.get("precip_mm").getAsString() + " mm");
//
//            if (airQuality != null && airQuality.has("us-epa-index")) {
//                int aqiValue = airQuality.get("us-epa-index").getAsInt();
//                String aqiText = getAQIText(aqiValue);
//                aqiLabel.setText("Air Quality Index: " + aqiValue + " (" + aqiText + ")");
//            } else {
//                aqiLabel.setText("Air Quality Index: Not available");
//            }
//        } catch (Exception e) {
//            showError("Failed to update current weather UI: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void updateForecastUI(JsonObject data) {
//        try {
//            forecastContainer.getChildren().clear();
//
//            JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");
//
//            for (JsonElement dayElement : forecastDays) {
//                JsonObject day = dayElement.getAsJsonObject();
//                JsonObject dayData = day.getAsJsonObject("day");
//
//                HBox dayRow = new HBox(10);
//                dayRow.setPadding(new Insets(5));
//                dayRow.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 5;");
//                dayRow.setAlignment(Pos.CENTER_LEFT);
//
//                Label dateLabel = new Label(day.get("date").getAsString());
//                dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
//                dateLabel.setPrefWidth(80);
//
//                Label tempLabel = new Label(dayData.get("maxtemp_c").getAsString() + "°/" +
//                        dayData.get("mintemp_c").getAsString() + "°C");
//                tempLabel.setFont(Font.font("Arial", 12));
//                tempLabel.setPrefWidth(80);
//
//                Label conditionLabel = new Label(dayData.getAsJsonObject("condition").get("text").getAsString());
//                conditionLabel.setFont(Font.font("Arial", 12));
//
//                dayRow.getChildren().addAll(dateLabel, tempLabel, conditionLabel);
//                forecastContainer.getChildren().add(dayRow);
//            }
//        } catch (Exception e) {
//            showError("Failed to update forecast UI: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void addHistoryDay(JsonObject data, int daysAgo) {
//        try {
//            JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");
//
//            if (forecastDays.size() > 0) {
//                JsonObject day = forecastDays.get(0).getAsJsonObject();
//                JsonObject dayData = day.getAsJsonObject("day");
//                String dateStr = day.get("date").getAsString();
//
//                // Check if this date is already present → skip
//                boolean exists = historyContainer.getChildren().stream()
//                        .anyMatch(node -> {
//                            if (node instanceof HBox) {
//                                HBox hbox = (HBox) node;
//                                if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
//                                    Label lbl = (Label) hbox.getChildren().get(0);
//                                    return lbl.getText().equals(dateStr);
//                                }
//                            }
//                            return false;
//                        });
//
//                if (exists) return;
//
//                HBox dayRow = new HBox(10);
//                dayRow.setPadding(new Insets(5));
//                dayRow.setStyle("-fx-background-color: #fff8dc; -fx-background-radius: 5;");
//                dayRow.setAlignment(Pos.CENTER_LEFT);
//
//                Label dateLabel = new Label(dateStr);
//                dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
//                dateLabel.setPrefWidth(80);
//
//                Label tempLabel = new Label(dayData.get("maxtemp_c").getAsString() + "°/" +
//                        dayData.get("mintemp_c").getAsString() + "°C");
//                tempLabel.setFont(Font.font("Arial", 12));
//                tempLabel.setPrefWidth(80);
//
//                Label conditionLabel = new Label(dayData.getAsJsonObject("condition").get("text").getAsString());
//                conditionLabel.setFont(Font.font("Arial", 12));
//
//                // Store days ago for sorting
//                dayRow.setUserData(daysAgo);
//                dayRow.getChildren().addAll(dateLabel, tempLabel, conditionLabel);
//                historyContainer.getChildren().add(dayRow);
//            }
//        } catch (Exception e) {
//            System.err.println("Failed to add history day: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void sortHistoryByDate() {
//        try {
//            historyContainer.getChildren().sort((node1, node2) -> {
//                Integer days1 = (Integer) node1.getUserData();
//                Integer days2 = (Integer) node2.getUserData();
//                if (days1 == null || days2 == null) return 0;
//                return days1.compareTo(days2); // Ascending order (oldest first)
//            });
//        } catch (Exception e) {
//            System.err.println("Failed to sort history: " + e.getMessage());
//        }
//    }
//
//    private String getAQIText(int aqi) {
//        switch (aqi) {
//            case 1: return "Good";
//            case 2: return "Moderate";
//            case 3: return "Unhealthy for sensitive group";
//            case 4: return "Unhealthy";
//            case 5: return "Very Unhealthy";
//            case 6: return "Hazardous";
//            default: return "Unknown";
//        }
//    }
//
//    private void showError(String message) {
//        try {
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Weather App Error");
//            alert.setHeaderText(null);
//            alert.setContentText(message);
//            alert.showAndWait();
//        } catch (Exception e) {
//            System.err.println("Error showing alert: " + e.getMessage());
//        }
//    }
//}

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

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets; // Ensure this is imported
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class WeatherController {

    private static final String API_KEY = "b258f6e31ef04dd4a4581201252008";
    private static final String BASE_URL = "https://api.weatherapi.com/v1";

    private HttpClient httpClient = HttpClient.newHttpClient();
    private Gson gson = new Gson();
    private String currentWorkingCity = ""; // Store the working city format

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
        // Set up event handlers for search button and text field
        searchButton.setOnAction(e -> handleSearch());
        cityInput.setOnAction(e -> handleSearch()); // Allows searching by pressing Enter in the text field

        // Load default city weather for Dhaka when the app starts
        loadWeatherData("Dhaka");
    }

    @FXML
    private void handleSearch() {
        String city = cityInput.getText().trim(); // Get the city name from the input field
        if (!city.isEmpty()) {
            loadWeatherData(city); // Load weather data if the city input is not empty
        } else {
            showError("Please enter a city name"); // Show an error if the input is empty
        }
    }

    private void loadWeatherData(String city) {
        loadingIndicator.setVisible(true); // Show loading indicator

        // Clear previous forecast and history data on the UI thread
        Platform.runLater(() -> {
            forecastContainer.getChildren().clear();
            historyContainer.getChildren().clear();
        });

        // Attempt to load weather data using multiple location formats sequentially
        tryMultipleLocationFormats(city)
                .thenCompose(success -> {
                    if (success) {
                        // If current weather data loaded successfully, then load weather history
                        return loadWeatherHistory(currentWorkingCity);
                    } else {
                        // If current weather data failed to load, complete with false
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .thenRun(() -> Platform.runLater(() -> loadingIndicator.setVisible(false))) // Hide loading indicator when all tasks are complete
                .exceptionally(throwable -> {
                    // Handle any exceptions that occur during the process
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        showError("Failed to load weather data: " + throwable.getMessage());
                        System.err.println("Error details: " + throwable);
                        throwable.printStackTrace();
                    });
                    return null;
                });
    }

    private CompletableFuture<Boolean> tryMultipleLocationFormats(String city) {
        // Generate an array of possible city formats to try for the API request
        String[] cityFormats = generateCityFormats(city);

        System.out.println("Trying city formats: " + Arrays.toString(cityFormats));

        // Start trying location formats sequentially from the first one
        return tryLocationFormatsSequentially(cityFormats, 0);
    }

    private CompletableFuture<Boolean> tryLocationFormatsSequentially(String[] cityFormats, int index) {
        if (index >= cityFormats.length) {
            // If all formats have been tried and failed, show an error message
            Platform.runLater(() -> showError("Location not found. Please try:\n" +
                    "• City name: 'London'\n" +
                    "• City, Country: 'Dhaka, Bangladesh'\n" +
                    "• City, Country Code: 'Dhaka, BD'\n" +
                    "• ZIP/Postal Code: '10001'\n" +
                    "• Coordinates: '23.8103,90.4125'"));
            return CompletableFuture.completedFuture(false);
        }

        // Try the current location format
        return tryLocationFormat(cityFormats[index])
                .thenCompose(success -> {
                    if (success) {
                        // If successful, store the working city format and complete with true
                        currentWorkingCity = cityFormats[index];
                        return CompletableFuture.completedFuture(true);
                    } else {
                        // If unsuccessful, try the next format in the array
                        return tryLocationFormatsSequentially(cityFormats, index + 1);
                    }
                });
    }

    private String[] generateCityFormats(String city) {
        city = city.trim();

        // Provide specific formats for common cities to improve search accuracy
        if (city.equalsIgnoreCase("Dhaka")) {
            return new String[]{
                    "Dhaka,Bangladesh",
                    "Dhaka,BD",
                    "23.8103,90.4125", // Dhaka coordinates
                    "Dhaka"
            };
        } else if (city.equalsIgnoreCase("Chittagong")) {
            return new String[]{
                    "Chittagong,Bangladesh",
                    "Chittagong,BD",
                    "22.3569,91.7832",
                    "Chittagong"
            };
        } else if (city.equalsIgnoreCase("Sylhet")) {
            return new String[]{
                    "Sylhet,Bangladesh",
                    "Sylhet,BD",
                    "24.8949,91.8687",
                    "Sylhet"
            };
        } else if (city.toLowerCase().contains("dhaka")) {
            // If the input contains "Dhaka", prioritize Dhaka-specific formats
            return new String[]{
                    "Dhaka,Bangladesh",
                    "Dhaka,BD",
                    "23.8103,90.4125",
                    city
            };
        } else {
            // For other cities, try general formats
            return new String[]{
                    city
            };
        }
    }

    private CompletableFuture<Boolean> tryLocationFormat(String cityFormat) {
        // This method initiates the API call for current weather and forecast
        return loadCurrentWeatherAndForecast(cityFormat);
    }

    private CompletableFuture<Boolean> loadCurrentWeatherAndForecast(String city) {
        // Construct the URL for the forecast API
        // *** FIX: Changed StandardCharsets.UTF_16 to StandardCharsets.UTF_8 for proper URL encoding ***
        String url = BASE_URL + "/forecast.json?key=" + API_KEY + "&q=" +
                URLEncoder.encode(city, StandardCharsets.UTF_8) + // Changed from UTF_16
                "&days=3&aqi=yes";

        System.out.println("Making request to: " + url);

        // Build the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "WeatherApp/1.0")
                .timeout(java.time.Duration.ofSeconds(30)) // Set a timeout for the request
                .build();

        // Send the asynchronous HTTP request
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Response status: " + response.statusCode());
                    System.out.println("Response body: " + response.body().substring(0, Math.min(500, response.body().length())));

                    if (response.statusCode() == 200) {
                        // If the request was successful, parse and update the UI
                        try {
                            JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject();
                            Platform.runLater(() -> {
                                updateCurrentWeatherUI(data);
                                updateForecastUI(data);
                            });
                            return true; // Indicate success
                        } catch (Exception e) {
                            Platform.runLater(() -> showError("Failed to parse weather data: " + e.getMessage()));
                            e.printStackTrace();
                            return false; // Indicate parsing failure
                        }
                    } else if (response.statusCode() == 400) {
                        // Handle bad request errors (e.g., invalid location).
                        // Added more detailed logging for debugging.
                        System.err.println("API responded with 400 (Bad Request) for city: " + city);
                        System.err.println("Full 400 response body: " + response.body()); // Log the entire body
                        try {
                            JsonObject errorData = JsonParser.parseString(response.body()).getAsJsonObject();
                            JsonObject error = errorData.getAsJsonObject("error");
                            String errorMessage = error.get("message").getAsString();
                            System.err.println("WeatherAPI error message: " + errorMessage); // Explicitly log API message
                        } catch (Exception e) {
                            System.err.println("Could not parse 400 error response body: " + e.getMessage());
                        }
                        return false; // Indicate failure, allowing other formats to be tried
                    } else if (response.statusCode() == 401) {
                        Platform.runLater(() -> showError("API Key is invalid or expired"));
                        return false; // Indicate API key issue
                    } else if (response.statusCode() == 403) {
                        Platform.runLater(() -> showError("API Key doesn't have permission for this request"));
                        return false; // Indicate permission issue
                    } else {
                        Platform.runLater(() -> showError("API Error: HTTP " + response.statusCode()));
                        return false; // Indicate other HTTP errors
                    }
                })
                .exceptionally(throwable -> {
                    // Handle network-related exceptions (e.g., connection issues)
                    System.err.println("Network error for location: " + city + " - " + throwable.getMessage());
                    return false; // Indicate network error
                });
    }

    private CompletableFuture<Boolean> loadWeatherHistory(String city) {
        CompletableFuture<Void>[] historyTasks = new CompletableFuture[7]; // Array to hold 7 history API calls

        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i); // Get date for the last 7 days
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Format date
            String url = BASE_URL + "/history.json?key=" + API_KEY + "&q=" +
                    URLEncoder.encode(city, StandardCharsets.UTF_8) + "&dt=" + dateStr; // History API URL

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "WeatherApp/1.0")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            final int dayIndex = i;
            historyTasks[i - 1] = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                JsonObject data = JsonParser.parseString(response.body()).getAsJsonObject();
                                Platform.runLater(() -> addHistoryDay(data, dayIndex)); // Add history day to UI
                            } catch (Exception e) {
                                System.err.println("Failed to parse history data for day " + dayIndex + ": " + e.getMessage());
                            }
                        } else {
                            System.err.println("Failed to load history for day " + dayIndex + ": HTTP " + response.statusCode());
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Network error for history day " + dayIndex + ": " + throwable.getMessage());
                        return null;
                    });
        }

        // Wait for all history tasks to complete, then sort the history by date
        return CompletableFuture.allOf(historyTasks)
                .thenRun(() -> Platform.runLater(this::sortHistoryByDate))
                .thenApply(v -> true);
    }

    private void updateCurrentWeatherUI(JsonObject data) {
        try {
            JsonObject location = data.getAsJsonObject("location");
            JsonObject current = data.getAsJsonObject("current");
            // Check if air_quality field exists before trying to get it
            JsonObject airQuality = current.has("air_quality") ? current.getAsJsonObject("air_quality") : null;

            // Update UI labels with current weather data
            cityLabel.setText("City: " + location.get("name").getAsString() + ", " + location.get("country").getAsString());
            timeLabel.setText("Local Time: " + location.get("localtime").getAsString());
            currentTempLabel.setText("Temperature: " + current.get("temp_c").getAsString() + "°C");
            feelsLikeLabel.setText("Feels like: " + current.get("feelslike_c").getAsString() + "°C");
            humidityLabel.setText("Humidity: " + current.get("humidity").getAsString() + "%");
            cloudCoverageLabel.setText("Cloud coverage: " + current.get("cloud").getAsString() + "%");
            precipitationLabel.setText("Precipitation: " + current.get("precip_mm").getAsString() + " mm");

            // Update AQI label, handling cases where it might not be available
            if (airQuality != null && airQuality.has("us-epa-index")) {
                int aqiValue = airQuality.get("us-epa-index").getAsInt();
                String aqiText = getAQIText(aqiValue);
                aqiLabel.setText("Air Quality Index: " + aqiValue + " (" + aqiText + ")");
            } else {
                aqiLabel.setText("Air Quality Index: Not available");
            }
        } catch (Exception e) {
            showError("Failed to update current weather UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateForecastUI(JsonObject data) {
        try {
            forecastContainer.getChildren().clear(); // Clear previous forecast entries

            JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");

            // Iterate through forecast days and add them to the UI
            for (JsonElement dayElement : forecastDays) {
                JsonObject day = dayElement.getAsJsonObject();
                JsonObject dayData = day.getAsJsonObject("day");

                HBox dayRow = new HBox(10); // HBox for each day's forecast
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
        } catch (Exception e) {
            showError("Failed to update forecast UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addHistoryDay(JsonObject data, int daysAgo) {
        try {
            JsonArray forecastDays = data.getAsJsonObject("forecast").getAsJsonArray("forecastday");

            if (forecastDays.size() > 0) {
                JsonObject day = forecastDays.get(0).getAsJsonObject();
                JsonObject dayData = day.getAsJsonObject("day");
                String dateStr = day.get("date").getAsString();

                // Prevent adding duplicate history entries
                boolean exists = historyContainer.getChildren().stream()
                        .anyMatch(node -> {
                            if (node instanceof HBox) {
                                HBox hbox = (HBox) node;
                                if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                                    Label lbl = (Label) hbox.getChildren().get(0);
                                    return lbl.getText().equals(dateStr);
                                }
                            }
                            return false;
                        });

                if (exists) return; // Skip if already present

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

                dayRow.setUserData(daysAgo); // Store daysAgo for sorting later
                dayRow.getChildren().addAll(dateLabel, tempLabel, conditionLabel);
                historyContainer.getChildren().add(dayRow);
            }
        } catch (Exception e) {
            System.err.println("Failed to add history day: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sortHistoryByDate() {
        try {
            // Sort history entries based on the 'daysAgo' UserData
            historyContainer.getChildren().sort((node1, node2) -> {
                Integer days1 = (Integer) node1.getUserData();
                Integer days2 = (Integer) node2.getUserData();
                if (days1 == null || days2 == null) return 0;
                return days1.compareTo(days2); // Sort in ascending order (oldest first)
            });
        } catch (Exception e) {
            System.err.println("Failed to sort history: " + e.getMessage());
        }
    }

    private String getAQIText(int aqi) {
        // Returns a descriptive text for the given AQI value
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
        try {
            // Display an error alert to the user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Weather App Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
        }
    }
}

