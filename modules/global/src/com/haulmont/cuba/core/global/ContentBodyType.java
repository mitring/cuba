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

package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

public enum ContentBodyType implements EnumClass<String>{
    TEXT("text/plain; charset=UTF-8"),
    HTML("text/html; charset=UTF-8");

    private String id;

    ContentBodyType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ContentBodyType fromId(String id) {
        for (ContentBodyType bodyType : ContentBodyType.values()) {
            if (id.equals(bodyType.getId())) {
                return bodyType;
            }
        }
        return null;
    }
}