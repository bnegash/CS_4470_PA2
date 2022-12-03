package Model;

import java.io.Serial;
import java.io.Serializable;

public class NodeServer {

    private int id;
    private String ipAddress;
    private int port;



    public NodeServer(int id, String ipAddress, int port){
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        NodeServer that = (NodeServer) o;
        if(id != that.id){
            return false;
        }
        if(ipAddress == null){
            if(that.ipAddress != null){
                return false;
            }
        }else if(!ipAddress.equals(that.ipAddress)){
            return false;
        }
        return port == that.port;
    }

//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int res = 1;
//        res = prime * res + id;
//        res = prime * res + ((ipAddress == null) ? 0 : ipAddress.hashCode());
//        res = prime * res + port;
//        return res;
//    }
}
