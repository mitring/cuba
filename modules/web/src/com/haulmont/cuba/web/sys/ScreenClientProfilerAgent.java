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

package com.haulmont.cuba.web.sys;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.web.ScreenProfiler;
import com.haulmont.cuba.web.widgets.client.profiler.ScreenClientProfilerServerRpc;
import com.haulmont.cuba.web.widgets.client.profiler.ScreenClientProfilerState;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.AbstractExtension;

public class ScreenClientProfilerAgent extends AbstractExtension {

    protected ScreenProfiler screenProfiler = AppBeans.get(ScreenProfiler.NAME);

    public ScreenClientProfilerAgent() {
        registerRpc((ScreenClientProfilerServerRpc) clientEvents ->
                screenProfiler.flush(clientEvents)
        );
    }

    @Override
    public void extend(AbstractClientConnector target) {
        super.extend(target);
    }

    @Override
    protected ScreenClientProfilerState getState() {
        return (ScreenClientProfilerState) super.getState();
    }

    @Override
    protected ScreenClientProfilerState getState(boolean markAsDirty) {
        return (ScreenClientProfilerState) super.getState(markAsDirty);
    }

    public void setFlushTimeout(int flushTimeout) {
        getState().flushTimeout = flushTimeout;
    }

    public int getFlushTimeout() {
        return getState(false).flushTimeout;
    }

    public void setFlushEventsCount(int flushEventsCount) {
        getState().flushEventsCount = flushEventsCount;
    }

    public int getFlushEventsCount() {
        return getState(false).flushEventsCount;
    }
}