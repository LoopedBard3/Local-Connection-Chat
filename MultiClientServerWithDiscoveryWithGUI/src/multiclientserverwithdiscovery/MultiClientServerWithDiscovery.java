/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multiclientserverwithdiscovery;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import static multiclientserverwithdiscovery.MultiClientServerWithDiscovery.oStreams;
import static multiclientserverwithdiscovery.MultiClientServerWithDiscovery.outgoing;
import static multiclientserverwithdiscovery.MultiClientServerWithDiscovery.sendMessage;
import static multiclientserverwithdiscovery.MultiClientServerWithDiscovery.sendReady;

/**
 *
 * @author pbibus
 */
public class MultiClientServerWithDiscovery {

    /**
     * @param args the command line arguments
     */
    public static ArrayList<PrintWriter> oStreams = new ArrayList();
    static ArrayList<String> messageList = new ArrayList();
    public static boolean sendReady = false;
    static JTextArea incoming;
    static JTextField outgoing;
    static JButton sendButton;

    public static void main(String[] args) {
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();
        Socket s = null;
        ServerSocket ss2 = null;
        System.out.println("Server Listening......");
        try {
            ss2 = new ServerSocket(10000); // can also use static final PORT_NUM , when defined
            guiStart();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Server error");

        }

        while (true) {
            try {
                s = ss2.accept();
                System.out.println("connection Established");
                ServerThread st = new ServerThread(s);
                st.start();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Connection Error");

            }
        }
    }

    public static void guiStart() {
        JFrame frame = new JFrame("Really Simple Server");
        JPanel mainPanel = new JPanel();
        incoming = new JTextArea(15, 50);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);
        JScrollPane qScroller = new JScrollPane(incoming);
        qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outgoing = new JTextField(20);
        sendButton = new JButton("Send");
        sendButton.addActionListener(new SendButtonListener());
        mainPanel.add(qScroller);
        mainPanel.add(outgoing);
        mainPanel.add(sendButton);
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(800, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static class SendButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ev) {
            try {
                if (outgoing.getText().compareTo("") != 0) {
                    sendReady = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    public static void addMessage(String message) {
        messageList.add(0, message);
        String totalMess = "";
        for (int i = 0; i < messageList.size() && i < 15; i++) {
            totalMess = messageList.get(i) + "\n" + totalMess;
        }
        incoming.setText(totalMess);
    }

    public static void sendMessage(String message) {
        addMessage(message);
        for (PrintWriter os : oStreams) {
            os.println(message);
            os.flush();
        }
    }
}

class DiscoveryThread implements Runnable {

    DatagramSocket socket;

    @Override
    public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (true) {
                System.out.println(getClass().getName() + ">>>Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                System.out.println(getClass().getName() + ">>>Packet received; data: " + new String(packet.getData()));

                //See if the packet holds the right command (message)
                String message = new String(packet.getData()).trim();
                if (message.equals("DISCOVER_REQUEST")) {
                    byte[] sendData = "DISCOVER_RESPONSE".getBytes();

                    //Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DiscoveryThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static DiscoveryThread getInstance() {
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {

        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }
}

class ServerThread extends Thread {

    String line = null;
    BufferedReader is = null;
    PrintWriter os = null;
    Socket s = null;
    BufferedReader br = null;
    boolean connected = true;

    public ServerThread(Socket s) {
        this.s = s;
    }

    @Override
    public void run() {
        try {
            is = new BufferedReader(new InputStreamReader(s.getInputStream()));
            os = new PrintWriter(s.getOutputStream());
            br = new BufferedReader(new InputStreamReader(System.in));
            oStreams.add(os);

        } catch (IOException e) {
            System.out.println("IO error in server thread");
        }

        try {
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        is.read();
                    } catch (IOException ex) {
                        connected = false;
                    }
                }
            }, 3000);
            line = "";
            String str = "";
            while (line.compareTo("QUIT") != 0 && connected) {
                if (is.ready()) {
                    line = is.readLine();
                    sendMessage(line);
                    /*os.println(line);
                    os.flush();*/
                    System.out.println("Response to Client  :  " + line);
                }
                if (br.ready()) {
                    str = br.readLine();
                    sendMessage(str);
                    /*os.println(line);
                    os.flush();*/
                    System.out.println("Server to Client: " + str);
                }
                if(sendReady){
                  str = outgoing.getText();
                  sendMessage(str);
                  sendReady = false;
                  outgoing.setText("");
                }
                /*os.println(line);
                os.flush();
                System.out.println("Response to Client  :  " + line);
                line = is.readLine();*/
            }
        } catch (IOException e) {

            line = this.getName(); //reused String line for getting thread name
            System.out.println("IO Error/ Client " + line + " terminated abruptly");
        } catch (NullPointerException e) {
            line = this.getName(); //reused String line for getting thread name
            System.out.println("Client " + line + " Closed");
        } finally {
            try {
                System.out.println("Connection Closing..");
                if (is != null) {
                    is.close();
                    System.out.println(" Socket Input Stream Closed");
                }

                if (os != null) {
                    os.close();
                    System.out.println("Socket Out Closed");
                }
                if (s != null) {
                    s.close();
                    System.out.println("Socket Closed");
                }

            } catch (IOException ie) {
                System.out.println("Socket Close Error");
            }
        }//end finally
    }
}
