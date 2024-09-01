package com.example.circuitsimulator.Model;

import javafx.scene.shape.Circle;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.PatonCycleBase;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.exception.NoDataException;
import java.util.*;

public class SimHandler {
    private static final ArrayList<ArrayList<Circle>> connections = new ArrayList<>();
    private static final Graph<Circle, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
    private static final PatonCycleBase<Circle, DefaultEdge> finder = new PatonCycleBase<>(graph);
    private static final ArrayList<ArrayList<Circle>> cycles = new ArrayList<>();

    public static void updateConnections(CircuitComponent c) {
        int remove_count = 0;
        for (ArrayList<Circle> connection : connections) {
            if (connection.remove(c.getNode1())) {
                remove_count++;
            } else if (connection.remove(c.getNode2())) {
                remove_count++;
            }
            if (remove_count == 2) {
                break;
            }
        }

        boolean node1_connect = false;
        boolean node2_connect = false;

        for (Circle node : CircuitComponent.getNodes()) {
            if (!(c.getNode1().equals(node)) && distCircles(c.getNode1(), node) < 10 && !node1_connect) {
                for (ArrayList<Circle> connection : connections) {
                    if (connection.contains(node)) {
                        node1_connect = true;
                        connection.add(c.getNode1());
                        break;
                    }
                }
            }
            if (!(c.getNode2().equals(node)) && distCircles(c.getNode2(), node) < 10 && !node2_connect) {
                for (ArrayList<Circle> connection : connections) {
                    if (connection.contains(node)) {
                        node2_connect = true;
                        connection.add(c.getNode2());
                        break;
                    }
                }
            }
        }

        if (!node1_connect) {
            ArrayList<Circle> node1_connection = new ArrayList<>();
            node1_connection.add(c.getNode1());
            connections.add(node1_connection);
        }

        if (!node2_connect) {
            ArrayList<Circle> node2_connection = new ArrayList<>();
            node2_connection.add(c.getNode2());
            connections.add(node2_connection);
        }

        connections.removeIf(ArrayList::isEmpty);

        SimHandler.generateCycles();
        SimHandler.addCurrents();
        SimHandler.Calculate();
    }

    public static void generateCycles() {

        // Reset Graph
        ArrayList<DefaultEdge> edges = new ArrayList<>(graph.edgeSet());
        ArrayList<Circle> vertexes = new ArrayList<>(graph.vertexSet());
        graph.removeAllEdges(edges);
        graph.removeAllVertices(vertexes);

        // Generate graph from connections array
        for (Circle c : CircuitComponent.getNodes()) {
            graph.addVertex(c);
        }

        for (CircuitComponent c : CircuitComponent.getConnectables()) {
            graph.addEdge(c.getNode1(), c.getNode2());
        }

        for (ArrayList<Circle> connection : connections) {
            Circle c = new Circle();
            graph.addVertex(c);
            for (Circle node : connection) {
                graph.addEdge(node, c);
            }
        }

        // Generate cycles and remove extra nodes added for facilitating connections
        cycles.clear();
        Set<GraphPath<Circle, DefaultEdge>> cycles_set = finder.getCycleBasis().getCyclesAsGraphPaths();
        for (GraphPath<Circle, DefaultEdge> c : cycles_set) {
            ArrayList<Circle> cycle = new ArrayList<>();
            List<Circle> cs = c.getVertexList();
            for (Circle cir : cs) {
                if (CircuitComponent.getNodes().contains(cir)) cycle.add(cir);
            }
            cycles.add(cycle);
        }

        if (cycles.isEmpty()) {
            return;
        }

        // Ensure cycles in clockwise order
        for (ArrayList<Circle> cycle : cycles) {
            double edge_sum = 0;

            for (int i = 0; i < cycle.size(); i++) {
                if (i != cycle.size() - 1) {
                    edge_sum += (cycle.get(i+1).getLayoutX() - cycle.get(i).getLayoutX())/(cycle.get(i+1).getLayoutY() + cycle.get(i).getLayoutY());
                } else {
                    edge_sum += (cycle.getFirst().getLayoutX() - cycle.get(i).getLayoutX())/(cycle.getFirst().getLayoutY() + cycle.get(i).getLayoutY());
                }
            }

            if (edge_sum < 0) {
                Collections.reverse(cycle);
            }

            cycle.removeLast();
        }
    }

    public static void addCurrents() {
        for (CircuitComponent c : CircuitComponent.getConnectables()) {
            c.clearCurrent12();
            c.clearCurrent21();
        }

        int cycle_id = 0;
        for (ArrayList<Circle> cycle : cycles) {
            CircuitComponent prev_component = null;
            int i = 0;
            for (Circle c : cycle) {
                CircuitComponent component = CircuitComponent.getNodeConnections().get(c);
                if (i == 0 && CircuitComponent.getNodeConnections().get(cycle.get(1)) != component) {
                    prev_component = component;
                    continue;
                }

                if (component.getNode1() == c && component != prev_component) {
                    if (!component.getCurrent21().contains(cycle_id)) component.addCurrent12(cycle_id);
                } else if (component.getNode2() == c && component != prev_component) {
                    if (!component.getCurrent12().contains(cycle_id)) component.addCurrent21(cycle_id);
                }
                prev_component = component;
                i++;
            }
            cycle_id++;
        }
    }

    public static void Calculate() {
        double[][] coefficients = new double[cycles.size()][cycles.size()];
        double[] constants = new double[cycles.size()];

        int c_num = 0;
        for (ArrayList<Circle> cycle : cycles) {
            CircuitComponent prev_component = null;
            int i = 0;
            for (Circle c : cycle) {
                CircuitComponent component = CircuitComponent.getNodeConnections().get(c);
                if (i == 0 && CircuitComponent.getNodeConnections().get(cycle.get(1)) != component) {
                    prev_component = component;
                    continue;
                }
                if (component.getNode1() == c && component != prev_component) {
                    if (component instanceof Resistor resistor) {
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] += resistor.getResistance();
                        }
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] -= resistor.getResistance();
                        }
                    } else if (component instanceof Battery battery) {
                        constants[c_num] += battery.getEMF();
                    } else if (component instanceof Switch switch_element) {
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] -= switch_element.getResistance();
                        }
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] += switch_element.getResistance();
                        }
                    } else if (component instanceof Load load) {
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] += load.getResistance();
                        }
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] -= load.getResistance();
                        }
                    }
                } else if (component.getNode2() == c && component != prev_component) {
                    if (component instanceof Resistor resistor) {
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] -= resistor.getResistance();
                        }
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] += resistor.getResistance();
                        }
                    } else if (component instanceof Battery battery) {
                        constants[c_num] -= battery.getEMF();
                    } else if (component instanceof Switch switch_element) {
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] -= switch_element.getResistance();
                        }
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] += switch_element.getResistance();
                        }
                    } else if (component instanceof Load load) {
                        for (int index : component.getCurrent21()) {
                            coefficients[c_num][index] += load.getResistance();
                        }
                        for (int index : component.getCurrent12()) {
                            coefficients[c_num][index] -= load.getResistance();
                        }
                    }
                }
                prev_component = component;
                i++;
            }
            c_num++;
        }

        try {
            RealMatrix coefficient_matrix = new Array2DRowRealMatrix(coefficients);
            DecompositionSolver solver = new LUDecomposition(coefficient_matrix).getSolver();
            RealVector constant_vect = new ArrayRealVector(constants);
            RealVector solution = solver.solve(constant_vect);

            double[] currents = solution.toArray();

            for (CircuitComponent c : CircuitComponent.getConnectables()) {
                double current_accum = 0;
                for (int x : c.getCurrent12()) {
                    current_accum += currents[x];
                }
                for (int x : c.getCurrent21()) {
                    current_accum -= currents[x];
                }
                c.setFinalCurrent(current_accum);
            }
        } catch (NoDataException ex) {
            for (CircuitComponent c : CircuitComponent.getConnectables()) {
                c.setFinalCurrent(0);
            }
        } catch (SingularMatrixException ex) {
            ex.printStackTrace();
        }
    }

    public static void removeFromConnections(CircuitComponent c) {
        int remove_count = 0;
        for (ArrayList<Circle> connection : connections) {
            if (connection.remove(c.getNode1())) {
                remove_count++;
            } else if (connection.remove(c.getNode2())) {
                remove_count++;
            }
            if (remove_count == 2) {
                break;
            }
        }
        connections.removeIf(ArrayList::isEmpty);
    }

    public static void ClearAll() {
        ArrayList<DefaultEdge> edges = new ArrayList<>(graph.edgeSet());
        ArrayList<Circle> vertexes = new ArrayList<>(graph.vertexSet());
        graph.removeAllEdges(edges);
        graph.removeAllVertices(vertexes);

        cycles.clear();
        connections.clear();
    }

    private static double distCircles(Circle node1, Circle node2) {
        return Math.sqrt(Math.pow(node2.getLayoutX() - node1.getLayoutX(), 2) + Math.pow(node2.getLayoutY() - node1.getLayoutY(), 2));
    }
}
