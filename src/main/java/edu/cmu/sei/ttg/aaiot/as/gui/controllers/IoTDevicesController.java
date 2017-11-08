package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Device;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.Set;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class IoTDevicesController
{
    private AuthorizationServer authorizationServer;
    @FXML
    private TableView<Device> devicesTableView;
    private ButtonCellHandler<Device> removeButtonHandler;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        // Setup the remove action.
        int actionsColumnIndex = 2;
        removeButtonHandler = new ButtonCellHandler<>("Un-pair", devicesTableView, actionsColumnIndex,
                selectedDevice ->
                {
                    authorizationServer.removeClient(((Device) selectedDevice).getId());
                    devicesTableView.getItems().remove(selectedDevice);
                });

        fillDevicesTable();
    }

    /**
     * Updates the data in the table with the current data in the DB.
     * @throws Exception
     */
    private void fillDevicesTable() throws Exception
    {
        ObservableList<Device> devicesTableData = devicesTableView.getItems();
        devicesTableData.clear();
        Set<String> rsSet = authorizationServer.getResourceServers();
        if(rsSet != null)
        {
            for (String rsId : rsSet)
            {
                String scopes = "";
                devicesTableData.add(new Device(rsId, scopes));
            }
        }
    }

    /**
     * Code to actually perform the pairing procedure with a client.
     * @param actionEvent The event info param.
     */
    public void pairIoTDevice(ActionEvent actionEvent)
    {
        System.out.println("Started pairing");

        try
        {
            String asId = authorizationServer.getAsId();
            PairingManager pairingManager = new PairingManager(authorizationServer);
            pairingManager.pair(asId, Application.DEFAULT_DEVICE_PAIRING_KEY, Application.DEFAULT_DEVICE_IP);
            fillDevicesTable();
        }
        catch(Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
        }

        System.out.println("Finished pairing");
    }
}
