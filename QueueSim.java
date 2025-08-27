import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

import org.w3c.dom.events.Event;

public class QueueSim {

    public static class LCG {
        private final long a;
        private final long c;
        private final long m;
        private long x;

        public LCG(long a, long c, long m, long seed) {
            this.a = a; this.c = c; this.m = m; this.x = seed % m;
        }

        public double nextRandom() {
            x = (a * x + c) % m;
            return (double) x / (double) m;
        }
    }

    public static class Simulator {
        private final LCG rng;
        private long budget;

        private final double arrivalMin, arrivalMax;
        private final double serviceMin, serviceMax;
        private final int servers;
        private final int capacity;

        private final PriorityQueue<Event> agenda = new PriorityQueue<>();
        private final Queue<Double> arrivalTimes = new ArrayDeque<>();

        private double currentTime = 0.0;
        private double lastEventTime = 0.0;

        private int inSystem = 0;
        private int busyServers = 0;

        private final double[] stateTime;
        private long completed = 0;
        private double totalResponseTime = 0.0;

        public Simulator(LCG rng, long budget,
                         double arrivalMin, double arrivalMax,
                         double serviceMin, double serviceMax,
                         int servers, int capacity) {
            this.rng = rng;
            this.budget = budget;
            this.arrivalMin = arrivalMin; this.arrivalMax = arrivalMax;
            this.serviceMin = serviceMin; this.serviceMax = serviceMax;
            this.servers = servers; this.capacity = capacity;
            this.stateTime = new double[capacity + 1];
        }
    }
}
