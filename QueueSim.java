import java.util.ArrayDeque;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Queue;

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

    enum EventType { CHEGADA, SAIDA, PASSAGEM }

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


        private final PriorityQueue<Event> agenda = new PriorityQueue<>();

        private double currentTime = 0.0;
        private double lastEventTime = 0.0;

        private final Fila fila1, fila2;

        public Simulator(LCG rng,long budget,Fila fila1,Fila fila2){
            this.rng=rng; this.budget=budget;
            this.fila1=fila1; this.fila2=fila2;
        }

        private double drawU() { budget--; return rng.nextRandom(); }

        private double uniform(double min, double max) {
            return min + (max - min) * drawU();
        }

        private void acumulate(double newTime){
            double dt=newTime-lastEventTime;
            fila1.stateTime[Math.min(fila1.status(), fila1.capacity())]+=dt;
            fila2.stateTime[Math.min(fila2.status(), fila2.capacity())]+=dt;
            lastEventTime=newTime;
        }

        private void scheduleInitialArrival(){
            double t=uniform(fila1.minArrival,fila1.maxArrival);
            agenda.add(new Event(currentTime+t,EventType.CHEGADA));
        }

         private void handleArrival(Event e){
            acumulate(e.time); currentTime=e.time;
            if(fila1.status()>=fila1.capacity){
                fila1.loss();
            }else{
                fila1.in();
                fila1.arrivalTimes.add(currentTime);
                if(fila1.busy<fila1.servers){
                    fila1.busy++;
                    double s=uniform(fila1.minService,fila1.maxService);
                    agenda.add(new Event(currentTime+s,EventType.PASSAGEM));
                }
            }
            double t=uniform(fila1.minArrival,fila1.maxArrival);
            agenda.add(new Event(currentTime+t,EventType.CHEGADA));
        }

        private void handlePassagem(Event e){
            acumulate(e.time); currentTime=e.time;
            fila1.out();
            Double a=fila1.arrivalTimes.poll();
            if(a!=null) fila1.totalResponse+=(currentTime-a);

            if(fila1.status()>=fila1.busy){
                double s=uniform(fila1.minService,fila1.maxService);
                agenda.add(new Event(currentTime+s,EventType.PASSAGEM));
            }else fila1.busy--;

            if(fila2.status()<fila2.capacity){
                fila2.in();
                fila2.arrivalTimes.add(currentTime);
                if(fila2.busy<fila2.servers){
                    fila2.busy++;
                    double s=uniform(fila2.minService,fila2.maxService);
                    agenda.add(new Event(currentTime+s,EventType.SAIDA));
                }
            }else fila2.loss();
        }

        private void handleSaida(Event e){
            acumulate(e.time); currentTime=e.time;
            fila2.out();
            Double a=fila2.arrivalTimes.poll();
            if(a!=null) fila2.totalResponse+=(currentTime-a);
            fila2.completed++;

            if(fila2.status()>=fila2.busy){
                double s=uniform(fila2.minService,fila2.maxService);
                agenda.add(new Event(currentTime+s,EventType.SAIDA));
            }else fila2.busy--;
        }

        public void run(){
            scheduleInitialArrival();
            while(!agenda.isEmpty() && budget>0){
                Event ev=agenda.poll();
                if(ev.type==EventType.CHEGADA) handleArrival(ev);
                else if(ev.type==EventType.PASSAGEM) handlePassagem(ev);
                else handleSaida(ev);
            }
        }

        public void report(){
            double total=lastEventTime;
            System.out.println("=== Resultados Fila 1 ===");
            for(int i=0;i<fila1.stateTime.length;i++){
                double p=total>0? fila1.stateTime[i]/total :0;
                System.out.printf(Locale.US,"Estado %d; Tempo %.2f; Prob %.4f\n",i,fila1.stateTime[i],p);
            }
            System.out.println("Perdas Fila1: "+fila1.loss);

            System.out.println("\n=== Resultados Fila 2 ===");
            for(int i=0;i<fila2.stateTime.length;i++){
                double p=total>0? fila2.stateTime[i]/total :0;
                System.out.printf(Locale.US,"Estado %d; Tempo %.2f; Prob %.4f\n",i,fila2.stateTime[i],p);
            }
            System.out.println("Perdas Fila2: "+fila2.loss);
            double avgResp=fila2.completed>0? fila2.totalResponse/fila2.completed:0;
            System.out.printf("Tempo médio resposta Fila2: %.4f\n",avgResp);
        }


       public static void main(String[] args) {
        long a = 16807L, c = 0, M = 2147483647L, seed = 12345L;
        long budget = 100000;

        // === Caso 1: G/G/2/3 (Uniforme 1–4 chegada, 3–4 serviço) ===
        Fila fila1_case1 = new Fila(2, 3, 1, 4, 3, 4); 
        Fila fila2_case1 = new Fila(2, 3, 0, 0, 3, 4); 
        Simulator sim3 = new Simulator(new LCG(a,c,M,seed), budget, fila1_case1, fila2_case1);
        System.out.println("\n=== Tandem G/G/2/3 (1-4 chegada, 3-4 serviço) ===");
        sim3.run(); 
        sim3.report();

        // === Caso 2: G/G/1/5 (sem chegadas, serviço 2–3) ===
        Fila fila1_case2 = new Fila(1, 5, 2, 5, 2, 3); 
        Fila fila2_case2 = new Fila(1, 5, 0, 0, 2, 3); 
        Simulator sim4 = new Simulator(new LCG(a,c,M,seed), budget, fila1_case2, fila2_case2);
        System.out.println("\n=== Tandem G/G/1/5 (2-5 chegada, 2-3 serviço) ===");
        sim4.run(); 
        sim4.report();
}

    
    }
}
