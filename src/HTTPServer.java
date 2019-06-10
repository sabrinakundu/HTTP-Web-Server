import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * HTTP Web Server using Sockets
 */
public class HTTPServer implements Runnable {
	
	static final File ROOT = new File(".");
	static final String HOME = "index.html";
	static final String FILE_NOT_FOUND = "fileNotFound.html";
	static final String METHOD_NOT_SUPPORTED = "methodNoSupport.html";
	static final int LOCALHOST = 8080;
	static final boolean verbose = true;
	private Socket socket;
	
	/**
	 * HTTPServer class constructor
	 * @param connection
	 */
	public HTTPServer(Socket connection) {
		socket = connection;
	}
	
	/**
	 * Run server and open localhost:8080 connection.
	 * Create thread to manage client connection.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ServerSocket serversocket = new ServerSocket(LOCALHOST);
		System.out.println("Server started.\nListening for connection on port: " + LOCALHOST + " ...\n");
		
		while(true) {
			HTTPServer httpserver = new HTTPServer(serversocket.accept());
			
			if (verbose) {
				System.out.println("Connection opened. (" + new Date() + ")");
			}
			
			Thread thread = new Thread(httpserver);
			thread.start();
		}
	}
	
	/**
	 * Get file and display in browser, or display methodNoSupport.html file 
	 * if method is not GET or HEAD.
	 */
	@Override
	public void run() {
		BufferedReader in = null;
		PrintWriter output = null;
		BufferedOutputStream outputData = null;
		String getFile = null;
		
		try {
			// read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// get character output stream to client
			output = new PrintWriter(socket.getOutputStream());
			// get binary output stream to client
			outputData = new BufferedOutputStream(socket.getOutputStream());
			// get first line of request from the client
			String input = in.readLine();
			// parse the request with a string tokenizer
			if (input == null) return;
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // get the HTTP method of the client
			// get requested file
			getFile = parse.nextToken().toLowerCase();
			
			// support only GET and HEAD methods
			if(!method.equals("GET") && !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + "method");
				}
				// send not supported file to the client
				File file = new File(ROOT, METHOD_NOT_SUPPORTED);
				int size = (int) file.length();
				String mimeType = "text/html";
				byte[] fileData = readFileData(file, size);
				// send HTTP Headers with data to client
				output.println("HTTP/1.1 501 Not Implemented");
				output.println("Server : Java HTTP Server from SSaurel : 1.0");
				output.println("Date: " + new Date());
				output.println("Content-type: " + mimeType);
				output.println("Content-length: " + size);
				output.println();
				output.flush(); // flush character output stream buffer
				outputData.write(fileData, 0, size);
				outputData.flush();
			} else {
				// it is GET or HEAD method
				if (getFile.endsWith("/")) {
					getFile = HOME;
				}		
				File file = new File(ROOT, getFile);
				int size = (int) file.length();
				String content = getContentType(getFile);	
				if (method.equals("GET")) {
					byte[] fileData = readFileData(file, size);		
					// send HTTP headers
					output.println("HTTP/1.1 200 OK");
					output.println("Server : Java HTTP Server from SSaurel : 1.0");
					output.println("Date: " + new Date());
					output.println("Content-type: " + content);
					output.println("Content-length: " + size);
					output.println();
					output.flush(); // flush character output stream buffer
					outputData.write(fileData, 0, size);
					outputData.flush();
				}		
				if (verbose) {
					System.out.println("File " + getFile + " of type " + content + "returned");
				}
			}
		} catch (FileNotFoundException e) {	
			try {
				fileNotFound(output, outputData, getFile);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}	
		} catch (IOException e) {
			System.err.println("Server error : " + e);
		} finally {
			try {
				in.close();
				output.close();
				outputData.close();
				socket.close(); 	
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());	
			} 
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
	}
	
	/**
	 * Read file
	 * @param file
	 * @param size
	 * @return byte[] fileData
	 * @throws IOException
	 */
	private byte[] readFileData(File file, int size) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[size];
		fileIn = new FileInputStream(file);
		fileIn.read(fileData);
		if (fileIn != null) {
			fileIn.close();
		}
		return fileData;
	}
	
	/**
	 * Get MIME type of file
	 * @param getFile
	 * @return String "text/html" or "text/plain"
	 */
	private String getContentType(String getFile) {
		if (getFile.endsWith(".htm") || getFile.endsWith(".html")) {
			return "text/html";
		} else {
			return "text/plain";
		}
	}
	
	/**
	 * Print File Not Found error
	 * @param out
	 * @param outputData
	 * @param getFile
	 * @throws IOException
	 */
	private void fileNotFound(PrintWriter out, OutputStream outputData, String getFile) throws IOException {
		File file = new File(ROOT, FILE_NOT_FOUND);
		int size = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, size);
		// send HTTP headers
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server : Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + size);
		out.println(); 
		out.flush(); // flush character output stream buffer
		outputData.write(fileData, 0, size);
		outputData.flush();
		if (verbose) {
			System.out.println("file " + getFile + " not found");
		}
	}
}
