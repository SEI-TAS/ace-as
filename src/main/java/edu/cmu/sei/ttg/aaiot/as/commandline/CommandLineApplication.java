package edu.cmu.sei.ttg.aaiot.as.commandline;

import edu.cmu.sei.ttg.aaiot.as.Application;

/**
 * Created by Sebastian on 2017-03-10.
 */

public class CommandLineApplication
{
    public static void main(String[] args)
    {
        try
        {
            CommandLineUI commandLineUI = new CommandLineUI();
            commandLineUI.run(Application.getInstance().getAuthorizationServer());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

}
