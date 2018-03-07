/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.test.ui;

import com.google.common.collect.ImmutableMap;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.components.ComponentGenerationStrategy;
import com.haulmont.cuba.gui.components.DefaultComponentGenerationStrategy;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.FieldGroupTest;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.web.gui.WebComponentsFactory;
import com.vaadin.v7.data.util.converter.DefaultConverterFactory;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.GridLayout;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WebFieldGroupTest extends FieldGroupTest {
    @Mocked
    protected VaadinSession vaadinSession;

    @Override
    protected void initExpectations() {
        super.initExpectations();

        new NonStrictExpectations() {
            {
                vaadinSession.getLocale(); result = Locale.ENGLISH;
                VaadinSession.getCurrent(); result = vaadinSession;

                vaadinSession.getConverterFactory(); result = new DefaultConverterFactory();

                globalConfig.getAvailableLocales(); result = ImmutableMap.of("en", Locale.ENGLISH);
                AppContext.getProperty("cuba.mainMessagePack"); result = "com.haulmont.cuba.web";
            }
        };
    }

    @Override
    protected ComponentsFactory createComponentsFactory() {
        return new WebComponentsFactory() {
            @Override
            public List<ComponentGenerationStrategy> getComponentGenerationStrategies() {
                DefaultComponentGenerationStrategy strategy = new DefaultComponentGenerationStrategy(messages);
                strategy.setComponentsFactory(this);
                return Collections.singletonList(strategy);
            }
        };
    }

    @Override
    protected int getGridRows(FieldGroup fieldGroup) {
        return fieldGroup.unwrap(GridLayout.class).getRows();
    }

    @Override
    protected int getGridColumns(FieldGroup fieldGroup) {
        return fieldGroup.unwrap(GridLayout.class).getColumns();
    }

    @Override
    protected Object getGridCellComposition(FieldGroup fieldGroup, int col, int row) {
        return fieldGroup.unwrap(GridLayout.class).getComponent(col, row);
    }
}