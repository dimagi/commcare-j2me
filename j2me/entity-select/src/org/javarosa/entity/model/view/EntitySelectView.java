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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.model.Entity;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledPItemStateListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.j2me.view.ProgressIndicator;

import de.enough.polish.ui.Container;
import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.ImageItem;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.Style;
import de.enough.polish.ui.TextField;

public class EntitySelectView<E> extends FramedForm implements HandledPItemStateListener, HandledCommandListener, ProgressIndicator {

    private int MAX_ROWS_ON_SCREEN = 10;

    private int SCROLL_INCREMENT = 5;

    public static final int NEW_DISALLOWED = 0;
    public static final int NEW_IN_LIST = 1;
    public static final int NEW_IN_MENU = 2;

    protected static final int INDEX_NEW = -1;

    //behavior configuration options
    public boolean wrapAround = false; //TODO: support this
    public int newMode = NEW_IN_LIST;

    private EntitySelectController<E> controller;
    private Entity<E> entityPrototype;
    private String baseTitle;

    private TextField tf;
    protected Command exitCmd;
    protected Command sortCmd;
    protected Command newCmd;

    private int firstIndex;
    protected int selectedIndex;
    private int[] sortOrder;

    private Style headerStyle;
    private Style rowStyle;

    private int progress = 0;
    private int count = 1;

    protected Vector<Integer> rowIDs; //index into data corresponding to current matches

    public EntitySelectView (EntitySelectController<E> controller, Entity<E> entityPrototype, String title, int newMode) {
        super(title);
        this.baseTitle = title;

        this.controller = controller;
        this.entityPrototype = entityPrototype;
        this.newMode = newMode;

        this.sortOrder = getDefaultSortOrder();

        tf = new TextField(Localization.get("entity.find") + " ", "", 20, TextField.ANY);

//        //#if !polish.blackberry
//        tf.setInputMode(TextField.MODE_UPPERCASE);
//        //#endif
        tf.setItemStateListener(this);

        append(Graphics.BOTTOM, tf);

        exitCmd = new Command(Localization.get("command.cancel"), Command.CANCEL, 4);
        sortCmd = new Command(Localization.get("entity.command.sort"), Command.SCREEN, 3);
        addCommand(exitCmd);
        if (this.getNumSortFields() > 1) {
            addCommand(sortCmd);
        }
        if (newMode == NEW_IN_MENU) {
            newCmd = new Command("New " + entityPrototype.entityType(), Command.SCREEN, 4);
            addCommand(newCmd);
        }
        this.setCommandListener(this);

        rowIDs = new Vector<Integer>();

        this.setScrollYOffset(0, false);
    }

    public void init () {
        selectedIndex = 0;
        firstIndex = 0;

        calculateStyles();

        estimateHeights();

        refresh();
    }

    public void refresh () {
        refresh(-1);
    }

    public void refresh (int selectedEntity) {
        if (selectedEntity == -1)
            selectedEntity = getSelectedEntity();

        getMatches(tf.getText());
        selectEntity(selectedEntity);
        refreshList();
    }

    private void calculateStyles() {
        headerStyle = genStyleFromHints(entityPrototype.getStyleHints(true));
        rowStyle = genStyleFromHints(entityPrototype.getStyleHints(false));
    }


    private void estimateHeights() {
        int screenHeight = J2MEDisplay.getScreenHeight(320);

        //TODO: we should find a sane way of computing this dynamically
        //the newer low-end phone have large-pixel screens but small dimensions, so the font size is large
        //and our 'guess' here is way off

        //This is _super_ basic based on commonly available
        //phones. We should actually wait for things to be drawn once
        //and then recalculate for real;

//        if(screenHeight >= 300) {
//            MAX_ROWS_ON_SCREEN = 10;
//        } else if(screenHeight >= 200) {
//            MAX_ROWS_ON_SCREEN = 6;
//        } else if(screenHeight >= 160) {
            MAX_ROWS_ON_SCREEN = 5;
//        } else {
//            MAX_ROWS_ON_SCREEN = 4;
//        }

        if(MAX_ROWS_ON_SCREEN > 5) {
            SCROLL_INCREMENT = 5;
        } else {
            SCROLL_INCREMENT = 4;
        }
    }

    private String[] padHints(String[] hints) {
        if(hints.length == 1) {
            String[] padded = new String[2];
            padded[0] = hints[0];
            padded[1] = "0";
            return padded;
        } else {
            return hints;
        }
    }

    private String[] padCells(String[] cells, String empty) {
        if(cells.length == 1) {
            String[] padded = new String[2];
            padded[0] = cells[0];
            padded[1] = empty;
            return padded;
        } else {
            return cells;
        }
    }

    private Style genStyleFromHints(String[] hints) {

        //polish doesn't deal with one column properly, so we need to create a second column with 0 width.
        hints = padHints(hints);

        int screenwidth = J2MEDisplay.getScreenWidth(240);
        
        // J2ME assumes that all widths are static, so remove any trailing '%' characters.
        for (int i = 0; i < hints.length; i++) {
            if (hints[i].indexOf("%") != -1) {
                hints[i] = hints[i].substring(0, hints[i].indexOf("%"));
            }
        }

        Style style = new Style();
        style.addAttribute("columns", new Integer(hints.length));

        int fullSize = 100;
        int sharedBetween = 0;
        for(String hint : hints) {
            if(hint == null) {
                sharedBetween ++;
            } else {
                fullSize -= Integer.parseInt(hint);
            }
        }

        double average = ((double)fullSize) / (double)sharedBetween;
        int averagePixels = (int)(Math.floor((average / 100.0) * screenwidth));

        String columnswidth = "";
        for(String hint : hints) {
            int width = hint == null ? averagePixels :
                (int)Math.floor((((double)Integer.parseInt(hint))/100.0) * screenwidth);
            columnswidth += width + ",";
        }
        columnswidth = columnswidth.substring(0, columnswidth.lastIndexOf(','));

        style.addAttribute("columns-width", columnswidth);
        return style;
    }

    public void show () {
        this.setActiveFrame(Graphics.BOTTOM);
        controller.setView(this);
    }

    private void getMatches (String key) {
        rowIDs = controller.search(key);
        sortRows();
        if (newMode == NEW_IN_LIST) {
            rowIDs.addElement(new Integer(INDEX_NEW));
        }
    }

    private void stepIndex (boolean increment) {
        selectedIndex += (increment ? 1 : -1);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        } else if (selectedIndex >= rowIDs.size()) {
            selectedIndex = rowIDs.size() - 1;
        }

        if (selectedIndex < firstIndex) {
            firstIndex -= SCROLL_INCREMENT;
            if (firstIndex < 0)
                firstIndex = 0;
        } else if (selectedIndex >= firstIndex + MAX_ROWS_ON_SCREEN) {
            firstIndex += SCROLL_INCREMENT;
            //don't believe i need to do any clipping in this case
        }
    }

    public int getSelectedEntity () {
        int selectedEntityID = -1;

        //save off old selected item
        if (!listIsEmpty()) {
            int rowID = rowID(selectedIndex);
            if (rowID != INDEX_NEW) {
                selectedEntityID = controller.getRecordID(rowID(selectedIndex));
            }
        }
        return selectedEntityID;
    }

    private int numMatches () {
        return rowIDs.size() - (newMode == NEW_IN_LIST ? 1 : 0);
    }

    private boolean listIsEmpty () {
        return numMatches() <= 0;
    }

    protected int rowID (int i) {
        return rowIDs.elementAt(i).intValue();
    }

    private void selectEntity (int entityID) {
        //if old selected item is in new search result, select it, else select first match
        selectedIndex = 0;
        if (entityID != -1) {
            for (int i = 0; i < rowIDs.size(); i++) {
                int rowID = rowID(i);
                if (rowID != INDEX_NEW) {
                    if (controller.getRecordID(rowID) == entityID) {
                        selectedIndex = i;
                    }
                }
            }
        }
        //position selected item in center of visible list
        firstIndex = selectedIndex - MAX_ROWS_ON_SCREEN / 2;
        if (firstIndex < 0)
            firstIndex = 0;
    }

    private void refreshList () {
        container.clear();


        this.setTitle(Localization.get("entity.title.layout", new String[] {baseTitle, String.valueOf(numMatches())}));

        //#style patselTitleRowContainer
        Container title = new Container(false);
        applyStyle(title, STYLE_TITLE);

        String[] titleData = padCells(controller.getTitleData(),"");
        for (int j = 0; j < titleData.length; j++) {
            //#style patselTitleRowText
            StringItem str = new StringItem("", titleData[j]);
            applyStyle(str, STYLE_CELL);
            title.add(str);
        }
        this.append(title);

        if (listIsEmpty()) {
            String emptyText = controller.getRawResultCount() > 0 ? Localization.get("entity.nomatch") : Localization.get("entity.nodata");

            this.append( new StringItem("", "(" + emptyText + ")"));
        }

        String[] colFormat = padCells(controller.getColumnFormat(false),null);

        for (int i = firstIndex; i < rowIDs.size() && i < firstIndex + MAX_ROWS_ON_SCREEN; i++) {
            Container row;
            int rowID = rowID(i);

            if (i == selectedIndex) {
                //#style patselSelectedRow
                row = new Container(false);
                applyStyle(row, STYLE_SELECTED);
            } else if (i % 2 == 0) {
                //#style patselEvenRow
                row = new Container(false);
                applyStyle(row, STYLE_EVEN);
            } else {
                //#style patselOddRow
                row = new Container(false);
                applyStyle(row, STYLE_ODD);
            }

            if (rowID == INDEX_NEW) {
                row.add(new StringItem("", "Add New " + entityPrototype.entityType()));
            } else {
                String[] rowData = padCells(controller.getDataFields(rowID),"");

                for (int j = 0; j < rowData.length; j++) {
                    if(colFormat[j] == null || "".equals(colFormat[j])) {
                        //#style patselCell
                        StringItem str = new StringItem("", rowData[j]);
                        applyStyle(str, STYLE_CELL);
                        row.add(str);
                    }
                    else if ("image".equals(colFormat[j])) {
                            String uri = rowData[j];
                            if(uri == null || uri.equals("")) {
                                //#style patselCell
                                StringItem str = new StringItem("", rowData[j]);
                                applyStyle(str, STYLE_CELL);
                                row.add(str);
                            } else {
                                Item img = getImageItem(uri);
                                applyStyle(img, STYLE_CELL);
                                row.add(img);
                        }
                    }
                }
            }

            append(row);
        }

        setActiveFrame(Graphics.BOTTOM);
    }

    Hashtable<String, Image> imageCache;
    private Item getImageItem(String uri) {
        if(imageCache == null) {
            imageCache = new Hashtable<String, Image>();
        }

        //TODO: Resizing. Do we have enough information for pixel dimensions?

        InputStream is = null;
        try {
            Image image;

            //See if we already have this image;
            if(imageCache.containsKey(uri)) {
                image = imageCache.get(uri);
            } else {
                //If not, try to fetch it
                is = ReferenceManager._().DeriveReference(uri).getStream();
                image = Image.createImage(is);

                if(image != null) {
                    //Store it for the rest of this view
                    imageCache.put(uri, image);
                }
            }

            //#style patselImageCell?, patselCell
            return new ImageItem("",image,ImageItem.LAYOUT_LEFT  | ImageItem.LAYOUT_VCENTER,"img");
        } catch (InvalidReferenceException e) {
            //Invalid reference is much worse than IOException, but still not sure if this is the right call.
            e.printStackTrace();
            throw new RuntimeException("Invalid reference while trying to create an image for Entity Select: " + e.getReferenceString());
        } catch (IOException e) {
            //Don't throw a trace for every one. Will be slooooooow.
        } finally {
            try {
                if( is != null) {
                    is.close();
                }
            } catch(IOException e) {
                //this is the dumbest exception that can be thrown
            }
        }

        //Just return something blank for now

        //#style patselCell
        return new StringItem("","");
    }

    private static final int STYLE_TITLE = 0;
    private static final int STYLE_CELL = 1;
    private static final int STYLE_EVEN = 2;
    private static final int STYLE_ODD = 3;
    private static final int STYLE_SELECTED = 4;

    private void applyStyle(Item i, int type) {

        if(type == STYLE_TITLE) {
            i.getStyle().addAttribute("columns",  headerStyle.getIntProperty("columns"));
            i.getStyle().addAttribute("columns-width", headerStyle.getProperty("columns-width"));
        } else {
            i.getStyle().addAttribute("columns",  rowStyle.getIntProperty("columns"));
            i.getStyle().addAttribute("columns-width", rowStyle.getProperty("columns-width"));
        }
    }

    boolean loaded = false;
    //needs no exception wrapping
    protected boolean handleKeyPressed(int keyCode, int gameAction) {
        loaded = true;

        //Supress these actions, letting the propogates screws up scrolling on some platforms.
        if (gameAction == Canvas.UP && keyCode != Canvas.KEY_NUM2) {
            return true;
        } else if (gameAction == Canvas.DOWN && keyCode != Canvas.KEY_NUM8) {
            return true;
        }
        return super.handleKeyPressed(keyCode, gameAction);
    }

    /* (non-Javadoc)
     * @see de.enough.polish.ui.Screen#hideNotify()
     */
    public void hideNotify() {
        super.hideNotify();
        loaded = false;
    }


    protected boolean handleKeyReleased(int keyCode, int gameAction) {
        //because we're manually processing these, we need to replicate some
        //of the structure we'd normally use for dealing with down/up events
        //across native input/etc.
        if(loaded) {
        try {
            if (gameAction == Canvas.UP && keyCode != Canvas.KEY_NUM2) {
                stepIndex(false);
                refreshList();
                return true;
            } else if (gameAction == Canvas.DOWN && keyCode != Canvas.KEY_NUM8) {
                stepIndex(true);
                refreshList();
                return true;
            } else if (gameAction == Canvas.FIRE && keyCode != Canvas.KEY_NUM5) {
                processSelect();
                return true;
            }

        } catch (Exception e) {
            Logger.die("gui-keyup", e);
        }
        }

        return super.handleKeyReleased(keyCode, gameAction);
    }

    protected void processSelect() {
        if (rowIDs.size() > 0) {
            int rowID = rowID(selectedIndex);
            if (rowID == INDEX_NEW) {
                controller.newEntity();
            } else {
                controller.itemSelected(rowID);
            }
        }
    }

    public void itemStateChanged(Item i) {
        CrashHandler.itemStateChanged(this, i);
    }

    public void _itemStateChanged(Item item) {
        if (item == tf) {
            refresh();
        }
    }

    private int getNumSortFields () {
        int[] fields = entityPrototype.getSortFields();
        return (fields == null ? 0 : fields.length);
    }

    public void changeSort (int[] sortOrder) {
        this.sortOrder = sortOrder;
        refresh();
    }

    public int[] getSortOrder () {
        return sortOrder;
    }

    //can't believe i'm writing a .. sort function
    private void sortRows () {
        count = rowIDs.size();
        if(getSortOrder().length == 0) {
            return;
        } else {
            mergeSort(rowIDs);
        }
    }

    //Start: SORT Code
      public void mergeSort(Vector<Integer> inputs) {
         if(inputs.size() == 0 ) return;
         int[] scratch = new int[inputs.size()];
         int[] inputArray = new int[inputs.size()];
         for(int i = 0 ; i < inputs.size() ; ++i) {
             inputArray[i] = inputs.elementAt(i).intValue();
         }
         mergeSortRecursiveStep(scratch, inputArray, 0, scratch.length - 1);

         for(int i = 0 ; i < inputArray.length ; ++i) {
             inputs.setElementAt(new Integer(inputArray[i]), i);
         }
      }

      private void mergeSortRecursiveStep(int[] scratch, int[] array, int low, int high) {
        if (low == high) // if range is 1,
          return; // no use sorting
        else { // find midpoint
          int mid = (low + high) / 2;
          // sort low half
          mergeSortRecursiveStep(scratch, array, low, mid);
          // sort high half
          mergeSortRecursiveStep(scratch, array, mid + 1, high);
          // merge them
          merge(scratch, array, low, mid + 1, high);
        }
      }

      private void merge(int[] scratch, int[] array, int lowPtr, int highPtr, int upperBound) {
        int j = 0; // workspace index
        int lowerBound = lowPtr;
        int mid = highPtr - 1;
        int n = upperBound - lowerBound + 1; // # of items

        while (lowPtr <= mid && highPtr <= upperBound)
          //if (array[lowPtr] < array[highPtr]) {
          if(compare(controller.getEntity(array[lowPtr]), controller.getEntity(array[highPtr])) < 0) {
            scratch[j++] = array[lowPtr++];
          }
          else {
            scratch[j++] = array[highPtr++];
          }

        while (lowPtr <= mid) {
          scratch[j++] = array[lowPtr++];
        }

        while (highPtr <= upperBound) {
          scratch[j++] = array[highPtr++];
        }

        for (j = 0; j < n; j++) {
          array[lowerBound + j] = scratch[j];
        }
      }

    //END: Sort code

    private int compare (Entity<E> eA, Entity<E> eB) {

        for(int i = 0 ; i < sortOrder.length ; ++i) {
            Object valA = eA.getSortKey(sortOrder[i]);
            Object valB = eB.getSortKey(sortOrder[i]);

            int cmp = compareVal(valA, valB) * (entityPrototype.isSortAscending(sortOrder[i]) ? 1 : -1);
            if(cmp != 0 ) { return cmp; }
        }
        return 0;
    }

    private int compareVal (Object valA, Object valB) {
        if (valA == null && valB == null) {
            return 0;
        } else if (valA == null) {
            return 1;
        } else if (valB == null) {
            return -1;
        }

        if (valA instanceof Integer) {
            return compareInt(((Integer)valA).intValue(), ((Integer)valB).intValue());
        } else if (valA instanceof Long) {
            return compareInt(((Long)valA).longValue(), ((Long)valB).longValue());
        } else if (valA instanceof Double) {
            return compareFloat(((Double)valA).doubleValue(), ((Double)valB).doubleValue());
        } else if (valA instanceof String) {
            return compareStr((String)valA, (String)valB);
        } else if (valA instanceof Date) {
            return compareInt((int)DateUtils.daysSinceEpoch((Date)valA), (int)DateUtils.daysSinceEpoch((Date)valB));
        } else if (valA instanceof Object[]) {
            Object[] arrA = (Object[])valA;
            Object[] arrB = (Object[])valB;

            for (int i = 0; i < arrA.length && i < arrB.length; i++) {
                int cmp = compareVal(arrA[i], arrB[i]);
                if (cmp != 0)
                    return cmp;
            }
            return compareInt(arrA.length, arrB.length);
        } else {
            throw new RuntimeException ("Don't know how to order type [" + valA.getClass().getName() + "]; only int, long, double, string, and date are supported");
        }
    }

    private int compareInt (long a, long b) {
        return (a == b ? 0 : (a < b ? -1 : 1));
    }

    /**
     * For sorting purposes, NaN is the lowest possible number.
     * @param a
     * @param b
     * @return
     */
    private int compareFloat (double a, double b) {
        if(Double.isNaN(a)) {
            if(Double.isNaN(b)) { return 0; }
            else { return -1; }
        } else if(Double.isNaN(b)) {
            return 1;
        }
        return (a == b ? 0 : (a < b ? -1 : 1));
    }

    private int compareStr (String a, String b) {
        return a.compareTo(b);
    }

    private int[] getDefaultSortOrder () {
        return entityPrototype.getDefaultSortOrder();
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command cmd, Displayable d) {
        if (d == this) {
            if (cmd == exitCmd) {
                controller.exit();
            } else if (cmd == sortCmd) {
                EntitySelectSortPopup<E> pssw = new EntitySelectSortPopup<E>(this, controller, entityPrototype);
                pssw.show();
            } else if (cmd == newCmd) {
                controller.newEntity();
            }
        }
    }

    public double getProgress() {
        if(count == 0) { return 0.0;};
        return (double)progress / count;
    }

    public String getCurrentLoadingStatus() {
        return null;
    }

    public int getIndicatorsProvided() {
        return ProgressIndicator.INDICATOR_PROGRESS;
    }


//#if polish.hasPointerEvents
//#
//#    private int selectedIndexFromScreen (int i) {
//#        return firstIndex + i;
//#    }
//#
//#    protected boolean handlePointerPressed (int x, int y) {
//#        boolean handled = false;
//#
//#        try {
//#
//#            int screenIndex = 0;
//#            for (int i = 0; i < this.container.size(); i++) {
//#                Item item = this.container.getItems()[i];
//#                if (item instanceof Container) {
//#                    if (this.container.isInItemArea(x - this.container.getAbsoluteX(), y - this.container.getAbsoluteY(), item)) {
//#                        selectedIndex = selectedIndexFromScreen(screenIndex);
//#                        refreshList();
//#                        processSelect();
//#
//#                        handled = true;
//#                        break;
//#                    }
//#
//#                    screenIndex++;
//#                }
//#            }
//#
//#        } catch (Exception e) {
//#            Logger.die("gui-ptrdown", e);
//#        }
//#
//#        if (handled) {
//#            return true;
//#        } else {
//#            return super.handlePointerPressed(x, y);
//#        }
//#    }
//#
//#endif


}
