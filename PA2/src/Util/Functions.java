package Util;

import Model.DVR;
import Model.Message;
import Model.NodeServer;
import Server.Server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.*;

import static Model.DVR.*;

public class Functions {
    private static final int INF = Integer.MAX_VALUE-2;
    boolean running = true;
    static boolean cmdInput = false;
    static Scanner input = new Scanner(System.in);
    public static int numOfPackets = 0;
    static int time;


    public static void cmdFunctions() {


        boolean running = true;
        boolean cmdInput = false;

        Timer timer = new Timer();

        while (running) {
            String line = input.nextLine();
            String[] arguments = line.split(" ");
            String command = arguments[0];

            switch (command) {
                case "server":
                    if (arguments.length != 4) {
                        System.out.println("Invalid input for command.");
                        System.out.println("Please use server -t <topology-file-name> -i <routing-update-interval>.");
                        System.out.println("Please try again.");
                        break;
                    }
                    try {
                        if (Integer.parseInt(arguments[3]) < 10) {
                            System.out.println("Please input routing update interval above 10 seconds.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter valid integer for update interval");
                        break;
                    }
                    if ((arguments[1] == "" || arguments[2] == "" || !arguments[2].equals("-i") || arguments[3] == "")) {
                        System.out.println("Incorrect command. Please try again.");
                        break;
                    } else {
                        cmdInput = true;
                        String fileName = arguments[1];
                        time = Integer.parseInt(arguments[3]);
                        DVR.readFile(fileName);
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                step();
                            }
                        }, time * 1000L, time * 1000L);
                    }
                    break;
                case "update":
                    if (cmdInput) {
                        update(Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]));
                    } else {
                        System.out.println("<Update> Failed. Please try again.");
                    }
                    break;
                case "step":
                    if (cmdInput) {
                        step();
                    } else {
                        System.out.println("<Step> Failed. Please try again.");
                    }
                    break;
                case "packets":
                    if (cmdInput) {
                        System.out.println("Number of packets received = " + numOfPackets);
                    } else {
                        System.out.println("<Packets> failed. Please try again.");
                    }
                    break;
                case "display":
                    if (cmdInput) {
                        displayRouting();
//                        displayNeighMap();
//                        displayNeigh();
                    } else {
                        System.out.println("<Display> failed. Please try again.");
                    }
                    break;
                case "disable":
                    if (cmdInput){
                        int serverToDisable = Integer.parseInt(arguments[1]);
                        NodeServer serverId = getNodeId(serverToDisable);
                        disable(serverId);
                    }else {
                        System.out.println("<Disable> Failed.");
                    }
                    break;
                case "crash":
                    if (cmdInput){
                        running = false;
                        for (NodeServer allNeighbors : DVR.neighborMap){
                            disable(allNeighbors);
                        }
                        System.out.println("<Crash> Success");
                        System.out.println("Closing all connections...");
                        timer.cancel();
                        System.exit(0);
                    }else {
                        System.out.println("<Crash> failed. Please try again!");
                    }
                    break;
                default:
                    System.out.println("Wrong command! Please check again.");
            }

        }
        input.close();
    }


    //Step
    public static void step() {
        if (DVR.neighborMap.size() >= 1) {

            Message message = new Message(DVR.myNodeServer.getId(), DVR.myNodeServer.getIpAddress(), DVR.myNodeServer.getPort());
            message.setRoutingUpdate(createMsg());

            for (NodeServer neighbor : DVR.neighborMap) {
                sendMessage(neighbor,message);
                System.out.println("Sent routing update to " + neighbor.getIpAddress());
            }
            System.out.println("<Step> Success");
        }else{
            System.out.println("No neighbors found for step command.");
        }
    }

    //Update
    public static void update(int serverId1,int serverId2, int cost){
        if(serverId1 == DVR.myId){
            NodeServer updateTo = DVR.getNodeId(serverId2);
            if(isPeer(updateTo)){
                DVR.updateTable.put(updateTo,cost);
                Message message = new Message(DVR.myNodeServer.getId(), DVR.myNodeServer.getIpAddress(), DVR.myNodeServer.getPort());
                message.setRoutingUpdate(createMsg());
                sendMessage(updateTo,message);
                System.out.println("<Update> SUCCESS");
            }
            else {
                System.out.println("<Update> failed, you may only update your cost to your own neighbor.");
            }
        }
        if(serverId2 == DVR.myId){
            NodeServer updateTo = DVR.getNodeId(serverId1);
            if(isPeer(updateTo)){
                DVR.updateTable.put(updateTo,cost);
                Message message = new Message(DVR.myNodeServer.getId(),DVR.myNodeServer.getIpAddress(),DVR.myNodeServer.getPort());
                message.setRoutingUpdate(createMsg());
                sendMessage(updateTo,message);
                System.out.println("<Update> SUCCESS");
            }
            else {
                System.out.println("<Update> failed, you may only update your cost to your own neighbor.");
            }
        }
    }

    public static void displayRouting(){
        //Creates new object of Display table
        DisplayTable dt = new DisplayTable();
        System.out.println("================Routing table==============================");
        String[] columnNames = {"Destination Server ID | ", "Next Hop Server ID | ", "Cost of Path"};
        //Adds column names to display table
        dt.addRow(columnNames);

        //Sorts the list of server nodes in the topology list using a Comparator
        DVR.topologyNode.sort(new serverOrder());

        //Iterate through the topology node list
        for (Iterator<NodeServer> iterator = topologyNode.iterator(); iterator.hasNext(); ) {
            //Assigns next element in the node server to the new node
            NodeServer node = iterator.next();
            //
            int cost = updateTable.get(node);
            String costStr = "" + cost;
            String hopId = "N.A.";

            if (cost == INF) {
                costStr = "Infinity";
                //System.out.println("!" + node.getId() + hopId + costStr);
            }

            if (nextHop.get(node) != null) {
                hopId = "" + nextHop.get(node).getId();

            }
            String[] rows = {"" + node.getId(), hopId, costStr};
            dt.addRow(rows);

        }
        System.out.println(dt);
        System.out.println("==========================================================");
    }

    /**
     *
     * @param server
     * @return
     */
    public static boolean disable(NodeServer server){
        //Check if the server is a neighbor
        if(isPeer(server)){
            //Loop through the channel list
            for(SocketChannel connection: DVR.connectionList){
                //Check if the server ip address is equal to the connected socket's ip
                if(server.getIpAddress().equals(Server.socketIp(connection))){
                    try {
                        //Closes the connection
                        connection.close();
                    }catch (IOException e){
                        System.out.println("Failed to close connection.");
                    }
                    //Removes the disabled connection from the list of connections
                    DVR.connectionList.remove(connection);
                    break;
                }
            }
            //Updates the routing table and assigns the disable server's link cost to infinity
            DVR.updateTable.put(server,INF);
            //Removes the server from the list of neighbors
            DVR.neighborMap.remove(server);
            System.out.println("<Disable> Success.");
            System.out.println("Connection to server " + server.getId() + " "
                    + "IP: " + server.getIpAddress() + " has been disabled.");
            return true;
        }
        else {
            System.out.println("<Disable> Failed. Can not disable connection with non neighboring servers.");
            return false;
        }
    }

    /**
     * Method to populate routing table
     */
    public static Map<NodeServer,Integer> initTable(List<String> table){
        //Creates a new table
        Map<NodeServer,Integer> newTable = new HashMap<>();
        //Iterate through the given table
        for (Iterator<String> iterator = table.iterator(); iterator.hasNext(); ) {
            String str = iterator.next();
            String[] lineArgs = str.split("\\s+");
            int serverId = Integer.parseInt(lineArgs[0]);
            int linkCost = Integer.parseInt(lineArgs[1]);
            //Adds server id and link cost to routing table
            newTable.put(getNodeId(serverId), linkCost);
        }
        return newTable;
    }


    //Packets
//    public void packets(){
//        int numOfPackets = 0;
//        for(int i = 0; i < numOfPackets; i++){
//            numOfPackets +=
//        }
//    }


    //Test methods======================================================================

    public static void displayNeigh(){
        System.out.println("Here are my neighbors");
        for (NodeServer neighbor: DVR.neighborMap){
            if (isPeer(neighbor)){
                System.out.println(neighbor);
            }
        }
    }

    public static void displayNeighMap(){
        System.out.println("Elements from Neighbor Map");
        for(NodeServer neigh : DVR.neighborMap){
            System.out.println("ID:" + neigh.getId() + " IP:" + neigh.getIpAddress() + " Port:" + neigh.getPort());
        }
    }
//================================================================================================

    /**
     * Boolean method to check if given server is a neighbor
     * @param server
     * @return
     */
    public static boolean isPeer(NodeServer server){
        return neighborMap.contains(server);
    }

    /**
     * Method to sort of server id from small to big
     */
    static class serverOrder implements Comparator<NodeServer>{
        @Override
        public int compare(NodeServer o1, NodeServer o2) {
            Integer server1 = o1.getId();
            Integer server2 = o2.getId();
            return server1.compareTo(server2);
        }
    }

}
