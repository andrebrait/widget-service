package io.andrebrait.widget.rtree;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A somewhat simplistic implementation of an R-Tree. This was inspired by existing libraries (see
 * list below), but due to time constraints it's not very optimized (and likely not even
 * correct).<br><br>
 *
 * Consider this a quick POC ;-)<br><br>
 *
 * This code is ugly and has plenty of room for improvements in many ways, such as:
 * <ul>
 *     <li>Parent-children relationship can be broken (e.g. setting a child doesn't automatically
 *     make the child aware of the new parent, etc.)</li>
 *     <li>Checking what node is the root node is done by comparing object references</li>
 *     <li>Determining what child to replace is done by comparing object references</li>
 *     <li>Flow control is definitely not clean and readable</li>
 * </ul>
 *
 * A few important notes:
 * <ul>
 *     <li>This is a binary version of the R-Tree</li>
 *     <li>Values are only stored in the leaves</li>
 *     <li>There is no rebalancing</li>
 *     <li>Performance may deteriorate after many modifications</li>
 * </ul>
 *
 * This is not how R-Trees are usually done but should be good enough for a demo.<br><br>
 *
 * Concurrency could be better with a patch approach to tree modification
 * (see <a href="https://github.com/npgall/concurrent-trees">concurrent-trees</a>),
 * but synchronization with a {@link ReadWriteLock} will be good enough here.<br><br>
 *
 * A {@link java.util.concurrent.locks.StampedLock} could provide better granularity + optimistic
 * reading. Ideally we code both and pit them against each other using JMH to see which one is
 * faster.<br><br>
 *
 * Worst-case time complexity is O(n). Average time complexity is O(log2 n).
 *
 * @see <a href="https://github.com/aled/jsi">JSI</a>
 * @see <a href="https://github.com/plokhotnyuk/rtree2d">RTree2D</a>
 */
public final class RTree {

    @Data
    @RequiredArgsConstructor
    private static class Node {

        private final Rectangle rectangle;

        @Nullable
        @ToString.Exclude
        private Node parent;
        @Nullable
        @ToString.Exclude
        private Node left;
        @Nullable
        @ToString.Exclude
        private Node right;

        public Node(Rectangle rectangle, @Nullable Node parent) {
            this(rectangle);
            this.parent = parent;
        }

        public boolean isLeaf() {
            return rectangle != Rectangle.GRID && left == null && right == null;
        }

        public void sortChildren() {
            if (left == null) {
                if (right != null) {
                    left = right;
                    right = null;
                }
            } else if (right != null && right.getRectangle().compareTo(left.getRectangle()) <= 0) {
                Node oldLeft = left;
                left = right;
                right = oldLeft;
            }
        }
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Node root = new Node(Rectangle.GRID);

    public void add(Rectangle rectangle) {
        lock.writeLock().lock();
        try {
            addInternal(rectangle);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Rectangle> findAllInside(Rectangle rectangle) {
        lock.readLock().lock();
        try {
            return searchInternal(rectangle);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<Rectangle> searchInternal(Rectangle rectangle) {
        List<Rectangle> result = new ArrayList<>();
        Deque<Node> nodeSearchStack = new ArrayDeque<>();
        nodeSearchStack.push(root);
        int maxDepth = 0;
        int visitedNodes = 0;
        while (!nodeSearchStack.isEmpty()) {
            Node currentNode = nodeSearchStack.pop();
            Node x = currentNode;
            int depth = 0;
            while(x.getParent() != null) {
                depth++;
                x = x.getParent();
            }
            maxDepth = Math.max(maxDepth, depth);
            visitedNodes++;
            if (currentNode.isLeaf()) {
                if (rectangle.contains(currentNode.getRectangle())) {
                    result.add(currentNode.getRectangle());
                }
            } else {
                /*
                Possible optimization: store the area of the smallest rectangle in that subtree.
                If the area of intersection between the search rectangle and the node's rectangle
                is smaller than the area of the smallest rectangle, there's no point going into
                that subtree.
                 */
                /*
                Possible (small) optimization 2: if the search rectangle contains the node's
                rectangle, add all leaves in that subtree and then skip it.
                There's no point testing intersections.
                 */
                Node left = currentNode.getLeft();
                Node right = currentNode.getRight();
                if (left != null && left.getRectangle().intersects(rectangle)) {
                    nodeSearchStack.push(left);
                }
                if (right != null && right.getRectangle().intersects(rectangle)) {
                    nodeSearchStack.push(right);
                }
            }
        }
        System.out.println("Max depth: " + maxDepth);
        System.out.println("Visited nodes: " + visitedNodes);
        return result;
    }

    private void addInternal(Rectangle rectangle) {
        Node currentNode = root;
        while (!currentNode.isLeaf()) {
            if (rectangle.contains(currentNode.getRectangle())) {
            /*
            C is a new rectangle that contains AB

                    R
                    | <- C
                    AB
                   /  \
                  A    B

                    R
                    |
                    C*       -> Same dimensions as C, but not C
                   /  \
                  AB   C
                 /  \
                A    B

             */
                // The condition guarantees this is not the root node
                Node node = new Node(rectangle, currentNode.getParent());
                replaceOnParent(currentNode, node);
                currentNode.setParent(node);
                node.setLeft(currentNode);
                node.setRight(new Node(rectangle, node));
                node.sortChildren();
                return;
            }
            if (currentNode.getRectangle().contains(rectangle)) {
                if (currentNode.getLeft() == null) {
                    // The only way to get here is if that's the root
                    currentNode.setLeft(new Node(rectangle, currentNode));
                    return;
                }
                if (currentNode.getLeft() != null
                        && currentNode.getLeft().getRectangle().contains(rectangle)) {
                    currentNode = currentNode.getLeft();
                } else if (currentNode.getRight() != null
                        && currentNode.getRight().getRectangle().contains(rectangle)) {
                    currentNode = currentNode.getRight();
                } else if (currentNode.getRight() == null) {
                    // The right side is still empty and this rectangle is not contained on the left
                    currentNode.setRight(new Node(rectangle, currentNode));
                    currentNode.sortChildren();
                    return;
                } else {
                    break;
                }
            } else {
                // Not a leaf
                merge(currentNode, rectangle);
                return;
            }
        }
        // A leaf
        merge(currentNode, rectangle);
    }

    private void replaceOnParent(Node oldNode, Node newNode) {
        Node parent = oldNode.getParent();
        if (parent != null) {
            if (parent.getLeft() == oldNode) {
                parent.setLeft(newNode);
            } else if (parent.getRight() == oldNode) {
                parent.setRight(newNode);
            } else {
                throw new IllegalStateException(String.format(
                        "Existing node %s has an invalid parent %s",
                        oldNode,
                        parent));
            }
        }
    }

    private void merge(Node existingNode, Rectangle rectangle) {
        Node node;
        if (existingNode.isLeaf()) {
            /*
            C is a new rectangle that is contained inside AB but does not contain B

                    R
                    |
                    AB
                   /  \
                  A    B <- C

                    R
                    |
                    AB
                   /  \
                  A    BC
                      /  \
                     B    C
             */
            /*
            C is a new rectangle that is contained inside AB but contains B

                    R
                    |
                    AB
                   /  \ <- C
                  A    B

                    R
                    |
                    AB
                   /  \
                  A    C*       * -> Same dimensions as C, but not C
                      /  \
                     B    C
             */
            Rectangle newParentRectangle =
                    createRectangleContaining(existingNode.getRectangle(), rectangle);
            node = new Node(newParentRectangle, existingNode.getParent());
            replaceOnParent(existingNode, node);
            existingNode.setParent(node);
            node.setLeft(existingNode);
            node.setRight(new Node(rectangle, node));
        } else {
          /*
                    R
                    |
                    AB <- C
                   /  \
                  A    B

                    R
                    |
                   ABC
                   /  \
                  A    BC
                      /  \
                     B    C
             */
            Node currentLeft = existingNode.getLeft();
            Node currentRight = existingNode.getRight();
            if (currentLeft == null || currentRight == null) {
                throw new IllegalStateException("The selected node is not full: " + existingNode);
            }
            Rectangle r1 = createRectangleContaining(currentLeft.getRectangle(), rectangle);
            Rectangle r2 = createRectangleContaining(currentRight.getRectangle(), rectangle);
            if (r1.getArea().compareTo(r2.getArea()) <= 0) {
                // Replace left child
                node = new Node(r1, existingNode);
                existingNode.setLeft(node);

                currentLeft.setParent(node);
                node.setLeft(currentLeft);
                node.setRight(new Node(rectangle, node));
            } else {
                // Replace right child
                node = new Node(r2, existingNode);
                existingNode.setRight(node);

                currentRight.setParent(node);
                node.setLeft(new Node(rectangle, node));
                node.setRight(currentRight);
            }
        }
        node.sortChildren();
        if (node.getParent() != null) {
            node.getParent().sortChildren();
        }
    }

    private Rectangle createRectangleContaining(Rectangle r1, Rectangle r2) {
        long minX = Math.min(r1.getX(), r2.getX());
        long minY = Math.min(r1.getY(), r2.getY());
        long maxX = Math.max(r1.getX2(), r2.getX2());
        long maxY = Math.max(r1.getY2(), r2.getY2());
        return Rectangle.of(minX, minY, maxX, maxY);
    }

}
