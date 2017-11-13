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
