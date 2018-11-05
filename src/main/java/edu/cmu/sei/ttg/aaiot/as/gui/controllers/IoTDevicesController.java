/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Device;
import edu.cmu.sei.ttg.aaiot.as.pairing.PairingManager;
import edu.cmu.sei.ttg.aaiot.pairing.PairingResource;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Set;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class IoTDevicesController implements IPairingHandler
{
    private AuthorizationServer authorizationServer;
    private ButtonCellHandler<Device> removeButtonHandler;

    @FXML private TableView<Device> devicesTableView;
    @FXML private GridPane gridPane;
    @FXML private TextField deviceIpTextField;
    @FXML private TextField devicePortTextField;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        devicePortTextField.setText(String.valueOf(PairingResource.PAIRING_PORT));

        // Setup the remove action.
        int actionsColumnIndex = 2;
        removeButtonHandler = new ButtonCellHandler<>("Un-pair", devicesTableView, actionsColumnIndex,
                selectedDevice ->
                {
                    authorizationServer.removeResourceServer(((Device) selectedDevice).getId());
                    devicesTableView.getItems().remove(selectedDevice);
                });

        fillDevicesTable();
    }

    /**
     * Updates the data in the table with the current data in the DB.
     * @throws Exception
     */
    private void fillDevicesTable()
    {
        System.out.println("Updating devices table.");
        try
        {
            ObservableList<Device> devicesTableData = devicesTableView.getItems();
            devicesTableData.clear();
            Set<String> rsSet = authorizationServer.getResourceServers();
            if (rsSet != null)
            {
                for (String rsId : rsSet)
                {
                    String scopes = String.join(" ", authorizationServer.getScopes(rsId));
                    devicesTableData.add(new Device(rsId, scopes));
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error updating devices data in view: " + e.toString());
        }
    }

    /**
     * Action to start pairing with default key.
     * @param actionEvent
     */
    public void pairWithDefaultKey(ActionEvent actionEvent)
    {
        pairIoTDevice(Application.DEFAULT_DEVICE_PAIRING_KEY);
        fillDevicesTable();
    }

    /**
     * Starts the webcam to get a QR code.
     * @param actionEvent
     */
    public void startCamera(ActionEvent actionEvent)
    {
        try
        {
            // Load stage and send client ID to controller.
            Stage currStage = (Stage) gridPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QRCapture.fxml"));
            Parent pane = loader.load();

            // Set up the IP address and port of the device.
            QRCaptureController controller = loader.getController();
            controller.setPairingHandler(this);

            // Set up the window.
            Stage dialog = new Stage();
            dialog.setTitle("Scanning QR Code");
            dialog.setScene(new Scene(pane, 500, 500));
            dialog.initOwner(currStage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setOnCloseRequest(e -> {controller.cleanup(); fillDevicesTable();});
            dialog.showAndWait();
        }
        catch(Exception e)
        {
            System.out.println("Error initializing the camera: " + e.toString());
        }
    }

    /**
     * Code to actually perform the pairing procedure with a client.
     */
    public boolean pairIoTDevice(byte[] pskBytes)
    {
        System.out.println("Started pairing");

        String deviceIpAddress = deviceIpTextField.getText();
        int devicePort = Integer.parseInt(devicePortTextField.getText());

        try
        {
            AuthorizationServer authorizationServer = Application.getInstance().getAuthorizationServer();
            String asId = authorizationServer.getAsId();
            PairingManager pairingManager = new PairingManager(authorizationServer);
            boolean success = pairingManager.pair(asId, pskBytes, deviceIpAddress, devicePort);
            if(success)
            {
                Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "Paired completed successfully.").showAndWait());
                System.out.println("Finished pairing");
            }
            else
            {
                Platform.runLater(() -> { new Alert(Alert.AlertType.WARNING, "Pairing was aborted since device did not respond.").showAndWait();});
            }

            return success;
        }
        catch(Exception e)
        {
            System.out.println("Error pairing: " + e.toString());
            e.printStackTrace();
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error during pairing: " + e.toString()).showAndWait());
            return false;
        }
    }

}
