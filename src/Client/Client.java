package Client;

import java.io.*;
import java.net.Socket;

public class Client {

    /**
     * Client
     *
     * Client can connect to server and exchange messages between other Clients
     *
     * Communication with the Server is done by using commands which are shown bellow:
     *
     * Connecting to server: CONNECT <username> <password>
     *
     * Sending message: MSG FOR <username>: <message>
     *
     * Reading messages: READ
     *
     * Logging out: LOGOUT
     *
     * If Client username is an admin, then the client can stop the server remotely using cmd: STOP
     */

    private static Socket clientSocket = null;

    public static void main(String args[]) {
        try {
            //default connection
            clientSocket = new Socket("localhost", 4242);

            BufferedReader rd = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            System.out.println("Connected");

            sendCommands(rd, wr);

            System.out.println("Disconnected");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendCommands(BufferedReader rd, BufferedWriter wr) throws IOException {
        while (!clientSocket.isClosed()) {

            BufferedReader clientInput = new BufferedReader(new InputStreamReader(System.in));
            String cmd = clientInput.readLine();

            msgServer(wr, cmd);
            String serverResponse;
            do {
                serverResponse = rd.readLine();
                System.out.println(serverResponse);
            } while (!(serverResponse.equals("OK") || serverResponse.equals("ERR")));

            if (cmd.equals("LOGOUT") || cmd.equals("STOP")) break;
        }
    }

    private static void msgServer(BufferedWriter wr, String cmd) throws IOException {
        wr.write(cmd);
        wr.write("\r\n");
        wr.flush();
    }
}
