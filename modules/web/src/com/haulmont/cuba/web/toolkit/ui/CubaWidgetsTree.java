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
package com.haulmont.cuba.web.toolkit.ui;

import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.v7.data.Container;

import java.io.Serializable;
import java.util.*;

public class CubaWidgetsTree extends CubaTree implements ComponentContainer {

    protected WidgetBuilder widgetBuilder;

    protected final List<Component> nodeWidgets = new ArrayList<>();

    protected final List<Object> itemIds = new ArrayList<>();

    @Override
    public void containerItemSetChange(Container.ItemSetChangeEvent event) {
        super.containerItemSetChange(event);

        refreshRenderedComponents();
    }

    @Override
    public void containerPropertySetChange(Container.PropertySetChangeEvent event) {
        super.containerPropertySetChange(event);

        refreshRenderedComponents();
    }

    protected void detachGeneratedComponents() {
        for (Component c : nodeWidgets) {
            if (c.isAttached()) {
                c.detach();
            }
        }
    }

    protected void refreshRenderedComponents() {
        detachGeneratedComponents();

        nodeWidgets.clear();
        itemIds.clear();

        if (widgetBuilder != null) {
            // Iterates through hierarchical tree using a stack of iterators
            final Stack<Iterator<?>> iteratorStack = new Stack<>();
            Collection<?> ids = rootItemIds();

            if (ids != null) {
                iteratorStack.push(ids.iterator());
            }

            while (!iteratorStack.isEmpty()) {

                // Gets the iterator for current tree level
                final Iterator<?> i = iteratorStack.peek();

                // If the level is finished, back to previous tree level
                if (!i.hasNext()) {

                    // Removes used iterator from the stack
                    iteratorStack.pop();
                } else {
                    final Object itemId = i.next();

                    itemIds.add(itemId);

                    Component c = widgetBuilder.buildWidget(this, itemId, areChildrenAllowed(itemId));
                    c.setParent(this);
                    c.markAsDirty();

                    nodeWidgets.add(c);

                    if (hasChildren(itemId) && areChildrenAllowed(itemId)) {
                        iteratorStack.push(getChildren(itemId).iterator());
                    }
                }
            }
        }
    }

    @Override
    protected void paintItem(
            PaintTarget target,
            Object itemId,
            LinkedList<String> selectedKeys,
            LinkedList<String> expandedKeys
    ) throws PaintException {
        super.paintItem(target, itemId, selectedKeys, expandedKeys);

        if (itemIds.indexOf(itemId) >= 0) {
            target.addAttribute("widgetIndex", itemIds.indexOf(itemId));
        }
    }

    public WidgetBuilder getWidgetBuilder() {
        return widgetBuilder;
    }

    public void setWidgetBuilder(WidgetBuilder widgetBuilder) {
        if (this.widgetBuilder != widgetBuilder) {
            this.widgetBuilder = widgetBuilder;
            refreshRenderedComponents();
        }
    }

    @Override
    public Iterator<Component> iterator() {
        return nodeWidgets.iterator();
    }

    @Override
    public Iterator<Component> getComponentIterator() {
        return iterator();
    }

    @Override
    public int getComponentCount() {
        return nodeWidgets.size();
    }

    @Override
    public void addComponent(Component c) {
        // do nothing
    }

    @Override
    public void addComponents(Component... nodeWidgets) {
        // do nothing
    }

    @Override
    public void removeComponent(Component c) {
        // do nothing
    }

    @Override
    public void removeAllComponents() {
        // do nothing
    }

    @Override
    public void replaceComponent(Component oldComponent, Component newComponent) {
        // do nothing
    }

    @Override
    public void moveComponentsFrom(ComponentContainer source) {
        // do nothing
    }

    @Override
    public Registration addComponentAttachListener(ComponentAttachListener listener) {
        // do nothing
        return (Registration) () -> {};
    }

    @Override
    public void removeComponentAttachListener(ComponentAttachListener listener) {
        // do nothing
    }

    @Override
    public Registration addComponentDetachListener(ComponentDetachListener listener) {
        // do nothing
        return (Registration) () -> {};
    }

    @Override
    public void removeComponentDetachListener(ComponentDetachListener listener) {
        // do nothing
    }

    public interface WidgetBuilder extends Serializable {
        Component buildWidget(CubaWidgetsTree source, Object itemId, boolean leaf);
    }
}