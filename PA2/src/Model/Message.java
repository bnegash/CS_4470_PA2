package Model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private int id;
    private int myPort;
    private String myIp;
    private List<String> routingUpdate = new ArrayList<>();

    public Message(){}
    public Message(int id, String ipAddress, int port) {
        super();
        this.id = id;
        this.myIp = myIp;
        this.myPort = myPort;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMyPort() {
        return myPort;
    }

    public void setMyPort(int myPort) {
        this.myPort = myPort;
    }

    public String getMyIp() {
        return myIp;
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public List<String> getRoutingUpdate() {
        return routingUpdate;
    }

    public void setRoutingUpdate(List<String> routingUpdate) {
        this.routingUpdate = routingUpdate;
    }
}
