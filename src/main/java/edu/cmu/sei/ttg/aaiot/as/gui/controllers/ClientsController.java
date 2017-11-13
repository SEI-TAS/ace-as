package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Client;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Set;

/**
 * Created by sebastianecheverria on 11/2/17.
 */
public class ClientsController
{
    private AuthorizationServer authorizationServer;
    @FXML private TableView<Client> clientsTableView;
    @FXML private GridPane gridPane;
    @FXML private TextField clientIpTextField;
    private ButtonCellHandler<Client> removeButtonHandler;
    private ButtonCellHandler<Client> aclWindowButtonHandler;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        // Setup the remove action.
        int removeColumnIndex = 1;
        removeButtonHandler = new ButtonCellHandler<>("Un-pair", clientsTableView, removeColumnIndex,
                selectedClient ->
                    {
                        authorizationServer.removeClient(((Client) selectedClient).getId());
                        clientsTableView.getItems().remove(selectedClient);
                    });

        // Set up action to open the window to manage access rules for the client.
        int aclWindowColumnIndex = 2;
        aclWindowButtonHandler = new ButtonCellHandler<>("Permissions", clientsTableView, aclWindowColumnIndex,
                selectedClient ->
                {
                    // Load stage and send client ID to controller.
                    Stage currStage = (Stage) gridPane.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ACL.fxml"));
                    GridPane pane = loader.load();
                    ACLController controller = loader.getController();
                    controller.setupClientSpecificOptions(((Client) selectedClient).getId());

                    // Set up the window.
                    Stage dialog = new Stage();
                    dialog.setTitle("Permissions for Client " + ((Client) selectedClient).getId());
                    dialog.setScene(new Scene(pane, 450, 450));
                    dialog.initOwner(currStage);
                    dialog.initModality(Modality.APPLICATION_MODAL);
                    dialog.showAndWait();
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
            String clientIpAddress = clientIpTextField.getText();
            String asId = authorizationServer.getAsId();
            PairingManager pairingManager = new PairingManager(authorizationServer);
            boolean success = pairingManager.pair(asId, Application.CLIENT_PAIRING_KEY, clientIpAddress);
            if(success)
            {
                fillClientsTable();
                new Alert(Alert.AlertType.INFORMATION, "Paired completed successfully.").showAndWait();
                System.out.println("Finished pairing");
            }
            else
            {
                new Alert(Alert.AlertType.WARNING, "Pairing was aborted since client did not respond.").showAndWait();
            }
        }
        catch(Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
            new Alert(Alert.AlertType.ERROR, "Error during pairing: " + e.toString()).showAndWait();
        }
    }
}
