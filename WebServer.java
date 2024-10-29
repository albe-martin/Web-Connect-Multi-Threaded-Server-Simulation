

/**
 * WebServer Class
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * Name: Albe Martin
 * UCID: 30161964
 * Ucalgary mail: albe.martin@ucalgary.ca
 */


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;


public class WebServer extends Thread {
	// global logger object, configures in the driver class
	private static final Logger logger = Logger.getLogger("WebServer");

	private boolean shutdown = false; // shutdown flag
	private final int port;						//declaration
	private final int timeout;					//declaration
	private final String root;					//declaration
	private ServerSocket serverSocket;			//declaration

	
    /**
     * Constructor to initialize the web server
     * 
     * @param port 	Server port at which the web server listens > 1024
	 * @param root	Server's root file directory
	 * @param timeout	Idle connection timeout in milli-seconds
     * 
     */
	public WebServer(int port, String root, int timeout){
		this.port = port;				//Assigns the specified port number to the WebServer instance
		this.root = root;				//Sets the root directory for the WebServer instance
		this.timeout = timeout;			//Sets the timeout value for the WebServer instance
	}

	
    /**
	 * Main method in the web server thread.
	 * The web server remains in listening mode 
	 * and accepts connection requests from clients 
	 * until it receives the shutdown signal.
	 * 
     */
	public void run(){
        try {
            serverSocket = new ServerSocket(port);		//creating the socket and listening to the port
			serverSocket.setSoTimeout(100);				//setting a timeout

			while (!shutdown){							//while loop for not shutdown
				try{
					Socket socket = serverSocket.accept();									//accept the socket connection
					System.out.println("Client connected: IP Address - "+socket.getInetAddress().getHostAddress()+", Port -"+socket.getPort());
					WorkerThread workerThread = new WorkerThread(socket, root, timeout);	//creating a worker thread
					new Thread(workerThread).start();										//starting the worker thread
				}catch (SocketTimeoutException e){
				}
			}
			serverSocket.close();								//closing the socket
        } catch (IOException e) {
            throw new RuntimeException(e);						//throwing an exception
        }
    }


    /**
     * Signals the web server to shutdown.
	 *
     */
	public void shutdown() {
		shutdown = true;
	}
	
}
class WorkerThread implements Runnable{
	private Socket socket;				//declaration
	private String root;				//declaration
	private int timeout;				//declaration
	BufferedWriter output;				//declaration
	public WorkerThread(Socket socket, String root, int timeout){
		this.socket = socket;
		this.root = root;
		this.timeout = timeout;
	}
	public  void run() {
		try {
			socket.setSoTimeout(timeout);														//setting the timeout
			InputStream inputStream = socket.getInputStream();									//creating a input stream and getting the input
			OutputStream outputStream = socket.getOutputStream();								//creating an output stream
			BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));		//creating a bufferReader to take in the values from input stream
			output = new BufferedWriter(new OutputStreamWriter(outputStream));		//creating a buffer writer to give out the values

			String request = input.readLine();									//reading the input line
			if (request != null) {												//checks if the request is not null or not
				String[] requestSplit = request.split(" ");					//splitting the line into parts with ""
				String command = requestSplit[0];									//initialization of the command part
				if (command.equals("GET") && requestSplit.length >= 3) {		//checks if the command is GET and the request contains at least three parts
					String path;												//declaration of variable path
					if (requestSplit[1].equals("/")) {							//checks if the pathname is "/": no path name
						path = "/index.html";									//stores path with the given value
					} else {
						path = requestSplit[1];									//stores the path with the value
					}
					String protocol = requestSplit[2];							//storing the protocol name
					File f = new File(root+path);						//creating an instance of File
					if (f.exists() && f.isFile()) {						//checks if the file exists or not
						String date = ServerUtils.getCurrentDate();				//calls the function and gets the date
						String contentType = ServerUtils.getContentType(f);		//calls the function to get the content type
						String lastModified = ServerUtils.getLastModified(f);	//calls the function to store the last modified

						output.write("HTTP/1.1 200 OK\r\n");						//code to write to the output stream
						output.write("Date: " + date + "\r\n");						//code to write to the output stream
						output.write("Server: ALbe Server\r\n");						//code to write to the output stream
						output.write("Last-Modified: " + lastModified + "\r\n");		//code to write to the output stream
						output.write("Content-Length: " + f.length() + "\r\n");		//code to write to the output stream
						output.write("Content-Type: " + contentType + "\r\n");		//code to write to the output stream
						output.write("Connection: close\r\n\r\n");						//code to write to the output stream
						output.flush();

						System.out.println("HTTP/1.1 200 OK\r\n"+"Date: " + date + "\r\n"+"Server: ALbe Server\r\n"+"Last-Modified" + lastModified + "\r\n"+"Content-Length: " + f.length() + "\r\n"+"Content-Type: " + contentType + "\r\n"+"Connection: close\r\n\r\n");

						InputStream fileInput = new FileInputStream(f);					//creating a input stream for file input
						//code to write the file content to the output socket
						byte[] buff = new byte[1024];
						int numBytes;
						while ((numBytes = fileInput.read(buff)) != -1) {
							outputStream.write(buff, 0, numBytes);
						}
					} else {
						output.write("HTTP/1.1 404 Not Found\r\n\r\n");					//code to write to the output stream
						output.write("Date: " + ServerUtils.getCurrentDate() + "\r\n");	//code to write to the output stream
						output.write("Server: ALbe Server\r\n");						//code to write to the output stream
						output.write("Connection: close\r\n\r\n");						//code to write to the output stream
						output.flush();
						System.out.println("HTTP/1.1 404 Not Found\r\n\r\n"+"Date: " + ServerUtils.getCurrentDate() + "\r\n"+"Server: ALbe Server\r\n"+"Connection: close\r\n\r\n");
					}
				} else {
					output.write("HTTP/1.1 400 Bad Request\r\n\r\n");					//code to write to the output stream
					output.write("Date: " + ServerUtils.getCurrentDate() + "\r\n");		//code to write to the output stream
					output.write("Server: ALbe Server\r\n");						//code to write to the output stream
					output.write("Connection: close\r\n\r\n");						//code to write to the output stream
					output.flush();
					System.out.println("HTTP/1.1 400 Bad Request\r\n\r\n"+"Date: " + ServerUtils.getCurrentDate() + "\r\n"+"Server: ALbe Server\r\n"+"Connection: close\r\n\r\n");
				}
			}
			socket.close();
		} catch (SocketTimeoutException e) {														//catches the timeout
			if (output != null) {																	//checks if the output is not null or no
				try {
					output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					output.write("HTTP/1.1 408 Request Timeout\r\nConnection: close\r\n\r\n");		//code to write to the output stream
					output.write("Date: " + ServerUtils.getCurrentDate() + "\r\n");		//code to write to the output stream
					output.write("Server: ALbe Server\r\n");						//code to write to the output stream
					output.write("Connection: close\r\n\r\n");						//code to write to the output stream
					output.flush();
					System.out.println("HTTP/1.1 408 Request Timeout\r\n\r\n"+"Date: " + ServerUtils.getCurrentDate() + "\r\n"+"Server: ALbe Server\r\n"+"Connection: close\r\n\r\n");
					socket.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);													//throws an exception
				}
			}
		} catch (IOException e) {
			e.printStackTrace();																	//prints the exception
		}
	}
}