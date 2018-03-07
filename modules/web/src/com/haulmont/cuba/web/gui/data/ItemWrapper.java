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
package com.haulmont.cuba.web.gui.data;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.WeakCollectionChangeListener;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property;

import java.util.*;

public class ItemWrapper implements Item, Item.PropertySetChangeNotifier, UnsubscribableDsWrapper {

    protected Map<MetaPropertyPath, PropertyWrapper> properties = new HashMap<>();

    // lazily initialized listeners list
    protected List<PropertySetChangeListener> listeners = null;

    protected Object item;
    protected MetaClass metaClass;
    protected CollectionDatasource.CollectionChangeListener cdsCollectionChangeListener;
    protected WeakCollectionChangeListener weakCollectionChangeListener;

    public ItemWrapper(Object item, MetaClass metaClass) {
        this(item, metaClass, AppBeans.<MetadataTools>get(MetadataTools.NAME).getPropertyPaths(metaClass));
    }

    public ItemWrapper(Object item, MetaClass metaClass, Collection<MetaPropertyPath> properties) {
        this.item = item;
        this.metaClass = metaClass;

        for (MetaPropertyPath property : properties) {
            this.properties.put(property, createPropertyWrapper(item, property));
        }

        if (item instanceof CollectionDatasource) {
            cdsCollectionChangeListener = e -> fireItemPropertySetChanged();

            CollectionDatasource datasource = (CollectionDatasource) item;
            weakCollectionChangeListener = new WeakCollectionChangeListener(datasource, cdsCollectionChangeListener);
            //noinspection unchecked
            datasource.addCollectionChangeListener(weakCollectionChangeListener);
        }
    }

    protected void fireItemPropertySetChanged() {
        if (listeners != null) {
            PropertySetChangeEvent event = new PropertySetChangeEvent();

            for (PropertySetChangeListener listener : listeners) {
                listener.itemPropertySetChange(event);
            }
        }
    }

    protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
        return new PropertyWrapper(item, propertyPath);
    }

    @Override
    public Property getItemProperty(Object id) {
        if (id instanceof MetaPropertyPath) {
            return properties.get(id);
        } else if (id instanceof MetaProperty) {
            final MetaProperty metaProperty = (MetaProperty) id;
            return properties.get(new MetaPropertyPath(metaClass, metaProperty));
        } else {
            return null;
        }
    }

    @Override
    public Collection getItemPropertyIds() {
        return properties.keySet();
    }

    @Override
    public boolean addItemProperty(Object id, Property property) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeItemProperty(Object id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addPropertySetChangeListener(PropertySetChangeListener listener) {
        if (listeners == null) {
            listeners = new LinkedList<>();
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void addListener(PropertySetChangeListener listener) {
        addPropertySetChangeListener(listener);
    }

    @Override
    public void removePropertySetChangeListener(PropertySetChangeListener listener) {
        if (listeners != null) {
            listeners.remove(listener);

            if (listeners.isEmpty()) {
                listeners = null;
            }
        }
    }

    @Override
    public void removeListener(PropertySetChangeListener listener) {
        removePropertySetChangeListener(listener);
    }

    @Override
    public void unsubscribe() {
        if (item instanceof CollectionDatasource) {
            CollectionDatasource datasource = (CollectionDatasource) item;
            // noinspection unchecked
            datasource.removeCollectionChangeListener(weakCollectionChangeListener);
            weakCollectionChangeListener = null;
        }

        item = null;
        metaClass = null;

        properties.values().forEach(PropertyWrapper::unsubscribe);
        properties.clear();
    }

    private class PropertySetChangeEvent implements Item.PropertySetChangeEvent {
        @Override
        public Item getItem() {
            return ItemWrapper.this;
        }
    }

    @Override
    public String toString() {
        final Entity entity = getItem();
        return entity == null ? "" : entity.getInstanceName();
    }

    public Entity getItem() {
        return item instanceof Datasource ? ((Datasource) item).getItem() : (Entity) item;
    }
}