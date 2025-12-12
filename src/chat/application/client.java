package chat.application;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.io.*;


class client implements ActionListener {

    static DataOutputStream dout;
    JTextField text1 = new JTextField();
    static JPanel p1 = new JPanel();
    static Box vertical = Box.createVerticalBox();
    static JFrame frame = new JFrame();

    client(){

        frame.setLayout(null);

        JPanel p = new JPanel();
        p.setBackground(new Color(4, 68, 133));
        p.setBounds(0,0 ,450,70);
        frame.add(p);

        ImageIcon i = new ImageIcon(ClassLoader.getSystemResource("icon/3.png"));
        Image i2 = i.getImage().getScaledInstance(25,25,Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel back = new JLabel(i3);
        back.setBounds(5,20,25,25);
        p.setLayout(null);
        p.add(back);
        back.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });

        ImageIcon i4 = new ImageIcon(ClassLoader.getSystemResource("icon/2img.jpg"));
        Image i5 = i4.getImage().getScaledInstance(50,50,Image.SCALE_DEFAULT);
        ImageIcon i6 = new ImageIcon(i5);
        JLabel profile = new JLabel(i6);
        profile.setBounds(50,10,50,50);
        p.setLayout(null);
        p.add(profile);

        JLabel name = new JLabel("Client-2");
        name.setBounds(120,20,100,20);
        name.setForeground(Color.white);
        name.setFont(new Font("Times New Roman",Font.BOLD,25));
        p.add(name);

        JLabel status = new JLabel("Active Now");
        status.setBounds(120,50,100,10);
        status.setForeground(Color.white);
        status.setFont(new Font("Times New Roman",Font.BOLD,14));
        p.add(status);


        p1.setBounds(5,75,440,570);
        frame.add(p1);


        text1.setBounds(5,655,310,40);
        text1.setFont(new Font("SAN_SERIF",Font.PLAIN,20));
        frame.add(text1);

        Button send = new Button("Send");
        send.setBounds(320,655,123,40);
        send.setFont(new Font("SAN_SERIF",Font.PLAIN,20));
        send.setForeground(Color.white);
        send.addActionListener(this);
        send.setBackground(new Color(4, 68, 133));
        frame.add(send);

        frame.setSize(450, 700);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLocation(800,50);
    }
    public void actionPerformed(ActionEvent e) {
        try {
            String out = text1.getText();

            JPanel p2 = formatLable(out);

            p1.setLayout(new BorderLayout());

            JPanel right = new JPanel(new BorderLayout());
            right.add(p2, BorderLayout.LINE_END);
            vertical.add(right);
            vertical.add(Box.createVerticalStrut(15));

            p1.add(vertical, BorderLayout.PAGE_START);

            dout.writeUTF(out);

            text1.setText("");

            frame.repaint();
            frame.invalidate();
            frame.validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static JPanel formatLable(String out){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel output = new JLabel("<html><p style=\"width: 150px\">" + out + "</p></html>");
        output.setFont(new Font("Tahoma",Font.PLAIN,16));
        output.setBackground(new Color(29, 124, 251));
        output.setOpaque(true);
        output.setBorder(new EmptyBorder(15,15,15,50));

        panel.add(output);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        JLabel time = new JLabel();
        time.setText(sdf.format(cal.getTime()));
        panel.add(time);

        return panel;
    }

    public static void main(String[] args) {
        client c= new client();
        try{
            Socket s = new Socket("127.0.0.1",8000);
            DataInputStream din = new DataInputStream(s.getInputStream());
            dout = new DataOutputStream(s.getOutputStream());

            while(true){
                p1.setLayout(new BorderLayout());
                String str = din.readUTF();
                JPanel panel = formatLable(str);

                JPanel left = new JPanel(new BorderLayout());
                left.add(panel, BorderLayout.LINE_START);
                vertical.add(left);

                vertical.add(Box.createVerticalStrut(15));
                p1.add(vertical, BorderLayout.PAGE_START);

                frame.validate();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}