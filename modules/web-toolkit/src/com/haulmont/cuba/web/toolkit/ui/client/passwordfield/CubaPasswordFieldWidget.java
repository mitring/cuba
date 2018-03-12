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

package com.haulmont.cuba.web.toolkit.ui.client.passwordfield;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VPasswordField;

import java.util.function.Consumer;

public class CubaPasswordFieldWidget extends VPasswordField implements KeyPressHandler {

    protected boolean capsLock = false;

    public Consumer<Boolean> capsLockStateChangeConsumer;
    protected HandlerRegistration clickHandlerRegistration = null;

    public void setAutocomplete(boolean autocomplete) {
        if (autocomplete) {
            getElement().removeAttribute("autocomplete");
        } else {
            BrowserInfo browser = BrowserInfo.get();

            if (browser.isIE()
                    || (browser.isGecko() && browser.getGeckoVersion() < 47)
                    || (browser.isChrome() && browser.getBrowserMajorVersion() < 49)) {
                getElement().setAttribute("autocomplete", "off");
            } else {
                getElement().setAttribute("autocomplete", "new-password");
            }
        }
    }

    public void setIndicateCapsLock(boolean indicateCapsLock) {
        if (indicateCapsLock) {
            if (clickHandlerRegistration == null) {
                clickHandlerRegistration = addKeyPressHandler(this);
            }
        } else if (clickHandlerRegistration != null) {
            clickHandlerRegistration.removeHandler();
            clickHandlerRegistration = null;
        }
    }

    protected boolean isMacOS() {
        String userAgent = Window.Navigator.getUserAgent();
        return userAgent.contains("Mac");
    }

    @Override
    public void onKeyPress(KeyPressEvent event) {
        int charCode = event.getCharCode();
        boolean shiftKey = event.isShiftKeyDown();
        boolean prevCapsLock = capsLock;

        if (charCode >= 97 && charCode <= 122) {
            capsLock = shiftKey;
        } else if (charCode >= 65 && charCode <= 90 && !(shiftKey && isMacOS())) {
            capsLock = !shiftKey;
        }

        if (capsLock != prevCapsLock) {
            if (capsLockStateChangeConsumer != null) {
                capsLockStateChangeConsumer.accept(capsLock);
            }
        }
    }
}