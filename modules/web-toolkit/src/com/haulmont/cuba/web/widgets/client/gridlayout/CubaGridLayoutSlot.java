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

package com.haulmont.cuba.web.widgets.client.gridlayout;

import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ui.ManagedLayout;
import com.vaadin.client.ui.layout.ComponentConnectorLayoutSlot;

public class CubaGridLayoutSlot extends ComponentConnectorLayoutSlot {

    public CubaGridLayoutSlot(String baseClassName, ComponentConnector child, ManagedLayout layout) {
        super(baseClassName, child, layout);
    }
}