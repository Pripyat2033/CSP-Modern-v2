package com.ben.csp.opb;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads data/csp/opb82_rules.json into {@link Opb82Rules}.
 */
public class OPB82RuleLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("csp-modern");
    private static final Identifier ID = new Identifier("csp-modern", "opb82_rules");

    private static final Opb82Rules RULES = new Opb82Rules();

    public static Opb82Rules getRules() {
        return RULES;
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<String, Opb82Rules.Article> articles = new HashMap<>();
        Map<Identifier, Resource> found = manager.findResources(
            "", id -> id.getPath().equals("opb82_rules.json"));

        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                RULES.setMetadata(
                    getString(root, "document_title"),
                    getString(root, "document_source"));

                JsonObject sections = root.getAsJsonObject("sections");
                if (sections == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> section : sections.entrySet()) {
                    JsonObject sectionObject = section.getValue().getAsJsonObject();
                    String sectionTitle = getString(sectionObject, "title");
                    JsonObject sectionArticles = sectionObject.getAsJsonObject("articles");
                    if (sectionArticles == null) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> article : sectionArticles.entrySet()) {
                        JsonObject articleObject = article.getValue().getAsJsonObject();
                        articles.put(article.getKey(), new Opb82Rules.Article(
                            article.getKey(),
                            sectionTitle,
                            getString(articleObject, "source_citation"),
                            getString(articleObject, "description"),
                            getString(articleObject, "type"),
                            getString(articleObject, "applies_to_structure"),
                            articleObject.getAsJsonObject("params")));
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load OPB-82 rules from {}", entry.getKey(), exception);
            }
        }

        RULES.setArticles(articles);
        LOGGER.info("Loaded {} OPB-82 articles", articles.size());
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }
}
