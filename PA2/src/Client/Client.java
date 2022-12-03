package Client;

import Model.DVR;
import Model.Message;
import Model.NodeServer;
import Server.Server;
import Util.Functions;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Client extends Thread {
    /**
     * Infinity variable
     */
    private static final int INF = Integer.MAX_VALUE - 2;
    int bytes;
    /**
     * Variable for socket connection
     */
    SocketChannel socketChannel;
    /**
     * Variable to iterate over keys in SelectionKey
     */
    Iterator<SelectionKey> keyIterator;
    ByteBuffer buf = ByteBuffer.allocate(4000);
    /**
     * Set collection of keys and ensures no duplicate elements or keys
     * contains the keys representing the current channel registrations of this selector.
     */
    Set<SelectionKey> keySet;

    /**
     * Run method uses
     */
    public void run() {
        try {
            while (true) {
                //initialize clientReady with the selector object read and add the keys of channels
                int clientReady = DVR.read.selectNow();
                //Assign the set to the set of keys such that each key's channel was detected
                // to be ready for at least one of the operations
                keySet = DVR.read.selectedKeys();
                //Iterate through ket set
                keyIterator = keySet.iterator();

                //Checks if channel keys are not empty or 0
                if (clientReady != 0) {
                    //While key iterator has elements
                    while (keyIterator.hasNext()) {
                        //Assign next key set element to key
                        SelectionKey key = keyIterator.next();
                        //Assign the key of channel to socket channel
                        socketChannel = (SocketChannel) key.channel();
                        try {
                            //Assigns the sequence of bytes read from the socket channel
                            bytes = socketChannel.read(buf);
                        } catch (IOException e) {
                            //Removes key
                            keyIterator.remove();
                            //Assign ip address of the socket connection's socket
                            String ipAddress = Server.socketIp(socketChannel);
                            //Assign the parsed ip address of the socket to a new node
                            NodeServer newNode = DVR.getNodeIP(ipAddress);
                            //Disables this node
                            Functions.disable(newNode);
                            System.out.println(ipAddress + " closed the connection.");
                            break;
                        }
                        String msg = "";
                        //While bytes read are not zero
                        while (bytes != 0) {
                            //Prepares for a sequence of get operations and channel writing
                            buf.flip();
                            //While there are byte elements
                            while (buf.hasRemaining()) {
                                //get bytes and assign to String
                                msg += ((char) buf.get());
                            }
                            ObjectMapper mapper = new ObjectMapper();
                            Message message = null;
                            boolean recievedMsg = false;
                            int fromServerId = 0;
                            try {
                                message = mapper.readValue(msg, Message.class);
                                recievedMsg = true;
                                //Increase packets received by 1
                                Functions.numOfPackets++;
                                fromServerId = message.getId();
                            } catch (JsonMappingException e) {
                                System.out.println("Error! No neighbors.");
                                System.out.println(e.getMessage());
                                break;
                            }

                            //Create new node for the sender node's id
                            NodeServer fromNodeServer = DVR.getNodeId(fromServerId);
                            if (message != null) {
                                if (message.equals("update") && recievedMsg) {
                                    //Create new list for the incoming routing update
                                    List<String> tableRecieved = message.getRoutingUpdate();
                                    //Create new table and assign the received routing update
                                    Map<NodeServer, Integer> createTable = Functions.initTable(tableRecieved);

                                    int currentLinkCost = DVR.updateTable.get(fromNodeServer);
                                    int newLinkCost = createTable.get(DVR.myNodeServer);

                                    //Checks if current cost is not the same as new cost
                                    if (currentLinkCost != newLinkCost) {
                                        //updates routing table with new cost
                                        DVR.updateTable.put(fromNodeServer, newLinkCost);
                                    }
                                }
                                //Checks if message sent was a step
                                if (message.equals("step") && recievedMsg) {
                                    //Create new list for the incoming routing update
                                    List<String> tableRecieved = message.getRoutingUpdate();
                                    //Create new table and assign the received routing update
                                    Map<NodeServer, Integer> createTable = Functions.initTable(tableRecieved);

                                    //Iterate through each entry of the routing table
                                    for (Map.Entry<NodeServer, Integer> serverEntry : DVR.updateTable.entrySet()) {
                                        //Checks if an entry key is equal to host machine key
                                        if (serverEntry.getKey().equals(DVR.myNodeServer)) {
                                        } else {
                                            int currentLinkCost = serverEntry.getValue();
                                            int costToReceiver = createTable.get(DVR.myNodeServer);
                                            int linkCostFinal = createTable.get(serverEntry.getKey());

                                            if (costToReceiver + linkCostFinal < currentLinkCost) {
                                                DVR.updateTable.put(serverEntry.getKey(), costToReceiver + linkCostFinal);
                                                DVR.nextHop.put(serverEntry.getKey(), fromNodeServer);
                                            }
                                        }
                                    }
                                    //if message received was a disable message
                                    if (message.equals("disable") || !recievedMsg) {
                                        //updates routing table of the from serverId and sets link cost to infinity
                                        DVR.updateTable.put(fromNodeServer, INF);
                                        System.out.println("Routing table has been updated.");
                                        System.out.println("Server " + fromServerId + " link cost is now set to infinity.");

                                        //Checks if sender is a neighbor
                                        if (Functions.isPeer(fromNodeServer)) {

                                            //loop through the connection list
                                            for (SocketChannel socketChannel : DVR.connectionList) {
                                                //Check if sender's ip address is equal to the ip address of the socket
                                                // channels socket
                                                if (fromNodeServer.getIpAddress().equals(Server.socketIp(socketChannel))) {
                                                    try {
                                                        socketChannel.close();
                                                    } catch (IOException e) {
                                                        System.out.println("Could not close the connection.");
                                                        System.out.println(e.getMessage());
                                                    }
                                                    //Removes socket channel from connection list
                                                    DVR.connectionList.remove(socketChannel);
                                                    break;
                                                }
                                            }
                                            //updates routing table of the from serverId and sets link cost to infinity
                                            DVR.updateTable.put(fromNodeServer, INF);
                                            //Removes this server from neighbor map
                                            DVR.neighborMap.remove(fromNodeServer);
                                        }
                                    }
                                }
                                if (msg.isEmpty()) {
                                    break;
                                } else {
                                    System.out.println("Received a message from " + message.getId());
                                    Functions.displayRouting();
                                }
                                //Clears bytes
                                buf.clear();
                                if(msg.trim().isEmpty()){
                                    bytes = 0;
                                }else{
                                    try {
                                        bytes = socketChannel.read(buf);
                                    }catch (ClosedChannelException e){
                                        System.out.println("Channel closed with server " + fromServerId);
                                    }
                                }
                                bytes = 0;
                                //Removes key from iterator list
                                keyIterator.remove();
                            }
                        }
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
