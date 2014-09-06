package com.kozzztya.cycletraining;

import android.widget.BaseExpandableListAdapter;

import com.kozzztya.customview.CardView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract adapter for ExpandableListView with card style and filter of children
 *
 * @param <G> Group type
 * @param <C> Child type
 */

public abstract class MyExpListAdapter<G, C> extends BaseExpandableListAdapter {

    private Map<G, List<C>> mGroupsChildrenMap;
    private Map<G, List<C>> mOriginalMap;
    private List<G> mGroups;
    private List<List<C>> mChildren;

    public MyExpListAdapter(Map<G, List<C>> groupsChildrenMap) {
        mGroupsChildrenMap = groupsChildrenMap;

        mOriginalMap = new LinkedHashMap<>(groupsChildrenMap);
        mGroups = new ArrayList<>(groupsChildrenMap.keySet());
        mChildren = new ArrayList<>(groupsChildrenMap.values());
    }

    @Override
    public int getGroupCount() {
        return mGroupsChildrenMap.size();
    }

    @Override
    public int getChildrenCount(int pos) {
        return getChildrenOfGroup(pos).size();
    }

    @Override
    public G getGroup(int pos) {
        return mGroups.get(pos);
    }

    @Override
    public C getChild(int groupPos, int childPos) {
        return getChildrenOfGroup(groupPos).get(childPos);
    }

    public List<C> getChildrenOfGroup(int pos) {
        return mChildren.get(pos);
    }

    @Override
    public long getGroupId(int pos) {
        return pos;
    }

    @Override
    public long getChildId(int groupPos, int childPos) {
        return childPos;
    }

    @Override
    public boolean isChildSelectable(int groupPos, int childPos) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    protected void imitateCardGroup(boolean isExpanded, CardView cardView) {
        if (cardView != null) {
            cardView.setBottomShadow(!isExpanded);
        }
    }

    protected void imitateCardChild(boolean isLastChild, CardView cardView) {
        if (cardView != null) {
            cardView.setTopShadow(false);
            cardView.setBottomShadow(isLastChild);
        }
    }

    /**
     * Filter by children's method return value
     *
     * @param method      Getter or another method that returns child's value
     * @param filterValue Number or text value for comparison
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */

    public void filterChildren(Method method, Object filterValue) throws
            InvocationTargetException, IllegalAccessException {
        Map<G, List<C>> filtered = new LinkedHashMap<>();

        int groupCount = getGroupCount();
        for (int groupPos = 0; groupPos < groupCount; groupPos++) {
            G group = getGroup(groupPos);

            int childrenCount = getChildrenCount(groupPos);
            for (int childPos = 0; childPos < childrenCount; childPos++) {
                C child = getChild(groupPos, childPos);
                Object childValue = method.invoke(child, (Object[]) null);

                boolean comparison = false;

                if (childValue instanceof String && filterValue instanceof String) {
                    //Text comparison
                    String childStringValue = ((String) childValue).toLowerCase();
                    String filterStringValue = ((String) filterValue).toLowerCase();

                    comparison = childStringValue.contains(filterStringValue);
                } else {
                    //Simple object comparison
                    comparison = childValue.equals(filterValue);
                }

                //Put filtered values by groups
                if (comparison) {
                    if (!filtered.containsKey(group))
                        filtered.put(group, new ArrayList<C>());
                    filtered.get(group).add(child);
                }
            }
        }

        setItemsMap(filtered);
    }

    public void resetFilter() {
        setItemsMap(mOriginalMap);
    }

    public void setItemsMap(Map<G, List<C>> map) {
        mGroupsChildrenMap = new LinkedHashMap<>(map);
        mGroups = new ArrayList<>(map.keySet());
        mChildren = new ArrayList<>(map.values());
        notifyDataSetChanged();
    }
}
