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

//conditional include for polish?

package org.javarosa.formmanager.view.chatterbox;

import org.javarosa.core.api.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.helper.InvalidDataException;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.formmanager.api.FormMultimediaController;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.api.transitions.FormEntryTransitions;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.utility.SortedIndexSet;
import org.javarosa.formmanager.view.IFormEntryView;
import org.javarosa.formmanager.view.chatterbox.widget.ChatterboxWidget;
import org.javarosa.formmanager.view.chatterbox.widget.ChatterboxWidgetFactory;
import org.javarosa.formmanager.view.widgets.CollapsedWidget;
import org.javarosa.formmanager.view.widgets.GeoPointWidget;
import org.javarosa.formmanager.view.widgets.WidgetFactory;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledPCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.utilities.media.MediaUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.Container;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.UiAccess;


public class Chatterbox extends FramedForm implements HandledPCommandListener, IFormEntryView{

    private boolean USE_HASH_FOR_AUDIO = true;
    private static int POUND_KEYCODE = Canvas.KEY_POUND;
    private static final String PROMPT_REQUIRED_QUESTION = Localization.get("view.sending.RequiredQuestion");

    private static final String PROMPT_DEFAULT_CONSTRAINT_VIOL = Localization.get("form.entry.constraint.msg");

    public static int KEY_CENTER_LETS_HOPE = -5;

    public static final int UIHACK_SELECT_PRESS = 1;

    public static final int Q_NORMAL = 0;
    public static final int Q_REPEAT_JUNCTURE = 1;
    public static final int Q_REPEAT_DELETE = 2;

    private JrFormEntryController controller;
    private JrFormEntryModel model;

    private ChatterboxWidgetFactory widgetFactory;
    private boolean multiLingual;
    private SortedIndexSet questionIndexes;
    private FormIndex activeQuestionIndex;

    /** The active question's index when a key is pressed */
    private FormIndex indexWhenKeyPressed = null;

    private boolean activeIsInterstitial = false; //true if the question corresponding to activeQuestionIndex is a 'create new repeat' question
    private boolean deleteInterstitial = false;
    private Vector<FormIndex> uncommittedRepeats = new Vector<FormIndex>();
    //active repeat for deleting? //TODO figure this out

    //TODO get the progress bar working

    //GUI elements
    private Command backCommand;
    private Command exitNoSaveCommand;
    private Command exitSaveCommand;
    private Command saveCommand;
    private Command languageSubMenu;
    private Command[] languageCommands;
    private String[] localeCommandMap;
    //private Command deleteRepeatCommand; //TODO do something with this
    private Gauge progressBar;

    public Chatterbox (String formTitle, JrFormEntryController controller) {
        //#style framedForm
        super(formTitle);
        this.controller = controller;
        this.model = controller.getModel();

        if (model.isReadOnlyMode()) {
            //#style ReviewFramedForm
            UiAccess.setStyle(this);
        }

        //22 Jan, 2009 - csims@dimagi.com
        //This constructor code supresses the ability to scroll the chatterbox by dragging the mouse
        //pointer. This "Feature" was causing tons of problems for our nurses. Let me know if anyone else
        //wants to make this a customizable inclusion.
        this.container = new Container(false) {
            protected boolean handlePointerScrollReleased(int relX, int relY) {
                return false;
            }
        };

        multiLingual = (model.getForm().getLocalizer() != null);
        questionIndexes = new SortedIndexSet();
        activeQuestionIndex = FormIndex.createBeginningOfFormIndex(); //null is not allowed

        //#if device.identifier == Sony-Ericsson/P1i
        KEY_CENTER_LETS_HOPE = 13;
        //#endif

        //#if device.identifier == Sony-Ericsson/K610i
        POUND_KEYCODE = Canvas.KEY_STAR;
        //#endif

        if(FormManagerProperties.EXTRA_KEY_AUDIO_PLAYBACK.equals(controller.getExtraKeyMode())){
            USE_HASH_FOR_AUDIO = true;
        }else{
            USE_HASH_FOR_AUDIO = false;
        }
    }


    public void destroy () {
        for (int i = 0; i < size(); i++) {
            //As long as everything is collapsed, we should clear all relevant resources.
            ChatterboxWidget cw = ((ChatterboxWidget)get(i));
            cw.releaseResources();
        }
    }

    public void show () {
        //Pre-set the screen for the main container. Otherwise commands for items won't
        //show up correctly until this is shown.
        UiAccess.setItemScreen(this.container, this);
        initGUI();
        J2MEDisplay.setView(this);
    }

    public void show (FormIndex index) {
        J2MEDisplay.setView(this);
    }

    private void initGUI () {
        setUpCommands();
        initProgressBar();

        //Mode 1: Read only review screen.
        if(model.isReadOnlyMode()) {
            while(controller.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM) {
                jumpToQuestion(FormIndex.createEndOfFormIndex());
            }
        } else if (null /*model.getStartIndex()*/ != null) { //TODO: Starting from a specific question
            //Mode 2: Seek to current question
            while(!model.getFormIndex().equals(null)) {
                controller.stepToNextEvent();
                jumpToQuestion(model.getFormIndex());
            }
        } else {
            //Default Mode: Start at first question
            controller.stepToNextEvent();
            jumpToQuestion(model.getFormIndex());
        }
        this.currentlyActiveContainer = this.container;
    }

    private void setUpCommands () {
        backCommand = new Command(Localization.get("command.back"), Command.BACK, 2);
        exitNoSaveCommand = new Command(Localization.get("command.exit"), Command.EXIT, 2);
        exitSaveCommand = new Command(Localization.get("command.saveexit"), Command.SCREEN, 4);
        saveCommand = new Command(Localization.get("command.save"), Command.SCREEN, 4);

        if (multiLingual) {
            languageSubMenu = new Command(Localization.get("command.language"), Command.SCREEN, 4);
            populateLanguages();
        }

        //next command is added on a per-widget basis

        //one place for adding back command to prevent accidentally adding it in read only mode
        addBackCommand();

        if(!model.isReadOnlyMode()) {
            //CTS (4/27/2010): We don't handle these appropriately, and it does nothing but confuse
            //users when they appear and break stuff.
            //addCommand(exitSaveCommand);
            //addCommand(saveCommand);
        }

        addCommand(exitNoSaveCommand);


        if (languageSubMenu != null) {
            addCommand(languageSubMenu);
            for (int i = 0; i < languageCommands.length; i++)
                addSubCommand(languageCommands[i], languageSubMenu);
        }

        setCommandListener(this);
    }

    private void populateLanguages () {
        String[] availableLocales = model.getForm().getLocalizer().getAvailableLocales();
        languageCommands = new Command[availableLocales.length];
        localeCommandMap = new String[languageCommands.length];
        for (int i = 0; i < languageCommands.length; i++) {
            String label = availableLocales[i];
            try {
                label = Localization.get("locale.name." + label.toLowerCase());
            } catch(NoLocalizedTextException nlte) {
                //nothing. Just don't have a way to check for this yet.
            }
            languageCommands[i] = new Command(label, Command.SCREEN, 4);
            localeCommandMap[i] = availableLocales[i];
        }
    }

    private void initProgressBar () {
        //CTS: Oct 11, 2011 - Progress bar is no longer included in Sense interface mode
        //CTS: Nov 11, 2011 - Disabling this behavior in 1.3, it's causing layout issues.
        //if(controller.isEntryOptimized()) {
            //#style progressbar
            progressBar = new Gauge(null, false, model.getNumQuestions(), 0);
            append(Graphics.BOTTOM, progressBar);
        //}
    }

    private void step(int event) {
        step(event, false);
    }

    private void step(int event, boolean backwards) {
        switch(event) {
        case FormEntryController.EVENT_BEGINNING_OF_FORM:
            if(backwards) {
                //we've stepped out of the form. Bail.
                controller.abort();
            }
            break;
        case FormEntryController.EVENT_END_OF_FORM:
            formComplete();
            break;
        default:
            FormIndex index = model.getFormIndex();
            jumpToQuestion(index);
            break;
        }
    }

    //make given question active; deal with all necessary questions in between
    private void jumpToQuestion (FormIndex questionIndex) {

        boolean newRepeat = false;

        if (questionIndex.isInForm() && !model.isIndexRelevant(questionIndex)) {
            throw new IllegalStateException("Attempt to jump to irrelevant index!");
        }

        // Determine what should and shouldn't be pinned.
        updatePins(questionIndex);

        //figure out kind of reference and how to handle it
        IFormElement last = model.getForm().getChild(questionIndex);

        if (last instanceof GroupDef) {
            if (FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR != model.getRepeatStructure() && ((GroupDef)last).getRepeat() &&
                model.getForm().getInstance().resolveReference(model.getForm().getChildInstanceRef(questionIndex)) == null) {

                //We're at a repeat interstitial point. If the group has the right configuration, we are able
                //to trigger a new repeat here. Otherwise, we'll have to ask the controller to move along.

                if(((GroupDef)last).noAddRemove) {
                    //We can't show anything meaningful here. Go back to the controller.
                    boolean forwards = questionIndex.compareTo(activeQuestionIndex) > 0;
                    if(forwards) {
                        step(controller.stepToNextEvent());
                    } else {
                        step(controller.stepToPreviousEvent());
                    }
                    return;
                } else {
                    //All Systems Go. Display an interstitial "Add another FOO" question.
                    newRepeat = true;
                }
            } else if (FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR == model.getRepeatStructure() &&
                    model.getEvent() == FormEntryController.EVENT_REPEAT_JUNCTURE) {

                //show repeat juncture question here
                System.out.println("you've reached a repeat");
                newRepeat = true; //note: hijacking the current interstitial repeat question variable; should rename

            } else {
                boolean forwards = questionIndex.compareTo(activeQuestionIndex) > 0;
                if(forwards) {
                    createHeaderForElement(questionIndex, false);
                    step(controller.stepToNextEvent());
                } else {
                    removeHeaderForElement(questionIndex);
                    step(controller.stepToPreviousEvent());
                }
                return;
            }
        } else if (questionIndex.isInForm() && model.isIndexReadonly(questionIndex)) {
            boolean forwards = questionIndex.compareTo(activeQuestionIndex) > 0;
            if(forwards) {
                step(controller.stepToNextEvent());
            } else {
                step(controller.stepToPreviousEvent());
            }
            return;
        }

        if (questionIndex.compareTo(activeQuestionIndex) > 0) {
            if (activeQuestionIndex.isInForm()) {
                if (activeIsInterstitial) {
                    removeFrame(activeQuestionIndex);
                } else {
                    ((ChatterboxWidget)get(questionIndexes.indexOf(activeQuestionIndex, true))).setViewState(ChatterboxWidget.VIEW_COLLAPSED);
                }
            }

            FormIndex index = activeQuestionIndex;
            while(!index.equals(questionIndex)) {
                index = model.incrementIndex(index);
                putQuestion(index, index.equals(questionIndex), newRepeat ? Q_REPEAT_JUNCTURE : Q_NORMAL);
            }
        } else if (questionIndex.compareTo(activeQuestionIndex) <= 0) {
            FormIndex index = activeQuestionIndex;
            while(!index.equals(questionIndex)) {
                removeFrame(index);
                index = model.decrementIndex(index);
            }

            if (questionIndex.isInForm()) {
                if (newRepeat) {
                    removeFrame(questionIndex);
                    putQuestion(questionIndex, true, newRepeat ? (deleteInterstitial ? Q_REPEAT_DELETE : Q_REPEAT_JUNCTURE) : Q_NORMAL);
                } else {
                    ((ChatterboxWidget)get(questionIndexes.indexOf(questionIndex, true))).setViewState(ChatterboxWidget.VIEW_EXPANDED);
                }
            }
        }

        if (!questionIndex.equals(activeQuestionIndex) || activeIsInterstitial) {
            activeQuestionIndex = questionIndex;

            if (activeQuestionIndex.isInForm()) {
                int index = questionIndexes.indexOf(activeQuestionIndex, true);
                ChatterboxWidget widget = (ChatterboxWidget)get(index);

                //Feb 4, 2009 - csims@dimagi.com
                //The current widget's header should always be pinned in case it overruns the
                //screen with options
                //Feb 9, 2009 - csims@dimagi.com
                //behaves strangely with the 3110c sized screens. Disabling for now.
                //Feb 20, 2009 - csims@dimagi.com
                //Tweaked a bunch of settings, should be fine for ~90% of our use cases now, if it's
                //causing problems, set the polish flag.
                //#if chatterbox.pinning.current != false
                widget.setPinned(true);
                //#endif

                widget.showCommands();

                //Focus's efforts end up trying to scroll the focussed item upwards as if it were
                //already displayed. If we supress the scrolling ahead of time we prevent the
                //new item from getting a double-dose of scrolling.
                int prevheight = this.container.getScrollHeight();
                this.container.setScrollHeight(-1);

                this.focus(widget, true);

                //Return to normal scrolling behavior.
                this.container.setScrollHeight(prevheight);
            }

            //CTS: Nov 11, 2011 - Disabling this behavior in 1.3, it's causing layout issues.
            //if(controller.isEntryOptimized()) {
                //FIXME: no!
                progressBar.setMaxValue(model.getNumQuestions());
                progressBar.setValue(questionIndexes.size());
            //}
        }

        //CTS: Nov 11, 2011 - actually we want to mimic OQPS here and display back on the first question but use
        //it to exit the form instead.
        addBackCommand();

        //UI hacks ho!
        babysitStyles();
    }

    private void addBackCommand() {
        if(!this.model.isReadOnlyMode()) {
            this.addCommand(backCommand);
        }
    }

    private void updatePins(FormIndex questionIndex) {
        for (int i = 0; i < this.size(); ++i) {
            FormIndex index = this.questionIndexes.get(i);
            ChatterboxWidget cw = this.getWidgetAtIndex(i);

            //First reset everything by default
            cw.setPinned(false);

            if (cw.getViewState() == ChatterboxWidget.VIEW_LABEL) {
                if (FormIndex.isSubElement(index, questionIndex)) {
                    cw.setPinned(true);
                } else {
                    cw.setPinned(false);
                }
            }
        }
    }


    private void createHeaderForElement(FormIndex questionIndex, boolean newRepeat) {
        FormEntryCaption prompt = model.getCaptionPrompt(questionIndex);

        String headerText; //decide what text form to use.

        boolean isNew = uncommittedRepeats.contains(model.decrementIndex(questionIndex)); //this is ghetto
        headerText = prompt.getRepetitionText(isNew); //droos: this doesn't feel right... should this if/else be wrapped up in the caption?
        if (headerText == null)
            headerText = prompt.getLongText();

        if(headerText != null) {
            if (newRepeat) {
                removeFrame(activeQuestionIndex);
            }

            ChatterboxWidget headerWidget = widgetFactory.getNewLabelWidget(questionIndex, headerText);
            //If there is no valid header, there's no valid header. Possibly no label.
            this.append(headerWidget);
            this.questionIndexes.add(questionIndex);
            headerWidget.setPinned(true);
        }
    }

    private void removeHeaderForElement(FormIndex questionIndex) {
        //int headerIndex = this.questionIndexes.remove(questionIndex);
        this.removeFrame(questionIndex);
    }

    //create a frame for a question and show it at the appropriate place in the form
    private void putQuestion (FormIndex questionIndex, boolean expanded, int qType) {
        ChatterboxWidget cw = null;

        if (!questionIndex.isInForm())
            return;

        if (expanded && qType != Q_NORMAL) {
            if (FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR == model.getRepeatStructure()) {
                if (qType == Q_REPEAT_JUNCTURE) {
                    //TODO: make rollback work
//                    if (!forward && uncommittedRepeats.contains(questionIndex)) {
//                        controller.deleteRepeat(model.getForm().descendIntoRepeat(questionIndex, model.getRepetitions().size() - 1));
//                    }
                    uncommittedRepeats.removeElement(questionIndex);

                    cw = widgetFactory.getRepeatJunctureWidget(questionIndex, model, this);
                } else if (qType == Q_REPEAT_DELETE) {
                    cw = widgetFactory.getRepeatDeleteWidget(questionIndex, model, this);
                }
            } else {
                cw = widgetFactory.getNewRepeatWidget(questionIndex, model, this);
            }
            activeIsInterstitial = true;
        } else if (model.getForm().explodeIndex(questionIndex).lastElement() instanceof GroupDef) {
            //do nothing
        } else if (model.isIndexRelevant(questionIndex)) { //FIXME relevancy check
            cw = widgetFactory.getWidget(questionIndex, model,
                                          expanded ? ChatterboxWidget.VIEW_EXPANDED
                                                   : ChatterboxWidget.VIEW_COLLAPSED);
        }

        if (cw != null) {
            putFrame(cw, questionIndex);
        }
    }

    //insert a chatterbox widget into the form at the appropriate place
    private void putFrame (ChatterboxWidget widget, FormIndex questionIndex) {
        int frameIndex = questionIndexes.add(questionIndex);
        insert(frameIndex, widget);
        widget.requestInit();
    }

    //remove the frame corresponding to a particular question from display
    private void removeFrame (FormIndex questionIndex) {
        int frameIndex = questionIndexes.remove(questionIndex);
        if (frameIndex == -1)
            return; //question not present in chatterbox (hidden/non-relevant)

        ChatterboxWidget cw = (ChatterboxWidget)get(frameIndex);
        cw.destroy();
        delete(frameIndex);

        if (questionIndex.equals(activeQuestionIndex) && activeIsInterstitial) {
            activeIsInterstitial = false;
        }
    }

    public void formComplete () {
        if(!model.isReadOnlyMode()) {
            controller.jumpToIndex(FormIndex.createEndOfFormIndex());
            babysitStyles();

            //if(controller.isEntryOptimized()) {
                progressBar.setValue(progressBar.getMaxValue());
            //}

            repaint();

            controller.saveAndExit(true);
        }
    }


    private ChatterboxWidget activeFrame () {
        int frameIndex = questionIndexes.indexOf(activeQuestionIndex, true);
        if (frameIndex == -1)
            return null;
        else
            return (ChatterboxWidget)get(frameIndex);
    }

    //probably not the most efficient way of doing this...
    private void babysitStyles () {
        for (int i = 0; i < size(); i++) {
            ChatterboxWidget cw = (ChatterboxWidget)get(i);
            switch (cw.getViewState()) {
            case ChatterboxWidget.VIEW_COLLAPSED:
                if(model.isReadOnlyMode()) {
                    //#style ReviewSplit
                    UiAccess.setStyle(cw);
                } else {
                    //#style split
                    UiAccess.setStyle(cw);
                }
                break;
            case ChatterboxWidget.VIEW_EXPANDED:
                //#style container
                UiAccess.setStyle(cw);
                break;
            }
        }
        this.requestInit();
        this.requestRepaint();
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command command, Displayable s) {
        System.out.println("cbox: command action");

        if (command == backCommand) {
            if (deleteInterstitial) {
                deleteInterstitial = false;
                jumpToQuestion(this.activeQuestionIndex);
            } else {
                step(controller.stepToPreviousEvent(), true);
            }
        } else if (command == exitNoSaveCommand) {
            controller.abort();
        } else if (command == exitSaveCommand) {
            commitAndSave();
        } else if (command == saveCommand) {
            commitAndSave();
        } else if (command.getLabel() == Constants.ACTIVITY_TYPE_GET_IMAGES) {
            suspendActivity(FormEntryTransitions.MEDIA_IMAGE);
        } else if (command.getLabel() == Constants.ACTIVITY_TYPE_GET_AUDIO) {
            suspendActivity(FormEntryTransitions.MEDIA_AUDIO);
        } else if (command.getLabel() == "Capture") {
            doCapture();
        } else if (command.equals(GeoPointWidget.captureCommand)) {
            //This is an awful way to catch this condition, but we'll try it for now.
            suspendActivity(FormEntryTransitions.MEDIA_LOCATION);
        } else if (command.getLabel() == "Back") {
            backFromCamera();
        } else if (command.getLabel().equals(CollapsedWidget.UPDATE_TEXT)) { //TODO: Put this static string in a better place.
            System.out.println("not implemented: updating answers from review mode");

//            model.setQuestionIndex(this.questionIndexes.get(this.getCurrentIndex()));
//            throw new RuntimeException("NOT YET IMPLEMENTED: i don't where to transit to [droos 10/29]");
        } else {
            String language = null;
            if (multiLingual) {
                for (int i = 0; i < languageCommands.length; i++) {
                    if (command == languageCommands[i]) {
                        language = localeCommandMap[i];
                    }
                }
            }

            if (language != null) {
                controller.setLanguage(language);
            } else {
                System.err.println("Chatterbox: Unknown command event received [" + command.getLabel() + "]");
            }
        }
    }


    private void suspendActivity(int mediaType) {
        try {
            controller.suspendActivity(mediaType);
        } catch (UnavailableServiceException e) {
            J2MEDisplay.showError("Unavailable Media Type", e.getMessage());
        }
    }

    private void commitAndSave () {
           ChatterboxWidget frame = (activeIsInterstitial ? null : activeFrame());
        if (frame != null) {
            try {
                controller.answerQuestion(this.model.getFormIndex(), frame.getData());
            } catch(InvalidDataException ide) {
                //TODO: Do whatever we would on a constraint violation here
            }
        }
        //TODO: DEAL;
        controller.saveAndExit(true);
    }

    public void questionAnswered () {
        ChatterboxWidget frame = activeFrame();

        if(activeQuestionIndex != this.model.getFormIndex()) {
            //this is an error that comes from polish sending two events for button up and button
            //down. We need to make it not send that message twice.
            System.out.println("mismatching indices");
            return;
        }
        if (activeIsInterstitial) {
            IAnswerData frameData;
            try {
                frameData = frame.getData();
            } catch (InvalidDataException e) {
                //Interstitial questions are internal to the engine, if this happens
                //it's a structural bug with the program;
                throw new IllegalArgumentException("Couldn't acquire answer data from source" + e.getMessage());
            }
            if (frameData == null) {
                this.queueError(null, PROMPT_REQUIRED_QUESTION, null, null);
                return;
            }
            String answer = ((Selection)frameData.getValue()).getValue();

            if (FormEntryModel.REPEAT_STRUCTURE_NON_LINEAR != model.getRepeatStructure()) {

                if (answer.equals("y")) {
                    controller.newRepeat(this.model.getFormIndex());
                    createHeaderForElement(this.model.getFormIndex(), true);
                }
                step(controller.stepToNextEvent());

            } else {

                if (answer.startsWith("rep")) {
                    removeFrame(this.activeQuestionIndex);

                    int n = Integer.parseInt(answer.substring(3));
                    this.activeQuestionIndex = controller.descendIntoRepeat(n);

                    createHeaderForElement(activeQuestionIndex, false);
                } else if (answer.equals("new")) {
                    removeFrame(this.activeQuestionIndex);

                    uncommittedRepeats.addElement(this.activeQuestionIndex);
                    this.activeQuestionIndex = controller.descendIntoNewRepeat();

                    createHeaderForElement(activeQuestionIndex, false);
                } else if (answer.equals("del")) {
                    deleteInterstitial = true;
                    jumpToQuestion(this.activeQuestionIndex);
                    return;
                } else if (answer.startsWith("del")) {
                    int n = Integer.parseInt(answer.substring(3));
                    controller.deleteRepeat(n);

                    deleteInterstitial = false;
                    jumpToQuestion(this.activeQuestionIndex);
                    return;
                } else if (answer.equals("done")) {
                    //do nothing
                }

                step(controller.stepToNextEvent());

            }
        } else {
            IAnswerData data;
            try {
                data = frame.getData();
            } catch (InvalidDataException e) {
                //We can't know whether this failed due to a violation of the question
                //constraint, so if there's a message, display it.
                IAnswerData uncast = e.getUncastStandin();

                String msg = frame.getPrompt().getConstraintText(uncast);
                if(msg == null) {
                    this.queueError(null, e.getMessage(), null, null);
                    return;
                }
                else {
                    String image = frame.getPrompt().getConstraintText(FormEntryCaption.TEXT_FORM_IMAGE, uncast);
                    String audio = frame.getPrompt().getConstraintText(FormEntryCaption.TEXT_FORM_AUDIO, uncast);
                    this.queueError(null, msg, image, audio);
                    return;
                }

            }
            int status = controller.answerQuestion(this.model.getFormIndex(), data);
            if (status == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
                this.queueError(null, PROMPT_REQUIRED_QUESTION, null, null);
            } else if (status == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
                String msg = frame.getPrompt().getConstraintText(data);
                String image = frame.getPrompt().getConstraintText(FormEntryCaption.TEXT_FORM_IMAGE, data);
                String audio = frame.getPrompt().getConstraintText(FormEntryCaption.TEXT_FORM_AUDIO, data);
                this.queueError(null, msg != null ? msg : PROMPT_DEFAULT_CONSTRAINT_VIOL, image, audio);
             } else {
                 step(controller.stepToNextEvent());
             }
        }
    }

    public void keyPressed(int keyCode) {
        try {
            FormIndex keyDownSelectedWidget = this.activeQuestionIndex;
            super.keyPressed(keyCode);
            if(!controller.handleKeyEvent(keyCode)) {
                if (keyCode == KEY_CENTER_LETS_HOPE) {
                    if (keyDownSelectedWidget == this.activeQuestionIndex) {
                        ChatterboxWidget widget = activeFrame();
                        if (widget != null) {
                            widget.UIHack(UIHACK_SELECT_PRESS);
                        }
                }
                indexWhenKeyPressed = keyDownSelectedWidget;
            }
            }

        } catch (Exception e) {
            Logger.die("gui-keydown", e);
        }
    }

    //no exception handling needed
    public void keyReleased(int keyCode) {
        if(keyCode == KEY_CENTER_LETS_HOPE && !(indexWhenKeyPressed == this.activeQuestionIndex)) {
            //The previous select keypress was for a different item.
        } else {
            //#if javarosa.supresscycle
            if(keyCode != POUND_KEYCODE) {
                super.keyReleased(keyCode);
            }
            //#else
            super.keyReleased(keyCode);
            //#endif
        }
    }




    public ChatterboxWidget getWidgetAtIndex(int index) {
        return (ChatterboxWidget)get(index);
    }

    private void computeHeaders() {
        int threshold = this.contentY;
        //bar.clearSpans();

        // Clayton Sims - Apr 3, 2009 : Removed this code. Tested on the MediaControlSkin
        // and the 3110c, and it seems to be behaving correctly now. this should all
        // be handled by this.contentY above.
        //if(this.topFrame != null && this.topFrame.size() != 0) {
        //    threshold += this.topFrame.getContentHeight();
        //}
        Vector headers = new Vector();
        for (int i = 0; i < size(); i++) {
            ChatterboxWidget cw = getWidgetAtIndex(i);

            //If the widget is in the screen area
            if(cw.getAbsoluteY() + cw.getContentHeight()  > threshold ) {
                //if(i > 7 && i < 25) {
                    //bar.addSpan(cw.getAbsoluteY() + threshold, cw.getAbsoluteY() + cw.getContentHeight() + threshold);
                //}
            }

            // Test for whether this is a header, and should be pinned to the top of the screen because it is above
            // the visible area
            if(cw.isPinned()) {
                //if(cw.getAbsoluteY() + cw.getContentHeight() < threshold ) {
                if(cw.getAbsoluteY() + cw.getPinnableHeight() < threshold ) {
                    headers.addElement(cw.generateHeader());
                }
            }
        }

        Item[] newHeaders = new Item[headers.size()];
        headers.copyInto(newHeaders);
        if((this.topFrame == null && headers.size() > 0) || (this.topFrame != null && !itemArraysEqual(newHeaders, this.topFrame.getItems()))) {
            int oldHeaderHeight = 0;
            if(topFrame != null) {
                oldHeaderHeight = this.topFrame.getContentHeight();
                this.topFrame.clear();
            }
            for(int i = 0 ; i < newHeaders.length ; ++i ) {
                append(Graphics.TOP, newHeaders[i]);
            }

            if(this.topFrame != null) {
                // Clayton Sims - Apr 3, 2009
                //Nuclear Option: Just figure everything out again.
                //Might slow down bad phones. Not sure yet.
                this.topFrame.requestFullInit();
            }

            if(newHeaders.length == 0) {
                int curOffset = this.getScrollYOffset();
                this.calculateContentArea(0, 0,this.getWidth(), this.getHeight());
                this.setScrollYOffset(curOffset + oldHeaderHeight, true);
            }
        }
    }

    private boolean itemArraysEqual(Item[] array1, Item[] array2) {
        if(array1.length != array2.length) {
            return false;
        }
        boolean retVal = true;
        for(int i = 0 ; i < array1.length ; ++i ) {
            if(array1[i] instanceof StringItem && array2[i] instanceof StringItem) {
                return ((StringItem)array1[i]).getText().equals(((StringItem)array2[i]).getText());
            } else {
                if (!array1[i].equals(array2[i])) {
                    retVal = false;
                }
            }
        }
        return retVal;
    }

    /* (non-Javadoc)
     * @see de.enough.polish.ui.Screen#paint(javax.microedition.lcdui.Graphics)
     */
    public void paint(Graphics g) {
        super.paint(g);
        //There are a lot of things that will need to happen after sizes for dynamic
        //things have been computed. These should happen after a paint.

        computeHeaders();
        raiseAlert();
    }

    private void backFromCamera() {
        // TODO Auto-generated method stub
        System.out.println("And we're back...");
    }

    private void doCapture() {
        // TODO Auto-generated method stub
        System.out.println("Click!");
    }

    String alertTitle;
    String msg;
    Image alertImage;
    String audioURI;
    private void raiseAlert() {
        if(alertTitle != null || msg != null) {
            Runnable r = new Runnable() {
                String at = alertTitle;
                String m = msg;
                Image alIm = alertImage;
                String aURI = audioURI;
                long time = new Date().getTime();


                public void run() {
                    J2MEDisplay.showError(at, m, alIm);
                    if(aURI != null) {
                        MediaUtils.playAudio(aURI);
                    }
                }

            };
            new HandledThread(r).start();
            alertTitle = null;
            msg = null;
            alertImage = null;
            audioURI = null;
        }
    }

    private void queueError(String title, String msg, String image, String audio) {
            alertTitle = title;
            this.msg = msg;

            //Try to load the image
            this.alertImage = null;
            if(image != null) {
                try {
                    Reference ref = ReferenceManager._().DeriveReference(image);
                    InputStream in = ref.getStream();
                    this.alertImage = Image.createImage(in);
                    in.close();
                } catch (IOException e) {
                    Logger.exception(e);
                } catch(InvalidReferenceException ire) {
                    Logger.exception(ire);
                }
            }

            //Try to load the image
            this.audioURI = audio;
    }


    public void attachFormMediaController(FormMultimediaController mediacontroller) {
        widgetFactory = new ChatterboxWidgetFactory(this, mediacontroller,new WidgetFactory(controller.isEntryOptimized()));
        widgetFactory.setReadOnly(model.isReadOnlyMode());
    }
}