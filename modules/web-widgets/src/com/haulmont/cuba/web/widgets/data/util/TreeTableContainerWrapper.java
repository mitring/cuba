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

package com.haulmont.cuba.web.widgets.data.util;

import com.haulmont.cuba.web.widgets.data.AggregationContainer;
import com.haulmont.cuba.web.widgets.data.TableContainer;
import com.haulmont.cuba.web.widgets.data.TreeTableContainer;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.util.ContainerHierarchicalWrapper;

import java.util.*;

public class TreeTableContainerWrapper
        extends ContainerHierarchicalWrapper
        implements TreeTableContainer, AggregationContainer, Container.Ordered, Container.Sortable {

    protected Set<Object> expanded; // Contains expanded items ids

    protected LinkedList<Object> inline; // Contains visible (including children of expanded items) items ids inline

    protected Hashtable<Object, String> captions;

    protected Object first;

    protected boolean treeTableContainer;

    public TreeTableContainerWrapper(Container toBeWrapped) {
        super(toBeWrapped);
        treeTableContainer = toBeWrapped instanceof TreeTableContainer;

        inline = new LinkedList<>();
        expanded = new HashSet<>();
        captions = new Hashtable<>();
    }

    @Override
    public void updateHierarchicalWrapper() {
        super.updateHierarchicalWrapper();

        updateFirst();

        if (inline == null || expanded == null || captions == null) {
            inline = new LinkedList<>();
            expanded = new HashSet<>();
            captions = new Hashtable<>();
        } else {
            inline.clear();
            final Set<Object> s = new HashSet<>();
            s.addAll(expanded);
            s.addAll(captions.keySet());
            for (Object o : s) {
                if (!_container().containsId(o)) {
                    expanded.remove(o);
                    captions.remove(o);
                }
            }
        }
        makeInlineElements(inline, rootItemIds());
    }

    @Override
    protected void addToHierarchyWrapper(Object itemId) {
        super.addToHierarchyWrapper(itemId);

        // Add item to the end of the list
        if (!inline.contains(itemId)) {
            inline.add(itemId);
            if (areChildrenAllowed(itemId)) {
                makeInlineElements(inline, getChildren(itemId));
            }
        }
    }

    @Override
    protected void removeFromHierarchyWrapper(Object itemId) {
        boolean b = isFirstId(itemId);

        if (containsInline(itemId)) {
            List<Object> inlineChildren;
            if (areChildrenAllowed(itemId)
                    && (inlineChildren = getInlineChildren(itemId)) != null) {
                inline.removeAll(inlineChildren);
            }
            inline.remove(itemId);
        }

        super.removeFromHierarchyWrapper(itemId);

        if (b) {
            updateFirst();
        }
    }

    @Override
    public boolean setParent(Object itemId, Object newParentId) {
        if (itemId == null) {
            throw new NullPointerException("Item id cannot be NULL");
        }

        if (!_container().containsId(itemId)) {
            return false;
        }

        final Object oldParentId = getParent(itemId);

        if ((newParentId == null && oldParentId == null)
                || (newParentId != null && newParentId.equals(oldParentId))) {
            return true;
        }

        boolean b = super.setParent(itemId, newParentId);
        if (b) {
            final LinkedList<Object> inlineList = new LinkedList<>();
            inlineList.add(itemId);
            inlineList.addAll(getInlineChildren(itemId));

            if (containsInline(itemId)) {
                inline.removeAll(inlineList);
            }

            if (containsInline(newParentId)
                    && areChildrenAllowed(newParentId)
                    && isExpanded(newParentId)) {
                int lastChildInlineIndex = lastInlineIndex(newParentId);
                if (lastChildInlineIndex > -1) {
                    inline.addAll(lastChildInlineIndex + 1, inlineList);
                } else {
                    inline.addAll(inlineIndex(newParentId) + 1, inlineList);
                }
            }
        }
        return b;
    }

    @Override
    public int size() {
        return inline.size();
    }

    @Override
    public Object nextItemId(Object itemId) {
        if (itemId == null) {
            return null;
        }
        int index = inlineIndex(itemId);
        if (index == -1 || isLastId(itemId)) {
            return null;
        }
        return inline.get(index + 1);
    }

    @Override
    public Object prevItemId(Object itemId) {
        if (itemId == null) {
            return null;
        }
        int index = inlineIndex(itemId);
        if (index == -1 || isFirstId(itemId)) {
            return null;
        }
        return inline.get(index - 1);
    }

    @Override
    public Object firstItemId() {
        return first;
    }

    @Override
    public Object lastItemId() {
        return inline.peekLast();
    }

    @Override
    public boolean isFirstId(Object itemId) {
        return itemId != null && itemId.equals(first);
    }

    @Override
    public boolean isLastId(Object itemId) {
        return itemId != null && itemId.equals(lastItemId());
    }

    @Override
    public Object addItemAfter(Object previousItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item addItemAfter(Object previousItemId, Object newItemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCaption(Object itemId) {
        if (itemId != null) {
            if (!treeTableContainer) {
                return captions.containsKey(itemId);
            } else {
                return ((TreeTableContainer) _container()).isCaption(itemId);
            }
        }
        throw new NullPointerException("Item id cannot be NULL");
    }

    @Override
    public String getCaption(Object itemId) {
        if (itemId != null) {
            if (!treeTableContainer) {
                return captions.get(itemId);
            } else {
                return ((TreeTableContainer) _container()).getCaption(itemId);
            }
        }
        throw new NullPointerException("Item id cannot be NULL");
    }

    @Override
    public boolean setCaption(Object itemId, String caption) {
        if (itemId != null) {
            if (!treeTableContainer) {
                if (caption != null) {
                    captions.put(itemId, caption);
                } else {
                    captions.remove(itemId);
                }
                return true;
            } else {
                return ((TreeTableContainer) _container()).setCaption(itemId, caption);
            }
        }
        throw new NullPointerException("Item id cannot be NULL");
    }

    @Override
    public int getLevel(Object itemId) {
        if (itemId != null) {
            if (!treeTableContainer) {
                return getItemLevel(itemId);
            } else {
                return ((TreeTableContainer) _container()).getLevel(itemId);
            }
        }
        throw new NullPointerException("Item id cannot be NULL");
    }

    protected int getItemLevel(Object itemId) {
        if (rootItemIds().size() == _container().size()) //no children;
            return -1;
        Object parentId;
        if ((parentId = getParent(itemId)) == null) {
            return 0;
        }
        return getItemLevel(parentId) + 1;
    }

    public boolean isExpanded(Object itemId) {
        if (itemId == null) {
            throw new NullPointerException("Item id cannot be NULL");
        }
        return expanded.contains(itemId);
    }

    public boolean setExpanded(Object itemId) {
        if (itemId == null) {
            throw new NullPointerException("Item id cannot be NULL");
        }

        if (areChildrenAllowed(itemId)) {
            if (isExpanded(itemId)) {
                return true;
            }
            expanded.add(itemId);

            int itemIndex;
            if ((itemIndex = inlineIndex(itemId)) > -1) {
                final List<Object> inlineChildren = getInlineChildren(itemId);
                if (inlineChildren != null) {
                    inline.addAll(itemIndex + 1, inlineChildren);
                }
            }

            return true;
        }
        return false;
    }

    public boolean setCollapsed(Object itemId) {
        if (itemId == null) {
            throw new NullPointerException("Item id cannot be NULL");
        }

        if (areChildrenAllowed(itemId)) {
            if (!isExpanded(itemId)) {
                return true;
            }

            if (containsInline(itemId)) {
                final List<Object> inlineChildren = getInlineChildren(itemId);
                if (inlineChildren != null) {
                    inline.removeAll(inlineChildren);
                }
            }

            expanded.remove(itemId);

            return true;
        }
        return false;
    }

    public void expandAll() {
        collapseAll();
        if (_hierarchical()) {
            expandAll(rootItemIds());
        } else {
            if (_children() != null) {
                for (final Object itemId : _children().keySet()) {
                    setExpanded(itemId);
                }
            }
        }
    }

    protected void expandAll(Collection itemIds) {
        for (final Object itemId : itemIds) {
            if (areChildrenAllowed(itemId) && hasChildren(itemId)) {
                setExpanded(itemId);
                expandAll(getChildren(itemId));
            }
        }
    }

    public boolean isAllCollapsed() {
        return expanded.isEmpty();
    }

    public void collapseAll() {
        expanded.clear();
        inline.clear();
        makeInlineElements(inline, rootItemIds());
    }

    protected void collapseAll(Collection itemIds) {
        for (final Object itemId : itemIds) {
            if (areChildrenAllowed(itemId) && hasChildren(itemId)) {
                setCollapsed(itemId);
                collapseAll(getChildren(itemId));
            }
        }
    }

    protected LinkedList<Object> getInlineChildren(Object itemId) {
        if (areChildrenAllowed(itemId)) {
            final LinkedList<Object> inlineChildren = new LinkedList<>();
            if (isExpanded(itemId)) {
                makeInlineElements(inlineChildren, getChildren(itemId));
            }
            return inlineChildren;
        }
        return null;
    }

    private void makeInlineElements(final List<Object> inline, final Collection elements) {
        if (elements != null) {
            for (final Object e : elements) {
                inline.add(e);
                if (areChildrenAllowed(e) && isExpanded(e)) {
                    makeInlineElements(inline, getChildren(e));
                }
            }
        }
    }

    private void updateFirst() {
        Collection roots = rootItemIds();
        if (roots.size() > 0) {
            first = roots.iterator().next();
        } else {
            first = null;
        }
    }

    private boolean containsInline(Object itemId) {
        return inline.contains(itemId);
    }

    private int inlineIndex(Object itemId) {
        return inline.indexOf(itemId);
    }

    private int lastInlineIndex(Object itemId) {
        LinkedList<Object> inlineChildren = getInlineChildren(itemId);
        if (inlineChildren != null && !inlineChildren.isEmpty()) {
            return inlineIndex(inlineChildren.getLast());
        }
        return -1;
    }

    @Override
    public void sort(Object[] propertyId, boolean[] ascending) {
        if (_container() instanceof Sortable) {
            ((Sortable) _container()).sort(propertyId, ascending);
            updateHierarchicalWrapper();
        } else
            throw new IllegalStateException("Wrapped container is not Sortable: " + _container().getClass());
    }

    @Override
    public Collection getSortableContainerPropertyIds() {
        if (_container() instanceof Sortable)
            return ((Sortable) _container()).getSortableContainerPropertyIds();
        else
            throw new IllegalStateException("Wrapped container is not Sortable: " + _container().getClass());
    }

    @Override
    public Collection getAggregationPropertyIds() {
        if (_container() instanceof AggregationContainer) {
            return ((AggregationContainer) _container()).getAggregationPropertyIds();
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + _container().getClass());
    }

    @Override
    public Type getContainerPropertyAggregation(Object propertyId) {
        if (_container() instanceof AggregationContainer) {
            return ((AggregationContainer) _container()).getContainerPropertyAggregation(propertyId);
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + _container().getClass());
    }

    @Override
    public void addContainerPropertyAggregation(Object propertyId, Type type) {
        if (_container() instanceof AggregationContainer) {
            ((AggregationContainer) _container()).addContainerPropertyAggregation(propertyId, type);
        } else {
            throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                    + _container().getClass());
        }
    }

    @Override
    public void removeContainerPropertyAggregation(Object propertyId) {
        if (_container() instanceof AggregationContainer) {
            ((AggregationContainer) _container()).removeContainerPropertyAggregation(propertyId);
        } else {
            throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                    + _container().getClass());
        }
    }

    @Override
    public Map<Object, Object> aggregate(Context context) {
        if (_container() instanceof AggregationContainer) {
            return ((AggregationContainer) _container()).aggregate(context);
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + _container().getClass());
    }

    @Override
    public void resetSortOrder() {
        if (_container() instanceof TableContainer) {
            ((TableContainer) _container()).resetSortOrder();
        }
    }
}