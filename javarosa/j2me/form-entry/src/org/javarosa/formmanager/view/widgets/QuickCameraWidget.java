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

package org.javarosa.formmanager.view.widgets;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.PointerAnswerData;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.utilities.media.CameraItem;

import java.io.IOException;

import javax.microedition.media.MediaException;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.Container;
import de.enough.polish.ui.ImageItem;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.ItemCommandListener;
import de.enough.polish.ui.StringItem;

/**
 * This class represents a small widget to do an image chooser activity.  It
 * is basically a small display that does a pass through to the ImageChooser
 *
 * @author Cory Zue
 *
 */

public class QuickCameraWidget extends ExpandedWidget {

    private PointerAnswerData data;

    private StringItem label;

    private WidgetEscapeComponent wec = new WidgetEscapeComponent();

    private Container parent;

    CameraItem current;

    protected IAnswerData getWidgetValue() {
        return data;
    }


    protected void setWidgetValue(Object o) {
        IDataPointer casted = (IDataPointer) o;
        if (casted != null) {
            data = new PointerAnswerData((IDataPointer) o);
        } else {
            data = null;
        }
    }


    protected void updateWidget(FormEntryPrompt prompt) {
        // do nothing?
    }

    public int widgetType() {
        return Constants.CONTROL_IMAGE_CHOOSE;
    }

    public int getNextMode() {
        return wec.wrapNextMode(super.getNextMode());
    }

    public Item getInteractiveWidget() {
        return wec.wrapInteractiveWidget(super.getInteractiveWidget());
    }

    protected Item getEntryWidget (FormEntryPrompt prompt) {
        parent = new Container(true);
        init();
        return wec.wrapEntryWidget(parent);
    }

    protected void init() {
        parent.clear();
        if(current != null) {
            current.releaseResources();
            current = null;
        }
        if(data != null) {
            parent.add(retakeContainer(data));
        } else {
            parent.add(cameraContainer());
        }
    }

    private Container cameraContainer() {
        Container wrapper = new Container(false);
        try {
            current = new CameraItem();
            //getMultimediaController().attachVideoPlayer(ci.getPlayer());
            current.getPlayer().start();
            wrapper.add(current);


            //#style button
            StringItem retake = new StringItem(null,"Snap",Item.BUTTON);
            retake.setDefaultCommand(new Command("Snap", Command.ITEM, 1));
            retake.setItemCommandListener(new ItemCommandListener() {
                public void commandAction(Command c, Item item) {
                    try{
                        IDataPointer data = current.snap();
                        setWidgetValue(data);
                        init();
                    }
                    catch(SecurityException se){
                        se.printStackTrace();
                    }
                }
            });
            wrapper.add(retake);

        } catch (MediaException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidReferenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return wrapper;
    }


    protected Container retakeContainer(PointerAnswerData data) {
        Container c = new Container(false);
        try {
            ImageItem im = getImageItemForScreen(((IDataPointer)data.getValue()).getDataStream());
            c.add(im);
        } catch (IOException e) {
            StringItem failed = new StringItem(null, "Failed to capture image");
            c.add(failed);
        } catch(SecurityException se){
            se.printStackTrace();
        }

        //#style button
        StringItem retake = new StringItem(null,"Retake",Item.BUTTON);
        retake.setDefaultCommand(new Command("Retake", Command.ITEM, 1));
        retake.setItemCommandListener(new ItemCommandListener() {
            public void commandAction(Command c, Item item) {
                try{
                    QuickCameraWidget.this.clear();
                    init();
                }
                catch(SecurityException se){
                    se.printStackTrace();
                }
            }
        });

        c.add(retake);
        c.requestDefocus(retake);
        return c;
    }

    private void clear() {
        this.data = null;

    }

    protected IAnswerData getAnswerTemplate() {
        return new PointerAnswerData();
    }

    /* (non-Javadoc)
     * @see org.javarosa.formmanager.view.widgets.ExpandedWidget#releaseMedia()
     */
    public void releaseMedia() {
        if(current != null) {
            current.releaseResources();
        }
        super.releaseMedia();
    }

}
