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
