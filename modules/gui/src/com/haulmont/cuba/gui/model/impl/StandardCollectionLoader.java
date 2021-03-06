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

package com.haulmont.cuba.gui.model.impl;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.queryconditions.Condition;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.DataContext;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StandardCollectionLoader<E extends Entity> implements CollectionLoader<E> {

    private ApplicationContext applicationContext;

    private DataContext dataContext;
    private CollectionContainer<E> container;
    private String query;
    private Condition condition;
    private Map<String, Object> parameters = new HashMap<>();
    private int firstResult = 0;
    private int maxResults = Integer.MAX_VALUE;
    private boolean softDeletion = true;
    private boolean cacheable;
    private View view;
    private String viewName;
    private Sort sort;

    public StandardCollectionLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected ViewRepository getViewRepository() {
        return applicationContext.getBean(ViewRepository.NAME, ViewRepository.class);
    }

    protected DataManager getDataManager() {
        return applicationContext.getBean(DataManager.NAME, DataManager.class);
    }

    @Nullable
    @Override
    public DataContext getDataContext() {
        return dataContext;
    }

    @Override
    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    @Override
    public void load() {
        if (container == null)
            throw new IllegalStateException("container is null");
        if (query == null)
            throw new IllegalStateException("query is null");

        @SuppressWarnings("unchecked")
        LoadContext<E> loadContext = LoadContext.create(container.getEntityMetaClass().getJavaClass());

        LoadContext.Query query = loadContext.setQueryString(this.query);

        query.setCondition(condition);
        query.setSort(sort);
        query.setParameters(parameters);

        query.setCacheable(cacheable);

        if (firstResult > 0)
            query.setFirstResult(firstResult);
        if (maxResults < Integer.MAX_VALUE)
            query.setMaxResults(maxResults);

        loadContext.setSoftDeletion(softDeletion);

        if (view == null && viewName != null) {
            this.view = getViewRepository().getView(container.getEntityMetaClass(), viewName);
        }
        if (view != null) {
            loadContext.setView(view);
        }

        List<E> list = getDataManager().loadList(loadContext);

        if (dataContext != null) {
            for (E entity : list) {
                dataContext.merge(entity);
            }
        }
        container.setItems(list);
    }

    @Override
    public CollectionContainer<E> getContainer() {
        return container;
    }

    @Override
    public void setContainer(CollectionContainer<E> container) {
        this.container = container;
        container.setSorter(new CollectionContainerSorter(this));
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters.clear();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public void setParameter(String name, Object value) {
        if (value == null || (value instanceof String && value.equals(""))) {
            parameters.remove(name);
        } else {
            parameters.put(name, value);
        }
    }

    @Override
    public int getFirstResult() {
        return firstResult;
    }

    @Override
    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    @Override
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public boolean isSoftDeletion() {
        return softDeletion;
    }

    @Override
    public void setSoftDeletion(boolean softDeletion) {
        this.softDeletion = softDeletion;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void setView(View view) {
        this.view = view;
    }

    @Override
    public void setView(String viewName) {
        if (this.view != null)
            throw new IllegalStateException("view is already set");
        this.viewName = viewName;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public void setSort(Sort sort) {
        if (sort == null || sort.getOrders().isEmpty()) {
            this.sort = null;
        } else {
            this.sort = sort;
        }
    }
}
