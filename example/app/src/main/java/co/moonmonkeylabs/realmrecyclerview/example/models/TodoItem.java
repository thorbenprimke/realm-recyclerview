package co.moonmonkeylabs.realmrecyclerview.example.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * The model for a to-do item.
 */
public class TodoItem extends RealmObject {

    @PrimaryKey
    private long id;

    private String description;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
