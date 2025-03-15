package com.sismics.reader.core.model;

/**
 * A simple POJO representing a feed update.
 */
public class FeedUpdate {
    private String title;
    private int categoryDepth; // 1 for top-level, higher for nested subcategories

    public FeedUpdate(String title, int categoryDepth) {
        this.title = title;
        this.categoryDepth = categoryDepth;
    }

    public String getTitle() {
        return title;
    }

    public int getCategoryDepth() {
        return categoryDepth;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategoryDepth(int categoryDepth) {
        this.categoryDepth = categoryDepth;
    }
}
