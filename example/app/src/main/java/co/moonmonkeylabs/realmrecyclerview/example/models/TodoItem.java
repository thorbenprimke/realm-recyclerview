package co.moonmonkeylabs.realmrecyclerview.example.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * The model for a to-do item.
 */
public class TodoItem extends RealmObject {

    @PrimaryKey
    private long id;

    private String toDo;

    public TodoItem() {
    }

    public TodoItem(long id, String toDo) {
        this.id = id;
        this.toDo = toDo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getToDo() {
        return toDo;
    }

    public void setToDo(String toDo) {
        this.toDo = toDo;
    }
}
