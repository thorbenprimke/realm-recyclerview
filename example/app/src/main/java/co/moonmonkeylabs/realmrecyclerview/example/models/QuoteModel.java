package co.moonmonkeylabs.realmrecyclerview.example.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * The quote model contains a quote and a unique id.
 */
public class QuoteModel extends RealmObject {

    @PrimaryKey
    private long id;
    private String quote;
    private Date date;

    public QuoteModel() {
        date = new Date();
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
