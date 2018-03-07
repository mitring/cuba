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

package com.haulmont.cuba.web.widgets;

import com.haulmont.cuba.web.widgets.client.clientmanager.CubaClientManagerClientRpc;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;

import java.util.HashMap;
import java.util.Map;

public class CubaClientManager extends AbstractExtension {

    public void updateSystemMessagesLocale(SystemMessages msgs) {
        Map<String, String> localeMap = new HashMap<>();

        localeMap.put(CubaClientManagerClientRpc.COMMUNICATION_ERROR_CAPTION_KEY, msgs.communicationErrorCaption);
        localeMap.put(CubaClientManagerClientRpc.COMMUNICATION_ERROR_MESSAGE_KEY, msgs.communicationErrorMessage);

        localeMap.put(CubaClientManagerClientRpc.SESSION_EXPIRED_ERROR_CAPTION_KEY, msgs.sessionExpiredErrorCaption);
        localeMap.put(CubaClientManagerClientRpc.SESSION_EXPIRED_ERROR_MESSAGE_KEY, msgs.sessionExpiredErrorMessage);

        localeMap.put(CubaClientManagerClientRpc.AUTHORIZATION_ERROR_CAPTION_KEY, msgs.authorizationErrorCaption);
        localeMap.put(CubaClientManagerClientRpc.AUTHORIZATION_ERROR_MESSAGE_KEY, msgs.authorizationErrorMessage);

        getRpcProxy(CubaClientManagerClientRpc.class).updateSystemMessagesLocale(localeMap);
    }

    @Override
    public void extend(AbstractClientConnector target) {
        super.extend(target);
    }

    public static class SystemMessages {

        public String communicationErrorCaption;
        public String communicationErrorMessage;

        public String authorizationErrorCaption;
        public String authorizationErrorMessage;

        public String sessionExpiredErrorCaption;
        public String sessionExpiredErrorMessage;
    }
}