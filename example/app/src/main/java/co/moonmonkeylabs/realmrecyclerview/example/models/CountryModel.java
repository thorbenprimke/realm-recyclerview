package co.moonmonkeylabs.realmrecyclerview.example.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * The name model contains a name and a unique id.
 */
public class CountryModel extends RealmObject {

    @PrimaryKey
    private long id;
    private String name;

    public CountryModel() {
    }

    public CountryModel(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
