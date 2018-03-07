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
 */

package com.haulmont.cuba.web.toolkit.ui.client.notification;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.vaadin.client.ui.VNotification;

import static com.haulmont.cuba.web.toolkit.ui.client.appui.AppUIConnector.CUBA_NOTIFICATION_MODALITY_CURTAIN;

public class CubaNotification extends VNotification {
    public static final String TRAY_STYLE = "tray";

    @Override
    public boolean onEventPreview(Event event) {
        int type = DOM.eventGetType(event);

        if ((type == Event.ONCLICK || type == Event.ONTOUCHEND)
                && event.getEventTarget() != null) {
            Element target = Element.as(event.getEventTarget());
            if (target.getClassName() != null && target.getClassName().contains(CUBA_NOTIFICATION_MODALITY_CURTAIN)) {
                hide();
                return false;
            }
        }

        if (type == Event.ONKEYDOWN && event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
            if (!getElement().getClassName().contains(TRAY_STYLE)) {
                hide();
                return false;
            }
        }

        return super.onEventPreview(event);
    }
}