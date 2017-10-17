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

public class ftpClient {
    static Socket server;
    private static final int BUFSIZE = 32768;

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
    
        /* For sending and retrieving file */
        byte[] byteBuffer = new byte[BUFSIZE];
        
        /* For reading text from the user */
        Scanner input = new Scanner(System.in);
        
        /* String ip_address = "127.0.0.1", controlPort = "1078" */
        int dataPort = 1079;
        
        /* This socket is the "Welcome socket" for the data-link */
        ServerSocket dataListen; 
        
        /* This socket handles data-links with the server */
        Socket dataConnection = null;
        
        /* This socket handles communicating main commands to server */
        Socket controlSocket;
        
        /* QUIT controls the loop that listens for new command */
        boolean quit = false;
        
        /* Checks for the user being already connected */
        boolean isConnected = false;
        
        /* This value handles the file transfer */
        int recvMsgSize; 

        /* This value holds the single string of the command */
        String userCommand;
        
        /* This handles the control-line out stream */
        PrintWriter outToServer_Control = null;
        
        /* This handles the control-line in stream */
        Scanner inFromServer_Control = null;

        /* Initial text shown to the user */
        System.out.print("\n");
        System.out.println("What would you like to do?");

        while (!quit) {
            /* Menu sent to the user before every command */
            System.out.println("");
            System.out.println("Valid commands:"); 
            System.out.println("CONNECT <servername> <port>");
            System.out.println("LIST");
            System.out.println("STOR <filename>");
            System.out.println("RETR <filename>");
            System.out.println("QUIT\n");
            System.out.print("Input: ");
            
            /* Take user command */
            userCommand = input.nextLine();
            System.out.println(" ");
			
            if (userCommand.equals("")){
                System.out.println("ERROR: No command entered.");
                continue;
            }
            
            /* Break the user command to tokens */
            String currentToken;
            StringTokenizer tokens = new StringTokenizer(userCommand);
            currentToken = tokens.nextToken();
            String Command = currentToken;
            Command = Command.toUpperCase();

            /* Connect to Server Command */
            if (Command.equals("CONNECT") ){
                if (isConnected){
                    System.out.println("ERROR: Already connected. " 
			+ "Please quit and try again.");
                    continue;
                }
                
                String serverIP = "";
                String controlPort = "";
                
                /* Check arguments */
                try {
                    serverIP = tokens.nextToken();
                    controlPort = tokens.nextToken();
                }catch (Exception q){
                    System.out.println("ERROR: Improper or invalid " +
                        "arguments!");
                    continue;
                }
                
                /* Connect to server's welcome socket */
                try {
                    controlSocket = new Socket(serverIP, 
                                         Integer.parseInt(controlPort));
                    boolean controlSocketOpen = true;
                }catch(Exception p){
                    System.out.println("ERROR: Did not find socket!");
                    continue;
                }
                                    
                // Set-up the control-stream,
                // if there's an error, report the non-connection.
                try {
                    inFromServer_Control = 
                       new Scanner(controlSocket.getInputStream());
                    outToServer_Control = 
                       new PrintWriter(controlSocket.getOutputStream());
                    isConnected = true;
                    System.out.println("Connected to server!");
                }
                catch (Exception e) {
                    System.out.println("ERROR: Did not connect to " +
                        "server!");
                    isConnected = false;
                    continue;
                }
                
            /* Quit Command */
            } else if (Command.equals("QUIT") && isConnected == true) {
            
                /* Tells the server that this client wants disconnect */
                String toSend = dataPort + " " + "QUIT";
                outToServer_Control.println(toSend);
                outToServer_Control.flush();
                /* Tells the client to stop itself */
                quit = true;            
                
            /* Quit Command */
            } else if (Command.equals("QUIT") && isConnected == false) {
            
                /* Tells the client to stop itself */
                quit = true; 
                
            /* Store a File Command */
            } else if (Command.contains("STOR") 
                        && isConnected == true) {
                
                String fileToStore;
                try {
                    fileToStore = tokens.nextToken();
                } catch (Exception e) {
                   System.out.println("ERROR: Did not give arguement " +
                       "to STOR.");
                   continue;
                }
                
                String toSend = dataPort + " " + "STOR" + " " + 
                                            fileToStore;
                File myFile = new File(fileToStore);
                /* If file exists, write file. */
                /* Otherwise write error message */
                if (myFile.exists()) {
                    try {
                        /* Write the inital start command to server */
                        outToServer_Control.println(toSend);
                        outToServer_Control.flush();
                    
                        // Connect to server and establish variables.
                        dataListen = new ServerSocket(dataPort);
                        /* Waits here for server's "connection-ACK" */
                        dataConnection = dataListen.accept();
                        /* Setup the data i/o streams */
                        InputStream inFromServer_Data = 
                            dataConnection.getInputStream();
                        OutputStream outToServer_Data = 
                            dataConnection.getOutputStream();
                        
                        /* Load file to byte array */
                        byte[] fileByteArray;
                        FileInputStream fileInputStream;
                        try {
                            fileInputStream = 
                                new FileInputStream(myFile);
                            fileByteArray = 
                                new byte[(int) myFile.length()];
                        } catch (Exception e) {
                           System.out.println("ERROR: Did not load " +
                                "file to byte array!");
                           continue;
                        }
                        
                        /* Prepare file to be transferred */
                        try {
                            fileInputStream.read(fileByteArray);
                        } catch (Exception e) {
                           System.out.println("ERROR: Failed to read" +
                                " to byte array!");
                           continue;
                        }
                        /* Close the filereader */
                        try {
                            fileInputStream.close();
                        } catch (Exception e) {
                           System.out.println("ERROR: Failed to close" +
                                " after reading file!");
                           continue;
                        }
                        /* Write the data over the line */
                        try {
                           outToServer_Data.write(fileByteArray);
                           outToServer_Data.flush();
                           System.out.println("File sent across wire!");
                        } catch (Exception e) {
                           System.out.println("ERROR: Failed to send" +
                                " the file across wire.");
                           continue;
                        }
                        /* Close the data connection */
                        try {
                            dataListen.close();
                            dataConnection.close();
                        } catch (Exception e) {
                            System.out.println("ERROR: Could not " + 
                                "close data connection");
                            continue;
                        }   
                    /* This catch-all handles the whole function */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                /* If the requested file does not exist */
                } else {
                    System.out.println("ERROR: File does not exist.");
                }
                
            /* Retrieve a File Command */
            } else if (Command.equals("RETR") && isConnected == true ) {
                /* Declared here for scope issue */
                String fileToStore;
                
                /* Grab the filename from the user */
                try {
                    fileToStore = tokens.nextToken();
                } catch (Exception e) {
                    System.out.println("ERROR: Did not give " + 
                        "arguement to RETR.");
                    continue;
                }
                
                try {
                    /* Send the request over the control line */
                    String toSend = dataPort + " " + "RETR" + " " + 
                        fileToStore;
                    outToServer_Control.println(toSend);
                    outToServer_Control.flush();
                    
                    /* Connect to server and establish variables */
                    dataListen = new ServerSocket(dataPort);
                    dataConnection = dataListen.accept();
                    InputStream inFromServer_Data = 
                        dataConnection.getInputStream();

                    /* Below this handles sending the file itself */
                    String fileRequestedString = 
                        new String(fileToStore.getBytes());
                    while ((recvMsgSize = 
                            inFromServer_Data.read(byteBuffer)) != -1) {
                        try {
                            File fileRequested = 
                                new File(fileRequestedString);
                            FileOutputStream fileOutputStream = 
                                new FileOutputStream(fileRequested);

                            fileOutputStream.write(byteBuffer, 
                                                        0, recvMsgSize);
                            fileOutputStream.close();
                            
                            Scanner scanner = new Scanner(fileRequested);
                            String errorCheck = scanner.nextLine();
                            if(errorCheck.equals(
                                              "File does not exist.")) { 
                                System.out.println(
                                  "File does not exist.");
                                fileRequested.delete();
                            } else {
                                System.out.println("File retrieved!");
                            }
                        } catch (Exception e) {
                            System.out.println("Error trying to " + 
                             "retrieve file.");
                        }
                    }
                
                } catch (Exception e) {
                    System.out.println("ERROR: Failure in " + 
                        "file retrieval.");
                    continue;
                }
                
                /* Close the data connection */
                try {
                    dataListen.close();
                    dataConnection.close();
                } catch (Exception e) {
                    System.out.println("ERROR: Could not close data" +
                        " connection");
                    continue;
                }
                
            } else if (Command.equals("LIST") && isConnected == true) {
                /* Sends "1078 LIST" over the data-line */
                String toSend = dataPort + " " + "LIST";
                
                outToServer_Control.println(toSend);
                outToServer_Control.flush();
                
                /* Connect to server and establish variables */
                try {
                    System.out.println("Files in the server: ");
                    dataListen = new ServerSocket(dataPort);
                    dataConnection = dataListen.accept();
                    InputStream inFromServer_Data = 
                        dataConnection.getInputStream();
                    OutputStream outToServer_Data = 
                        dataConnection.getOutputStream();
                    while ((recvMsgSize = 
                        inFromServer_Data.read(byteBuffer)) != -1) {
                        System.out.println(
                            new String(byteBuffer, 0, recvMsgSize));
                    }
                } catch (Exception e) {
                    System.out.println("Error creating list.");
                    continue;
                }
                
                /* Close the data connection */
                try {
                    dataListen.close();
                    dataConnection.close();
                } catch (Exception e) {
                    System.out.println("ERROR: Could not close data" +
                        " connection");
                    continue;
                }
                
            /* None of the above commands matched */
            } else {
                System.out.println("ERROR: Command not valid!!");
            }
            
        /* End of controlling while */
        }
    
    /* End of Main() */
    }
    
/* End of FTP client */
}
