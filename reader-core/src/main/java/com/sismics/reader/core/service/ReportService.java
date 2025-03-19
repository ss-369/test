package com.sismics.reader.core.service;

import com.google.common.collect.Lists;
import com.sismics.reader.core.dao.jpa.CategoryDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.util.ConfigUtil;
import com.sismics.reader.core.util.ai.GeminiClient;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Report service.
 * Uses strategy pattern for different report generation strategies.
 * 
 */
public class ReportService {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    /**
     * User article DAO.
     */
    private UserArticleDao userArticleDao;

    /**
     * Category DAO.
     */
    private CategoryDao categoryDao;

    /**
     * Strategy interface for report generation.
     */
    public interface ReportStrategy {
        String generateReport(List<UserArticleDto> articles, Map<String, Integer> categoryWeights);
    }

    /**
     * Daily report strategy implementation using AI.
     */
    public class DailyReportStrategy implements ReportStrategy {
        private GeminiClient geminiClient;

        public DailyReportStrategy(String apiKey) {
            this.geminiClient = new GeminiClient(apiKey);
        }

        @Override
        public String generateReport(List<UserArticleDto> articles, Map<String, Integer> categoryWeights) {
            if (articles.isEmpty()) {
                return "# Daily Summary\n\nNo recent articles to summarize.";
            }

            // Group articles by feed subscription id
            Map<String, List<UserArticleDto>> articlesByFeed = new HashMap<>();
            Map<String, String> feedTitles = new HashMap<>();

            for (UserArticleDto article : articles) {
                String feedId = article.getFeedSubscriptionId();
                // Use feed title if available, otherwise "Uncategorized"
                String feedTitle = (article.getFeedTitle() != null && !article.getFeedTitle().isEmpty())
                        ? article.getFeedTitle()
                        : "Uncategorized";

                if (!articlesByFeed.containsKey(feedId)) {
                    articlesByFeed.put(feedId, new ArrayList<>());
                    feedTitles.put(feedId, feedTitle);
                }
                articlesByFeed.get(feedId).add(article);
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("Generate a concise and informative daily summary of these articles. ");
            prompt.append("Format the output in markdown with these sections:\n");
            prompt.append("1. A brief 'Overview' section at the top\n");
            prompt.append("2. Separate sections for each feed\n");
            prompt.append("3. For each article, create a bullet point with a brief summary\n");
            prompt.append("4. Prioritize important content based on the weight values\n\n");
            prompt.append("Start with '# Daily Summary' as the title\n\n");
            prompt.append("Articles to summarize:\n\n");

            for (Map.Entry<String, List<UserArticleDto>> entry : articlesByFeed.entrySet()) {
                String feedId = entry.getKey();
                List<UserArticleDto> feedArticles = entry.getValue();
                String feedTitle = feedTitles.get(feedId);
                Integer weight = categoryWeights.getOrDefault(feedId, 1);

                prompt.append("Feed: ").append(feedTitle)
                        .append(" (Weight: ").append(weight).append(")\n");

                for (UserArticleDto article : feedArticles) {
                    prompt.append("- Title: ").append(article.getArticleTitle()).append("\n");
                    if (article.getArticleDescription() != null) {
                        String desc = article.getArticleDescription();
                        if (desc.length() > 150) {
                            desc = desc.substring(0, 147) + "...";
                        }
                        prompt.append("  Description: ").append(desc).append("\n");
                    }
                    prompt.append("\n");
                }
                prompt.append("\n");
            }

            try {
                return geminiClient.generateContent(prompt.toString());
            } catch (Exception e) {
                log.error("Error generating content with Gemini API", e);
                return "# Daily Summary Error\n\nFailed to generate report using AI: " + e.getMessage();
            }
        }
    }

    /**
     * Constructor.
     */
    public ReportService() {
        userArticleDao = new UserArticleDao();
        categoryDao = new CategoryDao();
    }

    /**
     * Generate daily report for a user.
     * 
     * @param userId User ID
     * @return Generated report
     */
    public String generateDailyReport(String userId) {
        try {
            // Get user articles from the last 7 days
            UserArticleCriteria criteria = new UserArticleCriteria();
            criteria.setUserId(userId);

            // Add date filter to get only recent articles (last 7 days)
            DateTime dateTime = new DateTime().minusDays(7);
            criteria.setDateMin(dateTime.toDate());

            // Include both read and unread articles for better coverage
            // criteria.setUnread(Boolean.FALSE); // Commented out to get all articles

            // Increase page size for more comprehensive summary
            PaginatedList<UserArticleDto> paginatedList = PaginatedLists.create(50, 0);

            userArticleDao.findByCriteria(paginatedList, criteria, null, null);
            List<UserArticleDto> articles = paginatedList.getResultList();

            if (articles.isEmpty()) {
                return "# Daily Summary\n\nNo recent articles found in your subscriptions. Try subscribing to more feeds or checking back later.";
            }

            // Get all categories for the user and calculate weights
            Map<String, Integer> categoryWeights = calculateCategoryWeights(userId);

            String apiKey = ConfigUtil.CONFIG_GEMINI_API_KEY;

            // Use strategy pattern to generate the report
            ReportStrategy strategy = new DailyReportStrategy(apiKey);
            return strategy.generateReport(articles, categoryWeights);
        } catch (Exception e) {
            log.error("Error generating daily report", e);
            return "# Error Generating Report\n\nFailed to generate report: " + e.getMessage();
        }
    }

    /**
     * Calculate weights for categories.
     * Top-level categories get higher weight than subcategories.
     * 
     * @param userId User ID
     * @return Map of category ID to weight
     */
    private Map<String, Integer> calculateCategoryWeights(String userId) {
        List<Category> categories = categoryDao.findAllCategory(userId);
        Map<String, Integer> weights = new HashMap<>();

        for (Category category : categories) {
            if (category.getParentId() == null) {
                // Top-level category
                weights.put(category.getId(), 3);
            } else {
                // Subcategory
                weights.put(category.getId(), 1);
            }
        }

        return weights;
    }
}
