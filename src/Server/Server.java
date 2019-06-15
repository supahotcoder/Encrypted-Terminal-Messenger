package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {

    /**
     * Server
     *
     * Multiple Clients can be simultaneously connected to Server
     *
     * Users can check their messages which are encrypted and stored in (running) Server
     * After shutting down the Server all messages are deleted (they are stored only in memory)
     *
     * Server is processing Client commands
     *
     * All Users information are stored and encrypted in *.txt (in my case it's data.txt) (username,password)
     *
     * WARNING: Database has to be populated with at least 1 user for server to function
     *
     */

    private static List<Message> messages = new ArrayList<>();

    private static UserDatabase database = new UserDatabase("src/Server/data.txt");

    private static boolean serverStop = false;

    private static Socket client;

    public static void main(String args[]) throws IOException {
        //default listening port
        ServerSocket serverSocket = new ServerSocket(4242);

        while (!isServerStopped()) {
            System.out.println("Server listening");
            client = serverSocket.accept();
            System.out.println("Client Connected");
            Thread serverThread = new ServerThread(client);
            serverThread.start();
        }
        System.out.println("Server shuting down");

    }

    private synchronized static void addMsg(String message, String reciever, String sender) {
        Message msg = new Message(sender, reciever, message);
        messages.add(msg);
    }

    private synchronized static void removeMsg(Message msg) {
        messages.remove(msg);
    }

    private static synchronized void setStopServer(){
        serverStop = true;
    }

    private static synchronized boolean isServerStopped(){
        return serverStop;
    }

    private static class ServerThread extends Thread{

        private Socket clientSocket;

        private User currentUser = null;

        public ServerThread(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

                processCommands(rd, wr,clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client Disconnected");
        }

        private void processCommands(BufferedReader rd, BufferedWriter wr, Socket clientSocket) throws IOException {
            while (true) {
                String line = rd.readLine();
                if (line == null) break;

                String[] words = line.split(" ");
                String response = "ERR";
                switch (words[0]) {
                    case "CONNECT":
                        if (words.length == 3)
                            response = connectUser(words[1], words[2]);
                        break;
                    case "READ":
                        response = readMessage(wr);
                        break;
                    case "MSG":
                        if (words[1].equals("FOR") && words.length >= 4)
                            response = sendMessage(words);
                        break;
                    case "LOGOUT":
                        response = logoutUser();
                        msgClient(wr, response);
                        clientSocket.close();
                        return;
                    case "STOP":
                        msgClient(wr, "OK");
                        stopServer();
                        return;
                    default:
                        break;
                }
                msgClient(wr, response);
            }
        }

        private void stopServer() {
            if (currentUser == null) return;
            if (currentUser.getUsername().equals("admin")) {
                setStopServer();
                logoutUser();
                //client.shutdownInput(); // sice by vyhodilu vyjímku, ale ukončil by se server hned
                //takže se server nevypne dokud se poslední klient neodhlásí
            }
        }

        private void msgClient(BufferedWriter wr, String response) throws IOException {
            wr.write(response);
            wr.write("\n");
            wr.flush();
        }

        private String sendMessage(String[] words) {
            String to = words[2];
            String message = parseMessage(words);

            String reciever = to.substring(0, to.length() - 1); //kvůli <username>:
            if (currentUser != null && database.checkUser(reciever)) {

                String sender = currentUser.getUsername();

                addMsg(message, reciever, sender);
                return "OK";
            } else
                return "ERR";
        }

        private String parseMessage(String[] words) {
            String message = Arrays.toString(words);
            int crop = message.indexOf(":");
            message = message.substring(crop + 1, message.length() - 1).replace(",", "");
            return message;
        }


        private String readMessage(BufferedWriter wr) throws IOException {
            if (currentUser == null) return "ERR";
            for (int i = 0; i < messages.size(); i++) {
                var msg = messages.get(i);
                if (msg.getTo().equals(currentUser.getUsername())) {
                    msgClient(wr, msg.prepareRead());
                    removeMsg(msg);
                    i--;// messages.size() je zmenšena o 1
                }
            }
            return "OK";
        }

        private String connectUser(String username, String password) {
            if (database.authUser(username, password)) {
                currentUser = new User(username, password);
                return "OK";
            } else
                return "ERR";
        }

        private String logoutUser() {
            currentUser = null;
            return "OK";
        }
    }

}
