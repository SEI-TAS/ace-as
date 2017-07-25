package edu.cmu.sei.ttg.aaiot.as.pairing;

import java.util.Set;

/**
 * Created by sebastianecheverria on 7/25/17.
 */
public interface ICredentialsStore
{
    boolean storeClient(String id, byte[] psk);
    boolean storeRS(String id, byte[] psk, Set<String> scopes);
}
