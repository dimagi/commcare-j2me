package org.javarosa.demo.activity.remoteformlist;

import org.javarosa.core.services.locale.Localization;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.List;

public class JRDemoRemoteFormListView extends List {

    public final Command CMD_BACK = new Command(Localization.get("polish.command.back"), Command.BACK, 1);

    public JRDemoRemoteFormListView (String title, Hashtable remoteForms, boolean admin) {
        super(title, List.IMPLICIT);

        Enumeration e = remoteForms.keys();
        String key ;
        while(e.hasMoreElements())
        {
            key = (String) e.nextElement();
            append(key, null);
        }
        addCommand(CMD_BACK);
    }

}
