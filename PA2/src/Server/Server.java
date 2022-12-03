/**
 * Connect: when a client attempts to connect to the server. Represented by SelectionKey.OP_CONNECT
 * Accept: when the server accepts a connection from a client. Represented by SelectionKey.OP_ACCEPT
 * Read: when the server is ready to read from the channel. Represented by SelectionKey.OP_READ
 * Write: when the server is ready to write to the channel. Represented by SelectionKey.OP_WRITE
 */




package Server;

import Model.DVR;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server extends Thread {
    private int port = 0;

    public Server(int port){
        this.port = port;
    }

    /**
     *
     */
    public void run(){
        try{
            DVR.read = Selector.open();
            DVR.write = Selector.open();
            //open a server-socket channel for a socket
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            //Set nonblocking mode for the listening socket
            serverSocketChannel.configureBlocking(false);
            //Binds the specified port number
            serverSocketChannel.bind(new InetSocketAddress(port));

            while(true){
                SocketChannel socketChannel = serverSocketChannel.accept();
                if(socketChannel != null){
                    socketChannel.configureBlocking(false);
                    //Register the SocketChannel with the selector
                    socketChannel.register(DVR.read, SelectionKey.OP_READ);
                    socketChannel.register(DVR.write, SelectionKey.OP_WRITE);
                    //Adds socket channel to ArrayList of open channels
                    DVR.connectionList.add(socketChannel);
                    System.out.println("Connection to client: " + socketIp(socketChannel) + " was successful." );
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Method to get socket channel's socket ip address
     * @param socketChannel
     * @return
     */
    public static String socketIp(SocketChannel socketChannel){
        String socketIp = "";
        try{
            socketIp = socketChannel.getRemoteAddress().toString().split(":")[0];
        } catch (IOException e) {
            System.out.println("Error cannot get socketChannel's ip.");
        }
        return socketIp.substring(1);
    }

//    public static void main(String[] args) {
//        Server server = new Server(5000);
//        server.start();
//    }
}
