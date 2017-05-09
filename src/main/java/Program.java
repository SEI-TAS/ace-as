/**
 * Created by Sebastian on 2017-03-10.
 */

public class Program {
    public static void main(String[] args)
    {
        try {
            AuthorizationServer as = new AuthorizationServer();
            as.createDB();
            as.connectToDB();
            as.storeClientsAndResourceServers();
            as.start();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }
}
