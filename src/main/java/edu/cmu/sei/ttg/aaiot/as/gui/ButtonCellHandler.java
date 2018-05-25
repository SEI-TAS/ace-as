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

package edu.cmu.sei.ttg.aaiot.as.gui;

import edu.cmu.sei.ttg.aaiot.as.gui.controllers.IActionHandler;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public class ButtonCellHandler<T>
{
    private TableView<T> tableView;
    private IActionHandler actionHandler;

    public ButtonCellHandler(String buttonText, TableView<T> tableView, int actionsColumnIndex, IActionHandler actionHandler) throws Exception
    {
        this.tableView = tableView;
        this.actionHandler = actionHandler;

        // Set up the actions column.
        TableColumn actionColumn = tableView.getColumns().get(actionsColumnIndex);
        actionColumn.setCellFactory(p -> new ButtonCell(buttonText));
        actionColumn.setStyle( "-fx-alignment: CENTER;");
    }

    /**
     * Used to execute the actual action.
     * @param actionEvent
     */
    public void executeAction(ActionEvent actionEvent)
    {
        ObservableList<T> tableData = tableView.getItems();
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        T selectedItem = tableData.get(selectedIndex);

        try
        {
            actionHandler.handleAction(selectedItem);
        }
        catch(Exception e)
        {
            System.out.println("Error executing action: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * The class that models a cell with a button.
     */
    class ButtonCell<T, S> extends TableCell<T, S>
    {
        final Button actionButton;

        public ButtonCell(String buttonText)
        {
            actionButton = new Button(buttonText);
            actionButton.setOnAction(actionEvent ->
            {
                // Select current row so that handler can identify the row.
                ButtonCell.this.getTableView().getSelectionModel().select(ButtonCell.this.getTableRow().getIndex());
                ButtonCellHandler.this.executeAction(actionEvent);
            });
        }

        @Override
        protected void updateItem(S item, boolean empty)
        {
            // Show button only for non-empty rows.
            super.updateItem(item, empty);
            if(!empty)
            {
                setGraphic(actionButton);
            }
            else
            {
                setGraphic(null);
            }
        }
    }
}
