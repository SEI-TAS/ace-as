package edu.cmu.sei.ttg.aaiot.as;

import se.sics.ace.AceException;
import se.sics.ace.as.DBConnector;
import se.sics.ace.as.PDP;

import java.io.IOException;
import java.util.*;

/**
 * Created by Sebastian on 2017-08-04.
 */
public class InMemoryPDP implements PDP, AutoCloseable {

    private DBConnector db = null;

    /**
     * The identifiers of the clients allowed to submit requests to /token
     */
    private Set<String> clients = new HashSet<>();

    /**
     * The identifiers of the resource servers allowed to submit requests to
     * /introspect
     */
    private Set<String> resourceServers = new HashSet<>();

    /**
     * Maps identifiers of client to a map that maps the audiences they may
     * access to the scopes they may access for these audiences.
     *
     * Note that this storage assumes that scopes are split by whitespace as
     * per the standard's specification.
     */
    private Map<String, Map<String, Set<String>>> acl = new HashMap<>();

    /**
     * @param db  the database connector
     * @return  the PDP
     * @throws AceException
     * @throws IOException
     */
    public InMemoryPDP(DBConnector db) throws AceException, IOException
    {
        this.db = db;
    }

    @Override
    public boolean canAccessToken(String clientId) {
        return this.clients.contains(clientId);
    }

    @Override
    public boolean canAccessIntrospect(String rsId) {
        return this.resourceServers.contains(rsId);
    }

    @Override
    public String canAccess(String clientId, String aud, String scope)
            throws AceException {
        Map<String,Set<String>> clientACL = this.acl.get(clientId);
        if (clientACL == null || clientACL.isEmpty()) {
            return null;
        }

        Set<String> scopes = null;
        Set<String> rss = this.db.getRSS(aud);
        if (rss == null) {
            return null;
        }
        for (String rs : rss) {
            if (scopes == null) {
                scopes = new HashSet<>();
                Set<String> bar = clientACL.get(rs);
                if (bar != null) {
                    scopes.addAll(bar);
                }
            } else {
                Set<String> remains = new HashSet<>(scopes);
                for (String foo : scopes) {
                    if (clientACL.get(rs) == null ) {
                        //The client can access nothing on this RS
                        return null;
                    }
                    if (!clientACL.get(rs).contains(foo)) {
                        remains.remove(foo);
                    }
                }
                scopes = remains;
            }
        }

        if (scopes == null || scopes.isEmpty()) {
            return null;
        }

        String scopeStr = scope;
        String[] requestedScopes = scopeStr.split(" ");
        String grantedScopes = "";
        for (int i=0; i<requestedScopes.length; i++) {
            if (scopes.contains(requestedScopes[i])) {
                if (!grantedScopes.isEmpty()) {
                    grantedScopes += " ";
                }
                grantedScopes += requestedScopes[i];
            }
        }
        //all scopes found
        if (grantedScopes.isEmpty()) {
            return null;
        }
        return grantedScopes;
    }

    @Override
    public void close() throws Exception {
        this.db.close();
    }

    public void addClient(String clientId)
    {
        if(!clients.contains(clientId))
        {
            clients.add(clientId);
        }
    }

    public void addRS(String rsId)
    {
        if(!resourceServers.contains(rsId))
        {
            resourceServers.add(rsId);
        }
    }

    public void addRule(String clientId, String rsId, String scope)
    {
        Map<String, Set<String>> permissions;
        if(acl.containsKey(clientId))
        {
            permissions = acl.get(clientId);
        }
        else
        {
            permissions = new HashMap<>();
            acl.put(clientId, permissions);
        }

        Set<String> scopes;
        if(permissions.containsKey(rsId))
        {
            scopes = permissions.get(rsId);
        }
        else
        {
            scopes = new HashSet<>();
            permissions.put(rsId, scopes);
        }

        if(!scopes.contains(scope))
        {
            scopes.add(scope);
        }
    }

    public void removeRule(String clientId, String rsId, String scope)
    {
        acl.get(clientId).get(rsId).remove(scope);
    }

    public Set<String> getClients()
    {
        return clients;
    }

    public Map<String, Set<String>> getRules(String clientId)
    {
        return acl.get(clientId);
    }
}