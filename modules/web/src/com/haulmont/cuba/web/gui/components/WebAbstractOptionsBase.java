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

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.haulmont.cuba.web.gui.components.converters.ObjectToObjectConverter;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.ui.AbstractSelect;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public abstract class WebAbstractOptionsBase<T extends com.vaadin.v7.ui.AbstractSelect> extends
        WebAbstractOptionsField<T> {

    @Override
    public void setOptionsDatasource(CollectionDatasource datasource) {
        super.setOptionsDatasource(datasource);

        assignAutoDebugId();
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        super.setDatasource(datasource, property);

        assignAutoDebugId();
    }

    protected <V> V getValueFromKey(Object key) {
        if (key instanceof Collection) {
            final Set<Object> set = new LinkedHashSet<>();
            for (Object o : (Collection) key) {
                Object t = getValue(o);
                set.add(t);
            }
            return (V) set;
        } else {
            final Object o = getValue(key);
            return wrapAsCollection(o);
        }
    }

    protected Object getValue(Object o) {
        Object t;
        if (o instanceof Enum) {
            t = o;
        } else if (o instanceof Entity) {
            t = o;
        } else if (optionsDatasource != null) {
            if (Datasource.State.INVALID == optionsDatasource.getState()) {
                optionsDatasource.refresh();
            }
            t = optionsDatasource.getItem(o);
        } else {
            t = o;
        }
        return t;
    }

    @Override
    public void setValue(Object value) {
        // TODO (abramov) need to be changed
        super.setValue(getKeyFromValue(value));
    }

    protected Object getKeyFromValue(Object value) {
        Object v;
        if (isMultiSelect()) {
            if (value instanceof Collection) {
                final Set<Object> set = new LinkedHashSet<>();
                for (Object o : (Collection) value) {
                    Object t = getKey(o);
                    set.add(t);
                }
                v = set;
            } else {
                v = getKey(value);
            }
        } else {
            v = getKey(value);
        }

        return v;
    }

    protected Object getKey(Object o) {
        Object t;
        if (o instanceof Entity) {
            if (optionsDatasource != null) {
                if (Datasource.State.INVALID == optionsDatasource.getState()) {
                    optionsDatasource.refresh();
                }
                return ((Entity) o).getId();
            } else {
                if ((optionsList != null) || (optionsMap != null)) {
                    return o;
                }
                return ((Entity) o).getId();
            }
        } /*else if (o instanceof Enum) {
            t = o;
        } */ else {
            t = o;
        }
        return t;
    }

    protected void initDefaults(AbstractSelect component){
        component.setInvalidCommitted(true);
        component.setItemCaptionMode(AbstractSelect.ItemCaptionMode.ITEM);
        component.setConverter(new ObjectToObjectConverter());
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <V> V getValue() {
        if (optionsDatasource != null) {
            final Object key = super.getValue();
            return getValueFromKey(key);
        } else {
            return wrapAsCollection(super.getValue());
        }
    }

    @Override
    protected ItemWrapper createDatasourceWrapper(Datasource datasource, Collection<MetaPropertyPath> propertyPaths) {
        return new ItemWrapper(datasource, datasource.getMetaClass(), propertyPaths) {

            @Override
            protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
                return new CollectionPropertyWrapper(item, propertyPath);
            }
        };
    }

    public static class CollectionPropertyWrapper extends PropertyWrapper {
        public CollectionPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
            super(item, propertyPath);
        }

        @Override
        public void setValue(Object newValue) throws ReadOnlyException, Converter.ConversionException {
            if (newValue instanceof Collection) {
                Class propertyType = propertyPath.getMetaProperty().getJavaType();
                if (Set.class.isAssignableFrom(propertyType)) {
                    newValue = new LinkedHashSet<>((Collection<?>) newValue);
                } else if (List.class.isAssignableFrom(propertyType)) {
                    newValue = new ArrayList<>((Collection<?>) newValue);
                }
            }
            super.setValue(newValue);
        }

        @Override
        public Object getValue() {
            Object value = super.getValue();
            if (value instanceof Collection) {
                Class propertyType = propertyPath.getMetaProperty().getJavaType();
                if (Set.class.isAssignableFrom(propertyType)) {
                    value = new LinkedHashSet<>((Collection<?>) value);
                } else if (List.class.isAssignableFrom(propertyType)) {
                    value = new ArrayList<>((Collection<?>) value);
                }
            }
            return value;
        }

        @Override
        public Class getType() {
            return Object.class;
        }
    }

    @Override
    protected String getAlternativeDebugId() {
        if (id != null) {
            return id;
        }
        if (datasource != null && StringUtils.isNotEmpty(datasource.getId()) && metaPropertyPath != null) {
            return getClass().getSimpleName() + datasource.getId() + "_" + metaPropertyPath.toString();
        }
        if (optionsDatasource != null && StringUtils.isNotEmpty(optionsDatasource.getId())) {
            return getClass().getSimpleName() + optionsDatasource.getId();
        }

        return getClass().getSimpleName();
    }

    @Override
    public void setRequired(boolean required){
        super.setRequired(required);
    }
}