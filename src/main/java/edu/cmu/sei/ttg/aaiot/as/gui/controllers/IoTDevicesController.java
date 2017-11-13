package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Device;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Set;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class IoTDevicesController
{
    private AuthorizationServer authorizationServer;
    private ButtonCellHandler<Device> removeButtonHandler;

    @FXML private TableView<Device> devicesTableView;
    @FXML private GridPane gridPane;
    @FXML private TextField deviceIpTextField;

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

            // Set up the IP address of the device.
            String deviceIpAddress = deviceIpTextField.getText();
            QRCaptureController controller = loader.getController();
            controller.setIpAddress(deviceIpAddress);

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
}
