package utils;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author alberto delgado
 * @author anchit bhatia
 * <p>
 * Node:
 * - Can host a server
 * - Has hostname, ip, port
 */
public class Node {
    public final String hostname;
    public final String ip;
    public final int port;
    protected ServerSocket serverSocket;
    private Thread serverThread;
    protected boolean isServerRunning;

    public Node(String hostname, String ip, int port) {
        this.hostname = hostname;
        this.ip = ip;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    protected void initializeServer(Runnable serverObj) throws IOException {
        this.serverSocket = new ServerSocket(this.port);
        this.isServerRunning = false;
        this.serverThread = new Thread(serverObj, "Server");
    }

    public void startServer() {
        this.isServerRunning = true;
        this.serverThread.start();
    }

    public void stopServer() {
        this.isServerRunning = false;
    }
}
