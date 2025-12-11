import java.util.*;

/**
 * Experiments for sensor placement using a greedy set cover heuristic.
 */
public class SensorExperiments {

    static class SensorInstance {
        int nZones;
        List<BitSet> subsets; // each subset represents coverage of a sensor

        SensorInstance(int nZones, List<BitSet> subsets) {
            this.nZones = nZones;
            this.subsets = subsets;
        }
    }

    static SensorInstance generateInstance(int nZones, int alpha, double q, Random rnd) {
        int m = alpha * nZones;
        List<BitSet> subsets = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            BitSet bs = new BitSet(nZones);
            for (int z = 0; z < nZones; z++) {
                if (rnd.nextDouble() < q) {
                    bs.set(z);
                }
            }
            // ensure non-empty coverage set
            if (bs.isEmpty()) {
                int z = rnd.nextInt(nZones);
                bs.set(z);
            }
            subsets.add(bs);
        }
        return new SensorInstance(nZones, subsets);
    }

    static class GreedyResult {
        List<Integer> chosenSensors;
        BitSet remainingZones;

        GreedyResult(List<Integer> chosenSensors, BitSet remainingZones) {
            this.chosenSensors = chosenSensors;
            this.remainingZones = remainingZones;
        }
    }

    static GreedyResult greedySensorPlacement(SensorInstance inst) {
        int n = inst.nZones;
        List<BitSet> S = inst.subsets;

        BitSet Urem = new BitSet(n);
        Urem.set(0, n); // initially all zones uncovered
        List<Integer> chosen = new ArrayList<>();

        while (!Urem.isEmpty()) {
            int bestIdx = -1;
            BitSet bestCover = new BitSet(n);
            int bestSize = 0;

            for (int i = 0; i < S.size(); i++) {
                BitSet candidate = (BitSet) S.get(i).clone();
                candidate.and(Urem);
                int coverSize = candidate.cardinality();
                if (coverSize > bestSize) {
                    bestSize = coverSize;
                    bestIdx = i;
                    bestCover = candidate;
                }
            }

            if (bestIdx == -1 || bestSize == 0) {
                // no further progress possible
                break;
            }

            chosen.add(bestIdx);
            // remove covered zones from remaining
            Urem.andNot(S.get(bestIdx));
        }

        return new GreedyResult(chosen, Urem);
    }

    public static void main(String[] args) {
        int[] sizes = {50, 100, 200, 400, 800};
        int alpha = 2;
        double q = 0.2;

        Random rnd = new Random(123);
        System.out.println("Sensor Placement Experiments (Java, Greedy Set Cover)");
        System.out.println("nZones\tChosen\tRemaining\tTime(ms)");

        for (int n : sizes) {
            SensorInstance inst = generateInstance(n, alpha, q, rnd);

            long start = System.nanoTime();
            GreedyResult res = greedySensorPlacement(inst);
            long end = System.nanoTime();
            double ms = (end - start) / 1_000_000.0;

            int chosenCount = res.chosenSensors.size();
            int remainingCount = res.remainingZones.cardinality();

            System.out.printf("%d\t%d\t%d\t\t%.3f%n",
                    n, chosenCount, remainingCount, ms);
        }
    }
}
