package co.moonmonkeylabs.realmrecyclerview.example.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * The quote model contains a quote and a unique id.
 */
public class QuoteModel extends RealmObject {

    @PrimaryKey
    private long id;
    private String quote;

    public QuoteModel() {
    }

    public QuoteModel(long id, String quote) {
        this.id = id;
        this.quote = quote;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }
}
