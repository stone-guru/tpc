package net.eric.tpc.base;

import java.io.Serializable;

public class Node implements Serializable {

    private static final long serialVersionUID = -196279432426379129L;

    private String address;
    private int port;
    private String alias;

    public Node() {
    }

    public Node(String address, int port) {
        super();
        this.address = address;
        this.port = port;
    }

    public Node(String address, int port, String alias) {
        this(address, port);
        this.alias = alias;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (port != other.port)
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (alias == null) {
            return address + ":" + port;
        }
        return alias;
    }
}
