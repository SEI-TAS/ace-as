package edu.cmu.sei.ttg.aaiot.as.gui.models;

import javafx.beans.property.SimpleStringProperty;

/**
 * Created by sebastianecheverria on 11/9/17.
 */
public class Permission
{
    private final SimpleStringProperty clientId = new SimpleStringProperty("");
    private final SimpleStringProperty rsId = new SimpleStringProperty("");
    private final SimpleStringProperty scope = new SimpleStringProperty("");

    public Permission(String clientId, String rsId, String scope)
    {
        setClientId(clientId);
        setRsId(rsId);
        setScope(scope);
    }

    public void setClientId(String clientId)
    {
        this.clientId.set(clientId);
    }

    public String getClientId()
    {
        return this.clientId.get();
    }

    public void setRsId(String rsId)
    {
        this.rsId.set(rsId);
    }

    public String getRsId()
    {
        return rsId.get();
    }

    public void setScope(String scope)
    {
        this.scope.set(scope);
    }

    public String getScope()
    {
        return scope.get();
    }
}
