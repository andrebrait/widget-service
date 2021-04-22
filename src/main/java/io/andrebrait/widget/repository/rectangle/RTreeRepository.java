package io.andrebrait.widget.repository.rectangle;

import io.andrebrait.widget.domain.Rectangle;
import io.andrebrait.widget.domain.Widget;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A somewhat simplistic implementation of an R-Tree. This was inspired by existing libraries (see
 * list below), but due to time constraints it's not very optimized.<br><br>
 *
 * Consider this a quick POC. The code is a bit ugly, but it should be fairly
 * comprehensible.<br><br>
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
 *     <li>Performance <strong>does</strong> deteriorate if rectangles are too close together</li>
 *     <li>The tree does not balance itself or repartition itself in order to get a better
 *     distribution of rectangles</li>
 * </ul>
 *
 * This is not how R-Trees are usually done but should be good enough for a demo.<br><br>
 *
 * This class it not thread-safe. It was, but I removed the locks because I realized I had to
 * synchronize it externally. <br><br>
 *
 * If this was not synchronized externally, concurrency could be good with a patch approach to
 * tree modification
 * (see <a href="https://github.com/npgall/concurrent-trees">concurrent-trees</a>).<br><br>
 *
 * At the expense of making insertions a bit more complex, I made the search a bit faster by
 * storing the smallest area of a leaf node inside a parent nodes. That way we can avoid going
 * into a subtree if the search rectangle intersection with a node's rectangle has an area
 * smaller than the smallest item inside it. There can be no item in that subtree that can be
 * contained inside the search rectangle, as even the smallest item has an area larger than
 * the intersection.<br><br>
 *
 * Worst-case time complexity for queries is O(n) (if you insert only rectangles inside one
 * another). <br>
 * Average time complexity for queries is higher than O(log2 n) but I didn't validate precisely
 * what it is. It's far lower than O(n), most of the time, however. It seems to be proportional
 * to the size of the search rectangle, which is expected, given more nodes will be visited the
 * more nodes intersect with the search rectangle.<br> In a few random sets of
 *
 * @see <a href="https://github.com/aled/jsi">JSI</a>
 * @see <a href="https://github.com/plokhotnyuk/rtree2d">RTree2D</a>
 */
public final class RTreeRepository implements RectangleRepository<Widget> {

    private static final Comparator<Rectangle> RECTANGLE_COMPARATOR =
            Comparator.comparingLong(Rectangle::getX)
                    .thenComparingLong(Rectangle::getX2)
                    .thenComparingLong(Rectangle::getY)
                    .thenComparingLong(Rectangle::getY2);

    private static final InternalRectangle Q1 =
            InternalRectangle.of(Long.MIN_VALUE, 0, 0, Long.MAX_VALUE);
    private static final InternalRectangle Q2 =
            InternalRectangle.of(0, 0, Long.MAX_VALUE, Long.MAX_VALUE);
    private static final InternalRectangle Q3 =
            InternalRectangle.of(0, Long.MIN_VALUE, Long.MAX_VALUE, 0);
    private static final InternalRectangle Q4 =
            InternalRectangle.of(Long.MIN_VALUE, Long.MIN_VALUE, 0, 0);

    private static final InternalRectangle Q1Q4 = Q1.join(Q4);
    private static final InternalRectangle Q2Q3 = Q2.join(Q3);

    private static final InternalRectangle GRID = Q1Q4.join(Q2Q3);

    @Value
    public static class Stats {

        long maxDepth;
        double averageDepth;
        long nodes;
        long leaves;
        double maxIntersectingArea;
        double averageIntersectingArea;
    }

    @Data
    private static class Node {

        @Setter(AccessLevel.NONE)
        private Rectangle rectangle;

        @Nullable
        @Getter(AccessLevel.NONE)
        @ToString.Exclude
        private final Rectangle originalRectangle;

        @Setter(AccessLevel.NONE)
        @ToString.Exclude
        private BigInteger minimumAreaInside;

        @Nullable
        @ToString.Exclude
        private Node parent;
        @Nullable
        @ToString.Exclude
        private Node left;
        @Nullable
        @ToString.Exclude
        private Node right;

        private final boolean internal;

        public Node(Rectangle rectangle, boolean internal) {
            this.rectangle = rectangle;
            this.internal = internal;
            if (internal) {
                this.originalRectangle = rectangle;
            } else {
                this.originalRectangle = null;
            }
            this.minimumAreaInside = rectangle.area();
        }

        public Node(Rectangle rectangle) {
            this(rectangle, false);
        }

        public boolean isLeaf() {
            return rectangle instanceof Widget;
        }

        private void sortChildren() {
            if (left == null) {
                if (right != null) {
                    left = right;
                    right = null;
                }
            } else if (right != null) {
                if (RECTANGLE_COMPARATOR.compare(right.getRectangle(), left.getRectangle()) < 0) {
                    Node oldLeft = left;
                    left = right;
                    right = oldLeft;
                }
            }
        }

        public void setLeft(@Nullable Node left) {
            this.left = left;
            if (left != null) {
                left.parent = this;
            }
            propagateUpwards();
            sortUpwards();
        }

        public void setRight(@Nullable Node right) {
            this.right = right;
            if (right != null) {
                right.parent = this;
            }
            propagateUpwards();
            sortUpwards();
        }

        public void replaceChild(Node oldChild, Node newChild) {
            if (oldChild == this.left) {
                setLeft(newChild);
            } else if (oldChild == this.right) {
                setRight(newChild);
            }
        }

        private void propagateUpwards() {
            Node current = this;
            while (current != null) {
                Node currentLeft = current.getLeft();
                Node currentRight = current.getRight();

                Rectangle selectedRectangle = current.rectangle;
                BigInteger selectedMinimumArea;

                if (currentLeft != null && currentRight != null) {
                    Rectangle r = currentLeft.getRectangle().join(currentRight.getRectangle());
                    if (current.isInternal() && current.originalRectangle != null) {
                        if (current.originalRectangle.contains(r)) {
                            r = current.originalRectangle;
                        } else {
                            r = r.join(current.originalRectangle);
                        }
                    }
                    selectedRectangle = r;
                }

                if (currentLeft == null && currentRight == null) {
                    selectedMinimumArea = current.rectangle.area();
                } else if (currentRight == null) {
                    selectedMinimumArea = currentLeft.minimumAreaInside;
                } else if (currentLeft == null) {
                    selectedMinimumArea = currentRight.minimumAreaInside;
                } else {
                    BigInteger leftArea = currentLeft.minimumAreaInside;
                    BigInteger rightArea = currentRight.minimumAreaInside;
                    if (leftArea.compareTo(rightArea) <= 0) {
                        selectedMinimumArea = leftArea;
                    } else {
                        selectedMinimumArea = rightArea;
                    }
                }

                boolean changedRectangle =
                        !selectedRectangle.equalDimensions(current.getRectangle());
                boolean changedAreaInside =
                        selectedMinimumArea.compareTo(current.minimumAreaInside) != 0;

                if (changedRectangle) {
                    current.rectangle = selectedRectangle;
                }

                if (changedAreaInside) {
                    current.minimumAreaInside = selectedMinimumArea;
                }

                if (!changedRectangle && !changedAreaInside) {
                    return;
                }

                current = current.getParent();
            }
        }

        private void sortUpwards() {
            Node current = this;
            while (current != null && current.isInternal()) {
                current.sortChildren();
                current = current.getParent();
            }
        }
    }

    private final Node root;
    private final Map<UUID, InternalRectangle> nodesById;

    public RTreeRepository() {
        // Messy initialization code
        this.root = new Node(GRID, true);
        this.nodesById = new HashMap<>();
        Node q1q4Node = new Node(Q1Q4, true);
        Node q2q3Node = new Node(Q2Q3, true);
        q1q4Node.setLeft(new Node(Q4, true));
        q1q4Node.setRight(new Node(Q1, true));
        q2q3Node.setLeft(new Node(Q3, true));
        q2q3Node.setRight(new Node(Q2, true));
        this.root.setLeft(q1q4Node);
        this.root.setRight(q2q3Node);
    }

    public Stats stats() {
        List<Long> depths = new ArrayList<>();
        List<Double> intersectionAreas = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        Deque<Node> nodeSearchStack = new ArrayDeque<>();
        nodeSearchStack.push(root);
        while (!nodeSearchStack.isEmpty()) {
            Node currentNode = nodeSearchStack.pop();
            nodes.add(currentNode);
            if (currentNode.isLeaf()) {
                int depth = 0;
                Node n = currentNode;
                while (n != null) {
                    depth++;
                    n = n.getParent();
                }
                depths.add((long) depth);
            } else {
                Node left = currentNode.getLeft();
                Node right = currentNode.getRight();
                if (left != null) {
                    nodeSearchStack.push(left);
                }
                if (right != null) {
                    nodeSearchStack.push(right);
                }
                if (left != null && right != null) {
                    InternalRectangle leftRectangle = left.getRectangle();
                    InternalRectangle rightRectangle = right.getRectangle();
                    if (leftRectangle.intersects(rightRectangle)) {
                        InternalRectangle intersection = InternalRectangle.of(
                                Math.max(leftRectangle.getX(), rightRectangle.getX()),
                                Math.max(leftRectangle.getY(), rightRectangle.getY()),
                                Math.min(leftRectangle.getX2(), rightRectangle.getX2()),
                                Math.min(leftRectangle.getY2(), rightRectangle.getY2()));
                        BigInteger totalArea =
                                leftRectangle.area().add(rightRectangle.area());
                        BigDecimal intersectionArea = new BigDecimal(intersection.area())
                                .divide(new BigDecimal(totalArea), 4, RoundingMode.HALF_UP);
                        intersectionAreas.add(intersectionArea.doubleValue());
                    } else {
                        intersectionAreas.add(0.0);
                    }
                }
            }
        }
        return new Stats(
                depths.stream().mapToLong(i -> i).max().orElse(0),
                depths.stream().mapToLong(i -> i).average().orElse(0),
                nodes.size(),
                nodes.stream().filter(Node::isLeaf).count(),
                intersectionAreas.stream().mapToDouble(i -> i).max().orElse(0),
                intersectionAreas.stream().mapToDouble(i -> i).average().orElse(0));
    }

    @Override
    public boolean add(Widget rectangle) {
        if (nodesById.containsKey(rectangle.getId())) {
            return false;
        }
        Node currentNode = root;
        while (!currentNode.isLeaf()) {
            if (!currentNode.isInternal() && rectangle.contains(currentNode.getRectangle())) {
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
                merge(currentNode, rectangle);
                return true;
            }

            Node currentLeft = currentNode.getLeft();
            Node currentRight = currentNode.getRight();
            if (currentLeft != null && currentLeft.getRectangle().contains(rectangle)) {
                currentNode = currentLeft;
            } else if (currentRight != null && currentRight.getRectangle().contains(rectangle)) {
                currentNode = currentRight;
            } else {
                BigInteger areaIncrease1 = getAreaIncrease(rectangle, currentLeft);
                BigInteger areaIncrease2 = getAreaIncrease(rectangle, currentRight);
                if (areaIncrease1.compareTo(areaIncrease2) <= 0) {
                    if (currentLeft == null) {
                        currentNode.setLeft(new Node(rectangle));
                        nodesById.put(rectangle.getId(), rectangle);
                        return true;
                    }
                    currentNode = currentLeft;
                } else {
                    if (currentRight == null) {
                        currentNode.setRight(new Node(rectangle));
                        nodesById.put(rectangle.getId(), rectangle);
                        return true;
                    }
                    currentNode = currentRight;
                }
            }
        }
        /*

                    R
                    |
                    AB
                   /  \
                  A    B <- C

                    R
                    |
                    ABC
                   /   \
                  A    BC
                      /  \
                     B    C

             */
        merge(currentNode, rectangle);
        return true;
    }

    @Override
    @Nullable
    public boolean remove(InternalRectangle rectangle) {
        if (!nodesById.containsKey(rectangle.getId())) {
            return false;
        }
        Deque<Node> nodeSearchStack = new ArrayDeque<>();
        Node found = null;
        nodeSearchStack.push(root);
        while (!nodeSearchStack.isEmpty()) {
            Node currentNode = nodeSearchStack.pop();
            if (currentNode.isLeaf() && currentNode.getRectangle().equals(rectangle)) {
                found = currentNode;
                break;
            }
            if (currentNode.getLeft() != null
                    && currentNode.getLeft().getRectangle().contains(rectangle)) {
                nodeSearchStack.push(currentNode.getLeft());
            }
            if (currentNode.getRight() != null
                    && currentNode.getRight().getRectangle().contains(rectangle)) {
                nodeSearchStack.push(currentNode.getRight());
            }
        }
        if (found == null) {
            return false;
        }

        Node parent = found.getParent();
        if (parent == null) {
            throw new IllegalStateException("Invalid parent for node: " + found);
        }

        if (found == parent.getLeft()) {
            if (parent.getRight() != null && parent.getParent() != null) {
                parent.getParent().replaceChild(parent, parent.getRight());
            } else {
                parent.setLeft(null);
            }
        } else if (found == parent.getRight()) {
            if (parent.getLeft() != null && parent.getParent() != null) {
                parent.getParent().replaceChild(parent, parent.getLeft());
            } else {
                parent.setRight(null);
            }
        }

        nodesById.remove(found.getRectangle().getId());
        return true;
    }

    @Override
    public List<InternalRectangle> findAllInside(InternalRectangle rectangle) {
        List<InternalRectangle> result = new ArrayList<>();
        Deque<Node> nodeSearchStack = new ArrayDeque<>();
        nodeSearchStack.push(root);
        while (!nodeSearchStack.isEmpty()) {
            Node currentNode = nodeSearchStack.pop();
            if (currentNode.isLeaf()) {
                if (rectangle.contains(currentNode.getRectangle())) {
                    result.add(currentNode.getRectangle());
                }
            } else {
                Node left = currentNode.getLeft();
                Node right = currentNode.getRight();
                if (left != null
                        && left.getRectangle().intersectionArea(rectangle)
                        .compareTo(left.getMinimumAreaInside()) >= 0) {
                    nodeSearchStack.push(left);
                }
                if (right != null
                        && right.getRectangle().intersectionArea(rectangle)
                        .compareTo(right.getMinimumAreaInside()) >= 0) {
                    nodeSearchStack.push(right);
                }
            }
        }
        return result;
    }

    private BigInteger getAreaIncrease(InternalRectangle rectangle, @Nullable Node node) {
        if (node == null) {
            return rectangle.area();
        }
        BigInteger joinededArea = node.getRectangle().joinedArea(rectangle);
        return joinededArea.subtract(node.getRectangle().area());
    }

    private void merge(Node existingNode, InternalRectangle rectangle) {
        InternalRectangle newParentRectangle = existingNode.getRectangle().join(rectangle);
        Node node = new Node(newParentRectangle);
        if (existingNode.getParent() != null) {
            existingNode.getParent().replaceChild(existingNode, node);
        }
        node.setLeft(existingNode);
        node.setRight(new Node(rectangle));

        nodesById.put(rectangle.getId(), rectangle);
    }

}
