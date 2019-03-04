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

package edu.cmu.sei.ttg.aaiot.as;

import COSE.*;
import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.as.pairing.ICredentialsStore;
import edu.cmu.sei.ttg.aaiot.config.Config;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.Constants;
import se.sics.ace.TimeProvider;
import se.sics.ace.as.AccessTokenFactory;
import se.sics.ace.as.PDP;
import se.sics.ace.coap.as.CoapDBConnector;
import se.sics.ace.coap.as.DtlsAS;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.PostgreSQLDBAdapter;

import java.io.IOException;
import java.security.Security;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Sebastian on 2017-05-09.
 */
public class AuthorizationServer implements ICredentialsStore
{
    private static final String AAIOT_ACE_DB_NAME = "aaiotacedb";

    private String asId;
    private long tokenDurationInMs;

    private Set<String> supportedProfiles = new HashSet<>();
    private Set<String> supportedKeyTypes = new HashSet<>();
    private Set<Short> supportedTokenTypes = new HashSet<>();
    private Set<COSEparams> supportedCOSEParams = new HashSet<>();

    private PostgreSQLDBAdapter dbAdapter;
    private DtlsAS coapServer = null;
    private CoapDBConnector dbCon;
    private TimeProvider timeProvider;
    private KissPDP pdp;

    public AuthorizationServer(String asId)
    {
        // Manually add BouncyCastle as sec provider so we can use AES_CCM.
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        this.asId = asId;
        supportedProfiles.add("coap_dtls");
        supportedKeyTypes.add("PSK");
        supportedTokenTypes.add(AccessTokenFactory.CWT_TYPE);
        supportedCOSEParams.add(new COSEparams(MessageTag.Encrypt0, AlgorithmID.AES_CCM_16_64_128, AlgorithmID.Direct));

        timeProvider = new KissTime();

        dbAdapter = new PostgreSQLDBAdapter();
        dbAdapter.setParams(Config.data.get("db_user"), Config.data.get("db_pwd"), AAIOT_ACE_DB_NAME, null);

        tokenDurationInMs = Long.parseLong(Config.data.get("token_duration_in_mins")) * 60 * 1000;
    }

    public String getAsId()
    {
        return asId;
    }

    public void createDB(String rootPwd) throws AceException
    {
        //dbAdapter.wipeDB(rootPwd);
        dbAdapter.createUser(rootPwd);
        dbAdapter.createDBAndTables(rootPwd);
    }

    public void connectToDB() throws AceException, IOException, SQLException, CoseException
    {
        dbCon = CoapDBConnector.getInstance(dbAdapter);
        this.pdp = new KissPDP(dbCon);
        coapServer = new DtlsAS(asId, dbCon, pdp, timeProvider, null, "token",
                "introspect", Integer.parseInt(Config.data.get("local_coaps_port")), null, true);
    }

    public Set<String> getClients() throws AceException
    {
        return dbCon.getClients();
    }

    public Set<String> getResourceServers() throws AceException
    {
        return dbCon.getRSS();
    }

    public Set<String> getScopes(String rsId) throws AceException
    {
        return dbCon.getScopes(rsId);
    }

    public Map<String, Set<String>> getRules(String clientId) throws AceException
    {
        return pdp.getAllAccess(clientId);
    }

    public void addRule(String clientId, String rsId, String scope) throws AceException
    {
        pdp.addAccess(clientId, rsId, scope);
    }

    public void removeRule(String clientId, String rsId, String scope) throws AceException
    {
        pdp.revokeAccess(clientId, rsId, scope);
    }

    // This should be the result of the pairing procedure, adding a RS along with the shared key to use with it.
    public void addResourceServer(String rsName, OneKey PSK, Set<String> scopes) throws AceException {
        Set<String> auds = new HashSet<>();
        auds.add(rsName);

        long resouceServerKnownExpiration = tokenDurationInMs;

        dbCon.addRS(rsName, supportedProfiles, scopes, auds, supportedKeyTypes, supportedTokenTypes, supportedCOSEParams,
                resouceServerKnownExpiration, PSK, PSK,null);

        // Authorize RS to introspect.
        pdp.addIntrospectAccess(rsName, PDP.IntrospectAccessLevel.ACTIVE_ONLY);
    }

    public void removeResourceServer(String rsName) throws AceException
    {
        System.out.println("Removing resource server if it was there.");
        dbCon.deleteRS(rsName);
        pdp.revokeIntrospectAccess(rsName);

        for(String clientId : getClients())
        {
            pdp.revokeAllRsAccess(clientId, rsName);
        }
    }

    // This should be the result of the pairing procedure, adding a client along with the shared key to use with it.
    public void addClient(String clientName, OneKey PSK) throws AceException
    {
        dbCon.addClient(clientName, supportedProfiles, null, null, supportedKeyTypes, PSK,
                null);

        // Authorize new client to ask for tokens and introspect.
        pdp.addTokenAccess(clientName);
        pdp.addIntrospectAccess(clientName, PDP.IntrospectAccessLevel.ACTIVE_ONLY);
    }

    public void removeClient(String clientName) throws AceException
    {
        System.out.println("Removing client if it was there.");
        dbCon.deleteClient(clientName);
        pdp.revokeTokenAccess(clientName);
        pdp.revokeIntrospectAccess(clientName);
        pdp.revokeAllAccess(clientName);
    }

    @Override
    public boolean storeClient(String id, byte[] psk)
    {
        try
        {
            removeClient(id);
            System.out.println("Adding new client " + id);
            addClient(id, createOneKeyFromBytes(psk));
            return true;
        }
        catch (Exception ex)
        {
            System.out.println("Error storing client: " + ex.toString());
            return false;
        }
    }

    @Override
    public boolean storeRS(String id, byte[] psk, Set<String> scopes)
    {
        try
        {
            removeResourceServer(id);
            System.out.println("Adding new RS " + id);
            addResourceServer(id, createOneKeyFromBytes(psk), scopes);
            return true;
        }
        catch (Exception ex)
        {
            System.out.println("Error storing RS: " + ex.toString());
            return false;
        }
    }

    public void start()
    {
        if(coapServer != null)
        {
            System.out.println("Starting AS server");
            coapServer.start();
        }
    }

    public void stop() throws Exception
    {
        if(coapServer != null)
        {
            coapServer.close();
        }
    }

    public void wipeData(String rootPwd) throws AceException
    {
        this.dbAdapter.wipeDB(rootPwd);
    }

    private OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        OneKey key = new OneKey(keyData);
        return key;
    }

    public Map<String, Set<String>> getAllTokensByRS() throws AceException
    {
        // First clear expired tokens.
        dbCon.purgeExpiredTokens(this.timeProvider.getCurrentTime());

        // Then get all token ids (cti).
        Set<String> tokenIds = new HashSet<>();
        for(String clientName : getClients())
        {
            tokenIds.addAll(dbCon.getCtis4Client(clientName));
        }

        // Then structure all token ids by resource server associated to them.
        Map<String, Set<String>> tokensByResourceServer = new HashMap<>();
        for(String tokenId : tokenIds)
        {
            Map<Short, CBORObject> claims = dbCon.getClaims(tokenId);
            CBORObject rsNameCBOR = claims.get(Constants.AUD);
            if(rsNameCBOR != null)
            {
                String rsName = rsNameCBOR.AsString();
                if (!tokensByResourceServer.containsKey(rsName))
                {
                    tokensByResourceServer.put(rsName, new HashSet<>());
                }
                tokensByResourceServer.get(rsName).add(tokenId);
            }
        }

        return tokensByResourceServer;
    }

    public String getClientForCti(String cti) throws AceException
    {
        return dbCon.getClient4Cti(cti);
    }

    public void revokeToken(String cti) throws AceException
    {
        // Remove the token from the valid ones.
        dbCon.deleteToken(cti);
    }

}
