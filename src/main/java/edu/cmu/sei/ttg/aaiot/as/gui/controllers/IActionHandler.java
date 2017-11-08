package edu.cmu.sei.ttg.aaiot.as.gui.controllers;

/**
 * Created by sebastianecheverria on 11/8/17.
 */
public interface IActionHandler<T>
{
    void handleAction(T item) throws Exception;
}
