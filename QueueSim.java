import java.util.ArrayDeque;
import java.util.Locale;
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

    enum EventType { CHEGADA, SAIDA }

    static class Event implements Comparable<Event> {
        final double time;
        final EventType type;

        Event(double time, EventType type) { this.time = time; this.type = type; }
        @Override public int compareTo(Event o) { return Double.compare(this.time, o.time); }
    }

    static class Fila {
        final int servers, capacity;
        final double minArrival, maxArrival;
        final double minService, maxService;

        int customers=0;
        int busy=0;
        int loss=0;

        double[] stateTime;
        Queue<Double> arrivalTimes = new ArrayDeque<>();
        double totalResponse=0;
        long completed=0;

        public Fila(int servers,int capacity,double minArrival,double maxArrival,double minService,double maxService){
            this.servers=servers; this.capacity=capacity;
            this.minArrival=minArrival; this.maxArrival=maxArrival;
            this.minService=minService; this.maxService=maxService;
            stateTime = new double[capacity+1];
        }

        int status(){ return customers; }
        int capacity(){ return capacity; }
        int servers(){ return servers; }
        void in(){ customers++; }
        void out(){ customers--; }
        void loss(){ loss++; }
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

        private double drawU() { budget--; return rng.nextRandom(); }

        private double uniform(double min, double max) {
            return min + (max - min) * drawU();
        }

        private void accumulate(double newTime) {
            double dt = newTime - lastEventTime;
            int s = Math.min(inSystem, capacity);
            stateTime[s] += dt;
            lastEventTime = newTime;
        }

        private void scheduleInitialArrival() {
            double t = uniform(arrivalMin, arrivalMax);
            agenda.add(new Event(currentTime + t, EventType.CHEGADA));
        }

        private void handleArrival(Event e) {
            accumulate(e.time); currentTime = e.time;
            if (inSystem >= capacity) {
                // perda
                double t = uniform(arrivalMin, arrivalMax);
                agenda.add(new Event(currentTime + t, EventType.CHEGADA));
                return;
            }
            inSystem++;
            arrivalTimes.add(currentTime);
            if (busyServers < servers) {
                busyServers++;
                double s = uniform(serviceMin, serviceMax);
                agenda.add(new Event(currentTime + s, EventType.SAIDA));
            }
            double t = uniform(arrivalMin, arrivalMax);
            agenda.add(new Event(currentTime + t, EventType.CHEGADA));
        }

        private void handleDeparture(Event e) {
            accumulate(e.time); currentTime = e.time;
            inSystem--;
            Double a = arrivalTimes.poll();
            if (a != null) totalResponseTime += (currentTime - a);
            completed++;

            if (inSystem >= busyServers) {
                double s = uniform(serviceMin, serviceMax);
                agenda.add(new Event(currentTime + s, EventType.SAIDA));
            } else {
                busyServers--;
            }
        }

        public void run() {
            scheduleInitialArrival();
            while (!agenda.isEmpty() && budget > 0) {
                Event e = agenda.poll();
                if (e.type == EventType.CHEGADA) handleArrival(e);
                else handleDeparture(e);
            }
        }

        public void report() {
            double total = lastEventTime;
            System.out.println("Estado;Tempo;Probabilidade");
            for (int i = 0; i <= capacity; i++) {
                double p = total > 0 ? stateTime[i]/total : 0;
                System.out.printf(Locale.US, "%d;%.4f;%.4f\n", i, stateTime[i], p);
            }
            double avgResponse = completed > 0 ? totalResponseTime/completed : 0.0;
            System.out.println("Clientes completos: " + completed);
            System.out.printf(Locale.US, "Tempo médio de resposta: %.4f\n", avgResponse);
        }


        public static void main(String[] args) {
            long a=16807L, c=0, M=2147483647L, seed=12345L;
            LCG rng = new LCG(a,c,M,seed);
            long budget = 100000;
    
            System.out.println("=== G/G/1/5 (Uniforme 2-5, 3-5) ===");
            Simulator sim1 = new Simulator(rng, budget, 2,5,3,5, 1,5);
            sim1.run(); sim1.report();
    
            System.out.println("\n=== G/G/2/5 (Uniforme 2-5, 3-5) ===");
            LCG rng2 = new LCG(a,c,M,seed);
            Simulator sim2 = new Simulator(rng2, budget, 2,5,3,5, 2,5);
            sim2.run(); sim2.report();
        }
    
    }
}
