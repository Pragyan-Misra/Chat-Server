// MultiClientServer.java
package chat.application;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MultiClientServer extends JFrame {
    private static final int PORT = 8000;
    private static final int MAX_CLIENTS = 50;
    private static ServerSocket serverSocket;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> onlineUsers = new CopyOnWriteArrayList<>();

    // UI Components
    private JTextArea logArea;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton startBtn, stopBtn;
    private boolean isRunning = false;

    public MultiClientServer() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat Server - Admin Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 242, 245));

        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(59, 89, 152));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("💬 Multi-Chat Server");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        startBtn = createStyledButton("▶ Start Server", new Color(46, 204, 113));
        stopBtn = createStyledButton("⏹ Stop Server", new Color(231, 76, 60));
        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());

        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);

        // Left Panel - Online Users
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("🟢 Online Users"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.PLAIN, 14));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(userScroll, BorderLayout.CENTER);

        JLabel userCountLabel = new JLabel("Users: 0");
        userCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        leftPanel.add(userCountLabel, BorderLayout.SOUTH);

        // Right Panel - Server Logs
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("📊 Server Logs"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(220, 220, 220));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(logScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(statusLabel);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(900, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void startServer() {
        if (isRunning) return;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                logMessage("✅ Server started on port " + PORT);

                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                if (isRunning) {
                    logMessage("❌ Server error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void stopServer() {
        isRunning = false;
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Disconnect all clients
            for (ClientHandler client : clients.values()) {
                client.disconnect();
            }
            clients.clear();
            onlineUsers.clear();
            userListModel.clear();

            logMessage("🛑 Server stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : onlineUsers) {
                userListModel.addElement("👤 " + user);
            }
        });
    }

    // ClientHandler class
    class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                // Get username
                username = dis.readUTF();

                // Check if username already exists
                if (clients.containsKey(username)) {
                    dos.writeUTF("USERNAME_EXISTS");
                    disconnect();
                    return;
                }

                dos.writeUTF("WELCOME");

                // Add to clients list
                clients.put(username, this);
                onlineUsers.add(username);
                updateUserList();

                logMessage("👤 " + username + " connected");

                // Send welcome message to all
                broadcastSystemMessage("🌟 " + username + " joined the chat!");

                // Send online users list
                broadcastOnlineUsers();

                // Handle messages
                while (isRunning) {
                    String message = dis.readUTF();

                    if (message.equalsIgnoreCase("/exit")) {
                        break;
                    }

                    // Broadcast message
                    String formattedMessage = username + ":" + message;
                    broadcast(formattedMessage, username);
                    logMessage("💬 " + username + ": " + message);
                }

            } catch (IOException e) {
                // Connection closed
            } finally {
                disconnectClient();
            }
        }

        private void broadcast(String message, String sender) throws IOException {
            for (ClientHandler client : clients.values()) {
                if (!client.username.equals(sender)) {
                    client.dos.writeUTF("MESSAGE:" + message);
                }
            }
        }

        private void broadcastSystemMessage(String message) throws IOException {
            for (ClientHandler client : clients.values()) {
                client.dos.writeUTF("SYSTEM:" + message);
            }
        }

        private void broadcastOnlineUsers() throws IOException {
            StringBuilder users = new StringBuilder("ONLINE_USERS:");
            for (String user : onlineUsers) {
                users.append(user).append(",");
            }

            for (ClientHandler client : clients.values()) {
                client.dos.writeUTF(users.toString());
            }
        }

        private void disconnectClient() {
            if (username != null) {
                clients.remove(username);
                onlineUsers.remove(username);
                updateUserList();
                logMessage("👋 " + username + " disconnected");

                try {
                    broadcastSystemMessage("👋 " + username + " left the chat");
                    broadcastOnlineUsers();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void disconnect() {
            try {
                dos.writeUTF("SERVER_STOPPED");
                disconnectClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MultiClientServer::new);
    }
}