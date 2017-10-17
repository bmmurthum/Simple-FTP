import java.net.*;
import java.util.*;
import java.io.*;

/***********************************************************************
 *
 * An FTP Simulation.
 *
 * This program along with its other components (both server and client) 
 * represents a barebones FTP solution to host a server with files and
 * allow for a connecting client to (1) request a list of the contained
 * files, (2) to store a file from the client onto the server, (3) to
 * retrieve a file from the server to the client, and then to later (4)
 * send a command from the client to the server to disconnect the 
 * current user. A few components of this program are (A) to exercise
 * multithreading on the server to handle multiple clients 
 * simultaneously, as well as (B) to exercise initializing TCP 
 * connections, and (C), to exercise using various data-streams.
 * 
 * @author -        Brendon Murthum
 *                  Javier Ramirez-Moyano
 *                  Matthew Schuck
 *                  Louis Zullivan
 * 
 * @version -       October 16, 2017
 *
 * Part of GVSU Computer Science Program
 *
 * Latest Updates - Trying to work out the control-stream sending.
 *
 **********************************************************************/

public class ftpServer{
    /* This is the port that the server to listens for connections. */
    private static final int welcomePort = 1078;
    private static ServerSocket welcomeSocket;
    
    public static void main(String[] args) throws IOException {
        System.out.println("\nServer initialized. Waiting for " +
                                "connections.");
        try {
            welcomeSocket = new ServerSocket(welcomePort);
        }
        catch (IOException ioEx) {
            System.out.println("\nERROR: Unable to set up port!");
            System.exit(1);
        }

        do {
            /* Wait for client... */
            Socket connectionSocket = welcomeSocket.accept();

            System.out.println("\nNew Client Connected to Server.");
            System.out.println("IP: " + 
                connectionSocket.getInetAddress());

            /* Create a thread to handle communication with  */
            /* this client and pass the constructor for this */
            /* thread a reference to the relevant socket...  */
            ClientHandler handler = new ClientHandler(connectionSocket);
            handler.start();
        
        } while (true);
    }
}

/***********************************************************************
 *
 * This class handles allows for threading and handling the various 
 * clients that are connected to server simultaneously.
 *
 **********************************************************************/
class ClientHandler extends Thread {
    
    /** This sets the packet size to be sent across to this size */
    private static final int BUFSIZE = 32768;
    
    /** Port for the commands to be sent across */
    private static final int controlPort = 1078;
    
    /** This socket takes commands from client */
    private Socket controlListen;
    
    /** This socket takes data from client */
    private Socket dataSocket;
    
    /** This handles the stream from the command-line of client */
    private Scanner inFromClient;
    
    /** This handles the output stream by command-line to client */
    private PrintWriter outToClient;
    
    /** This is used for handling the buffering of files over /
        the data stream */
    private int recvMsgSize;
    
    /** This is used to grab bytes over the data-line */
    private String recvMsg;
    
    /** This allows for identification of specific users in streams */
    private String remoteIP;
    
    /*******************************************************************
     *
     * Beginning of thread.
     * This constructor marks the beginning of a thread on the server.
     * Things here happen once, exclusively with THIS connected client.
     *
     ******************************************************************/
    public ClientHandler(Socket controlListen) {
        try {
            /* Setting up a threaded input control-stream */
            inFromClient = new Scanner (
                controlListen.getInputStream());
            /* Setting up a threaded output control-stream */
            outToClient = 
                new PrintWriter(controlListen.getOutputStream());
            /* For error handling */
            /* Get IP from the control socket for future connections */
            remoteIP = controlListen.getInetAddress().getHostAddress();
            System.out.println("A new thread was successfully setup.");
            System.out.println("");
        } catch(IOException ioEx) {
            ioEx.printStackTrace();
            System.out.println("ERROR: Could not set up a " +
                        "threaded client input and output stream.");
        }
    }
     
    /******************************************************************
     *
     * Beginning of main thread code.
     * This method marks the threaded area that this client receives
     * commands and handles. When this receives "QUIT" from the client,
     * the thread closes.
     * 
     ******************************************************************/
    public void run(){   
        /* For sending and retrieving file */
        int BUFSIZE = 32768;
        byte[] byteBuffer = new byte[BUFSIZE];
    
        Socket dataConnection;
        boolean stayAlive = true;

        while (stayAlive) {
            try {
                recvMsg = inFromClient.nextLine();
            } catch (Exception e) {
                /* Client left early, or otherwise */
                System.out.println("");
                System.out.println("ERROR: Client left early");
                break;
            }

            /* line = "1079 LIST" */
            String returnedString = "";
            String currentToken;
            StringTokenizer tokens = new StringTokenizer(recvMsg);

            while (tokens.hasMoreTokens()) {
                /* Client opened dataport, "1079," for the server to
                   connect to */
                String clientDataPort = tokens.nextToken();
                /* Client command, "LIST," or another. */
                String commandFromClient = tokens.nextToken();
                
                /* Other arguments sent across the wire will depend
                   on the command sent. */
                   
               /*******************************************************
                *
                * LINK Command Handler.
                * When the server receives a LIST command, it 
                * establishes a link with the data port, sets up the I/O
                * streams, then retrieves the contents of its directory,
                * before sending it back to the client in a formatted 
                * string.
                *
                *******************************************************/
                if (commandFromClient.equals("LIST")) {
                    /* For server debugging */
                    System.out.println("Function LIST started.");
                    
                    try {
                        // Data connection socket
                        dataConnection = new Socket(remoteIP,
                            Integer.parseInt(clientDataPort));
                    } catch (Exception e) {
                        /* For server debugging */
                        System.out.println("ERROR: Data connection " +
                            "failure.");
                        continue;
                    }

                    /* Setting up Data I/O Stream */
                    InputStream inFromClient_Data;
                    OutputStream outToClient_Data;
                    try {
                       // Initiate data Input/Output streams
                       inFromClient_Data = 
                            dataConnection.getInputStream();
                       outToClient_Data = 
                            dataConnection.getOutputStream();
                       System.out.println("Data line started.");
                    } catch (Exception e) {
                        System.out.println("ERROR: Input/out stream " +
                                           "failure.");
                        continue;
                    }
                    
                    /* Preparing the string list of files */
                    File folder = 
                        new File(System.getProperty("user.dir"));
                    File[] listOfFiles = folder.listFiles();
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile()) {
                            returnedString += "- " + 
                            listOfFiles[i].getName() + "\n";
                        }
                    }
                    returnedString = returnedString + "\n";
                    
                    /* Sending that string over the line */
                    try {
                        outToClient_Data.write(
                                             returnedString.getBytes());
                        outToClient_Data.flush();
                        System.out.println("Writing data.");
                    } catch (Exception e) {
                        System.out.println("ERROR: Input/out stream " +
                                           "failure.");
                        continue;
                    }
                    
                    /* Close after the data is written to client */
                    try {
                        
                        dataConnection.close();
                        System.out.println("Data connect closed " + 
                            "successfully");
                    } catch (Exception e) {
                        System.out.println("ERROR: Closing data" +
                                           "-connection failure.");
                        continue;
                    }
                    
               /*******************************************************
                *
                * RETR Command Handler.
                * When the server receives a RETR command, it 
                * establishes a link with the data port, sets up the 
                * I/O streams, then retrieves the file the client asked 
                * for, converts it into bytes, then outputs it back to 
                * the client to be converted back into a file.
                *
                *******************************************************/
                } else if (commandFromClient.equals("RETR")) {
                    /* For server debugging */
                    System.out.println("Function RETR started.");

                    /* Get the file name */
                    String fileName = tokens.nextToken();

                    /* Setting up Data I/O Stream */
                    try {
                        // Data connection socket
                        dataConnection = new Socket(remoteIP,
                                Integer.parseInt(clientDataPort));
                    } catch (Exception e) {
                        /* For server debugging */
                        System.out.println("ERROR: Data connection " +
                                "failure.");
                        continue;
                    }
                    InputStream inFromClient_Data;
                    OutputStream outToClient_Data;
                    try {
                        // Initiate data Input/Output streams
                        inFromClient_Data = 
                            dataConnection.getInputStream();
                        outToClient_Data = 
                            dataConnection.getOutputStream();
                        System.out.println("Data line started.");
                    } catch (Exception e) {
                        System.out.println("ERROR: Input/out stream " +
                                "failure.");
                        continue;
                    }

                    /* If file exists, then write file. 
                        In not, write error message and break. */
                    File myFile = new File(fileName);
                    if (myFile.exists()) {
                        try {
                            /* Declare variables for converting 
                                file to byte[] */
                            FileInputStream fileInputStream = 
                                new FileInputStream(myFile);
                            byte[] fileByteArray = 
                                new byte[(int) myFile.length()];
                            // Grabs file to memory to then be passed
                            fileInputStream.read(fileByteArray);
                            fileInputStream.close();

                            // Write to client over DATA line
                            outToClient_Data.write(fileByteArray);
                            outToClient_Data.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    
                    /* Write error message over data-line */
                    } else {
                        String errMsg = new String("File does " +
                            "not exist.");
                        
                        try {
                            outToClient_Data.write(errMsg.getBytes());
                            System.out.println("Writing data.");
                        } catch (Exception e) {
                            System.out.println("ERROR: Input/out " +
                                        "stream failure.");
                            continue;
                        }
                        
                        try {
                            outToClient_Data.flush();
                        } catch (Exception e) {
                            System.out.println("ERROR: Input/out " +
                                        "flush failure.");
                            continue;
                        }
                    }
    
                    /* Close data-link after done */
                    try {
                        
                        dataConnection.close();
                    } catch (Exception e) {
                        System.out.println("ERROR: Closing data" +
                                        "-connection failure.");
                        continue;
                    }
                    
               /*******************************************************
                *
                * STOR Command Handler.
                * When the server receives a STOR command, it 
                * establishes a link with the data port, sets up the I/O
                * streams, then receives an incoming file sent by the 
                * client as a series of bytes, then writes the bytes to 
                * a file, creating the file in the server's directory.
                *
                *******************************************************/
                } else if (commandFromClient.equals("STOR")) {
                    /* For server debugging */
                    System.out.println("Function STOR started.");

                    /* Get the file name */
                    String fileName = tokens.nextToken();
                    
                    /* Openning temp data connection socket */
                    try {
                        dataConnection = new Socket(remoteIP,
                            Integer.parseInt(clientDataPort));
                    } catch (Exception e) {
                        System.out.println("ERROR: Openning data" +
                            "-connection failure.");
                        continue;
                    }
                    
                    /* Initiate data Input/Output streams */
                    InputStream inFromClient_Data;
                    try {
                        inFromClient_Data = 
                            dataConnection.getInputStream();
                    } catch (Exception e) {
                        System.out.println("ERROR: Openning data" +
                            "-connection failure.");
                        continue;
                    }
                    
                    /* Initiate data Input/Output streams */
                    OutputStream outToClient_Data;
                    try {
                        outToClient_Data = 
                            dataConnection.getOutputStream();
                    } catch (Exception e) {
                        System.out.println("ERROR: Openning data" +
                            "-connection failure.");
                        continue;
                    }
                    
                    /* For server debugging */
                    System.out.println("Data line started.");
                    
                    try {
                        String fileRequestedString = 
                            new String(fileName.getBytes());
                        while ((recvMsgSize = 
                            inFromClient_Data.read(byteBuffer)) != -1){
                            try {
                                File fileRequested = 
                                    new File(fileRequestedString);
                                FileOutputStream fileOutputStream = 
                                    new FileOutputStream(fileRequested);

                                fileOutputStream.write(byteBuffer, 0, 
                                    recvMsgSize);
                                fileOutputStream.close();
                                
                                Scanner scanner = 
                                    new Scanner(fileRequested);
                                String errorCheck = scanner.nextLine();
                                if(errorCheck.equals("File does " + 
                                    "not exist.")) { 
                                    System.out.println("File does " + 
                                        "not exist.");
                                    fileRequested.delete();
                                }else {
                                    System.out.println("File " + 
                                        "retrieved!");
                                }
                            } catch (Exception e) {
                                System.out.println("Error trying to " +
                                    "retrieve file.");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR: Transferring data" +
                                        " failure.");
                        continue;
                    }        
                
               /*******************************************************
                *
                * QUIT Command Handler.
                * When the server receives a QUIT command, it changes 
                * the boolean variable "stayAlive" to false and breaks 
                * out of the loop, resulting in the while loop 
                * responsible for maintaining a connection to the client
                * being terminated.
                *
                *******************************************************/
                } else if (commandFromClient.equals("QUIT")) {
                    System.out.println("Function QUIT started.");
                    
                    stayAlive = false;
                    break;
                
               /*******************************************************
                *
                * Invalid Command Handler.
                * The client normally filters out all invalid commands 
                * from ever reaching the server. As such, this should 
                * never becalled in standard operation. If it is, then 
                * someone has manipulated the client to send unwelcome 
                * data; thus, the connection auto-terminates in 
                * self-defense.
                *
                *******************************************************/
                } else {
                    /* An invalid command-string was sent across wire */
                    System.out.println("ERROR: Unexpected data sent " +
                        "by client. Terminating connection.");
                    stayAlive = false;
                    break;
                }
                
                /* After every command */
                System.out.println("Done processing command.");
                System.out.println("");
            
            /* End of the while loop for handling data-stream data */
            }
        
        /* End of the other while loop */
        }
    
    /* End of threading run() */  
    }
    
/* End of the thread class */  
}
