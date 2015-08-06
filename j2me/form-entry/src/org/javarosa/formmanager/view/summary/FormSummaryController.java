package org.javarosa.formmanager.view.summary;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledPCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.List;

public class FormSummaryController implements HandledPCommandListener {

    private FormSummaryTransitions transistions;
    private FormSummaryView view;
    private final FormEntryModel model;

    public FormSummaryController(FormEntryModel model) {
        this.model = model;
        view = new FormSummaryView(model);
        view.setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command command, Displayable d) {
        if (d == view) {
            if (command == view.CMD_EXIT) {
                transistions.exit();
            } else if (command == view.CMD_SAVE_EXIT) {
                int counter = countUnansweredQuestions(model,true);
                if (counter > 0) {
                    String txt = "There are unanswered compulsory questions and must be completed first to proceed";
                    J2MEDisplay.showError("Question Required!", txt);
                } else {
                    transistions.saveAndExit(true);
                }
            } else if (command == List.SELECT_COMMAND) {
                transistions.viewForm(view.getFormIndex());
            }
        }
    }

    /**
     * @param countRequiredOnly
     *            if true count only the questions that are unanswered and also
     *            required
     * @return number of unanswered questions
     */
    public static int countUnansweredQuestions(FormEntryModel model, boolean countRequiredOnly) {
        //ctsims - Made this list only count relevant questions
        int counter = 0;

        for(FormIndex index = model.incrementIndex(FormIndex.createBeginningOfFormIndex());!index.isEndOfFormIndex();index = model.incrementIndex(index)) {
            if(!model.isIndexRelevant(index)) {
                continue;
            }

            if(model.getEvent(index) == FormEntryController.EVENT_QUESTION) {
                FormEntryPrompt prompt = model.getQuestionPrompt(index);
                if(prompt.getAnswerValue() == null) {
                    if(!countRequiredOnly || prompt.isRequired()) {
                        counter++;
                    }
                }
            }
        }

        return counter;
    }

    /**
     * @param model Form entry model to test index against
     * @return number of unanswered questions
     */

    public static int countQuestionsToIndex(FormEntryModel model, FormIndex last) {
        //ctsims - Made this list only count relevant questions
        int counter = 0;

        for(FormIndex index = model.incrementIndex(FormIndex.createBeginningOfFormIndex());!index.equals(last);index = model.incrementIndex(index)) {
            if(!model.isIndexRelevant(index)) {
                continue;
            }

            if(model.getEvent(index) == FormEntryController.EVENT_QUESTION) {
                counter++;
            }
        }

        return counter;
    }

    public void setTransitions(FormSummaryState transitions) {
        this.transistions = transitions;
    }

    public void start() {
        view.show();
    }
}
