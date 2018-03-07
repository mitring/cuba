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

import com.haulmont.cuba.gui.components.OptionsList;
import com.haulmont.cuba.web.toolkit.ui.CubaListSelect;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.converter.Converter;

public class WebOptionsList extends WebAbstractOptionsBase<CubaListSelect> implements OptionsList {
    public WebOptionsList() {
        component = new CubaListSelect() {
            @Override
            public void setPropertyDataSource(Property newDataSource) {
                if (newDataSource == null) {
                    super.setPropertyDataSource(null);
                    return;
                }

                super.setPropertyDataSource(new PropertyAdapter(newDataSource) {

                    @Override
                    public Class getType() {
                        // we ourselves convert values in this property adapter
                        return Object.class;
                    }

                    @Override
                    public Object getValue() {
                        final Object o = itemProperty.getValue();
                        return getKeyFromValue(o);
                    }

                    @Override
                    public void setValue(Object newValue) throws ReadOnlyException, Converter.ConversionException {
                        final Object v = getValueFromKey(newValue);
                        itemProperty.setValue(v);
                    }
                });
            }
        };
        attachListener(component);
        initDefaults(component);
    }

    @Override
    public void setNullOptionVisible(boolean nullOptionVisible) {
        component.setNullSelectionAllowed(nullOptionVisible);
    }

    @Override
    public boolean isNullOptionVisible() {
        return component.isNullSelectionAllowed();
    }
}