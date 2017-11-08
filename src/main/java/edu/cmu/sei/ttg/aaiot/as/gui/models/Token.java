package edu.cmu.sei.ttg.aaiot.as.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class Token
{
    private final SimpleStringProperty id = new SimpleStringProperty("");
    private final SimpleStringProperty clientId = new SimpleStringProperty("");
    private final SimpleStringProperty rsId = new SimpleStringProperty("");

    public Token(String id, String clientId, String rsId)
    {
        setId(id);
        setClientId(clientId);
        setRsId(rsId);
    }

    public void setId(String id)
    {
        this.id.set(id);
    }

    public void setClientId(String clientId)
    {
        this.clientId.set(clientId);
    }

    public void setRsId(String rsId)
    {
        this.rsId.set(rsId);
    }

    public String getId()
    {
        return id.get();
    }

    public String getClientId()
    {
        return clientId.get();
    }

    public String getRsId()
    {
        return rsId.get();
    }
}
