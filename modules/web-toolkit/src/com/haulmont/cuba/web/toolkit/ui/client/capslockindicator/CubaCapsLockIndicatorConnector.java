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

package com.haulmont.cuba.web.toolkit.ui.client.capslockindicator;

import com.haulmont.cuba.web.toolkit.ui.CubaCapsLockIndicator;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.label.LabelConnector;
import com.vaadin.shared.ui.Connect;

@Connect(CubaCapsLockIndicator.class)
public class CubaCapsLockIndicatorConnector extends LabelConnector implements CapsLockChangeHandler {

    @Override
    public CubaCapsLockIndicatorWidget getWidget() {
        return (CubaCapsLockIndicatorWidget) super.getWidget();
    }

    @Override
    public CubaCapsLockIndicatorState getState() {
        return (CubaCapsLockIndicatorState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        if (stateChangeEvent.hasPropertyChanged("isCapsLock")) {
            if (getState().isCapsLock) {
                getWidget().removeStyleName("caps-lock-off");
                getWidget().addStyleName("caps-lock-on");
            } else {
                getWidget().removeStyleName("caps-lock-on");
                getWidget().addStyleName("caps-lock-off");
            }
        }

        super.onStateChanged(stateChangeEvent);
    }

    @Override
    public void changeState(boolean isCapsLock) {
        getState().isCapsLock = isCapsLock;
        forceStateChange();
    }
}