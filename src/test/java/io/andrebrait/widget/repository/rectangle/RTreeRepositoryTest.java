package io.andrebrait.widget.repository.rectangle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is not an actual test class, so tests here are disabled by default. To test the performance
 * here, enable them. Using JMH would be better, of course, but this enough to give you a rough
 * idea. For a more accurate picture, run each "test" separately.
 */
class RTreeRepositoryTest {

    @Test
    @Disabled
    void testRTreePerformance() {
        System.out.println("## Checking performance for " + RTreeRepository.class.getSimpleName());
        for (int i = 1_000; i < 10_000_000; i *= 10) {
            RTreeRepository repository = new RTreeRepository();
            addRandom(repository, i);
            System.out.println("Stats: " + repository.stats());
            checkQueryPerformance(repository);
        }
    }

    @Test
    @Disabled
    void testHashSetPerformance() {
        System.out.println("## Checking performance for " + RTreeRepository.class.getSimpleName());
        for (int i = 1_000; i < 10_000_000; i *= 10) {
            RectangleRepository repository = new HashSetRepository();
            addRandom(repository, i);
            checkQueryPerformance(repository);
        }
    }

    private void checkQueryPerformance(RectangleRepository repository) {
        List<Double> times = new ArrayList<>();
        List<Integer> results = new ArrayList<>();
        Random r = ThreadLocalRandom.current();
        for (int j = 0; j < 1000; j++) {
            int x = r.nextInt(1_000_000) * (r.nextBoolean() ? 1 : -1);
            int y = r.nextInt(1_000_000) * (r.nextBoolean() ? 1 : -1);
            InternalRectangle search = InternalRectangle.of(
                    x,
                    y,
                    x + 1 + r.nextInt(50_000),
                    y + 1 + r.nextInt(50_000));
            long startTime = System.nanoTime();
            List<InternalRectangle> allInside = repository.findAllInside(search);
            times.add((System.nanoTime() - startTime) / 1_000_000.0);
            results.add(allInside.size());
        }
        System.out.printf(
                "Results (avg) %.5f rectangles\n",
                results.stream().mapToDouble(x -> x).average().orElse(0));
        System.out.printf(
                "Search (avg) %.5f ms\n",
                times.stream().mapToDouble(x -> x).average().orElse(0));
        System.out.println("Total nodes: " + repository.findAllInside(InternalRectangle.of(
                Long.MIN_VALUE,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
                Long.MAX_VALUE)).size() + "\n");
    }

    private void addRandom(RectangleRepository t, int n) {
        Random r = ThreadLocalRandom.current();
        List<Double> times = new ArrayList<>(n);
        System.out.println("Inserting " + i + " random rectangles");
        for (int i = 0; i < n; i++) {
            int x = r.nextInt(1_000_000) * (r.nextBoolean() ? 1 : -1);
            int y = r.nextInt(1_000_000) * (r.nextBoolean() ? 1 : -1);
            InternalRectangle rec = InternalRectangle.of(x, y, x + 1 + r.nextInt(5_000), y + 1 + r.nextInt(5_000));
            long startTime = System.nanoTime();
            t.add(rec);
            times.add((System.nanoTime() - startTime) / 1_000_000.0);
        }
        System.out.printf(
                "Insertion (avg) %.5f ms\n",
                times.stream().mapToDouble(i -> i).average().orElse(0));
    }
}