package com.sismics.reader.core.service;

import com.sismics.reader.core.model.FeedUpdate;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.util.GeminiAPI;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Concrete implementation of DailyReportGenerator that uses the Gemini API
 * to produce a natural language summary.
 */
public class GeminiDailyReportGenerator extends DailyReportGenerator {

    @Override
    protected List<FeedUpdate> fetchLatestUpdates(User user) {
        // Use a stub service to retrieve updates for the user.
        return FeedUpdateService.getUpdatesForUser(user);
    }

    @Override
    protected Map<FeedUpdate, Double> applyWeighting(List<FeedUpdate> updates) {
        Map<FeedUpdate, Double> weighted = new HashMap<>();
        for (FeedUpdate update : updates) {
            // For demonstration: if category depth is 1, weight is 1.0; otherwise weight decreases.
            int depth = update.getCategoryDepth();
            double weight = (depth == 1) ? 1.0 : (1.0 / depth);
            weighted.put(update, weight);
        }
        return weighted;
    }

    @Override
    protected String formatSummary(Map<FeedUpdate, Double> weightedUpdates) {
        // Build a raw text summary with weighted details.
        StringBuilder rawSummary = new StringBuilder();
        for (Map.Entry<FeedUpdate, Double> entry : weightedUpdates.entrySet()) {
            rawSummary.append("Weight: ")
                      .append(entry.getValue())
                      .append(" - Update: ")
                      .append(entry.getKey().getTitle())
                      .append("\n");
        }
        // Call the Gemini API (dummy implementation here) to produce a natural language summary.
        String summary = GeminiAPI.summarize(rawSummary.toString());
        return summary;
    }
}
