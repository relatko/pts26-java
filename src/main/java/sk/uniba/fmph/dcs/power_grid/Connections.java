package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Connections represents the weighted undirected city graph on the Power Grid map
 * and computes minimum connection costs between an existing network and target cities.
 */
public final class Connections {

    /**
     * Weighted undirected edge between two cities.
     */
    public static final class Edge {
        private final String city1;
        private final String city2;
        private final int cost;

        public Edge(final String city1, final String city2, final int cost) {
            this.city1 = Objects.requireNonNull(city1, "city1 must not be null");
            this.city2 = Objects.requireNonNull(city2, "city2 must not be null");
            if (cost < 0) {
                throw new IllegalArgumentException("Connection cost must be non-negative: " + cost);
            }
            this.cost = cost;
        }

        public String getCity1() {
            return city1;
        }

        public String getCity2() {
            return city2;
        }

        public int getCost() {
            return cost;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge edge = (Edge) o;
            return cost == edge.cost
                    && Objects.equals(city1, edge.city1)
                    && Objects.equals(city2, edge.city2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(city1, city2, cost);
        }

        @Override
        public String toString() {
            return city1 + " --(" + cost + ")-- " + city2;
        }
    }

    private static final class Neighbor {
        private final String city;
        private final int cost;

        private Neighbor(final String city, final int cost) {
            this.city = city;
            this.cost = cost;
        }
    }

    private static final class QueueEntry {
        private final int distance;
        private final String city;

        private QueueEntry(final int distance, final String city) {
            this.distance = distance;
            this.city = city;
        }
    }

    private final List<Edge> distances;
    private final Map<String, List<Neighbor>> adjacency;

    public Connections(final List<Edge> distances) {
        Objects.requireNonNull(distances, "distances must not be null");
        this.distances = Collections.unmodifiableList(new ArrayList<>(distances));
        this.adjacency = buildAdjacency(this.distances);
    }

    /**
     * Calculates the minimum edge cost required to connect all cities in {@code to}
     * to the player's existing network {@code from}.
     * If {@code from} is empty, the player starts their network in one of the target cities
     * (incurring 0 connection cost for that starting city) and connects the remaining targets.
     *
     * @param from cities already owned by the player
     * @param to   new cities the player wishes to connect
     * @return minimum total connection cost, or {@link Integer#MAX_VALUE} if any target is unreachable
     */
    public int calculateConnectingCost(final List<String> from, final List<String> to) {
        if (to == null || to.isEmpty()) {
            return 0;
        }

        List<String> initialSources = (from == null) ? Collections.emptyList() : from;
        if (!initialSources.isEmpty()) {
            return computeSteinerCost(initialSources, to);
        }

        // Player has no existing cities: try each target city as the starting city and take the minimum cost.
        int bestCost = Integer.MAX_VALUE;
        for (String startCity : to) {
            List<String> seed = Collections.singletonList(startCity);
            int cost = computeSteinerCost(seed, to);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        return bestCost;
    }

    public List<Edge> getDistances() {
        return distances;
    }

    @Override
    public String toString() {
        return "Connections{edges=" + distances.size() + "}";
    }

    private int computeSteinerCost(final List<String> initialConnected, final List<String> targets) {
        Set<String> connected = new HashSet<>(initialConnected);
        List<String> remaining = new ArrayList<>();
        for (String target : targets) {
            if (!connected.contains(target) && !remaining.contains(target)) {
                remaining.add(target);
            }
        }

        int totalCost = 0;
        while (!remaining.isEmpty()) {
            Map<String, Integer> dist = new HashMap<>();
            Map<String, String> prev = new HashMap<>();
            runMultiSourceDijkstra(connected, dist, prev);

            String cheapestCity = null;
            int cheapestCost = Integer.MAX_VALUE;
            for (String candidate : remaining) {
                int d = dist.getOrDefault(candidate, Integer.MAX_VALUE);
                if (d < cheapestCost) {
                    cheapestCost = d;
                    cheapestCity = candidate;
                }
            }

            if (cheapestCity == null || cheapestCost == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }

            totalCost += cheapestCost;
            String current = cheapestCity;
            while (current != null) {
                connected.add(current);
                current = prev.get(current);
            }
            remaining.remove(cheapestCity);
        }

        return totalCost;
    }

    private void runMultiSourceDijkstra(final Set<String> sources,
                                        final Map<String, Integer> dist,
                                        final Map<String, String> prev) {
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.distance));
        for (String src : sources) {
            dist.put(src, 0);
            prev.put(src, null);
            pq.offer(new QueueEntry(0, src));
        }

        while (!pq.isEmpty()) {
            QueueEntry top = pq.poll();
            if (top.distance > dist.getOrDefault(top.city, Integer.MAX_VALUE)) {
                continue;
            }
            List<Neighbor> neighbors = adjacency.getOrDefault(top.city, Collections.emptyList());
            for (Neighbor neighbor : neighbors) {
                int candidateDist = top.distance + neighbor.cost;
                if (candidateDist < dist.getOrDefault(neighbor.city, Integer.MAX_VALUE)) {
                    dist.put(neighbor.city, candidateDist);
                    prev.put(neighbor.city, top.city);
                    pq.offer(new QueueEntry(candidateDist, neighbor.city));
                }
            }
        }
    }

    private static Map<String, List<Neighbor>> buildAdjacency(final List<Edge> edges) {
        Map<String, List<Neighbor>> map = new HashMap<>();
        for (Edge edge : edges) {
            map.computeIfAbsent(edge.city1, k -> new ArrayList<>())
                    .add(new Neighbor(edge.city2, edge.cost));
            map.computeIfAbsent(edge.city2, k -> new ArrayList<>())
                    .add(new Neighbor(edge.city1, edge.cost));
        }
        return map;
    }
}
