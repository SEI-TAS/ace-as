package edu.cmu.sei.ttg.aaiot.as.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class Device
{
    private final SimpleStringProperty id = new SimpleStringProperty("");
    private final SimpleStringProperty scopes = new SimpleStringProperty("");

    public Device(String id, String scopes)
    {
        setId(id);
        setScopes(scopes);
    }

    public void setId(String id)
    {
        this.id.set(id);
    }

    public void setScopes(String scopes)
    {
        this.scopes.set(scopes);
    }

    public String getId()
    {
        return id.get();
    }

    public String getScopes()
    {
        return scopes.get();
    }
}
