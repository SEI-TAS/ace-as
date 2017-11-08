package edu.cmu.sei.ttg.aaiot.as.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/7/17.
 */
public class Client
{
    private final SimpleStringProperty id = new SimpleStringProperty("");

    public Client(String id)
    {
        setId(id);
    }

    public void setId(String id)
    {
        this.id.set(id);
    }

    public String getId()
    {
        return id.get();
    }

}
