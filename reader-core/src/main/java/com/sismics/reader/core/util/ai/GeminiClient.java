package com.sismics.reader.core.util.ai;

import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Client for the Gemini API.
 */
public class GeminiClient {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /**
     * API key.
     */
    private final String apiKey;

    /**
     * API endpoint.
     */
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    /**
     * Constructor.
     *
     * @param apiKey API key
     */
    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Generate content from the provided prompt.
     *
     * @param prompt Text prompt
     * @return Generated content
     * @throws Exception If there's an error during API communication
     */
    public String generateContent(String prompt) throws Exception {
        if (Strings.isNullOrEmpty(apiKey)) {
            log.warn("No API key provided for Gemini. Using fallback report generation.");
            return generateFallbackReport(prompt);
        }

        try {
            // Create HTTP client
            HttpClient httpClient = HttpClientBuilder.create().build();

            // Create HTTP POST request
            HttpPost request = new HttpPost(API_URL + "?key=" + apiKey);
            request.setHeader("Content-Type", "application/json");

            // Prepare request body
            JSONObject requestBody = new JSONObject();
            JSONObject contents = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            contents.put("parts", parts);
            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contents);
            requestBody.put("contents", contentsArray);

            // Set request body
            StringEntity entity = new StringEntity(requestBody.toString(), StandardCharsets.UTF_8);
            request.setEntity(entity);

            // Execute HTTP request
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            // Process response
            if (statusCode == 200) {
                // Get response as string
                String responseBody = EntityUtils.toString(response.getEntity());

                // Parse JSON response
                JSONObject responseJson = new JSONObject(responseBody);

                // Extract text from response
                return extractTextFromResponse(responseJson);
            } else {
                // Handle error
                String errorMessage = "API error: " + statusCode + " " + response.getStatusLine().getReasonPhrase();
                log.error(errorMessage);
                return generateFallbackReport(prompt);
            }

        } catch (Exception e) {
            log.error("Error generating content with Gemini API", e);
            return generateFallbackReport(prompt);
        }
    }

    /**
     * Extract text content from the API response.
     *
     * @param responseJson Response JSON object
     * @return Extracted text
     * @throws JSONException If there's an error parsing the JSON
     */
    private String extractTextFromResponse(JSONObject responseJson) throws JSONException {
        StringBuilder result = new StringBuilder();

        try {
            JSONArray candidates = responseJson.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");

                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.getJSONObject(i);
                    if (part.has("text")) {
                        result.append(part.getString("text"));
                    }
                }
            }
        } catch (JSONException e) {
            log.error("Error parsing Gemini API response", e);
            throw e;
        }

        return result.length() > 0 ? result.toString() : "No content generated.";
    }

    /**
     * Generate a fallback report when the API is unavailable.
     *
     * @param prompt Original prompt
     * @return Simple fallback report
     */
    private String generateFallbackReport(String prompt) {
        StringBuilder report = new StringBuilder();
        report.append("# Daily Summary\n\n");

        String[] lines = prompt.split("\n");
        String currentCategory = null;

        report.append("## Overview\n");
        report.append("Today's summary contains your recent reads. Here are the highlights.\n\n");

        for (String line : lines) {
            if (line.startsWith("Category: ")) {
                currentCategory = line.substring("Category: ".length()).trim();
                report.append("\n## ").append(currentCategory).append("\n\n");
            } else if (line.startsWith("- [Weight:") && currentCategory != null) {
                int titleStart = line.indexOf("Title: ");
                int titleEnd = line.indexOf(", Description:");

                if (titleStart > 0 && titleEnd > titleStart) {
                    String title = line.substring(titleStart + "Title: ".length(), titleEnd).trim();
                    report.append("* ").append(title).append("\n");
                }
            }
        }

        return report.toString();
    }
}
