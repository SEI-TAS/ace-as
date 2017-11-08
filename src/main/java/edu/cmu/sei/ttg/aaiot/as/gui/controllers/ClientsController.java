package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Client;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.Set;

/**
 * Created by sebastianecheverria on 11/2/17.
 */
public class ClientsController
{
    private AuthorizationServer authorizationServer;
    @FXML private TableView<Client> clientsTableView;
    private ButtonCellHandler<Client> removeButtonHandler;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        // Setup the remove action.
        int actionsColumnIndex = 1;
        removeButtonHandler = new ButtonCellHandler<>("Un-pair", clientsTableView, actionsColumnIndex,
                selectedClient ->
                    {
                        authorizationServer.removeClient(((Client) selectedClient).getId());
                        clientsTableView.getItems().remove(selectedClient);
                    });

        fillClientsTable();
    }

    /**
     * Updates the data in the table with the current data in the DB.
     * @throws Exception
     */
    private void fillClientsTable() throws Exception
    {
        ObservableList<Client> clientsTableData = clientsTableView.getItems();
        clientsTableData.clear();
        Set<String> clientSet = authorizationServer.getClients();
        if(clientSet != null)
        {
            for (String clientId : clientSet)
            {
                clientsTableData.add(new Client(clientId));
            }
        }
    }

    /**
     * Code to actually perform the pairing procedure with a client.
     * @param actionEvent The event info param.
     */
    public void pairClient(ActionEvent actionEvent)
    {
        System.out.println("Started pairing");

        try
        {
            String asId = authorizationServer.getAsId();
            PairingManager pairingManager = new PairingManager(authorizationServer);
            pairingManager.pair(asId, Application.CLIENT_PAIRING_KEY, Application.DEFAULT_CLIENT_IP);
            fillClientsTable();
        }
        catch(Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
        }

        System.out.println("Finished pairing");
    }
}
