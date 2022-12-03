

package Model;

import Client.Client;
import Server.Server;
import Util.Functions;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;



public class DVR {
    public static Selector read;
    public static Selector write;
    /**
     * Variable to hold local host id
     */
    public static int myId = Integer.MIN_VALUE + 2;
    /**
     * Variable to hold local host ip address
     */
    public static String myIp = "";

    /**
     * List to hold the information from topology file
     */
    public static List<NodeServer> topologyNode = new ArrayList<>();

    /**
     * Node to hold host machine's topology information
     */
    public static NodeServer myNodeServer = null;

    /**
     * Map of routing update table
     */
    public static Map<NodeServer, Integer> updateTable = new HashMap<>();

    /**
     * NeighborMap is the set of neighbors the server is has
     */
    public static Set<NodeServer> neighborMap = new HashSet<>();

    /**
     * List of open channels/connections
     */
    public static List<SocketChannel> connectionList = new ArrayList<>();

    /**
     * nextHop is the map of next-hop-server-id
     */
    public static Map<NodeServer,NodeServer> nextHop = new HashMap<>();

    //boolean running = true;


    /**
     *This method is used to read the topology file name
     * @param fileName
     */
    public static void readFile(String fileName) {
        File file = new File("src/" + fileName);
        NodeServer from;
        try {
            Scanner input = new Scanner(file);
            //Reads the number of servers
            int numOfServers = input.nextInt();
            //Reads the number of neighbors
            int numOfNeighbors = input.nextInt();
            input.nextLine();

            //Loop through number of servers
            for (int i = 0; i < numOfServers; i++) {
                String line = input.nextLine();
                String[] lineArgs = line.split("\\s+");
                //Assigns each server's id, ip address and port number to a node in the topology list
                NodeServer newNode = new NodeServer(
                        Integer.parseInt(lineArgs[0]), lineArgs[1], Integer.parseInt(lineArgs[2]));
                topologyNode.add(newNode);

                //int cost = Integer.parseInt(lineArgs[2]);
                //Setting the cost to infinity
                int cost = Integer.MAX_VALUE-2;

                //Checks if a server's ip is the same as the host's
                if (lineArgs[1].equals(myIp)) {
                    //Assigns id to host id
                    myId = Integer.parseInt(lineArgs[0]);
                    myNodeServer = newNode;
                    //set cost to 0 since the connection is
                    cost = 0;
                    nextHop.put(newNode,myNodeServer);
                }else{
                    nextHop.put(newNode,null);
                }
                //Adds NodeServer node and cost to routing table
                updateTable.put(newNode, cost);
                connect(lineArgs[1], Integer.parseInt(lineArgs[2]), myId);
            }
            //Loops through the section of server id, neighbor id, and cost
            for (int i = 0; i < numOfNeighbors; i++) {
                String line = input.nextLine();
                String[] args = line.split("\\s+");

                //Initializing the server id that will connect to other server
                int fromID = Integer.parseInt(args[0]);

                //Initializing the neighbor id that is being connected to
                int toId = Integer.parseInt(args[1]);

                //Initializing the link cost between the server and neighbor
                int cost = Integer.parseInt(args[2]);

                //Checks if connecting serverId is the host id
                if (fromID == myId) {
                    //Initialize a node for the neighbor id and assigns the id to neighbor
                    NodeServer to = getNodeId(toId);
                    //Updates routing table with neighbor id and link cost
                    updateTable.put(to, cost);
                    //Adds neighbor id to the Node Set of neighbors
                    neighborMap.add(to);

                    nextHop.put(to,to);
                }
                //Checks if neighboring id is host id
                if (toId == myId) {
                    //assigns the id to server id
                    from = getNodeId(fromID);
                    //Puts the id of the sender and link cost to routing table
                    updateTable.put(from, cost);
                    //Adds the id of the sender to the Neighbor map set
                    neighborMap.add(from);

                    nextHop.put(from,from);
                }

            }
            System.out.println("Topology successfully read.");
            input.close();

        } catch (FileNotFoundException e) {
            System.out.println(file.getAbsolutePath() + " was not found or does not exist.");
        }
    }


    /**
     * Method to obtain the ip address
     * Uses the Network Interface
     * @return
     */
    public static String getIP() {
        String ip;
        try {
            //Returns all the interfaces on this machine
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            //Loops while variable interfaces contains more elements
            while (interfaces.hasMoreElements()) {
                //Assigns the next element that is returned in the enumeration
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // *EDIT*
                    if (addr instanceof Inet6Address) continue;

                    ip = addr.getHostAddress();
                    return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    /**
     * This method is used to connect the local server to other server's
     * @param ip
     * @param port
     * @param id
     */
    public static void connect(String ip, int port, int id) {
        System.out.println("Connecting to ip: " + ip);

        try {
            //Checks if the ip address being connected to is the same as the host machine
            if (!ip.equals(myIp)) {
                //Creates a variable of type SocketChannel and opens a socket channel
                SocketChannel socketChannel = SocketChannel.open();
                //The open socket channel connects to the created socket address from the ip and port number
                socketChannel.connect(new InetSocketAddress(ip, port));
                //sets the channel in non-blocking mode
                socketChannel.configureBlocking(false);
                //Registering socketChannel to monitor any channels with the register method
                //First parameter (read,write) is the selector object
                socketChannel.register(read, SelectionKey.OP_READ);
                socketChannel.register(write, SelectionKey.OP_WRITE);
                //Adds the connected socketChannel between server and host to the connectionList
                connectionList.add(socketChannel);
                System.out.println("Successfully connected to: " + ip);
            }

        } catch (IOException e) {
            System.out.println("Error! " + e.getMessage() + ",attempted to connect to: " + ip);
            //System.out.println("You cannot connect to yourself.");
        }
    }

    public static List<String> createMsg(){
        List<String> msg = new ArrayList<>();
        for (Map.Entry<NodeServer,Integer> entry: updateTable.entrySet()){
            NodeServer node = entry.getKey();
            Integer entryVal = entry.getValue();
            msg.add(node.getId() + "#" + entryVal);
        }
        return msg;
    }

    /**
     * Method used to send messages between machinces such as routing update, connection status, step command, etc.
     * @param neighborNode
     * @param msg
     */
    public static void sendMessage(NodeServer neighborNode, Message msg){
        //Variable to hold set of keys
        int keySet = 0;
       try {
           //assigns set of keys to the selector object write
           //then selects a set of keys whose respective channels are ready for I/O operations
           keySet = write.select();

           if(keySet > 0){
               //Assigns this selector's selected key set
               Set<SelectionKey> keys = write.selectedKeys();
               //Iterates over the elements in the SelectionKet set
               Iterator<SelectionKey> keySetIterator = keys.iterator();
               //ByteBuffer buffer = ByteBuffer.allocateDirect(5000);

               //Using byte buffer for transferring bytes from a source to a destination
               ByteBuffer sendData = ByteBuffer.allocate(5000);
               //
               ObjectMapper mapper = new ObjectMapper();
               String message = mapper.writeValueAsString(msg);
               //sendData = message.getBytes();

               //Transfers content of the given byte array into the buffer
               sendData.put(message.getBytes());
               //
               sendData.flip();

               //While keySetIterator has more elements
               while (keySetIterator.hasNext()){
                   //Assign next element in keySetIterator to the key selection
                   SelectionKey selectionKey = keySetIterator.next();

                   //Checks if the ip address of the selected key in the socket channel is equal to
                   // the neighbor's ip address
                   if(Server.socketIp((SocketChannel) selectionKey.channel()).equals(neighborNode.getIpAddress())){
                       //Assigns the open/connected channel to the valid channel of the selected key
                       SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                       //Writes header and data to channel
                       socketChannel.write(sendData);
                   }
                   //Removes the last item returned from the iterator
                   keySetIterator.remove();
               }
           }
       } catch (IOException e) {
           System.out.println("Message failed to send, " + e.getMessage());
       } catch (IllegalArgumentException i){
           System.out.println("Bytes reached capacity." + i.getMessage());
       }
    }


    /**
     * Returns the node given the id
     * @param id
     * @return
     */
    public static NodeServer getNodeId(int id) {
        for (NodeServer node : topologyNode) {
            if (node.getId() == id) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns the node given the ip address
     * @param ip
     * @return
     */
    public static NodeServer getNodeIP(String ip) {
        for (NodeServer node : topologyNode) {
            if (node.getIpAddress().equals(ip)) {
                return node;
            }
        }
        return null;
    }


    /**
     * Driver method
     * @param args
     * @throws IOException
     */

    public static void main(String[] args) throws IOException {
        Server server = new Server(5000);
        Client client = new Client();

        //Creates two selectors
        read = Selector.open();
        write = Selector.open();

        //Timer timer = new Timer();
        //Scanner input = new Scanner(System.in);

        boolean running = true;
        boolean cmdInput = false;

        //myIp = getMyIP();
        myIp = getIP();

        server.start();
        System.out.println("Server is now running...");

        client.start();
        System.out.println("Client is now running...");

        System.out.println();
        System.out.println("Distance Vector Routing Protocol is now online...");
        //System.out.println("Host ip: " + myIp);

        while (running) {
            System.out.println();
            System.out.println("********************Help Menu************************");
            System.out.println("1. server -t <topology-file> -i <time-interval-in-seconds>");
            System.out.println("2. update <server-id1> <server-id2> <new-cost>");
            System.out.println("3. step");
            System.out.println("4. packets");
            System.out.println("5. display");
            System.out.println("6. disable <server-id>");
            System.out.println("7. crash");
            System.out.println("*****************************************************");
            System.out.println();

            Functions.cmdFunctions();
        }

    }
}

