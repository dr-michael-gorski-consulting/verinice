/*******************************************************************************
 * Copyright (c) 2011 Sebastian Hagedorn <sh[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Sebastian Hagedorn <sh[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.iso27k.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import sernet.gs.model.Baustein;
import sernet.gs.model.Gefaehrdung;
import sernet.gs.model.Massnahme;
import sernet.gs.service.GSServiceException;
import sernet.gs.service.RetrieveInfo;
import sernet.gs.ui.rcp.main.common.model.BuildInput;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.CnAElementHome;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.interfaces.IAuthService;
import sernet.verinice.interfaces.ICommandService;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.Permission;
import sernet.verinice.model.iso27k.Control;
import sernet.verinice.model.iso27k.ControlGroup;
import sernet.verinice.model.iso27k.Group;
import sernet.verinice.model.iso27k.IncidentScenario;
import sernet.verinice.model.iso27k.IncidentScenarioGroup;
import sernet.verinice.service.commands.LoadElementById;
import sernet.verinice.service.commands.SaveElement;
import sernet.verinice.service.parser.GSScraperUtil;

public class GS2BSITransformService {

    private IProgressObserver progressObserver;

    private static final Logger log = Logger.getLogger(GS2BSITransformService.class);

    private int numberOfControls;

    private List<Object> itemList;

    private IModelUpdater modelUpdater;

    private Group<?> selectedGroup;

    private int numberProcessed;

    private IAuthService authService;

    private ICommandService commandService;

    private boolean isScenario = false;

    private ItemTransformer transformer = new ItemTransformer() {

        @Override
        public void transformElement(Group<?> group, Object item, List<CnATreeElement> elements) {
            transformGsElement(group, item, elements);
        }
    };

    /**
     * Strategy interface to transform the given item to a target object.
     *
     * @author uz[at]sernet.de
     *
     */
    public interface ItemTransformer {
        /**
         * Transforms the given item to a target object and stores it in the
         * elements list.
         *
         * @param group
         *            - the group the transformed elements are added to
         * @param item
         *            - the item to transform
         * @param elements
         *            - the list of transformed elements
         */
        void transformElement(Group<?> group, Object item, List<CnATreeElement> elements);
    }

    public GS2BSITransformService(IProgressObserver progressObserver, IModelUpdater modelUpdater,
            Group<?> selectedGroup, Object data) {
        this.progressObserver = progressObserver;
        itemList = new ArrayList<>(20);
        if (data instanceof Object[]) {
            Object[] o = (Object[]) data;
            for (Object object : o) {
                itemList.add(object);
            }
        } else if (data instanceof Collection) {
            Collection<?> c = (Collection<?>) data;
            itemList.addAll(c);
        } else {
            itemList.add(data);
        }
        this.modelUpdater = modelUpdater;
        this.selectedGroup = selectedGroup;
    }

    /**
     * Creates a Grundschutz transform operation.
     *
     * @param progressObserver
     *            - progress
     * @param modelUpdater
     *            - model change
     * @param selectedGroup
     *            - the target group to add to
     * @param data
     *            - the source item
     * @param transformer
     *            - the transformer to transform the item to the target item
     */
    public GS2BSITransformService(IProgressObserver progressObserver, IModelUpdater modelUpdater,
            Group<?> selectedGroup, Object data, ItemTransformer transformer) {
        this(progressObserver, modelUpdater, selectedGroup, data);
        this.transformer = transformer;
    }

    public boolean isScenario() {
        return isScenario;
    }

    public void run() {
        try {
            this.numberOfControls = itemList.size();
            progressObserver.beginTask(
                    Messages.getString("GS2BSITransformService.0", numberOfControls), -1); //$NON-NLS-1$
            for (Object o : itemList) {
                insertItem(progressObserver, selectedGroup, o);
            }
        } catch (Exception e) {
            log.error("Error while transforming GS element to ISM element", e);
        }
    }

    /**
     * @param monitor
     * @param group
     * @param item
     */
    private void insertItem(IProgressObserver monitor, Group<?> group, Object item) {
        if (monitor.isCanceled()) {
            log.warn("Transforming canceled. " //$NON-NLS-1$
                    + numberProcessed + " items transformed."); // $NON-NLS-2$
                                                                // //$NON-NLS-3$
            return;
        }
        List<CnATreeElement> elements = new ArrayList<>();
        if (item != null) {
            transformer.transformElement(group, item, elements);
        }
        saveItems(elements, monitor);
    }

    /**
     * @param group
     * @param item
     * @param elements
     */
    private void transformGsElement(Group<?> group, Object item, List<CnATreeElement> elements) {
        if (item instanceof Massnahme) {
            Massnahme m = (Massnahme) item;
            elements.add((CnATreeElement) generateControl(m, group));
        }
        if (item instanceof Gefaehrdung) {
            Gefaehrdung g = (Gefaehrdung) item;
            elements.add(generateScenario(g, group));
        }
        if (item instanceof Baustein) {
            try {
                Baustein b = (Baustein) item;
                if (group.canContain(new IncidentScenario())) {
                    IncidentScenarioGroup newGroup = new IncidentScenarioGroup(group);
                    newGroup.setTitel(b.getId() + " " + b.getTitel());
                    CnATreeElement saveNew = null;
                    saveNew = CnAElementFactory.getInstance().saveNew(group,
                            IncidentScenarioGroup.TYPE_ID,
                            new BuildInput<IncidentScenarioGroup>(newGroup),
                            false /* do not notify single elements */,
                            false /* do not inherit icon */);
                    saveNew.setTitel(b.getId() + " " + b.getTitel());
                    CnAElementHome.getInstance().updateEntity(saveNew);
                    CnAElementFactory.getLoadedModel().childAdded(group, saveNew);
                    for (Gefaehrdung g : b.getGefaehrdungen()) {
                        IncidentScenario scen = generateScenario(g,
                                (IncidentScenarioGroup) saveNew);
                        elements.add(scen);
                    }
                } else if (group.canContain(new Control())) {
                    ControlGroup newGroup = new ControlGroup(group);
                    newGroup.setTitel(b.getTitel());
                    CnATreeElement saveNew = null;
                    saveNew = CnAElementFactory.getInstance().saveNew(group, ControlGroup.TYPE_ID,
                            new BuildInput<ControlGroup>(newGroup),
                            false /* do not notify single elements */,
                            false /* do not inherit icon */);
                    saveNew.setTitel(b.getId() + " " + b.getTitel());
                    CnAElementHome.getInstance().updateEntity(saveNew);
                    CnAElementFactory.getLoadedModel().childAdded(group, saveNew);
                    for (Massnahme m : b.getMassnahmen()) {
                        elements.add(generateControl(m, (ControlGroup) saveNew));
                    }
                }
            } catch (Exception e) {
                log.error("Error while transforming baustein", e);
            }
        }
    }

    private void saveItems(List<CnATreeElement> elements, IProgressObserver monitor) {
        SaveElement<CnATreeElement> command = null;
        boolean errorOccured = false;
        if (elements.size() > 0) {
            for (CnATreeElement e : elements) {
                monitor.setTaskName(getText(numberProcessed, e.getTitle()));
                if (e.getParent().canContain(e)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating element,  UUID: " + e.getUuid() + ", title: " //$NON-NLS-1$ //$NON-NLS-2$
                                + e.getTitle());
                    }
                } else {
                    log.warn(
                            "trying to drop an item into a group that is unable to accept this type of items");
                    errorOccured = true;
                }
                if (!errorOccured) {
                    try {
                        HashSet<Permission> newperms = new HashSet<Permission>();
                        newperms.add(Permission.createPermission(e, getAuthService().getUsername(),
                                true, true));
                        e.setPermissions(newperms);
                        command = new SaveElement<>(e);
                        if (e instanceof IncidentScenario) {
                            isScenario = true;
                        }
                        command = getCommandService().executeCommand(command);
                    } catch (CommandException ce) {
                        log.error("Error while inserting control", ce); //$NON-NLS-1$
                        throw new RuntimeException("Error while inserting control", ce); //$NON-NLS-1$
                    }
                    e = command.getElement();
                    RetrieveInfo info = new RetrieveInfo().setParent(true).setChildren(true)
                            .setProperties(true);
                    LoadElementById<CnATreeElement> c2 = new LoadElementById<>(e.getDbId(),
                            info);
                    try {
                        c2 = ServiceFactory.lookupCommandService().executeCommand(c2);
                        e = c2.getElement();
                        c2 = new LoadElementById<>(e.getParent().getDbId(), info);
                        c2 = ServiceFactory.lookupCommandService().executeCommand(c2);
                        e.setParent(c2.getElement());
                    } catch (CommandException e1) {
                        log.error("Error while loading element", e1);
                    }
                    CnATreeElement parent = e.getParent();
                    parent.addChild(e);
                    e.setParentAndScope(parent);
                    modelUpdater.childAdded((Group<?>) parent, e);
                    monitor.processed(1);
                    numberProcessed++;
                }
            }
        }
    }

    /**
     * @param i
     * @param title
     */
    private String getText(int i, String title) {
        return Messages.getString("GS2BSITransformService.1", i, title); //$NON-NLS-1$
    }

    private Control generateControl(Massnahme m, CnATreeElement parent) {
        Control c = new Control(parent);
        c.setTitel(m.getId() + " " + m.getTitel());
        try {
            String description = GSScraperUtil.getInstance().getModel().getMassnahmeHtml(m.getUrl(),
                    m.getStand());
            c.setDescription(description);
        } catch (GSServiceException e) {
            log.error("Error while transforming massnahme into control", e);
        }
        return c;
    }

    private IncidentScenario generateScenario(Gefaehrdung g, Group<?> parent) {
        IncidentScenario s = new IncidentScenario(parent);
        if (g.getTitel() != null) {
            String title = g.getId() + " " + g.getTitel();
            s.setTitel(title);
        } else {
            s.setTitel("Dummy Scenario");
        }
        // TODO: add description from threat to scenario
        return s;
    }

    public int getNumberOfControls() {
        return numberOfControls;
    }

    public int getNumberProcessed() {
        return numberProcessed;
    }

    public IAuthService getAuthService() {
        if (authService == null) {
            authService = createAuthService();
        }
        return authService;
    }

    private IAuthService createAuthService() {
        return ServiceFactory.lookupAuthService();
    }

    public ICommandService getCommandService() {
        if (commandService == null) {
            commandService = createCommandServive();
        }
        return commandService;
    }

    private ICommandService createCommandServive() {
        return ServiceFactory.lookupCommandService();
    }

}
