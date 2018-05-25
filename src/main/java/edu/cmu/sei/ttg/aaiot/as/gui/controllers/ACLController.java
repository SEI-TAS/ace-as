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

import edu.cmu.sei.ttg.aaiot.as.Application;
import edu.cmu.sei.ttg.aaiot.as.AuthorizationServer;
import edu.cmu.sei.ttg.aaiot.as.gui.ButtonCellHandler;
import edu.cmu.sei.ttg.aaiot.as.gui.models.Permission;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;

import java.util.Map;
import java.util.Set;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class ACLController
{
    private String clientId = "";
    private AuthorizationServer authorizationServer;
    private ButtonCellHandler<Permission> removeButtonHandler;

    @FXML
    private TableView<Permission> permissionsTableView;
    @FXML
    private ComboBox<String> rsComboBox;
    @FXML
    private ComboBox<String> scopeComboBox;

    /**
     * Sets up the view.
     * @throws Exception
     */
    @FXML
    public void initialize() throws Exception
    {
        authorizationServer = Application.getInstance().getAuthorizationServer();

        Set<String> rsIds = authorizationServer.getResourceServers();
        ObservableList<String> rsOptions = FXCollections.observableArrayList(rsIds);
        rsComboBox.setItems(rsOptions);

        rsComboBox.valueProperty().addListener((observableValue, originalValue, newValue) ->
        {
            try
            {
                updateScopesComboBox();
            }
            catch(Exception e)
            {
                System.out.println("Error loading scopes: " + e.toString());
                e.printStackTrace();
            }
        });
    }

    public void setupClientSpecificOptions(String clientId) throws Exception
    {
        setClientId(clientId);
        setRemoveButton();
        fillPermissionsTable();
    }

    private void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    private void setRemoveButton() throws Exception
    {
        // Setup the remove action.
        int actionsColumnIndex = 2;
        removeButtonHandler = new ButtonCellHandler<>("Remove", permissionsTableView, actionsColumnIndex,
                selectedPermission ->
                {
                    Permission currPermision = (Permission) selectedPermission;
                    authorizationServer.removeRule(clientId, currPermision.getRsId(), currPermision.getScope());
                    permissionsTableView.getItems().remove(selectedPermission);
                });
    }

    /**
     * Updates the scopes combo box with the scopes associated to the currently selected RS.
     * @throws Exception
     */
    private void updateScopesComboBox() throws Exception
    {
        String selectedRS = rsComboBox.getSelectionModel().getSelectedItem();
        if(selectedRS != null && selectedRS != "")
        {
            Set<String> scopes = authorizationServer.getScopes(selectedRS);
            ObservableList<String> scopeOptions = FXCollections.observableArrayList(scopes);
            scopeComboBox.setItems(scopeOptions);
        }
    }

    /**
     * Loads all data from DB into the view.
     * @throws Exception
     */
    public void fillPermissionsTable() throws Exception
    {
        ObservableList<Permission> tableData = permissionsTableView.getItems();
        tableData.clear();
        Map<String, Set<String>> rules = authorizationServer.getRules(clientId);
        if(rules != null)
        {
            for (String rsId : rules.keySet())
            {
                Set<String> rsRules = rules.get(rsId);
                for(String scope : rsRules)
                {
                    tableData.add(new Permission(clientId, rsId, scope));
                }
            }
        }
    }

    /**
     * Adds permission for a certain client to access an RS and a scope.
     * @param event
     */
    public void addPermission(ActionEvent event)
    {
        try
        {
            String rsId = rsComboBox.getSelectionModel().getSelectedItem();
            String scope = scopeComboBox.getSelectionModel().getSelectedItem();
            if (rsId != null && rsId != "" && scope != null && scope != "")
            {
                authorizationServer.addRule(clientId, rsId, scope);
                fillPermissionsTable();
            }
        }
        catch (Exception e)
        {
            System.out.println("Error adding permission: " + e.toString());
            e.printStackTrace();
        }
    }
}
