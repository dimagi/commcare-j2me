/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.entity.model.view;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledPItemStateListener;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import de.enough.polish.ui.Choice;
import de.enough.polish.ui.ChoiceGroup;
import de.enough.polish.ui.Form;
import de.enough.polish.ui.Item;

public class EntitySelectSortPopup<E> extends Form implements HandledCommandListener, HandledPItemStateListener {
    private EntitySelectView<E> psv;
    private EntitySelectController<E> psa;
    private Entity<E> entityPrototype;

    private ChoiceGroup sortField;
    private Command cancelCmd;

    public EntitySelectSortPopup (EntitySelectView<E> psv, EntitySelectController<E> psa, Entity<E> entityPrototype) {
        //#style patselSortPopup
        super(Localization.get("entity.sort.title"));

        this.psv = psv;
        this.psa = psa;
        this.entityPrototype = entityPrototype;

        sortField = new ChoiceGroup("", Choice.EXCLUSIVE);

        int[] sortFields = entityPrototype.getSortFields();
        int[] sorted = psv.getSortOrder();

        int toChoose = -1;
        for (int i = 0; i < sortFields.length; i++) {
            String name = entityPrototype.getSortFieldName(sortFields[i]);
            for(int j = 0 ; j < sorted.length ; ++j ){
                if (sortFields[i] == sorted[j]) {
                    if(j == 0) {
                        toChoose = i;
                    }

                    if(sorted.length > 1) {
                        name = (j+1) + ") " + name;
                    }
                }
            }
            sortField.append(name, null);
        }
        if(toChoose != -1) {
            sortField.setSelectedIndex(toChoose, true);
        }

        append(sortField);
        sortField.setItemStateListener(this);

        cancelCmd = new Command(Localization.get("polish.command.cancel"), Command.CANCEL, 1);
        addCommand(cancelCmd);
        setCommandListener(this);
    }

    public void show () {
        psa.setView(this);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command cmd, Displayable d) {
        if (d == this) {
            if (cmd == cancelCmd) {
                psa.showList();
            }
        }
    }

    public void itemStateChanged(Item i) {
        CrashHandler.itemStateChanged(this, i);
    }

    public void _itemStateChanged(Item item) {
        if (item == sortField) {
            psv.changeSort(new int[] {entityPrototype.getSortFields()[sortField.getSelectedIndex()]});
            psa.showList();
        }
    }
}
