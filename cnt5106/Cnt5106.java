package cnt5106;

public class Cnt5106 {

    public static void main(String[] args) {
        int id = 1001;
        // TODO code application logic here
        System.out.println("#System starts up with totally 3 peers.");
        for (int i = 0; i < 3; i++) {
            Peer p = new Peer();
            //p.start(Integer.parseInt(args[0]));
            p.start(id + i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
