package ro.cs.tao.services.query.beans;

/**
 * Represents a filter for querying EOProducts based on category, collection, and provider.
 */
public class Filter {
    private final String category;
    private final String collection;
    private final String provider;

    public Filter(String category, String collection, String provider) {
        this.category = category;
        this.collection = collection;
        this.provider = provider;
    }

    public String getCategory() {
        return category;
    }

    public String getCollection() {
        return collection;
    }

    public String getProvider() {
        return provider;
    }
}
