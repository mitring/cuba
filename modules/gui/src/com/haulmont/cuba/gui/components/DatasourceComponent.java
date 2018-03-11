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

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.chile.core.model.MetaProperty;

/**
 * A component that represents data from one property of a datasource.
 * vaadin8
 */
@Deprecated
public interface DatasourceComponent<T> extends Component, Component.HasValue<T> {

    /**
     * vaadin8
     *
     * @return datasource instance
     */
    @Deprecated
    Datasource getDatasource();

    /**
     * vaadin8
     * @deprecated Use {@link #getMetaPropertyPath()}
     * @return datasource property
     */
    @Deprecated
    MetaProperty getMetaProperty();

    /**
     * vaadin8
     *
     * @return datasource property path
     */
    @Deprecated
    MetaPropertyPath getMetaPropertyPath();

    /**
     * Set datasource and its property.
     *
     * vaadin8
     */
    @Deprecated
    void setDatasource(Datasource datasource, String property);
}