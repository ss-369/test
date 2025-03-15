package com.sismics.reader.core.service;

import com.sismics.reader.core.model.FeedUpdate;
import com.sismics.reader.core.model.jpa.User;
import java.util.List;
import java.util.Map;

/**
 * Abstract daily report generator using the Template Method design pattern.
 * It defines the steps to generate a daily summary report.
 */
public abstract class DailyReportGenerator {

    /**
     * Template method to generate a daily report for a given user.
     * It fetches updates, applies weighting, and then formats the summary.
     */
    public final String generateDailyReport(User user) {
        List<FeedUpdate> updates = fetchLatestUpdates(user);
        Map<FeedUpdate, Double> weightedUpdates = applyWeighting(updates);
        return formatSummary(weightedUpdates);
    }

    /**
     * Fetch the latest feed updates for the user.
     * @param user The user for whom updates are fetched.
     * @return List of FeedUpdate objects.
     */
    protected abstract List<FeedUpdate> fetchLatestUpdates(User user);

    /**
     * Apply weighting based on category depth.
     * Top-level categories (depth == 1) receive higher weight.
     * @param updates The list of feed updates.
     * @return Map with each FeedUpdate and its computed weight.
     */
    protected abstract Map<FeedUpdate, Double> applyWeighting(List<FeedUpdate> updates);

    /**
     * Format the weighted updates into a human-readable summary.
     * This method should call the Gemini API to generate a natural language summary.
     * @param weightedUpdates Map of FeedUpdate to its weight.
     * @return A formatted summary string.
     */
    protected abstract String formatSummary(Map<FeedUpdate, Double> weightedUpdates);
}
