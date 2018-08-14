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

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.KeyCombination;
import com.haulmont.cuba.gui.components.security.ActionsPermissions;
import com.haulmont.cuba.gui.components.sys.ShortcutsDelegate;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.theme.ThemeConstantsManager;
import com.haulmont.cuba.web.gui.components.util.ShortcutListenerDelegate;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;
import static com.haulmont.cuba.gui.ComponentsHelper.findActionById;

/**
 * Base class for action holders with shortcuts support
 */
public abstract class WebAbstractActionsHolderComponent<T extends com.vaadin.ui.AbstractComponent
        & com.vaadin.event.Action.ShortcutNotifier>
        extends WebAbstractComponent<T> implements com.haulmont.cuba.gui.components.SecuredActionsHolder {

    protected final List<Action> actionList = new ArrayList<>(5);

    protected VerticalLayout contextMenuPopup;

    protected final ShortcutsDelegate<ShortcutListener> shortcutsDelegate;
    protected final ActionsPermissions actionsPermissions = new ActionsPermissions(this);

    protected boolean showIconsForPopupMenuActions;

    protected WebAbstractActionsHolderComponent() {
        contextMenuPopup = new VerticalLayout();
        contextMenuPopup.setSpacing(false);
        contextMenuPopup.setMargin(false);

        contextMenuPopup.setCubaId("cubaContextMenu");

        contextMenuPopup.setSizeUndefined();
        contextMenuPopup.setStyleName("c-cm-container");

        shortcutsDelegate = new ShortcutsDelegate<ShortcutListener>() {
            @Override
            protected ShortcutListener attachShortcut(String actionId, KeyCombination keyCombination) {
                ShortcutListener shortcut =
                        new ShortcutListenerDelegate(actionId, keyCombination.getKey().getCode(),
                                KeyCombination.Modifier.codes(keyCombination.getModifiers())
                        ).withHandler((sender, target) -> {
                            if (target == component) {
                                Action action = getAction(actionId);
                                if (action != null && action.isEnabled() && action.isVisible()) {
                                    action.actionPerform(WebAbstractActionsHolderComponent.this);
                                }
                            }
                        });

                component.addShortcutListener(shortcut);
                return shortcut;
            }

            @Override
            protected void detachShortcut(Action action, ShortcutListener shortcutDescriptor) {
                component.removeShortcutListener(shortcutDescriptor);
            }

            @Override
            protected Collection<Action> getActions() {
                return WebAbstractActionsHolderComponent.this.getActions();
            }
        };
    }

    @Inject
    public void setThemeConstantsManager(ThemeConstantsManager themeConstantsManager) {
        ThemeConstants theme = themeConstantsManager.getConstants();
        this.showIconsForPopupMenuActions = theme.getBoolean("cuba.gui.showIconsForPopupMenuActions", false);
    }

    @Override
    public void addAction(Action action) {
        int index = findActionById(actionList, action.getId());
        if (index < 0) {
            index = actionList.size();
        }

        addAction(action, index);
    }

    @Override
    public void addAction(Action action, int index) {
        checkNotNullArgument(action, "action must be non null");

        int oldIndex = findActionById(actionList, action.getId());
        if (oldIndex >= 0) {
            removeAction(actionList.get(oldIndex));
            if (index > oldIndex) {
                index--;
            }
        }

        if (StringUtils.isNotEmpty(action.getCaption())) {
            ContextMenuButton contextMenuButton = createContextMenuButton();
            contextMenuButton.setStyleName("c-cm-button");
            contextMenuButton.setAction(action);

            Component newVButton = contextMenuButton.unwrap(com.vaadin.ui.Component.class);

            int visibleActionsIndex = 0;
            int i = 0;
            while (i < index && i < actionList.size()) {
                if (StringUtils.isNotEmpty(actionList.get(i).getCaption())) {
                    visibleActionsIndex++;
                }

                i++;
            }

            contextMenuPopup.addComponent(newVButton, visibleActionsIndex);
        }

        actionList.add(index, action);

        shortcutsDelegate.addAction(null, action);

        attachAction(action);

        actionsPermissions.apply(action);
    }

    protected void attachAction(Action action) {
        action.refreshState();
    }

    protected abstract ContextMenuButton createContextMenuButton();

    @Override
    public void removeAction(@Nullable Action action) {
        if (actionList.remove(action)) {
            ContextMenuButton actionButton = null;

            for (Component component : contextMenuPopup) {
                if (component instanceof ContextMenuButton) {
                    ContextMenuButton button = (ContextMenuButton) component;
                    if (button.getAction() == action) {
                        actionButton = button;
                        break;
                    }
                }
            }

            if (actionButton != null) {
                actionButton.setAction(null);

                contextMenuPopup.removeComponent(actionButton.unwrap(com.vaadin.ui.Component.class));
            }

            shortcutsDelegate.removeAction(action);
        }
    }

    @Override
    public void removeAction(@Nullable String id) {
        Action action = getAction(id);
        if (action != null) {
            removeAction(action);
        }
    }

    @Override
    public void removeAllActions() {
        for (Action action : new ArrayList<>(actionList)) {
            removeAction(action);
        }
    }

    @Override
    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actionList);
    }

    @Override
    @Nullable
    public Action getAction(String id) {
        for (Action action : getActions()) {
            if (Objects.equals(action.getId(), id)) {
                return action;
            }
        }
        return null;
    }

    @Override
    public ActionsPermissions getActionsPermissions() {
        return actionsPermissions;
    }
}