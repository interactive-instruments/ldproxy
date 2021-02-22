/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Based on the k-d tree implementation by Justin Wetherell <phishman3579@gmail.com>.
 * See https://github.com/phishman3579/java-algorithms-implementation/blob/master/src/com/jwetherell/algorithms/data_structures/KdTree.java.
 */
public class KdTree {

    private final int k;
    private KdNode root = null;

    public KdTree(int k) {
        this.k = k;
    }

    public KdTree(int k, List<NdPoint> list) {
        this.k = k;
        root = createNode(list, 0);
    }

    private KdNode createNode(List<NdPoint> list, int depth) {
        if (list == null || list.size() == 0)
            return null;

        list.sort(Comparator.comparingDouble(o -> o.val[depth % k]));

        KdNode node = null;
        List<NdPoint> less = new ArrayList<>(list.size());
        List<NdPoint> more = new ArrayList<>(list.size());
        if (list.size() > 0) {
            int medianIndex = list.size() / 2;
            node = new KdNode(list.get(medianIndex), depth);
            // Process list to see where each non-median point lies
            for (int i = 0; i < list.size(); i++) {
                if (i == medianIndex)
                    continue;
                NdPoint p = list.get(i);
                // Cannot assume points before the median are less since they could be equal
                if (node.compareTo(depth, p) <= 0) {
                    less.add(p);
                } else {
                    more.add(p);
                }
            }

            if ((medianIndex-1 >= 0) && less.size() > 0) {
                node.lesser = createNode(less, depth + 1);
                node.lesser.parent = node;
            }

            if ((medianIndex <= list.size()-1) && more.size() > 0) {
                node.greater = createNode(more, depth + 1);
                node.greater.parent = node;
            }
        }

        return node;
    }

    public void add(NdPoint value) {
        if (value == null)
            return;

        if (root == null) {
            root = new KdNode(value);
            return;
        }

        KdNode node = root;
        while (true) {
            if (node.compareTo(node.depth, value) <= 0) {
                // Lesser
                if (node.lesser == null) {
                    KdNode newNode = new KdNode(value, node.depth + 1);
                    newNode.parent = node;
                    node.lesser = newNode;
                    break;
                }
                node = node.lesser;
            } else {
                // Greater
                if (node.greater == null) {
                    KdNode newNode = new KdNode(value, node.depth + 1);
                    newNode.parent = node;
                    node.greater = newNode;
                    break;
                }
                node = node.greater;
            }
        }
    }

    public boolean contains(NdPoint value) {
        if (value == null || root == null)
            return false;

        KdNode node = getNode(value);
        return (node != null);
    }

    private KdNode getNode(NdPoint value) {
        if (root == null || value == null)
            return null;

        KdNode node = root;
        while (true) {
            if (node.id.equals(value)) {
                return node;
            } else if (node.compareTo(node.depth, value) <= 0) {
                // Lesser
                if (node.lesser == null) {
                    return null;
                }
                node = node.lesser;
            } else {
                // Greater
                if (node.greater == null) {
                    return null;
                }
                node = node.greater;
            }
        }
    }

    public boolean remove(NdPoint value) {
        if (value == null || root == null)
            return false;

        KdNode node = getNode(value);
        if (node == null)
            return false;

        KdNode parent = node.parent;
        List<NdPoint> nodes = getTree(node);
        if (parent != null) {
            if (node.equals(parent.lesser)) {
                if (nodes.size() > 0) {
                    parent.lesser = createNode(nodes, node.depth);
                    if (parent.lesser != null) {
                        parent.lesser.parent = parent;
                    }
                } else {
                    parent.lesser = null;
                }
            } else {
                if (nodes.size() > 0) {
                    parent.greater = createNode(nodes, node.depth);
                    if (parent.greater != null) {
                        parent.greater.parent = parent;
                    }
                } else {
                    parent.greater = null;
                }
            }
        } else {
            // root
            if (nodes.size() > 0)
                root = createNode(nodes, node.depth);
            else
                root = null;
        }

        return true;
    }

    private List<NdPoint> getTree(KdNode subroot) {
        List<NdPoint> list = new ArrayList<>();
        if (subroot == null)
            return list;

        if (subroot.lesser != null) {
            list.add(subroot.lesser.id);
            list.addAll(getTree(subroot.lesser));
        }
        if (subroot.greater != null) {
            list.add(subroot.greater.id);
            list.addAll(getTree(subroot.greater));
        }

        return list;
    }

    public List<NdPoint> nearestNeighbourSearch(int K, float maxDistance, NdPoint value) {
        if (value == null || root == null)
            return ImmutableList.of();

        // Map used for results
        TreeSet<KdNode> results = new TreeSet<>(new NodeComparator(value));

        // Find the closest leaf node
        KdNode prev = null;
        KdNode node = root;
        while (node != null) {
            if (node.compareTo(node.depth, value) <= 0) {
                // Lesser
                prev = node;
                node = node.lesser;
            } else {
                // Greater
                prev = node;
                node = node.greater;
            }
        }
        KdNode leaf = prev;

        // Used to not re-examine nodes
        Set<KdNode> examined = new HashSet<>();

        // Go up the tree, looking for better solutions
        node = leaf;
        while (node != null) {
            // Search node
            searchNode(value, node, K, maxDistance, results, examined);
            node = node.parent;
        }

        return results.stream()
                      .map(n -> n.id)
                      .collect(Collectors.toList());
    }

    private void searchNode(NdPoint value, KdNode node, int K, float maxDistance, TreeSet<KdNode> results, Set<KdNode> examined) {
        examined.add(node);

        // Search node
        KdNode lastNode = null;
        float lastDistance = Float.MAX_VALUE;
        if (results.size() > 0) {
            lastNode = results.last();
            lastDistance = lastNode.id.distance(value);
        }
        Float nodeDistance = node.id.distance(value);
        if (nodeDistance <= maxDistance) {
            if (nodeDistance.compareTo(lastDistance) < 0) {
                if (results.size() == K && lastNode != null)
                    results.remove(lastNode);
                results.add(node);
            } else if (nodeDistance.equals(lastDistance)) {
                results.add(node);
            } else if (results.size() < K) {
                results.add(node);
            }
        }
        if (results.size() > 0) {
            lastNode = results.last();
            lastDistance = lastNode.id.distance(value);
        }

        int axis = node.depth % node.k;
        KdNode lesser = node.lesser;
        KdNode greater = node.greater;

        // Search children branches, if axis aligned distance is less than
        // current distance
        if (lesser != null && !examined.contains(lesser)) {
            examined.add(lesser);

            float nodePoint = NdPoint.getValueInKm(axis,node.id);
            float valuePlusDistance = NdPoint.getValueInKm(axis,value) - lastDistance;
            boolean lineIntersectsCube = (valuePlusDistance <= nodePoint);

            // Continue down lesser branch
            if (lineIntersectsCube)
                searchNode(value, lesser, K, maxDistance, results, examined);
        }
        if (greater != null && !examined.contains(greater)) {
            examined.add(greater);

            float nodePoint = NdPoint.getValueInKm(axis,node.id);
            float valuePlusDistance = NdPoint.getValueInKm(axis,value) + lastDistance;
            boolean lineIntersectsCube = (valuePlusDistance >= nodePoint);

            // Continue down greater branch
            if (lineIntersectsCube)
                searchNode(value, greater, K, maxDistance, results, examined);
        }
    }

    protected class NodeComparator implements Comparator<KdNode> {

        private final NdPoint point;

        public NodeComparator(NdPoint point) {
            this.point = point;
        }

        @Override
        public int compare(KdNode o1, KdNode o2) {
            Float d1 = point.distance(o1.id);
            Float d2 = point.distance(o2.id);
            if (d1.compareTo(d2) < 0)
                return -1;
            else if (d2.compareTo(d1) < 0)
                return 1;
            return o1.id.compareTo(o2.id);
        }
    }

    private static class KdNode implements Comparable<KdNode> {

        private final NdPoint id;
        private final int k;
        private final int depth;

        private KdNode parent = null;
        private KdNode lesser = null;
        private KdNode greater = null;

        public KdNode(NdPoint id) {
            this.id = id;
            this.k = id.k;
            this.depth = 0;
        }

        public KdNode(NdPoint id, int depth) {
            this.id = id;
            this.k = id.k;
            this.depth = depth;
        }

        public int compareTo(int depth, NdPoint o1) {
            int axis = depth % k;
            return Float.compare(o1.val[axis], id.val[axis]);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof KdNode))
                return false;

            KdNode node = (KdNode) other;
            return this.compareTo(node) == 0;
        }

        @Override
        public int compareTo(KdNode o) {
            return compareTo(depth, o.id);
        }

        @Override
        public String toString() {
            return String.format("k=%d, depth=%d, id=%s", k, depth, id);
        }
    }

}