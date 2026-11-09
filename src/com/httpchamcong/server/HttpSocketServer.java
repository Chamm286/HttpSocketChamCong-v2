package com.httpchamcong.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP Server — có thể start/stop, không blocking main.
 * UI gọi start() / stop().
 */
public class HttpSocketServer {

    public static final int PORT = 2020;
    public static final int POOL_SIZE = 20;

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;
    private Thread acceptThread;
    private final Router router = new Router();

    /** Callback để UI cập nhật log */
    public interface LogListener {
        void onLog(String message);
    }

    private LogListener logListener;

    public void setLogListener(LogListener listener) {
        this.logListener = listener;
    }

    private void log(String msg) {
        System.out.println(msg);
        if (logListener != null) logListener.onLog(msg);
    }

    /** Khởi động server — chạy accept loop trong thread riêng */
    public void start() throws IOException {
        if (running) return;

        // Lắng nghe trên mọi interface để hỗ trợ LAN (IPv4/IPv6).
        // Thử IPv6 wildcard (::) trước, fallback IPv4 wildcard (0.0.0.0).
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new java.net.InetSocketAddress(InetAddress.getByName("::"), PORT), 50);
        } catch (Exception e) {
            serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
        }
        pool = Executors.newFixedThreadPool(POOL_SIZE);
        running = true;
        ServerStats.serverStarted();

        acceptThread = new Thread(this::acceptLoop, "HttpServer-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        log("[SERVER] Started on port " + PORT + " with " + POOL_SIZE + " threads");
        logNetworkAddresses();
    }

    private void logNetworkAddresses() {
        log("[NETWORK] Listening on all interfaces (IPv4/IPv6). Reachable addresses:");
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    log("  " + ni.getDisplayName() + " -> " + addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            log("[NETWORK] Cannot enumerate interfaces: " + e.getMessage());
        }
        log("  Localhost: 127.0.0.1 / ::1");
        log("[WEB] http://localhost:" + PORT + "/  (web root: " + StaticFileHandler.describeRoot() + ")");
    }

    /** Vòng lặp chấp nhận client */
    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                ServerStats.clientConnected();
                pool.execute(new ClientHandler(client, router, this));
            } catch (IOException e) {
                if (running) log("[ERROR] Accept: " + e.getMessage());
            }
        }
    }

    /** Dừng server */
    public void stop() {
        if (!running) return;
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }

        if (pool != null) pool.shutdownNow();

        log("[SERVER] Stopped");
    }

    public boolean isRunning() { return running; }

    /** Log từ bên ngoài (ClientHandler gọi) */
    public void logExternal(String msg) { log(msg); }

    /**
     * Chạy server không giao diện (headless) — dùng để test hoặc chạy trên máy chủ.
     * Chạy: java com.httpchamcong.server.HttpSocketServer
     */
    public static void main(String[] args) throws Exception {
        HttpSocketServer server = new HttpSocketServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "HttpServer-Shutdown"));

        System.out.println("[SERVER] Headless mode — nhan Ctrl+C de dung.");
        new java.util.concurrent.CountDownLatch(1).await();
    }
}