/*
 * Copyright (c) 2008-2018 Haulmont.
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
 */

package com.haulmont.cuba.gui.components.data;

import com.haulmont.bali.events.Subscription;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.data.Datasource;

import java.util.function.Consumer;

// todo
public class DatasourceValueSource implements EntityValueSource {

    public DatasourceValueSource(Datasource datasource, String property) {

    }

    public Datasource getDatasource() {
        return null;
    }

    @Override
    public MetaClass getMetaClass() {
        return null;
    }

    @Override
    public MetaPropertyPath getMetaPropertyPath() {
        return null;
    }

    @Override
    public Entity getItem() {
        return null;
    }

    @Override
    public Subscription addInstanceChangeListener(Consumer listener) {
        return null;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {

    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Class getType() {
        return null;
    }

    @Override
    public ValueSourceStatus getStatus() {
        return null;
    }

    @Override
    public void addValueChangeListener(Component.ValueChangeListener listener) {

    }

    @Override
    public void removeValueChangeListener(Component.ValueChangeListener listener) {

    }
}