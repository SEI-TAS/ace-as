package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Token;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.Map;
import java.util.Set;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class TokensController
{
    private AuthorizationServer authorizationServer;
    @FXML
    private TableView<Token> tokensTableView;
    private ButtonCellHandler<Token> removeButtonHandler;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        // Set up the actions column.
        int actionsColumnIndex = 3;
        removeButtonHandler = new ButtonCellHandler<>("Revoke", tokensTableView, actionsColumnIndex,
                selectedToken ->
                {
                    authorizationServer.revokeToken(((Token) selectedToken).getId());
                    tokensTableView.getItems().remove(selectedToken);
                });

        fillTable();
    }

    /**
     * Updates the data in the table with the current data in the DB.
     * @throws Exception
     */
    public void fillTable() throws Exception
    {
        ObservableList<Token> tokensTableData = tokensTableView.getItems();
        tokensTableData.clear();
        Map<String, Set<String>> tokens = authorizationServer.getAllTokensByRS();
        if(tokens != null)
        {
            for (String rsId : tokens.keySet())
            {
                Set<String> rsTokens = tokens.get(rsId);
                for(String tokenId : rsTokens)
                {
                    tokensTableData.add(new Token(tokenId, authorizationServer.getClientForCti(tokenId), rsId));
                }
            }
        }
    }
}
