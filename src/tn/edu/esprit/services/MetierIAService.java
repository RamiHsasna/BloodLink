package tn.edu.esprit.services;

import tn.edu.esprit.entities.Alert;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetierIAService {

    private static final String PROVIDER_GEMINI = "gemini";
    private static final String PROVIDER_GROQ = "groq";
    private static final String PROVIDER_OPENROUTER = "openrouter";

    private static final String DEFAULT_PROVIDER = PROVIDER_OPENROUTER;
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.0-flash";
    private static final String DEFAULT_GROQ_MODEL = "llama-3.1-8b-instant";
    private static final String DEFAULT_OPENROUTER_MODEL = "openrouter/free";
    private static final int DEFAULT_OPENROUTER_MAX_TOKENS = 220;
    private static final int ALERT_MAX_TOKENS = 140;
    private static final int DEFAULT_ANALYSIS_MAX_TOKENS = 520;
    private static final int OPENROUTER_RETRY_EXTRA_TOKENS = 180;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Pattern GEMINI_TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern CHAT_CONTENT_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[\\s\\S]*?\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern CHAT_CONTENT_ARRAY_TEXT_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[\\s\\S]*?\"content\"\\s*:\\s*\\[[\\s\\S]*?\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern CHAT_CHOICE_TEXT_PATTERN = Pattern.compile(
            "\"choices\"\\s*:\\s*\\[[\\s\\S]*?\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern CHAT_OUTPUT_TEXT_PATTERN = Pattern.compile(
            "\"output_text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile(
            "\"error\"\\s*:\\s*\\{[\\s\\S]*?\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern REFUSAL_PATTERN = Pattern.compile(
            "\"refusal\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern FINISH_REASON_PATTERN = Pattern.compile(
            "\"finish_reason\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);
    private static final Pattern MESSAGE_REASONING_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[\\s\\S]*?\"reasoning\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL);

    private static final Properties CONFIG = loadConfig();

    private final HttpClient httpClient;

    public MetierIAService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        String[] paths = {
                "config.properties",
                System.getProperty("user.dir") + File.separator + "config.properties",
                System.getProperty("user.home") + File.separator + "bloodlink_config.properties"
        };
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                    System.out.println("metier_ia: config chargee depuis " + file.getAbsolutePath());
                    return props;
                } catch (Exception e) {
                    System.err.println("metier_ia: erreur lecture config: " + e.getMessage());
                }
            }
        }
        return props;
    }

    public String generateEmergencyMessage(Alert alert, int matchedDonorCount) {
        return generateText(buildPrompt(alert, matchedDonorCount), ALERT_MAX_TOKENS, true);
    }

    public String generateAuditLogAnalysis(String prompt) {
        return generateText(prompt, DEFAULT_ANALYSIS_MAX_TOKENS, false);
    }

    private String generateText(String prompt, int maxTokens, boolean truncateLongOutput) {
        String provider = normalizeProvider(readProvider());

        switch (provider) {
            case PROVIDER_GEMINI:
                return callGemini(prompt, maxTokens);
            case PROVIDER_GROQ:
                return callGroq(prompt, maxTokens);
            case PROVIDER_OPENROUTER:
                return callOpenRouter(prompt, maxTokens, truncateLongOutput);
            default:
                throw new IllegalStateException("metier_ia: provider IA inconnu: " + provider);
        }
    }

    private String callGemini(String prompt, int maxTokens) {
        String apiKey = readFirstNonBlankEnv("BLOODLINK_GEMINI_API_KEY", "GEMINI_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException(
                    "metier_ia: cle Gemini absente. Configurez BLOODLINK_GEMINI_API_KEY ou GEMINI_API_KEY.");
        }

        String model = readModelFromEnv("BLOODLINK_GEMINI_MODEL", DEFAULT_GEMINI_MODEL);
        String requestBody = buildGeminiRequestBody(prompt, maxTokens);

        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                    + ":generateContent?key=" + encodedKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String providerError = extractProviderErrorMessage(response.body());
                if (providerError != null && !providerError.isBlank()) {
                    throw new IllegalStateException(
                            "metier_ia: appel Gemini en echec (HTTP " + response.statusCode() + ") -> "
                                    + providerError);
                }
                throw new IllegalStateException(
                        "metier_ia: appel Gemini en echec (HTTP " + response.statusCode() + ").");
            }

            String generatedText = extractGeminiGeneratedText(response.body());
            if (generatedText == null || generatedText.isBlank()) {
                throw new IllegalStateException("metier_ia: reponse Gemini sans texte exploitable.");
            }

            return sanitizeText(generatedText);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("metier_ia: exception Gemini -> " + e.getMessage(), e);
        }
    }

    private String callGroq(String prompt, int maxTokens) {
        String apiKey = readFirstNonBlankEnv("BLOODLINK_GROQ_API_KEY", "GROQ_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException(
                    "metier_ia: cle Groq absente. Configurez BLOODLINK_GROQ_API_KEY ou GROQ_API_KEY.");
        }

        String model = readModelFromEnv("BLOODLINK_GROQ_MODEL", DEFAULT_GROQ_MODEL);
        String requestBody = buildOpenAiCompatibleRequestBody(model, prompt, maxTokens);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String providerError = extractProviderErrorMessage(response.body());
                if (providerError != null && !providerError.isBlank()) {
                    throw new IllegalStateException(
                            "metier_ia: appel Groq en echec (HTTP " + response.statusCode() + ") -> " + providerError);
                }
                throw new IllegalStateException("metier_ia: appel Groq en echec (HTTP " + response.statusCode() + ").");
            }

            String providerError = extractProviderErrorMessage(response.body());
            if (providerError != null && !providerError.isBlank()) {
                throw new IllegalStateException("metier_ia: Groq -> " + providerError);
            }

            String generatedText = extractChatGeneratedText(response.body());
            if (generatedText == null || generatedText.isBlank()) {
                String debug = compactJsonPreview(response.body(), 320);
                throw new IllegalStateException(
                        "metier_ia: reponse Groq sans texte exploitable. payload=" + debug);
            }

            return sanitizeText(generatedText);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("metier_ia: exception Groq -> " + e.getMessage(), e);
        }
    }

    private String callOpenRouter(String prompt, int maxTokens, boolean truncateLongOutput) {
        String apiKey = readFirstNonBlankEnv("BLOODLINK_OPENROUTER_API_KEY", "OPENROUTER_API_KEY");
        if (apiKey == null) {
            throw new IllegalStateException(
                    "metier_ia: cle OpenRouter absente. Configurez BLOODLINK_OPENROUTER_API_KEY ou OPENROUTER_API_KEY.");
        }

        String requestedModel = readModelFromEnv("BLOODLINK_OPENROUTER_MODEL", DEFAULT_OPENROUTER_MODEL);
        String[] modelsToTry = buildOpenRouterModelCandidates(requestedModel);
        int requestedMaxTokens = Math.max(maxTokens,
                readPositiveIntEnv("BLOODLINK_OPENROUTER_MAX_TOKENS", DEFAULT_OPENROUTER_MAX_TOKENS));
        String lastFailure = null;

        for (int i = 0; i < modelsToTry.length; i++) {
            String model = modelsToTry[i];
            String requestBody = buildOpenRouterRequestBody(model, prompt, requestedMaxTokens);
            try {
                HttpResponse<String> response = executeOpenRouterRequest(apiKey, requestBody);

                String providerError = extractProviderErrorMessage(response.body());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String baseError = "metier_ia: appel OpenRouter en echec (HTTP " + response.statusCode() + ")";
                    if (providerError != null && !providerError.isBlank()) {
                        baseError += " -> " + providerError;
                    }

                    // If a custom model has no endpoint, retry once with the free router.
                    if (i < modelsToTry.length - 1
                            && isRetryableOpenRouterModelError(response.statusCode(), providerError)) {
                        lastFailure = baseError + " [modele: " + model + "]";
                        continue;
                    }

                    throw new IllegalStateException(
                            baseError + " [modele: " + model + "]");
                }

                if (providerError != null && !providerError.isBlank()) {
                    throw new IllegalStateException(
                            "metier_ia: OpenRouter -> " + providerError + " [modele: " + model + "]");
                }

                String generatedText = extractChatGeneratedText(response.body());
                if ((generatedText == null || generatedText.isBlank()) && isLengthFinishReason(response.body())) {
                    int retryMaxTokens = Math.min(requestedMaxTokens + OPENROUTER_RETRY_EXTRA_TOKENS, 700);
                    String retryBody = buildOpenRouterRequestBody(model, prompt, retryMaxTokens);
                    HttpResponse<String> retryResponse = executeOpenRouterRequest(apiKey, retryBody);
                    String retryError = extractProviderErrorMessage(retryResponse.body());
                    if (retryResponse.statusCode() >= 200 && retryResponse.statusCode() < 300
                            && (retryError == null || retryError.isBlank())) {
                        generatedText = extractChatGeneratedText(retryResponse.body());
                        response = retryResponse;
                    }
                }

                if (generatedText == null || generatedText.isBlank()) {
                    if (i < modelsToTry.length - 1) {
                        lastFailure = "metier_ia: reponse OpenRouter sans texte exploitable [modele: " + model + "]";
                        continue;
                    }
                    String reasoning = extractReasoningText(response.body());
                    if (reasoning != null && !reasoning.isBlank()) {
                        throw new IllegalStateException(
                                "metier_ia: le modele a renvoye du raisonnement interne sans message final [modele: "
                                        + model + "]. Essayez un autre modele free ou relancez.");
                    }
                    String debug = compactJsonPreview(response.body(), 320);
                    throw new IllegalStateException(
                            "metier_ia: reponse OpenRouter sans texte exploitable [modele: " + model + "]. payload="
                                    + debug);
                }

                return finalizeGeneratedText(generatedText, truncateLongOutput);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                if (i < modelsToTry.length - 1) {
                    lastFailure = "metier_ia: exception OpenRouter [modele: " + model + "] -> " + e.getMessage();
                    continue;
                }
                throw new IllegalStateException("metier_ia: exception OpenRouter -> " + e.getMessage(), e);
            }
        }

        throw new IllegalStateException(lastFailure != null
                ? lastFailure
                : "metier_ia: appel OpenRouter impossible.");
    }

    private String[] buildOpenRouterModelCandidates(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return new String[] { DEFAULT_OPENROUTER_MODEL };
        }
        String normalized = requestedModel.trim();
        if (DEFAULT_OPENROUTER_MODEL.equalsIgnoreCase(normalized)) {
            return new String[] { DEFAULT_OPENROUTER_MODEL };
        }
        return new String[] { normalized, DEFAULT_OPENROUTER_MODEL };
    }

    private HttpResponse<String> executeOpenRouterRequest(String apiKey, String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean isRetryableOpenRouterModelError(int httpStatus, String providerError) {
        if (httpStatus == 401 || httpStatus == 403) {
            return false;
        }
        if (providerError == null || providerError.isBlank()) {
            return httpStatus == 400 || httpStatus == 404 || httpStatus == 429;
        }
        String normalized = providerError.toLowerCase();
        return normalized.contains("no endpoints found")
                || normalized.contains("model not found")
                || normalized.contains("unknown model")
                || normalized.contains("rate limit")
                || normalized.contains("temporarily unavailable");
    }

    private String readProvider() {
        String value = readFirstNonBlankEnv("BLOODLINK_IA_PROVIDER", "BLOODLINK_AI_PROVIDER");
        if (value == null) {
            return DEFAULT_PROVIDER;
        }
        return value;
    }

    private String normalizeProvider(String value) {
        if (value == null) {
            return DEFAULT_PROVIDER;
        }

        String normalized = value.trim().toLowerCase();
        if ("google".equals(normalized)) {
            return PROVIDER_GEMINI;
        }
        if ("router".equals(normalized)) {
            return PROVIDER_OPENROUTER;
        }
        return normalized;
    }

    private String readFirstNonBlankEnv(String... names) {
        for (String name : names) {
            // 1. config.properties d'abord
            String value = CONFIG.getProperty(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
            // 2. variable d'environnement ensuite
            value = System.getenv(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String readModelFromEnv(String envVar, String fallback) {
        String value = CONFIG.getProperty(envVar);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        value = System.getenv(envVar);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private int readPositiveIntEnv(String envVar, int fallback) {
        String value = CONFIG.getProperty(envVar);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envVar);
        }
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String buildPrompt(Alert alert, int matchedDonorCount) {
        String title = safe(alert.getTitle());
        String bloodType = safe(alert.getBloodTypeId());
        String severity = alert.getSeverity() != null ? alert.getSeverity().name() : "N/A";
        int quantity = alert.getQuantityNeeded();
        int radius = alert.getTargetRadiusKm();

        return "Tu es un assistant metier BloodLink.\n"
                + "Genere un message d'alerte SMS en francais (max 240 caracteres), ton urgent et professionnel.\n"
                + "Le message doit etre clair, actionnable, sans jargon, et pret a envoyer aux donneurs.\n"
                + "Format attendu: une seule phrase, sans markdown, sans guillemets.\n"
                + "Contexte:\n"
                + "- Titre: " + title + "\n"
                + "- Groupe sanguin requis: " + bloodType + "\n"
                + "- Severite: " + severity + "\n"
                + "- Quantite demandee: " + quantity + "\n"
                + "- Rayon cible (km): " + radius + "\n"
                + "- Donneurs compatibles identifies: " + matchedDonorCount + "\n"
                + "Retourne uniquement le texte final du message, sans guillemets ni markdown.";
    }

    private String buildGeminiRequestBody(String prompt, int maxTokens) {
        return "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}],"
                + "\"generationConfig\":{"
                + "\"temperature\":0.2,"
                + "\"maxOutputTokens\":" + maxTokens
                + "}"
                + "}";
    }

    private String buildOpenAiCompatibleRequestBody(String model, String prompt, int maxTokens) {
        String systemInstruction = "Tu es un assistant metier BloodLink. Reponds uniquement en francais.";
        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemInstruction) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.2,"
                + "\"max_tokens\":" + maxTokens
                + "}";
    }

    private String buildOpenRouterRequestBody(String model, String prompt, int maxTokens) {
        String systemInstruction = "Tu es un assistant metier BloodLink. Reponds uniquement en francais.";
        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemInstruction) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.2,"
                + "\"max_tokens\":" + maxTokens + ","
                + "\"include_reasoning\":false,"
                + "\"reasoning\":{\"exclude\":true}"
                + "}";
    }

    private String extractGeminiGeneratedText(String jsonResponse) {
        Matcher matcher = GEMINI_TEXT_PATTERN.matcher(jsonResponse);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private String extractChatGeneratedText(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }

        Matcher matcher = CHAT_CONTENT_PATTERN.matcher(jsonResponse);
        if (!matcher.find()) {
            matcher = CHAT_CONTENT_ARRAY_TEXT_PATTERN.matcher(jsonResponse);
            if (!matcher.find()) {
                matcher = CHAT_CHOICE_TEXT_PATTERN.matcher(jsonResponse);
                if (!matcher.find()) {
                    matcher = CHAT_OUTPUT_TEXT_PATTERN.matcher(jsonResponse);
                    if (!matcher.find()) {
                        return null;
                    }
                }
            }
        }
        return unescapeJson(matcher.group(1));
    }

    private String extractProviderErrorMessage(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }

        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            return sanitizeText(unescapeJson(matcher.group(1)));
        }

        matcher = REFUSAL_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            return "reponse refusee par le modele: " + sanitizeText(unescapeJson(matcher.group(1)));
        }

        return null;
    }

    private boolean isLengthFinishReason(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return false;
        }
        Matcher matcher = FINISH_REASON_PATTERN.matcher(jsonResponse);
        if (!matcher.find()) {
            return false;
        }
        String reason = unescapeJson(matcher.group(1));
        return reason != null && "length".equalsIgnoreCase(reason.trim());
    }

    private String extractReasoningText(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return null;
        }
        Matcher matcher = MESSAGE_REASONING_PATTERN.matcher(jsonResponse);
        if (!matcher.find()) {
            return null;
        }
        return sanitizeText(unescapeJson(matcher.group(1)));
    }

    private String compactJsonPreview(String json, int maxChars) {
        if (json == null || json.isBlank()) {
            return "(vide)";
        }
        String compact = json.replace("\r", " ").replace("\n", " ").replaceAll("\\s{2,}", " ").trim();
        if (compact.length() <= maxChars) {
            return compact;
        }
        return compact.substring(0, maxChars) + "...";
    }

    private String finalizeGeneratedText(String text, boolean truncateLongOutput) {
        String sanitized = sanitizeText(text);
        if (!truncateLongOutput || sanitized.length() <= 240) {
            return sanitized;
        }
        return sanitized.substring(0, 237).trim() + "...";
    }

    private String sanitizeText(String text) {
        return text.replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
