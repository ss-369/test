package com.sismics.reader.core.service;

import com.sismics.reader.core.model.FeedUpdate;
import com.sismics.reader.core.model.jpa.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub service for retrieving feed updates for a given user.
 * In a real system, this would query the database.
 */
public class FeedUpdateService {

    /**
     * Returns a list of FeedUpdate objects for the given user.
     * @param user The user whose updates are to be fetched.
     * @return List of FeedUpdate instances.
     */
    public static List<FeedUpdate> getUpdatesForUser(User user) {
        // For demonstration purposes, return a dummy list.
        List<FeedUpdate> updates = new ArrayList<>();
        updates.add(new FeedUpdate("Breaking News: AI Advances", 1));
        updates.add(new FeedUpdate("Tech Trends: Cloud Computing", 2));
        updates.add(new FeedUpdate("Sports Update: Championship Results", 1));
        updates.add(new FeedUpdate("Daily Digest: Market Summary", 3));
        return updates;
    }
}
