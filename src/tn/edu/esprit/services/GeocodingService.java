package tn.edu.esprit.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeocodingService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final Pattern LAT_LON_PATTERN = Pattern.compile(
            "\\\"lat\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?\\\"lon\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
            Pattern.DOTALL
    );

    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    public Optional<Coordinates> geocodeCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedCity = city.trim();

        try {
            String encodedQuery = URLEncoder.encode(normalizedCity, StandardCharsets.UTF_8);
            String requestUrl = NOMINATIM_BASE_URL
                    + "?q=" + encodedQuery
                    + "&format=json"
                    + "&limit=1";

            HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "BloodLink/1.0 (contact: bloodlink.app)");
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return Optional.empty();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                return parseCoordinates(response.toString());
            } finally {
                connection.disconnect();
            }
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private Optional<Coordinates> parseCoordinates(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Optional.empty();
        }

        Matcher matcher = LAT_LON_PATTERN.matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(2));
            return Optional.of(new Coordinates(latitude, longitude));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
