// SimpleClient.java
package chat.application;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimpleClient extends JFrame implements ActionListener {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;
    private boolean connected = false;

    // GUI Components
    private JPanel headerPanel;
    private JTextField messageField;
    private JButton sendBtn, backBtn;
    private JLabel nameLabel, statusLabel, profileLabel;
    private Box verticalBox;
    private JScrollPane scrollPane;

    // Constants for better proportions
    private static final int WINDOW_WIDTH = 450;
    private static final int WINDOW_HEIGHT = 700;
    private static final int HEADER_HEIGHT = 70;
    private static final int CHAT_HEIGHT = 530;
    private static final int INPUT_HEIGHT = 60;

    public SimpleClient() {
        // Get username
        username = JOptionPane.showInputDialog(this, "Enter your username:");
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }
        username = username.trim();

        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        setTitle("Simple Chat Client - " + username);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);

        // Header Panel - Top bar
        headerPanel = new JPanel();
        headerPanel.setBackground(new Color(4, 68, 133));
        headerPanel.setBounds(0, 0, WINDOW_WIDTH, HEADER_HEIGHT);
        headerPanel.setLayout(null);
        add(headerPanel);

        // Back button
        backBtn = createIconButton("←", 10, 20);
        backBtn.addActionListener(e -> System.exit(0));
        headerPanel.add(backBtn);

        // Profile icon
        profileLabel = createProfileLabel();
        headerPanel.add(profileLabel);

        // User name
        nameLabel = new JLabel(username);
        nameLabel.setBounds(110, 15, 300, 25);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(nameLabel);

        // Status
        statusLabel = new JLabel("Connecting...");
        statusLabel.setBounds(110, 40, 200, 15);
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        headerPanel.add(statusLabel);

        // Chat Panel with scroll - MINIMAL SPACING
        verticalBox = Box.createVerticalBox();
        verticalBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        verticalBox.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(verticalBox);
        scrollPane.setBounds(0, HEADER_HEIGHT, WINDOW_WIDTH, CHAT_HEIGHT);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Style the scrollbar
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16);

        add(scrollPane);

        // Input Panel - Bottom bar
        // Fixed: Calculate input panel position and height correctly
        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBounds(0, HEADER_HEIGHT + CHAT_HEIGHT, WINDOW_WIDTH, INPUT_HEIGHT);
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        add(inputPanel);

        // Message field
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        messageField.addActionListener(this);
        inputPanel.add(messageField, BorderLayout.CENTER);

        // Send button
        sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("Arial", Font.BOLD, 14));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBackground(new Color(4, 68, 133));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.setMargin(new Insets(5, 15, 5, 15));
        sendBtn.addActionListener(this);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        // Add welcome message with minimal spacing
        SwingUtilities.invokeLater(() -> {
            addSystemMessage("Welcome to the chat!");
        });

        setVisible(true);

        // Focus on message field
        messageField.requestFocus();
    }

    private JButton createIconButton(String text, int x, int y) {
        JButton button = new JButton(text);
        button.setBounds(x, y, 40, 40);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(4, 68, 133));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JLabel createProfileLabel() {
        JLabel label = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw circle
                g2d.setColor(new Color(29, 124, 251));
                g2d.fillOval(0, 0, 40, 40);

                // Draw initial
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                String initial = username.length() > 0 ?
                        username.substring(0, 1).toUpperCase() : "U";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (40 - fm.stringWidth(initial)) / 2;
                int textY = ((40 - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(initial, textX, textY);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40);
            }
        };

        label.setBounds(60, 10, 40, 40);
        label.setOpaque(false);

        return label;
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 8000);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                // Send username
                dos.writeUTF(username);

                String response = dis.readUTF();
                if (response.equals("USERNAME_EXISTS")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Username is already taken!");
                        System.exit(0);
                    });
                    return;
                }

                connected = true;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Online");
                    statusLabel.setForeground(new Color(100, 255, 100));
                    addSystemMessage("Connected to server!");
                });

                // Listen for messages
                while (connected) {
                    try {
                        String msg = dis.readUTF();
                        if (msg.startsWith("MESSAGE:")) {
                            String[] parts = msg.substring(8).split(":", 2);
                            if (parts.length == 2) {
                                final String sender = parts[0];
                                final String message = parts[1];
                                final boolean isMe = sender.equals(username);

                                SwingUtilities.invokeLater(() -> {
                                    addMessageToChat(sender, message, isMe);
                                });
                            }
                        } else if (msg.startsWith("USER_JOINED:")) {
                            final String joinedUser = msg.substring(12);
                            SwingUtilities.invokeLater(() -> {
                                addSystemMessage(joinedUser + " joined the chat");
                            });
                        } else if (msg.startsWith("USER_LEFT:")) {
                            final String leftUser = msg.substring(10);
                            SwingUtilities.invokeLater(() -> {
                                addSystemMessage(leftUser + " left the chat");
                            });
                        }
                    } catch (EOFException e) {
                        break; // Connection closed
                    }
                }

            } catch (ConnectException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Offline");
                    statusLabel.setForeground(Color.RED);
                    addSystemMessage("Cannot connect to server. Make sure server is running!");
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Offline");
                    statusLabel.setForeground(Color.RED);
                    addSystemMessage("Disconnected from server");
                });
            }
        }).start();
    }

    private void addMessageToChat(String sender, String message, boolean isMe) {
        JPanel messageWrapper = createCompactMessageBubble(sender, message, isMe);

        // Add minimal spacing - ONLY 2px between messages
        verticalBox.add(messageWrapper);
        verticalBox.add(Box.createVerticalStrut(2));

        // Update UI
        verticalBox.revalidate();
        verticalBox.repaint();

        // Auto scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            verticalBar.setValue(verticalBar.getMaximum());
        });
    }

    private JPanel createCompactMessageBubble(String sender, String message, boolean isMe) {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        wrapperPanel.setMaximumSize(new Dimension(WINDOW_WIDTH - 20, 1000));

        // Main message panel
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // Only show sender name for others' messages
        if (!isMe) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(new Font("Arial", Font.BOLD, 11));
            senderLabel.setForeground(new Color(80, 80, 80));
            senderLabel.setBorder(new EmptyBorder(0, 0, 1, 0));
            messagePanel.add(senderLabel);
        }

        // Message text panel with bubble
        JPanel textBubblePanel = new JPanel(new BorderLayout());
        textBubblePanel.setBackground(isMe ? new Color(29, 124, 251) : new Color(240, 240, 240));
        textBubblePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isMe ? new Color(20, 100, 220) : new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        textBubblePanel.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));

        // Message text - using JLabel for compactness
        JLabel messageLabel = new JLabel("<html><div style='width: 240px;'>" +
                message.replace("\n", "<br>") + "</div></html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        messageLabel.setForeground(isMe ? Color.WHITE : Color.BLACK);
        textBubblePanel.add(messageLabel, BorderLayout.CENTER);

        // Time label RIGHT BELOW the bubble (not in separate line)
        JPanel timePanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        timePanel.setBackground(Color.WHITE);
        timePanel.setBorder(new EmptyBorder(1, 5, 0, 5));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        JLabel timeLabel = new JLabel(sdf.format(new Date()));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        timeLabel.setForeground(Color.GRAY);
        timePanel.add(timeLabel);

        // Add components
        messagePanel.add(textBubblePanel);
        messagePanel.add(timePanel);

        // Align message
        if (isMe) {
            wrapperPanel.add(messagePanel, BorderLayout.EAST);
        } else {
            wrapperPanel.add(messagePanel, BorderLayout.WEST);
        }

        return wrapperPanel;
    }

    private void addSystemMessage(String message) {
        JLabel systemLabel = new JLabel(message);
        systemLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        systemLabel.setForeground(Color.GRAY);
        systemLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(2, 5, 2, 5));
        panel.add(systemLabel, BorderLayout.CENTER);

        verticalBox.add(panel);
        verticalBox.add(Box.createVerticalStrut(1));

        verticalBox.revalidate();
        verticalBox.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendBtn || e.getSource() == messageField) {
            sendMessage();
        }
    }

    private void sendMessage() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Not connected to server!");
            return;
        }

        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                dos.writeUTF(message);
                addMessageToChat(username, message, true);
                messageField.setText("");
                messageField.requestFocus();
            } catch (IOException e) {
                statusLabel.setText("Offline");
                statusLabel.setForeground(Color.RED);
                addSystemMessage("Failed to send message");
                connected = false;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SimpleClient();
        });
    }
}