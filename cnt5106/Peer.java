
package cnt5106;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.net.*;
import java.nio.file.*;
/*
class Have extends Thread {
	Peer P;
	private byte[] payload;
	public Have (byte[] payload) {
		this.payload = payload;
	}
	@Override
	public void run() {
				Message msg = new Message();
				msg.length = 8;// the length of a byte, supposedly
				msg.type = 4; // have
				msg.payload = this.payload; // 
				for(int i = 0; i<P.peer_id.length; i++) {
					P.send(P.peer_id[i], msg);
				}
			}
}

//called whence an unchoke message hast been received
//request from "neighbour"
class Request extends Thread {
	Peer P;
	private byte[] doWant;
	private int neighbour;
	private byte[] requestedPiece;
	int requestedPieceCount = 0;
	byte[] temp;
	byte[] r;
	Random rand = new Random();
	public Request (int neighbour) {
		this.neighbour = neighbour;
		for(int i = 0; i < P.bitfield.length; i++) {
			this.temp = P.neighbourBitfield[i];
			this.doWant[i] = (byte) (P.bitfield[i] & (this.temp[i] ^ P.bitfield[i]));
		}
		for(int i = 0; i < doWant.length; i++) {
			for(int j = 0; j<8;j++){
				if(this.doWant[i] >> j == 1) {
					requestedPiece[requestedPieceCount] = (byte) (i*8+j);
					requestedPieceCount++;
				}
			}
		}
		this.r[1] = requestedPiece[rand.nextInt(requestedPieceCount)];
	}
	@Override
	public void run() {
			Message msg = new Message();
			msg.length = 8;// the length of a byte, supposedly
			msg.type = 6; // request
			msg.payload = this.r; // 
			P.send(P.peer_id[neighbour], msg);
	}
}

// we presume that an empty file with "0"s shalt be created once the programme is initialized
//"piece" class sends bytes from the "start"th byte to the "end"th byte
class Piece extends Thread {
	Peer P;
	private String file;
	private FileInputStream input = null;
	private int start;
	private int end;
	private int neighbour;
	private byte[] faggot;
	
	public Piece (String file,int start,int end, int neighbour) {
		this.neighbour = neighbour;
		try {
			this.input = new FileInputStream(file);
			for(int i = 0; i<(end - start); i++) {
				try {
					input.read(faggot,start,(end-start));
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@Override
	public void run() {
			Message msg = new Message();
			msg.length = end - start;//
			msg.type = 7; // piece
			msg.payload = this.faggot; // 
			P.send(P.peer_id[neighbour], msg);
	}
}
*/

class Choke implements Runnable {  //choking/uncholing thread.
//normal choke/unchoke thread: For every 'unchoking_internal' seconds, 
//select 'num_preferred_neighbors' neighbors interesting in my data to unchoke according to
//download rates from them. neighbors are selected randomly if I have owned the entire file.

    Peer P; //the peer reference.
    private Thread t = null;

    int[] getIndices(float[] originalArray) {
        int len = originalArray.length;
        float[] sortedCopy = originalArray.clone();
        int[] indices = new int[len];
        // Sort the copy
        Arrays.sort(sortedCopy);
        // Go through the original array: for the same index, fill the position where the
        // corresponding number is in the sorted array in the indices array
        for (int index = 0; index < len; index++) {
            indices[index] = Arrays.binarySearch(sortedCopy, originalArray[index]);
        }
        return indices;
    }

    @Override
    public void run() {  //this function implements the multi-threading run interface.
        //This is an independent thread.
        List<Integer> list = new ArrayList<Integer>();
        int num_peer = P.peer_port.length;
        int ind = -1;
        for (int i = 0; i < num_peer; i++) {
            list.add(i);
        }
        while (P.choke_thread_running) {
            synchronized (P.lock_current_neighbors) { //lock the object

                if(P.current_neighbors==null) continue;

                int[] cn = P.current_neighbors.clone();  //current neighbor.
                Arrays.sort(cn);
                if (P.own_file || P.finished) { //select randomly.
                    java.util.Collections.shuffle(list);
                    int cnt = 0; //#peers selected.
                    for (int i = 0; i < num_peer && cnt < P.num_preferred_neighbors; i++) {
                        ind = list.get(i);
                        if (P.is_interested[ind] && P.peer_id[ind] != P.id) {
                            P.current_neighbors[cnt++] = P.peer_id[ind];
                            //send unchoke message to previously choked peer.
                            if (Arrays.binarySearch(cn, P.peer_id[ind]) < 0) {
                                Message msg = new Message();
                                msg.length = 0; //no payload.
                                msg.type = 1; //unchoke.
                                msg.payload = null; //no payload.
                                P.send(P.peer_id[ind], msg);
                            }
                        }
                    }
                    P.print("Choke selected " + cnt + " neighbors randomly.");
                } else { //select according to the download rates.
                    int[] order = getIndices(P.download_rates);
                    int cnt = 0; //#peers selected.
                    for (int i = 0; i < num_peer && cnt < P.num_preferred_neighbors; i++) {
                        ind = order[i];
                        if (P.is_interested[ind] && P.peer_id[ind] != P.id) {
                            P.current_neighbors[cnt++] = P.peer_id[ind];
                            //send unchoke message to previously choked peer.
                            if (Arrays.binarySearch(cn, P.peer_id[ind]) < 0) {
                                Message msg = new Message();
                                msg.length = 0; //no payload.
                                msg.type = 1; //unchoke.
                                msg.payload = null; //no payload.
                                P.send(P.peer_id[ind], msg);
                            }
                        }
                    }
                    //print debugging info.
                    P.print("Choke selected " + cnt + " neighbors according to download rates.");
                }
                //send choke message to peers previously unchoked but currently choked.
                Arrays.sort(Arrays.copyOfRange(P.current_neighbors, 0, P.num_preferred_neighbors - 1));
                for (int i = 0; i < P.num_preferred_neighbors; i++) {
                    if (Arrays.binarySearch(P.current_neighbors, cn[i]) < 0) { //cn[i] not in current neighbor list.
                        Message msg = new Message();
                        msg.length = 0; //no payload.
                        msg.type = 0; //choke.
                        msg.payload = null; //no payload.
                        P.send(P.peer_id[ind], msg);
                    }
                }
            }
            //sleep some interval.
            try {
                Thread.sleep(1000 * P.unchoking_internal);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void start(Peer p) { //this method starts up the thread.
        P = p;
        if (t == null) {
            t = new Thread(this, "thread-Choke-" + P.id);
            t.start();
        }
    }
}

class Receive implements Runnable {
//This thread listen on the socket, and receive message from other peers.
    //Each peer should have their own Receive thread.
    //This thread should be run after connection established.
    Peer P; //the peer reference.
    private Thread t = null;
    Socket socket;

    @Override
    public void run() {  //this function implements the multi-threading run interface
        P.print("Receive thread starts up.");
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            while (P.choke_thread_running) { //read input forever.
                try{
                    Object o = ois.readObject(); //blocking.
                    if (o instanceof Handshake) { // this is a Handshake.
                        Handshake hs = (Handshake)o;
                        P.print("Received handshake from "+hs.pid);
                    }
                    else if(o instanceof Message){  //this is a Message.
                        Message M = (Message) o;
                        if(M.type==0){
                            P.print("Received choke from "+t.getName());
                        }
                        else if(M.type==1){
                            P.print("Received unchoke from "+t.getName());
                        }
                        else if(M.type==2){
                            P.print("Received interested from "+t.getName());
                        }
                        else if(M.type ==3){
                            P.print("Received not_interested from "+t.getName());                    
                        }
                        else if(M.type==4){
                            P.print("Received have from "+t.getName());
                        }
                        else if(M.type==5){
                            P.print("Received bitfield from "+t.getName());
                        }
                        else if(M.type==6){
                            P.print("Received request from "+t.getName());
                        }
                        else if(M.type==7){
                            P.print("Received piece from "+t.getName());
                        }
                        else{
                            P.print("Error: undefined message type: "+M.type);
                        }
                    }
                    else {
                        System.out.println("Class not found.");
                    }
                }
                catch(ClassNotFoundException ex){
                    ex.printStackTrace();
                }
            }
            ois.close();
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void start(Peer p, Socket s, int id_of_other_peer) {//this method starts up the thread.
        P = p;
        socket = s;
        if (t == null) {
            t = new Thread(this, id_of_other_peer+"");
            t.start();
        }
    }
}

class Accept implements Runnable {
    //This is waiting for incomming TCP connection request.
    //Once acceptting a new incomming connection, it will starts up a Receive thread waiting for message.
    Peer P; //the peer reference.
    private Thread t = null;

    @Override
    public void run() {  //this function implements the multi-threading run interface
        P.print("Listening on port "+P.port+", waiting for incomming connection...");
        try {
            ServerSocket serverSocket = new ServerSocket(P.port);
            while (P.choke_thread_running) { //read input forever.
                 Receive receive_thread = new Receive();
                 Socket s = serverSocket.accept();
                 String addr = s.getInetAddress().toString();
                 int p = s.getPort();
                 int i;
                 for(i = 0;i<P.peer_ip.length;i++){
                     if(addr.equals(P.peer_ip[i])){
                         P.sockets[i] = s;
                         P.tcp_out_stream[i] = new ObjectOutputStream(s.getOutputStream());
                         break;
                     }
                 }
                 receive_thread.start(P,s,P.peer_id[--i]);
                 P.print("Accepted incomming connection: ip = "+addr.substring(1)+", port = "+p);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void start(Peer p) {//this method starts up the thread.
        P = p;
        if (t == null) {
            t = new Thread(this, "Accept-" + P.id);
            t.start();
        }
    }
}

class OptChoke implements Runnable {  //Optimistic choking/uncholing thread.
    //optimistic choke/unchoke thread: For every 'optimistic_unchoking_interval' seconds, 
    //select 1 neighbor currently unchoked and interesting in my data to unchoke.

    Peer P; //the peer reference.
    private Thread t = null;

    int[] getIndices(float[] originalArray) {
        int len = originalArray.length;
        float[] sortedCopy = originalArray.clone();
        int[] indices = new int[len];
        // Sort the copy
        Arrays.sort(sortedCopy);
        // Go through the original array: for the same index, fill the position where the
        // corresponding number is in the sorted array in the indices array
        for (int index = 0; index < len; index++) {
            indices[index] = Arrays.binarySearch(sortedCopy, originalArray[index]);
        }
        return indices;
    }

    @Override
    public void run() {  //this function implements the multi-threading run interface
        //This is an independent thread.
        List<Integer> list = new ArrayList<Integer>();
        int num_peer = P.peer_port.length;
        int ind = -1;
        while (P.choke_thread_running) {
            synchronized (P.lock_current_neighbors) { //lock the object
                for (int i = 0; i < num_peer; i++) {
                    list.add(i);
                }
                //select randomly.
                java.util.Collections.shuffle(list);
                int[] cn = P.current_neighbors.clone();
                Arrays.sort(cn);
                for (int i = 0; i < num_peer; i++) {
                    int ind2 = list.get(i);
                    if (P.is_interested[ind2] && Arrays.binarySearch(cn, P.peer_id[ind2]) < 0 && P.peer_id[ind2] != P.id && P.sockets[ind2]!=null) {
                        ind = ind2;
                        P.current_neighbors[P.current_neighbors.length - 1] = P.peer_id[ind];
                        break;
                    }

                }
            }
            //send unchoke message.
            Message msg = new Message();
            msg.length = 0; //no payload.
            msg.type = 1; //unchoke.
            msg.payload = null; //no payload.
            
            if (ind >= 0) //there is someone interested in my data.
            {
                P.send(P.peer_id[ind], msg);
                P.print("OptChoke unchoked peer with id " + P.peer_id[ind]);
            }
            ind = -1;
            //print debugging info.
            
            //sleep some interval.
            try {
                Thread.sleep(1000 * P.optimistic_unchoking_interval);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void start(Peer p) {//this method starts up the thread.
        P = p;
        if (t == null) {
            t = new Thread(this, "thread-OptChoke-" + P.id);
            t.start();
        }
    }
}

public class Peer implements Runnable{
    
    String host_ip = "abc";

    Object lock_current_neighbors = new Object();
    
    Object tcp_lock = new Object();
    
    private Thread t = null; //thread.
    
    ObjectOutputStream[] tcp_out_stream;

    boolean choke_thread_running = true; //this flag is to determine the choking thread: false if we want to terminate the choking thread.

    byte[] bitfield;//Each bit in the bitfield represents whether the sending peer has the corresponding piece or not. The first byte of the bitfield corresponds to the piece indices 0 – 7 from high bit to low bit, respectively. The next one corresponds to piece indices 8 – 15, etc. Spare bits at the end are set to zero. Peers that do not have any pieces yet may skip a ‘bitfield’ message.

    //peer status: this information change dynamically during the entire process.
    boolean connection = false; //whether the peer has connected into the network.
    int[] current_neighbors; //Each peer determines the preferred neighbors every p seconds. It should be size of num_preferred_neighbors + 1.
    float[] download_rates; //To make the decision, peer A calculates the downloading rate from each of its neighbors during the previous p- second unchoking interval.
    boolean[] is_interested; //If the peer is interested in my data, it is true, false otherwise.
    boolean[] choke; //the choke status of peers: true if choked, false otherwise.
    boolean finished = false;  //true if the peer has already downloaded the entire file.
    Socket[] sockets;  //the socket to peers, excluding to myself.

    //system configuration parameters: this information must be initialized from Common.cfg
    int unchoking_internal;
    int optimistic_unchoking_interval;
    String file_name;
    long file_size; //int bytes.
    int piece_size; //int bytes.
    byte[] file_pieces; //the sliced file pieces (actual data of the file.)
    int num_preferred_neighbors; //At any moment, each peer uploads its pieces to at most k preferred neighbor
    //and 1 optimistically-unchoked neighbor. The value of k is given as a parameter when the program starts. 

    //peer configuration parameters: this information must be initialized from PeerInfo.cfg
    int id; //peer id
    String host = "localhost";
    int port; //listening port.
    boolean own_file; //true if the peer owns the file.
    String[] peer_ip; //the ip of all the peers in the network, read from configuration file, excluding myself.
    int[] peer_port; //the port of all the peers in the network, excluding myself.
    int[] peer_id; //the id of all the peers in the network, excluding myself.

    static Vector<RemotePeerInfo> myPeerInfo;
    int myRank; //my rank in all the peers
    int num_peers; // number of peers except me
    int num_pieces;// number of pieces of the file

    //read PeerInfo.cfg
    void getPeerInfo() {

        String strcount;// string used to count peers

        String str;

        int peercount = 0;// to calculate total peers number

        int count = 0; //to calculate myrank

        myPeerInfo = new Vector<RemotePeerInfo>();//peers' info vector

        try {

            BufferedReader incount = new BufferedReader(new FileReader("PeerInfo.cfg"));//used to count lines

            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));

            //Calculate lines, i.e peer numbers in the system
            while ((strcount = incount.readLine()) != null) {

                peercount++;

            }

            incount.close();

            peercount--;

            peer_ip = new String[peercount];

            peer_port = new int[peercount];

            peer_id = new int[peercount];

            num_peers = peercount;
            
            int cnt2 = 1;

            while ((str = in.readLine()) != null) {

                String[] tokens = str.split("\\s+");

                myPeerInfo.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));

                if (this.id != Integer.parseInt(tokens[0])) {// if host id is not equal to id read, collect other peers

                    peer_id[count] = Integer.parseInt(tokens[0]);

                    peer_ip[count] = tokens[1];

                    peer_port[count] = Integer.parseInt(tokens[2]);
                    
                    peer_ip[count] = host_ip;

                    count++;

                } else {// if host id is equal to the id read, read info

                    own_file = Integer.parseInt(tokens[3])==1;

                    port = Integer.parseInt(tokens[2]);

                    myRank = count;

                }
                
                cnt2++;

            }

            in.close();

        } catch (Exception ex) {

        }

    }

    //read CommonInfo.cfg
    public void getCommonConfig() {
        String cfg;
        ArrayList<String> configInfo = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
            while ((cfg = in.readLine()) != null) {

                String[] tokens = cfg.split("\\s+");
                configInfo.add(tokens[1]);
            }

            num_preferred_neighbors = Integer.parseInt(configInfo.get(0));
            unchoking_internal = Integer.parseInt(configInfo.get(1));
            optimistic_unchoking_interval = Integer.parseInt(configInfo.get(2));
            file_name = configInfo.get(3);
            file_size = Integer.parseInt(configInfo.get(4));
            piece_size = Integer.parseInt(configInfo.get(5));
            num_pieces = (int) Math.ceil((double) file_size / piece_size);

            in.close();
        } catch (Exception ex) {

        }
    }

    boolean log(String msg) {
        //This function write 'msg' into the log file. return true if success, false otherwise.
        String name = "log_peer[" + id + "]" + ".log";
        java.util.Date date = new java.util.Date();
        Timestamp ts = new Timestamp(date.getTime());
        msg = "[" + ts + "]: " + msg;
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name, true)))) {
            out.println(msg);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            System.out.println(e);
            return false;
        }
        return true;
    }

    void init_debug() {
        System.out.println("unchoking_internal=" + unchoking_internal);
        System.out.println("optimistic_unchoking_interval=" + optimistic_unchoking_interval);
        System.out.println("file_name=" + file_name);
        System.out.println("file_size=" + file_size + " (bytes)");
        System.out.println("piece_size=" + piece_size);
        System.out.println("id=" + id);
        System.out.println("num_preferred_neighbors=" + num_preferred_neighbors);
        System.out.println("unchoking_internal=" + unchoking_internal);
        System.out.println("port=" + port);
        System.out.println("own_file=" + own_file);
        if (peer_ip.length != peer_id.length) {
            System.out.printf("Error: #ip=%d, #id=%d, #port=%d\n", peer_ip.length, peer_id.length, peer_port.length);
        }
        System.out.println("We have " + peer_ip.length + " peers in the network.");
        for (int i = 0; i < peer_ip.length; i++) {
            System.out.println("Peer [" + peer_id[i] + "]: ID = " + peer_id[i] + ", IP = " + peer_ip[i] + ", Port = " + peer_port[i]);
        }
    }

    boolean set_up_connection_to_peers() {
        //This function set up connection to all peers whose ID less than myself.
        sockets = new Socket[num_peers];
        int cnt = 0;
        for (int i = 0; i < peer_ip.length; i++) {
            if (peer_id[i] < id) {
                try {
                    sockets[i] = new Socket(peer_ip[i], peer_port[i]);
                    tcp_out_stream[i] = new ObjectOutputStream(sockets[i].getOutputStream());
                    print("Peer " + id + " successfully connects to " + peer_id[i]);
                    cnt++;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    print("Failed to connect to " + peer_id[i] + ", " + peer_ip[i] + ":" + peer_port[i]);
                    return false;
                }
            } else {
                sockets[i] = null;  //to be connected by later peers.
            }
        }
        print("Peer " + id + " TCP connection done. Connected to " + cnt + " peers");
        return true;
    }

    boolean send(int pid, Object M) { //send message to peer with ID = peer_id.
        synchronized (tcp_lock) {
            try {
                //look up the index for peer whose id = peer_id.
                int[] sortedCopy = peer_id.clone();
                Arrays.sort(sortedCopy);
                int index = Arrays.binarySearch(sortedCopy, pid);
                if(index<0){
                    print("*Error: peer "+pid+" not found.");
                }
                if(sockets[index]==null){
                    print("sockets[index]=null");
                    System.exit(-1);
                }
                tcp_out_stream[index].writeObject(M);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            return true;
        }
    }

    boolean handshake() {

        //construct handshake message.
        Handshake hs_send = new Handshake(this.id);
        Message bitfield_message = new Message();

        bitfield_message.length = bitfield.length;

        bitfield_message.type = 5;

        bitfield_message.payload = bitfield;

        //handshake with each peers and send them bitfield information
        for (int i = 0; i < this.num_peers; i++) {

            if (peer_id[i] < this.id) {

                send(peer_id[i], hs_send);

                send(peer_id[i], bitfield_message);

            }

        }
        return true;
    }

    void print(String msg) {
        System.out.println("[" + id + "] " + msg);
    }
    
    byte[] readPiece(int index){ //this function reads a piece of data.
        return Arrays.copyOfRange(file_pieces,index*piece_size,piece_size*(index+1)-1);
    }
    void writePiece(int index, byte[] values){ //this function writes a piece of data [values].
        int cnt = 0;
        for(int i = index*piece_size;i<(index+1)*piece_size;i++){
            file_pieces[i] = values[cnt++];
        }
    }
    
    void initialization(){
       //initialization
        download_rates = new float [num_peers];
        for(int i = 0;i<num_peers;i++) download_rates[i] = 0;
        if(num_preferred_neighbors>num_peers) num_preferred_neighbors = num_peers;
        current_neighbors = new int [num_preferred_neighbors+1];
        for(int i = 0;i<current_neighbors.length;i++)
            current_neighbors[i] = peer_id[i%num_peers];
        is_interested = new boolean [num_peers];
        for(int i = 0;i<num_peers;i++) is_interested[i] = true;
        try{
            tcp_out_stream = new ObjectOutputStream [num_peers];
        }catch(Exception ex){
            
        }
        //bitfield and file_pieces initialization.
        bitfield = new byte [num_pieces];
        Arrays.fill(bitfield, (byte)0);
        if(own_file){
            Arrays.fill(bitfield, (byte)1);  
            try{
                file_pieces = Files.readAllBytes(Paths.get(file_name));
                System.out.print("File content: ");
                for(int i = 0;i<file_pieces.length;i++)
                    System.out.print((char)file_pieces[i]);
                System.out.print("\n\n");
            }
            catch(Exception ex){
                print("Could not open the data file: "+file_name);
                System.exit(-1);
            }
        }
    }
    
    @Override
    public void run() {
        
        try{
            host_ip = InetAddress.getLocalHost().getHostAddress();
        }
        catch(Exception ex){
            System.out.println("Failed to get local IP.");
        }
        print("Peer id = "+id+" starts up.");
        
        //log("This is a log message");
        //get configuration.
        getPeerInfo();
        getCommonConfig();
        initialization();
        //init_debug();
        //start thread for accepting connection request.
        Accept accept_connection_thread = new Accept();
        accept_connection_thread.start(this);
        //set up connection to existing peers.
        if(!set_up_connection_to_peers()) return;
        //handshake
        handshake();
        //sendbitfield();
        //begin choking and unchoking                
        Choke choke_thread = new Choke();
        OptChoke opt_choke_thread = new OptChoke();
        choke_thread.start(this);
        opt_choke_thread.start(this);
    }
    
    void start(int pid) { //this function start the peer.
        id = pid;
        if (t == null) {
            t = new Thread(this, "thread-peer-"+pid);
            t.start();
        }
    }
    Peer() {
        
    }
}
