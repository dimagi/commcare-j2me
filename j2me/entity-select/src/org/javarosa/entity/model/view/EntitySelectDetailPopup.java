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



import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.Container;
import de.enough.polish.ui.Form;
import de.enough.polish.ui.StringItem;

public class EntitySelectDetailPopup<E> extends Form implements HandledCommandListener {
	EntitySelectController<E> psa;
	
	int recordID;
	String[] headers;
	String[] data;
	String[] forms;
	
	Command okCmd;
	Command backCmd;
	
	Command[] phoneCallouts;
	
	public EntitySelectDetailPopup (EntitySelectController<E> psa, Entity<E> entity, EntitySet<E> set) {
		//#style entityDetailScreen
		super(Localization.get("entity.detail.title", new String[] {entity.entityType()}));
		
		this.psa = psa;
		
		recordID = entity.getRecordID();
		headers = entity.getHeaders(true);
		data = entity.getLongFields(set.get(recordID));
		forms = entity.getLongForms(false);
		
		phoneCallouts = new Command[data.length];
		
		okCmd = new Command(Localization.get("command.ok"), Command.OK, 1);
		backCmd = new Command(Localization.get("command.back"), Command.BACK, 1);
		addCommand(okCmd);
		addCommand(backCmd);	
		setCommandListener(this);
		
		loadData();
	}
	
	public void loadData() {
		for (int i = 0; i < data.length; i++) {
			
			if("".equals(data[i])) {
				continue;
			}
			
			//#style patselDetailContainer
			Container c = new Container(false);
			
			//#style patselDetailLabel
			StringItem titleItem = new StringItem("", headers[i]);
			c.add(titleItem);
			
			//#style patselDetailData
			StringItem dataItem = new StringItem("", data[i]);
			c.add(dataItem);
			
			this.append(c);
			
			if("phone".equals(forms[i])) {
				phoneCallouts[i] = new Command("Call " + headers[i], Command.SCREEN, 3);
				if(data[i] != "") {
					this.addCommand(phoneCallouts[i]);
				}
			}
		}
	}
	
	public void show () {
		psa.setView(this);
	}

	public void commandAction(Command c, Displayable d) {
		CrashHandler.commandAction(this, c, d);
	}  

	public void _commandAction(Command cmd, Displayable d) {
		if (d == this) {
			if (cmd == okCmd) {
				psa.entityChosen(recordID);
			} else if (cmd == backCmd) {
				psa.showList();
			} else {
				for(int i = 0 ; i < phoneCallouts.length ; ++i) {
					if(cmd.equals(phoneCallouts[i])) {
						psa.attemptCallout(data[i]);
					}
				}
			}
		}		
	}
	
	//exception wrapping is delegated to commandAction
	public boolean handleKeyReleased (int keyCode, int gameAction) {
		boolean ret = super.handleKeyReleased(keyCode, gameAction);
		
		if (gameAction == Canvas.FIRE) {
			commandAction(okCmd, this);
			return true;
		}
		
		return ret;
	}
}
