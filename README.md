### General Overview

What does this program do? What does this program exercise?

### Commands

**Connect**   - "CONNECT <server-IP> <port>" This command connects the client to a waiting server.
              This command must be run before the other commands can be run.
            
**List**      - "LIST" This command calls for the server to return a list of file that it is storing.

**Store**     - "STOR <filename>" This command sends an available file from the client to the server. 
              In the case that the typed file is invalid, the client handles the error alone.

**Retrieve File** - "RETR <filename>" This command sends a request to the server to have the given
                  file be sent over the wire back to the client. In the situation that the server
                  finds that it does not have the file, the server sends an error.

**Quit**      - "QUIT" This command, if connected, sends a command to the server to disconnect the 
              current user (as there may be others currently connected). If this is called without
              initially connecting, it simply stops the program.
              
### To-Do

1. Make it work over a network
2. Encrypt control-traffic at the application layer
3. Smooth out error-handling and responses

### Updates

October 17, 2017 - Added the initial block of code.
