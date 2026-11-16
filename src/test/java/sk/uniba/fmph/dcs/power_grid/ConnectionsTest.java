package sk.uniba.fmph.dcs.power_grid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConnectionsTest {

    /**
     * Build a simple graph:
     *
     *   A --3-- B --2-- C
     *            \
     *             4
     *              \
     *               D
     */
    private Connections buildSimpleGraph() {
        List<Connections.Edge> edges = new ArrayList<>();
        edges.add(new Connections.Edge("A", "B", 3));
        edges.add(new Connections.Edge("B", "C", 2));
        edges.add(new Connections.Edge("B", "D", 4));
        return new Connections(edges);
    }

    @Test
    public void testDirectNeighbour() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A"), List.of("B"));
        assertEquals(3, cost);
    }

    @Test
    public void testTwoHopsPath() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A"), List.of("C"));
        assertEquals(5, cost);
    }

    @Test
    public void testMultipleCitiesSharePath() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A"), List.of("C", "D"));
        assertEquals(9, cost);
    }

    @Test
    public void testAlreadyConnected() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A", "B"), List.of("B"));
        assertEquals(0, cost);
    }

    @Test
    public void testEmptyTo() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A"), new ArrayList<>());
        assertEquals(0, cost);
    }

    @Test
    public void testEmptyFromSingleStartingCity() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(new ArrayList<>(), List.of("A"));
        assertEquals(0, cost);
    }

    @Test
    public void testEmptyFromMultipleStartingCities() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(new ArrayList<>(), List.of("A", "C", "D"));
        assertEquals(9, cost);
    }

    @Test
    public void testMultipleFromSources() {
        Connections conn = buildSimpleGraph();
        int cost = conn.calculateConnectingCost(List.of("A", "C"), List.of("D"));
        assertEquals(6, cost);
    }

    @Test
    public void testLargerGraph() {
        List<Connections.Edge> edges = new ArrayList<>();
        edges.add(new Connections.Edge("A", "B", 5));
        edges.add(new Connections.Edge("B", "C", 3));
        edges.add(new Connections.Edge("C", "E", 2));
        edges.add(new Connections.Edge("A", "D", 10));
        edges.add(new Connections.Edge("D", "E", 8));
        Connections conn = new Connections(edges);

        int cost = conn.calculateConnectingCost(List.of("A"), List.of("C", "E"));
        assertEquals(10, cost);
    }

    @Test
    public void testSingleCityNoEdges() {
        Connections conn = new Connections(new ArrayList<>());
        int cost = conn.calculateConnectingCost(List.of("A"), List.of("B"));
        assertEquals(Integer.MAX_VALUE, cost);
    }
}
