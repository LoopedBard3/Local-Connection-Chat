/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientconnectwithdiscovery;

/**
 *
 * @author pbibus
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pbibus
 */
public class ClientCode {

    // Find the server using UDP broadcast
    public static void main(String[] args) throws IOException {
        ClientCode cc = new ClientCode();
        String ip = cc.findConnection();
        if (!ip.equals("Unable to connect")) {
            connect(ip);// TO-DO add ip parameter to connect
        } else {
            System.out.println("Please try again later!");
        }
    }

    public String findConnection() {
        DatagramSocket c;
        try {
            //Open a random port to send the package
            c = new DatagramSocket();
            c.setBroadcast(true);

            byte[] sendData = "DISCOVER_REQUEST".getBytes();

            //Try the 255.255.255.255 first
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                c.send(sendPacket);
                System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
            }

            // Broadcast the message over all the network interfaces
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Send the broadcast package!
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception e) {
                    }

                    System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            //Wait for a response
            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            //We have a response
            System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals("DISCOVER_RESPONSE")) {
                //Controller_Base.setServerIp(receivePacket.getAddress());
            }

            //Close the port!
            c.close();
            return receivePacket.getAddress().getHostAddress();
        } catch (IOException ex) {
            Logger.getLogger("wait");//LoginWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Unable to connect";
    }

    public static void connect(String targetIP) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        Socket s1 = null;
        String line = null;
        BufferedReader br = null;
        BufferedReader is = null;
        PrintWriter os = null;
        Runtime rt = Runtime.getRuntime();
        System.out.println(targetIP + " is the target IP");
        try {
            s1 = new Socket(targetIP, 10000); // You can use static final constant PORT_NUM
            br = new BufferedReader(new InputStreamReader(System.in));
            is = new BufferedReader(new InputStreamReader(s1.getInputStream()));
            os = new PrintWriter(s1.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.print("IO Exception");
        }

        System.out.println("Client Address : " + address);
        System.out.println("Enter Data to echo Server ( Enter QUIT to end):");

        os.println("");
        os.flush();
        try {
            String str = "";
            line = "";
            os.println("connected");
            os.flush();
            while (line.compareTo("QUIT") != 0) {
                if (is.ready()) {
                    System.out.println("i am here");
                    line = is.readLine();
                    if (line.compareTo("kill") == 0) {
                        Process proc = rt.exec("shutdown -s -f -t 15 -c \" Shutting Down \"");
                        line = "QUIT";
                    }
                    if (line.compareTo("logoff") == 0) {
                        Process proc = rt.exec("shutdown -l -f -t 15 -c \" Logging Off \"");
                        line = "QUIT";
                    }

                    System.out.println("Server Response : " + line);
                }
                if (br.ready()) {
                    str = br.readLine();
                    os.println(str);
                    os.flush();
                }
                /*os.println(line);
                os.flush();
                response = is.readLine();
                System.out.println("Server Response : " + response);
                line = br.readLine();*/

            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Socket read Error");
        } finally {
            is.close();
            os.close();
            br.close();
            s1.close();
            System.out.println("Connection Closed");

        }
    }
}
