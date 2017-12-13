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

package com.haulmont.cuba.web.toolkit.ui.client.renderers;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.InlineHTML;
import com.vaadin.v7.client.renderers.ClickableRenderer;
import com.vaadin.v7.client.widget.grid.RendererCellReference;

public class CubaClickableTextRenderer extends ClickableRenderer<String, InlineHTML> {
    @Override
    public InlineHTML createWidget() {
        InlineHTML inlineHTML = GWT.create(InlineHTML.class);
        inlineHTML.addClickHandler(this);
        inlineHTML.setStyleName("v-link");
        return inlineHTML;
    }

    @Override
    public void render(RendererCellReference cell, String text, InlineHTML inlineHTML) {
        inlineHTML.setText(text);
    }
}