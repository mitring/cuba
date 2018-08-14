/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.data.EntityOptionsSource;
import com.haulmont.cuba.gui.components.data.EntityValueSource;
import com.haulmont.cuba.gui.components.data.OptionsSource;
import com.haulmont.cuba.gui.components.data.value.DatasourceValueSource;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.NestedDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generic UI component designed to select and display an entity instance. Consists of the text field and the set of buttons
 * defined by actions.
 *
 * @see LookupAction
 * @see OpenAction
 * @see ClearAction
 *
 * @see LookupPickerField
 */
public interface PickerField<V extends Entity> extends Field<V>, ActionsHolder, Buffered,
        LookupComponent, Component.Focusable {

    String NAME = "pickerField";

    CaptionMode getCaptionMode();
    void setCaptionMode(CaptionMode captionMode);

    String getCaptionProperty();
    void setCaptionProperty(String captionProperty);

    MetaClass getMetaClass();
    void setMetaClass(MetaClass metaClass);

    /**
     * Adds LookupAction to the component. If the LookupAction already exists, it will be replaced with the new instance.
     * @return added action
     */
    LookupAction addLookupAction();

    /**
     * @return LookupAction instance
     * @throws java.lang.IllegalArgumentException if the LookupAction does not exist in the component
     */
    default LookupAction getLookupAction() {
        return (LookupAction) getActionNN(LookupAction.NAME);
    }

    /**
     * Adds ClearAction to the component. If the ClearAction already exists, it will be replaced with the new instance.
     * @return added action
     */
    ClearAction addClearAction();

    /**
     * @return ClearAction instance
     * @throws java.lang.IllegalArgumentException if the ClearAction does not exist in the component
     */
    default ClearAction getClearAction() {
        return (ClearAction) getActionNN(ClearAction.NAME);
    }

    /**
     * Adds OpenAction to the component. If the OpenAction already exists, it will be replaced with the new instance.
     * @return added action
     */
    OpenAction addOpenAction();

    /**
     * @return OpenAction instance
     * @throws java.lang.IllegalArgumentException if the OpenAction does not exist in the component
     */
    default OpenAction getOpenAction() {
        return (OpenAction) getActionNN(OpenAction.NAME);
    }

    void addFieldListener(FieldListener listener);

    void setFieldEditable(boolean editable);

    interface FieldListener {
        void actionPerformed(String text, Object prevValue);
    }

    /**
     * Enumerates standard picker action types. Can create a corresponding action instance.
     */
    enum ActionType {

        LOOKUP("lookup") {
            @Override
            public Action createAction(PickerField pickerField) {
                return LookupAction.create(pickerField);
            }
        },

        CLEAR("clear") {
            @Override
            public Action createAction(PickerField pickerField) {
                return ClearAction.create(pickerField);
            }
        },

        OPEN("open") {
            @Override
            public Action createAction(PickerField pickerField) {
                return OpenAction.create(pickerField);
            }
        };

        private String id;

        ActionType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public abstract Action createAction(PickerField pickerField);
    }

    abstract class StandardAction extends BaseAction {

        public static final String PROP_EDITABLE = "editable";

        protected PickerField pickerField;

        protected boolean editable = true;

        protected ClientConfig clientConfig = AppBeans.<Configuration>get(Configuration.NAME).getConfig(ClientConfig.class);

        public StandardAction(String id, PickerField pickerField) {
            super(id);
            this.pickerField = pickerField;
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            boolean oldValue = this.editable;
            if (oldValue != editable) {
                this.editable = editable;
                firePropertyChange(PROP_EDITABLE, oldValue, editable);
            }
        }

        protected Datasource getPropertyDatasource() {
            // TODO: gg, use value source
            if (pickerField.getDatasource() == null
                    || pickerField.getMetaPropertyPath() == null
                    || pickerField.getMetaPropertyPath().getMetaProperty().getType() != MetaProperty.Type.COMPOSITION
                    || pickerField.getDatasource().getDsContext() == null)
                return null;

            for (Datasource datasource : pickerField.getDatasource().getDsContext().getAll()) {
                if (datasource instanceof NestedDatasource
                        && ((NestedDatasource) datasource).getMaster() == pickerField.getDatasource()
                        && ((NestedDatasource) datasource).getProperty() == pickerField.getMetaPropertyPath().getMetaProperty())
                    return datasource;
            }
            return null;
        }
    }

    /**
     * Action to select an entity instance through the entity lookup screen.
     * <p>
     * In order to provide your own implementation globally, create a subclass and register it in {@code web-spring.xml},
     * for example:
     * <pre>
     * &lt;bean id="cuba_LookupAction" class="com.company.sample.gui.MyLookupAction" scope="prototype"/&gt;
     * </pre>
     * Also, use {@code create()} static methods instead of constructors when creating the action programmatically.
     */
    @org.springframework.stereotype.Component("cuba_LookupAction")
    @Scope("prototype")
    class LookupAction extends StandardAction {

        public static final String NAME = ActionType.LOOKUP.getId();

        protected String lookupScreen;
        protected OpenType lookupScreenOpenType = OpenType.THIS_TAB;
        protected Map<String, Object> lookupScreenParams;

        protected Supplier<Map<String, Object>> lookupScreenParamsSupplier;

        protected AfterLookupCloseHandler afterLookupCloseHandler;
        protected AfterLookupSelectionHandler afterLookupSelectionHandler;

        protected WindowConfig windowConfig = AppBeans.get(WindowConfig.class);

        public static LookupAction create(PickerField pickerField) {
            return AppBeans.getPrototype("cuba_LookupAction", pickerField);
        }

        public LookupAction(PickerField pickerField) {
            super(NAME, pickerField);
            caption = "";
            icon = AppBeans.get(Icons.class)
                    .get(CubaIcon.PICKERFIELD_LOOKUP);
            setShortcut(clientConfig.getPickerLookupShortcut());
        }

        public void setAfterLookupCloseHandler(AfterLookupCloseHandler afterLookupCloseHandler) {
            this.afterLookupCloseHandler = afterLookupCloseHandler;
        }

        public void setAfterLookupSelectionHandler(AfterLookupSelectionHandler afterLookupSelectionHandler) {
            this.afterLookupSelectionHandler = afterLookupSelectionHandler;
        }

        public String getLookupScreen() {
            return lookupScreen;
        }

        /**
         * Set the lookup screen ID explicitly. By default a lookup screen ID is inferred from the entity metaclass
         * name by adding suffix {@code .lookup} to it.
         *
         * @param lookupScreen  lookup screen ID, e.g. {@code sec$User.lookup}
         */
        public void setLookupScreen(@Nullable String lookupScreen) {
            this.lookupScreen = lookupScreen;
        }

        public OpenType getLookupScreenOpenType() {
            return lookupScreenOpenType;
        }

        /**
         * How to open the lookup screen. By default it is opened in {@link OpenType#THIS_TAB} mode.
         *
         * @param lookupScreenOpenType  open type
         */
        public void setLookupScreenOpenType(OpenType lookupScreenOpenType) {
            this.lookupScreenOpenType = lookupScreenOpenType;
        }

        @Nullable
        public Map<String, Object> getLookupScreenParams() {
            return lookupScreenParams;
        }

        /**
         * Parameters to pass to the lookup screen. By default the empty map is passed.
         *
         * @param lookupScreenParams    map of parameters
         */
        public void setLookupScreenParams(Map<String, Object> lookupScreenParams) {
            this.lookupScreenParams = lookupScreenParams;
        }

        @Override
        public void actionPerform(Component component) {
            if (pickerField.isEditable()) {
                String windowAlias = getLookupScreen();
                if (windowAlias == null) {
                    final MetaClass metaClass = pickerField.getMetaClass();
                    if (metaClass == null) {
                        throw new DevelopmentException("Neither metaClass nor datasource/property is specified " +
                                "for the PickerField", "action ID", getId());
                    }
                    windowAlias = windowConfig.getAvailableLookupScreenId(metaClass);
                }

                WindowManager wm;
                Window window = ComponentsHelper.getWindow(pickerField);
                if (window == null) {
                    LoggerFactory.getLogger(PickerField.class).warn("Please specify Frame for PickerField");

                    wm = AppBeans.get(WindowManagerProvider.class).get();
                } else {
                    wm = window.getWindowManager();
                }

                OpenType openType = getLookupScreenOpenType();

                Map<String, Object> screenParams = prepareScreenParams();

                Window lookupWindow = wm.openLookup(
                        windowConfig.getWindowInfo(windowAlias),
                        this::handleLookupWindowSelection,
                        openType,
                        screenParams
                );
                lookupWindow.addCloseListener(actionId -> {
                    // if value is selected then options datasource is refreshed in select handler
                    if (!Window.Lookup.SELECT_ACTION_ID.equals(actionId)
                            && pickerField instanceof LookupPickerField) {
                        LookupPickerField lookupPickerField = (LookupPickerField) pickerField;

                        OptionsSource optionsSource = lookupPickerField.getOptionsSource();
                        if (optionsSource instanceof EntityOptionsSource
                                && lookupPickerField.isRefreshOptionsOnLookupClose()) {
                            ((EntityOptionsSource) optionsSource).refresh();
                        }
                    }

                    // move focus to owner
                    pickerField.focus();

                    afterCloseLookup(actionId);
                    if (afterLookupCloseHandler != null) {
                        afterLookupCloseHandler.onClose(lookupWindow, actionId);
                    }
                });
                afterLookupWindowOpened(lookupWindow);
            }
        }

        @Nonnull
        protected Map<String, Object> prepareScreenParams() {
            Map<String, Object> resultParams = Collections.emptyMap();

            Map<String, Object> explicitParams = getLookupScreenParams();
            if (explicitParams != null && !explicitParams.isEmpty()) {
                resultParams = explicitParams;
            }
            if (lookupScreenParamsSupplier != null) {
                Map<String, Object> params = lookupScreenParamsSupplier.get();
                if (params != null && !params.isEmpty()) {
                    if (resultParams.isEmpty()) {
                        resultParams = params;
                    } else {
                        resultParams = new HashMap<>(resultParams);
                        resultParams.putAll(params);
                    }
                }
            }
            return resultParams;
        }

        @SuppressWarnings("unchecked")
        protected void handleLookupWindowSelection(Collection items) {
            if (items.isEmpty()) {
                return;
            }

            Entity item = (Entity) items.iterator().next();
            Entity newValue = transformValueFromLookupWindow(item);

            if (pickerField instanceof LookupPickerField) {
                LookupPickerField lookupPickerField = (LookupPickerField) pickerField;

                OptionsSource optionsSource = lookupPickerField.getOptionsSource();
                if (optionsSource instanceof EntityOptionsSource) {
                    EntityOptionsSource entityOptionsSource = (EntityOptionsSource) optionsSource;
                    if (entityOptionsSource.containsItem(newValue)) {
                        entityOptionsSource.updateItem(newValue);
                    }

                    if (lookupPickerField.isRefreshOptionsOnLookupClose()) {
                        entityOptionsSource.refresh();
                    }
                }
            }

            pickerField.setValue(newValue);

            afterSelect(items);
            if (afterLookupSelectionHandler != null) {
                afterLookupSelectionHandler.onSelect(items);
            }
        }

        protected void afterLookupWindowOpened(Window lookupWindow) {
        }

        /**
         * Hook to be implemented in subclasses. Called by the action for new value selected from Lookup window.
         * Can be used for reloading of entity with different view or to replace value with another value.
         *
         * @param valueFromLookupWindow value selected in Lookup window.
         * @return value that will be set to PickerField
         */
        public Entity transformValueFromLookupWindow(Entity valueFromLookupWindow) {
            return valueFromLookupWindow;
        }

        /**
         * Hook to be implemented in subclasses. Called by the action when the user is selected some items in the
         * lookup screen and the PickerField value is set.
         *
         * @param items collection of entity instances selected by user, never null
         */
        public void afterSelect(Collection items) {
        }

        /**
         * Hook to be implemented in subclasses. Called by the action when the lookup screen is closed.
         *
         * @param actionId  ID of action that closed the screen. The following values are possible:
         *                  <ul>
         *                  <li>select - user selected some items</li>
         *                  <li>cancel - user pressed Cancel button</li>
         *                  <li>close - user closed the lookup screen by other means</li>
         *                  </ul>
         */
        public void afterCloseLookup(String actionId) {
        }

        public Supplier<Map<String, Object>> getLookupScreenParamsSupplier() {
            return lookupScreenParamsSupplier;
        }

        public void setLookupScreenParamsSupplier(Supplier<Map<String, Object>> supplier) {
            this.lookupScreenParamsSupplier = supplier;
        }
    }

    /**
     * Action to clear the PickerField content.
     * <p>
     * In order to provide your own implementation globally, create a subclass and register it in {@code web-spring.xml},
     * for example:
     * <pre>
     * &lt;bean id="cuba_ClearAction" class="com.company.sample.gui.MyClearAction" scope="prototype"/&gt;
     * </pre>
     * Also, use {@code create()} static methods instead of constructors when creating the action programmatically.
     */
    @org.springframework.stereotype.Component("cuba_ClearAction")
    @Scope("prototype")
    class ClearAction extends StandardAction {

        public static final String NAME = ActionType.CLEAR.getId();

        public static ClearAction create(PickerField pickerField) {
            return AppBeans.getPrototype("cuba_ClearAction", pickerField);
        }

        public ClearAction(PickerField pickerField) {
            super(NAME, pickerField);
            caption = "";
            icon = AppBeans.get(Icons.class)
                    .get(CubaIcon.PICKERFIELD_CLEAR);
            setShortcut(clientConfig.getPickerClearShortcut());
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerform(Component component) {
            if (pickerField.isEditable()) {
                Object value = pickerField.getValue();

                EntityValueSource entityValueSource = (EntityValueSource) pickerField.getValueSource();
                if (value instanceof Entity
                        && entityValueSource.getMetaPropertyPath() != null
                        && entityValueSource.getMetaPropertyPath().getMetaProperty().getType() == MetaProperty.Type.COMPOSITION) {
                    // TODO: gg, use value source
                    Datasource propertyDatasource = getPropertyDatasource();
                    if (propertyDatasource != null) {
                        for (Datasource datasource : propertyDatasource.getDsContext().getAll()) {
                            if (datasource instanceof NestedDatasource
                                    && ((NestedDatasource) datasource).getMaster() == propertyDatasource
                                    && ((NestedDatasource) datasource).getProperty().getType() == MetaProperty.Type.COMPOSITION) {
                                if (datasource instanceof CollectionDatasource) {
                                    CollectionDatasource collectionDatasource = (CollectionDatasource) datasource;
                                    for (Object id : collectionDatasource.getItemIds()) {
                                        collectionDatasource.removeItem(collectionDatasource.getItem(id));
                                    }
                                }
                            }
                        }
                        ((DatasourceImplementation) propertyDatasource).deleted((Entity) value);
                    }
                }

                pickerField.setValue(null);
            }
        }
    }

    interface AfterLookupCloseHandler {
        void onClose(Window window, String actionId);
    }

    interface AfterLookupSelectionHandler {
        void onSelect(Collection items);
    }

    /**
     * Action to open an edit screen for entity instance which is currently set in the PickerField.
     * <p>
     * In order to provide your own implementation globally, create a subclass and register it in {@code web-spring.xml},
     * for example:
     * <pre>
     * &lt;bean id="cuba_OpenAction" class="com.company.sample.gui.MyOpenAction" scope="prototype"/&gt;
     * </pre>
     * Also, use {@code create()} static methods instead of constructors when creating the action programmatically.
     */
    @org.springframework.stereotype.Component("cuba_OpenAction")
    @Scope("prototype")
    class OpenAction extends StandardAction {

        public static final String NAME = ActionType.OPEN.getId();

        protected String editScreen;
        protected OpenType editScreenOpenType = OpenType.THIS_TAB;
        protected Map<String, Object> editScreenParams;

        protected Supplier<Map<String, Object>> editScreenParamsSupplier;

        protected WindowConfig windowConfig = AppBeans.get(WindowConfig.class);

        public static OpenAction create(PickerField pickerField) {
            return AppBeans.getPrototype("cuba_OpenAction", pickerField);
        }

        public OpenAction(PickerField pickerField) {
            super(NAME, pickerField);
            caption = "";
            icon = AppBeans.get(Icons.class)
                    .get(CubaIcon.PICKERFIELD_OPEN);
            setShortcut(clientConfig.getPickerOpenShortcut());
        }

        public String getEditScreen() {
            return editScreen;
        }

        /**
         * Set the edit screen ID explicitly. By default an edit screen ID is inferred from the entity metaclass
         * name by adding suffix {@code .edit} to it.
         *
         * @param editScreen  edit screen ID, e.g. {@code sec$User.edit}
         */
        public void setEditScreen(String editScreen) {
            this.editScreen = editScreen;
        }

        public OpenType getEditScreenOpenType() {
            return editScreenOpenType;
        }

        /**
         * How to open the edit screen. By default it is opened in {@link OpenType#THIS_TAB} mode.
         *
         * @param editScreenOpenType  open type
         */
        public void setEditScreenOpenType(OpenType editScreenOpenType) {
            this.editScreenOpenType = editScreenOpenType;
        }

        @Nullable
        public Map<String, Object> getEditScreenParams() {
            return editScreenParams;
        }

        /**
         * Parameters to pass to the edit screen. By default the empty map is passed.
         *
         * @param editScreenParams    map of parameters
         */
        public void setEditScreenParams(Map<String, Object> editScreenParams) {
            this.editScreenParams = editScreenParams;
        }

        public Supplier<Map<String, Object>> getEditScreenParamsSupplier() {
            return editScreenParamsSupplier;
        }

        public void setEditScreenParamsSupplier(Supplier<Map<String, Object>> editScreenParamsSupplier) {
            this.editScreenParamsSupplier = editScreenParamsSupplier;
        }

        @Override
        public void actionPerform(Component component) {
            EntityValueSource entityValueSource = (EntityValueSource) pickerField.getValueSource();
            MetaPropertyPath metaPropertyPath = entityValueSource.getMetaPropertyPath();
            boolean composition = metaPropertyPath != null
                    && metaPropertyPath.getMetaProperty().getType() == MetaProperty.Type.COMPOSITION;

            Entity entity = getEntity();
            if (entity == null && composition) {
                entity = initEntity();
            }
            if (entity == null)
                return;

            WindowManager wm;
            Window window = ComponentsHelper.getWindow(pickerField);
            if (window == null) {
                throw new IllegalStateException("Please specify Frame for EntityLinkField");
            } else {
                wm = window.getWindowManager();
            }

            OpenType openType = getEditScreenOpenType();

            Map<String, Object> screenParams = prepareScreenParams();

            if (entity instanceof SoftDelete && ((SoftDelete) entity).isDeleted()) {
                wm.showNotification(
                        messages.getMainMessage("OpenAction.objectIsDeleted"),
                        Frame.NotificationType.HUMANIZED);
                return;
            }

            if (!composition) {
                entity = window.getDsContext().getDataSupplier().reload(entity, View.MINIMAL);
            }

            String windowAlias = getEditScreen();
            if (windowAlias == null) {
                windowAlias = windowConfig.getEditorScreenId(entity.getMetaClass());
            }

            Window.Editor editor = wm.openEditor(
                    windowConfig.getWindowInfo(windowAlias),
                    entity,
                    openType,
                    screenParams,
                    getPropertyDatasource()
            );
            editor.addCloseListener(actionId -> {
                if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                    Entity item = editor.getItem();
                    afterCommitOpenedEntity(item);
                }

                // move focus to owner
                pickerField.focus();

                afterWindowClosed(editor);
            });
        }

        @Nonnull
        protected Map<String, Object> prepareScreenParams() {
            Map<String, Object> resultParams = Collections.emptyMap();

            Map<String, Object> explicitParams = getEditScreenParams();
            if (explicitParams != null && !explicitParams.isEmpty()) {
                resultParams = explicitParams;
            }
            if (editScreenParamsSupplier != null) {
                Map<String, Object> params = editScreenParamsSupplier.get();
                if (params != null && !params.isEmpty()) {
                    if (resultParams.isEmpty()) {
                        resultParams = params;
                    } else {
                        resultParams = new HashMap<>(resultParams);
                        resultParams.putAll(params);
                    }
                }
            }
            return resultParams;
        }

        protected Entity getEntity() {
            Object value = pickerField.getValue();

            if (value instanceof Entity) {
                return (Entity) value;
            }

            if (pickerField.getValueSource() != null && !pickerField.isBuffered()) {
                EntityValueSource entityValueSource = (EntityValueSource) pickerField.getValueSource();
                Entity item = entityValueSource.getItem();
                if (item != null) {
                    Object dsValue = item.getValue(entityValueSource.getMetaPropertyPath().getMetaProperty().getName());
                    if (dsValue instanceof Entity)
                        return (Entity) dsValue;
                }
            }

            return null;
        }

        protected Entity initEntity() {
            EntityValueSource entityValueSource = (EntityValueSource) pickerField.getValueSource();
            Entity entity = AppBeans.get(Metadata.class).create(
                    entityValueSource.getMetaPropertyPath().getMetaProperty().getRange().asClass());

            Entity ownerEntity = entityValueSource.getItem();
            MetaProperty inverseProp = entityValueSource.getMetaPropertyPath().getMetaProperty().getInverse();
            if (inverseProp != null) {
                entity.setValue(inverseProp.getName(), ownerEntity);
            }

            return entity;
        }

        @SuppressWarnings("unchecked")
        protected void afterCommitOpenedEntity(Entity item) {
            if (pickerField instanceof LookupField) {
                LookupField lookupPickerField = ((LookupField) pickerField);

                EntityOptionsSource entityOptionsSource = (EntityOptionsSource) lookupPickerField.getOptionsSource();
                if (entityOptionsSource != null
                        && entityOptionsSource.containsItem(item)) {
                    entityOptionsSource.updateItem(item);
                }
            }

            if (pickerField.getValueSource() instanceof DatasourceValueSource) {
                DatasourceValueSource datasourceValueSource = (DatasourceValueSource) pickerField.getValueSource();

                boolean modified = datasourceValueSource.isModified();

                pickerField.setValue(item);

                datasourceValueSource.setModified(modified);
            } else {
                pickerField.setValue(item);
            }
        }

        /**
         * Hook invoked after the editor was closed
         * @param window    the editor window
         */
        protected void afterWindowClosed(Window window) {
        }

        @Override
        public void setEditable(boolean editable) {
            super.setEditable(editable);
            setIcon(getEditableIcon(icon, editable));
        }

        public static final String READONLY = "-readonly";

        protected String getEditableIcon(String icon, boolean editable) {
            if (icon == null)
                return null;

            int dot = icon.lastIndexOf('.');
            if (dot == -1)
                return icon;

            StringBuilder sb = new StringBuilder(icon);
            int len = READONLY.length();
            if (StringUtils.substring(icon, dot - len, dot).equals(READONLY)) {
                if (editable)
                    sb.delete(dot - len, dot);
            } else {
                if (!editable)
                    sb.insert(dot, READONLY);
            }

            return sb.toString();
        }
    }
}