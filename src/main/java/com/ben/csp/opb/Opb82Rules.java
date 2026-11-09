package com.ben.csp.opb;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.Map;

/**
 * Parsed form of data/csp/opb82_rules.json: a flat map of article id -> article,
 * plus the document metadata.
 */
public final class Opb82Rules {

    /**
     * A single OPB-82 article.
     *
     * @param id                 article id, e.g. "2.5.1"
     * @param sectionTitle       human-readable section title
     * @param sourceCitation     document citation
     * @param description        what the rule requires
     * @param type               rule type, e.g. "required_block_check"
     * @param appliesToStructure structure the rule applies to, or null
     * @param params             rule-specific parameters
     */
    public record Article(
            String id,
            String sectionTitle,
            String sourceCitation,
            String description,
            String type,
            String appliesToStructure,
            JsonObject params) {
    }

    private String documentTitle = "";
    private String documentSource = "";
    private Map<String, Article> articles = Collections.emptyMap();

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getDocumentSource() {
        return documentSource;
    }

    public Map<String, Article> getArticles() {
        return Collections.unmodifiableMap(articles);
    }

    public Article getArticle(String id) {
        return articles.get(id);
    }

    void setMetadata(String documentTitle, String documentSource) {
        this.documentTitle = documentTitle;
        this.documentSource = documentSource;
    }

    void setArticles(Map<String, Article> articles) {
        this.articles = articles;
    }
}
