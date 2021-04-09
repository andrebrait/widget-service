package io.andrebrait.widget.rtree;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class RTreeTest {

    @Test
    void add() {
        for (int i = 10; i < 10_000_000; i*=10) {
            RTree t = new RTree();
            addRandom(t, i);
            long nanos = System.nanoTime();
            System.out.println(t.findAllInside(Rectangle.of(-10, -10, 10, 10)).size());
            System.out.println(t.findAllInside(Rectangle.of(-30, -40, 400, 1200)).size());
            System.out.println(t.findAllInside(Rectangle.of(-1, -1200, 40, -100)).size());
            System.out.println(t.findAllInside(Rectangle.of(50, 60, 180, 590)).size());
            System.out.println(t.findAllInside(Rectangle.of(-500, -300, 30, 50)).size());
            System.out.println(t.findAllInside(Rectangle.of(0, 0, 500, 500)).size());
            System.out.println(t.findAllInside(Rectangle.of(0, -500, 500, 0)).size());
            System.out.println(t.findAllInside(Rectangle.of(-500, -500, 0, 0)).size());
            System.out.println(t.findAllInside(Rectangle.of(-500, 0, 0, 500)).size());
            System.out.println(t.findAllInside(Rectangle.GRID).size());
            System.out.printf("Passed %d us\n", (System.nanoTime() - nanos) / 1000);
        }
    }

    private void addRandom(RTree t, int n) {
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            int x = r.nextInt(500) * (r.nextBoolean() ? 1 : -1);
            int y = r.nextInt(500) * (r.nextBoolean() ? 1 : -1);
            t.add(Rectangle.of(x, y, x + 1 + r.nextInt(1000), y + 1 + r.nextInt(1000)));
        }
    }
}