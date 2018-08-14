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

package com.haulmont.cuba.gui.components.data.options;

import com.haulmont.bali.events.EventHub;
import com.haulmont.bali.events.Subscription;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.EntityOptionsSource;
import com.haulmont.cuba.gui.components.data.OptionsSource;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsHelper;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CollectionDatasourceOptions<E extends Entity<K>, K> implements OptionsSource<E>, EntityOptionsSource<E> {

    protected CollectionDatasource<E, K> datasource;
    protected EventHub events = new EventHub();

    protected BindingState state = BindingState.INACTIVE;

    public CollectionDatasourceOptions(CollectionDatasource<E, K> datasource) {
        this.datasource = datasource;

        this.datasource.addStateChangeListener(this::datasourceStateChanged);
        this.datasource.addItemPropertyChangeListener(this::datasourceItemPropertyChanged);
        this.datasource.addCollectionChangeListener(this::datasourceCollectionChanged);

        CollectionDsHelper.autoRefreshInvalid(datasource, true);

        if (datasource.getState() == Datasource.State.VALID) {
            setState(BindingState.ACTIVE);
        }
    }

    protected void datasourceCollectionChanged(CollectionDatasource.CollectionChangeEvent<E, K> e) {
        events.publish(OptionsChangeEvent.class, new OptionsChangeEvent<>(this));
    }

    @SuppressWarnings("unchecked")
    protected void datasourceItemPropertyChanged(Datasource.ItemPropertyChangeEvent<E> e) {
        events.publish(ValueChangeEvent.class, new ValueChangeEvent(this, e.getPrevValue(), e.getValue()));
    }

    protected void datasourceStateChanged(Datasource.StateChangeEvent<E> e) {
        if (e.getState() == Datasource.State.VALID) {
            setState(BindingState.ACTIVE);
        } else {
            setState(BindingState.INACTIVE);
        }
    }

    public CollectionDatasource<E, K> getDatasource() {
        return datasource;
    }

    @Override
    public Stream<E> getOptions() {
        return datasource.getItems().stream();
    }

    @Override
    public BindingState getState() {
        if (datasource.getState() == Datasource.State.VALID) {
            return BindingState.ACTIVE;
        }
        return BindingState.INACTIVE;
    }

    public void setState(BindingState state) {
        if (this.state != state) {
            this.state = state;

            events.publish(StateChangeEvent.class, new StateChangeEvent<>(this, state));
        }
    }

    @Override
    public boolean containsItem(E item) {
        return datasource.containsItem(item.getId());
    }

    @Override
    public void updateItem(E item) {
        datasource.updateItem(item);
    }

    @Override
    public void refresh() {
        datasource.refresh();
    }

    @Override
    public void refresh(Map<String, Object> parameters) {
        datasource.refresh(parameters);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Subscription addStateChangeListener(Consumer<StateChangeEvent<E>> listener) {
        return events.subscribe(StateChangeEvent.class, (Consumer) listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Subscription addValueChangeListener(Consumer<ValueChangeEvent<E>> listener) {
        return events.subscribe(ValueChangeEvent.class, (Consumer) listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Subscription addOptionsChangeListener(Consumer<OptionsChangeEvent<E>> listener) {
        return events.subscribe(OptionsChangeEvent.class, (Consumer) listener);
    }

    @Override
    public MetaClass getEntityMetaClass() {
        return datasource.getMetaClass();
    }

    @Override
    public void setSelectedItem(E item) {
        datasource.setItem(item);
    }
}