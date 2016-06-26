package org.javarosa.entity.api;
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

import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.Iterator;
import org.javarosa.entity.api.transitions.EntitySelectTransitions;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.entity.model.view.EntitySelectDetailPopup;
import org.javarosa.entity.model.view.EntitySelectView;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.j2me.view.ProgressIndicator;

import java.util.Vector;

import javax.microedition.lcdui.Displayable;

/**
 * Entity Select is a reusable activity for selecting a single record from a set of like records. For
 * example, choosing a patient from a list of patients, selecting a saved form from a list of recently
 * saved forms, etc.
 *
 * Two things must be passed to the activity to make this work:
 *
 *   1) A StorageUtility from which the set of records will be read
 *
 *   2) A wrapper class specific to the type of record that provides methods to control the UI and
 *      behavior of the activity (such as which columns to display and their contents, sorting, searching,
 *      and also filtering which records from the StorageUtility to include in the set). This is referred
 *      to as the 'Entity'. The Entity passed in is simply an empty prototype, but a new Entity is cloned
 *      for each record in the selectable set.
 *
 * When a record is selected, the activity exits via the 'entitySelected' transition, passing the record ID
 * of the chosen record.
 *
 * Optionally, it is possible to branch off to a 'create new entity' workflow from this activity, via the
 * 'newEntity' transition. Once a new entity has been created, this activity can be resumed with the new
 * entity added to the list, or the new entity can be immediately 'selected'. Resuming is done via the
 * newEntity() method in this class. Wiring up to the proper 'entity creation' activity, as well as keeping
 * a reference to this activity (for resuming) is the responsibility of the workflow architect. 'Create new'
 * behavior can be disabled.
 *
 * Overall flow of the activity:
 *
 *   * The set of selectable records is read from the StorageUtility, a new entity wrapper is created for
 *     each record, relevant fields are cached.
 *   * All entities are displayed as a list, with a few short summary fields displayed for each record
 *   * The user may scroll through the list, and also filter the list based on a search key. They may also
 *     change the sorting of the list, or branch off to create a new entity (if enabled).
 *   * The user selects an entity, and this brings them to a new screen that shows detailed information for
 *     just that record.
 *   * If they have the right record, they may select it and the activity exits. Or, they may go back to the
 *     list view to choose a different record.
 *
 * @author Drew Roos
 *
 * @param <E> underlying base class (e.g., patient, case, saved form, referral, ...) that this activity
 *    will be used to select
 */

public class EntitySelectController <E> implements ProgressIndicator{
    private EntitySelectTransitions transitions;

    private EntitySelectView<E> selView;

    protected EntitySet<E> entitySet;
    private Entity<E> entityPrototype;

    protected boolean immediatelySelectNewlyCreated;
    protected boolean bailOnEmpty;

    Vector<Entity<E>> entities;

    int progress = 0;
    int count =1;

    public EntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype) {
        this(title, set, entityPrototype, EntitySelectView.NEW_IN_LIST, true, false);
    }

    public EntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype, int newMode, boolean immediatelySelectNewlyCreated) {
        this(title, set, entityPrototype, newMode, immediatelySelectNewlyCreated, false);
    }

    /**
     * Create a new Entity Select activity instance
     *
     * @param title UI screen title
     * @param entityStorage StorageUtility to pull records from (must return records of type <E>)
     * @param entityPrototype an instance of the Entity -- the wrapper class for the records
     * @param newMode EntitySelectView.NEW_*; controls if and how you can create new entities from this activity
     * @param immediatelySelectNewlyCreated if you're allowed to create new entities, whether to immediately select
     *    the entity just created
     * @param bailOnEmpty if true, immediately exit the activity via the 'empty' transition if there are no entities
     *    in the set and creating new entities is disabled
     */
    public EntitySelectController (String title, EntitySet<E> set, Entity<E> entityPrototype,
            int newMode, boolean immediatelySelectNewlyCreated, boolean bailOnEmpty) {
        this.entitySet = set;
        this.entityPrototype = entityPrototype;

        this.immediatelySelectNewlyCreated = immediatelySelectNewlyCreated;
        this.bailOnEmpty = bailOnEmpty;

        selView = new EntitySelectView<E>(this, entityPrototype, title, newMode);
    }

    public void setTransitions (EntitySelectTransitions transitions) {
        this.transitions = transitions;
    }

    public void start () {
        loadEntities();

        if(entities.isEmpty() && bailOnEmpty && selView.newMode == EntitySelectView.NEW_DISALLOWED) {
            transitions.empty();
            return;
        }

        selView.init();
        showList();
    }

    private void loadEntities () {
        entities = new Vector<Entity<E>>();

        //CTS: 11/28/2011 - Filters are deprecated. Filters should be placed
        //directly in the entity set.
        //EntityFilter<? super E> filter = entityPrototype.getFilter();

        progress = 0;
        count = entitySet.getCount();

        Iterator<E> ei = entitySet.iterate();
        while (ei.hasMore()) {
            E obj = null;
            obj = ei.nextRecord();

            if (obj != null) {
                loadEntity(obj);
            }
            progress++;
        }
    }

    private void loadEntity (E obj) {
        Entity<E> entity = entityPrototype.factory();
        entity.readEntity(obj);
        entities.addElement(entity);
    }

    public void setView (Displayable view) {
        J2MEDisplay.setView(view);
    }

    public void newEntity (int newEntityID) {
        //note: it is assumed that the newly created entity satisfies any filters in effect
        if (immediatelySelectNewlyCreated) {
            entityChosen(newEntityID);
        } else {
            E obj = entitySet.get(newEntityID);
            loadEntity(obj);
            selView.refresh(newEntityID);
            showList();
        }
    }

    public Vector<Integer> search (String key) {
        Vector<Integer> matches = new Vector<Integer>();

        if (key == null || key.equals("")) {
            for (int i = 0; i < entities.size(); i++)
                matches.addElement(new Integer(i));
        } else {
            for (int i = 0; i < entities.size(); i++) {
                Entity<E> entity = entities.elementAt(i);
                if (entity.match(key)) {
                    matches.addElement(new Integer(i));
                }
            }
        }

        return matches;
    }

    public int getRawResultCount() {
        return entities.size();
    }

    public void showList () {
        selView.show();
    }

    public void itemSelected (int i) {
        Entity<E> entity = entities.elementAt(i);
        if(entity.getHeaders(true) ==null) {
            entityChosen(this.getRecordID(i));
        } else {
            EntitySelectDetailPopup<E> psdp = new EntitySelectDetailPopup<E>(this, entity, entitySet);
            psdp.show();
        }
    }

    public void entityChosen (int entityID) {
        transitions.entitySelected(entityID);
    }

    public void newEntity () {
        transitions.newEntity();
    }

    public void exit () {
        transitions.cancel();
    }

    public String[] getDataFields (int i) {
        return entities.elementAt(i).getShortFields();
    }

    public String[] getTitleData () {
        return entityPrototype.getHeaders(false);
    }

    public String[] getColumnFormat(boolean header) {
        return entityPrototype.getForms(header);
    }

    public Entity<E> getEntity (int i) {
        return entities.elementAt(i);
    }

    public int getRecordID (int i) {
        return entities.elementAt(i).getRecordID();
    }

    public void attemptCallout(String number) {
        J2MEDisplay.showError("Not Available", "Calling functionality is not enabled for this application");
    }

    public double getProgress() {
        return (double)progress / count;
    }
    public String getCurrentLoadingStatus() {
        return Localization.get("loading.screen.message");
    }

    public int getIndicatorsProvided() {
        return ProgressIndicator.INDICATOR_PROGRESS | ProgressIndicator.INDICATOR_STATUS;
    }
}
