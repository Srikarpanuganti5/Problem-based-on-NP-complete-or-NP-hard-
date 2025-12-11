import java.util.*;

/**
 * Experiments for TA assignment via Max Flow.
 * Nodes:
 *   0          : source
 *   1..numTAs  : TA nodes
 *   numTAs+1..numTAs+numSecs : Section nodes
 *   numTAs+numSecs+1 : sink
 */
public class TAExperiments {

    // Dinic's algorithm implementation
    static class Edge {
        int to;
        int rev;
        int cap;

        Edge(int to, int rev, int cap) {
            this.to = to;
            this.rev = rev;
            this.cap = cap;
        }
    }

    static class Dinic {
        int N;
        List<Edge>[] graph;
        int[] level;
        int[] prog;

        @SuppressWarnings("unchecked")
        Dinic(int n) {
            this.N = n;
            graph = new List[n];
            for (int i = 0; i < n; i++) {
                graph[i] = new ArrayList<>();
            }
            level = new int[n];
            prog = new int[n];
        }

        void addEdge(int from, int to, int cap) {
            Edge fwd = new Edge(to, graph[to].size(), cap);
            Edge rev = new Edge(from, graph[from].size(), 0);
            graph[from].add(fwd);
            graph[to].add(rev);
        }

        boolean bfs(int s, int t) {
            Arrays.fill(level, -1);
            Queue<Integer> q = new ArrayDeque<>();
            level[s] = 0;
            q.add(s);
            while (!q.isEmpty()) {
                int v = q.poll();
                for (Edge e : graph[v]) {
                    if (e.cap > 0 && level[e.to] < 0) {
                        level[e.to] = level[v] + 1;
                        q.add(e.to);
                    }
                }
            }
            return level[t] >= 0;
        }

        int dfs(int v, int t, int f) {
            if (v == t) return f;
            for (int i = prog[v]; i < graph[v].size(); i = ++prog[v]) {
                Edge e = graph[v].get(i);
                if (e.cap <= 0 || level[v] >= level[e.to]) continue;
                int d = dfs(e.to, t, Math.min(f, e.cap));
                if (d > 0) {
                    e.cap -= d;
                    graph[e.to].get(e.rev).cap += d;
                    return d;
                }
            }
            return 0;
        }

        int maxFlow(int s, int t) {
            int flow = 0;
            final int INF = Integer.MAX_VALUE;
            while (bfs(s, t)) {
                Arrays.fill(prog, 0);
                int f;
                while ((f = dfs(s, t, INF)) > 0) {
                    flow += f;
                }
            }
            return flow;
        }
    }

    // Represents a random TA assignment instance
    static class TAInstance {
        int numTAs;
        int numSecs;
        int[] c; // capacities of TAs
        int[] r; // requirements of sections
        List<int[]> eligibleEdges; // list of (taIdx, secIdx) pairs

        TAInstance(int numTAs, int numSecs, int[] c, int[] r, List<int[]> eligibleEdges) {
            this.numTAs = numTAs;
            this.numSecs = numSecs;
            this.c = c;
            this.r = r;
            this.eligibleEdges = eligibleEdges;
        }
    }

    static TAInstance generateInstance(int numTAs, int numSecs, double pElig,
                                       int capMin, int capMax, int reqMin, int reqMax,
                                       Random rnd) {
        int[] c = new int[numTAs];
        int[] r = new int[numSecs];
        for (int i = 0; i < numTAs; i++) {
            c[i] = capMin + rnd.nextInt(capMax - capMin + 1);
        }
        for (int j = 0; j < numSecs; j++) {
            r[j] = reqMin + rnd.nextInt(reqMax - reqMin + 1);
        }
        List<int[]> eligible = new ArrayList<>();
        for (int i = 0; i < numTAs; i++) {
            for (int j = 0; j < numSecs; j++) {
                if (rnd.nextDouble() < pElig) {
                    eligible.add(new int[]{i, j});
                }
            }
        }
        return new TAInstance(numTAs, numSecs, c, r, eligible);
    }

    static Dinic buildFlowNetwork(TAInstance inst) {
        int numTAs = inst.numTAs;
        int numSecs = inst.numSecs;

        int source = 0;
        int taOffset = 1;
        int secOffset = 1 + numTAs;
        int sink = 1 + numTAs + numSecs;
        int N = sink + 1;

        Dinic dinic = new Dinic(N);

        // Edges from source to TA nodes
        for (int i = 0; i < numTAs; i++) {
            int taNode = taOffset + i;
            dinic.addEdge(source, taNode, inst.c[i]);
        }

        // Edges from section nodes to sink
        for (int j = 0; j < numSecs; j++) {
            int secNode = secOffset + j;
            dinic.addEdge(secNode, sink, inst.r[j]);
        }

        // Edges from TAs to Sections for each eligible pair
        for (int[] pair : inst.eligibleEdges) {
            int taIdx = pair[0];
            int secIdx = pair[1];
            int taNode = taOffset + taIdx;
            int secNode = secOffset + secIdx;
            dinic.addEdge(taNode, secNode, 1);
        }

        return dinic;
    }

    public static void main(String[] args) {
        int[] sizes = {25, 50, 100, 200, 400};
        double pElig = 0.25;
        int capMin = 1, capMax = 3;
        int reqMin = 1, reqMax = 3;

        Random rnd = new Random(42);
        System.out.println("TA Assignment Experiments (Java, Dinic Max Flow)");
        System.out.println("Size\tFlowValue\tTime(ms)");

        for (int n : sizes) {
            TAInstance inst = generateInstance(n, n, pElig,
                                               capMin, capMax,
                                               reqMin, reqMax,
                                               rnd);
            Dinic dinic = buildFlowNetwork(inst);

            int source = 0;
            int sink = 1 + inst.numTAs + inst.numSecs;

            long start = System.nanoTime();
            int flow = dinic.maxFlow(source, sink);
            long end = System.nanoTime();
            double ms = (end - start) / 1_000_000.0;

            System.out.printf("%d\t%d\t\t%.3f%n", n, flow, ms);
        }
    }
}
