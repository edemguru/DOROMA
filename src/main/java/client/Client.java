package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import cli.Command;
import cli.AdvancedShell;
import util.Config;
import util.Keys;
import util.Message;
import util.SecurityUtils;
import util.SecurityUtils.Encryption;

public class Client implements IClientCli, Runnable {

	private String clientName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private PrintStream userErrorStream;
	private AdvancedShell shell;
	private String loggedInUser = null;
	private Socket controllerSocket = null;
	private boolean acceptCommands = true;
	private Message message = new Message();
	private Thread shellThread;
	private PublicKey controllerPublicKey;
	private PrivateKey userPrivateKey;
	private SecretKey secretKey;
	private IvParameterSpec initVector;

	/**
	 * @param clientName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 * @param userErrorStream
	 *            the error output stream to write the console error output to
	 */
	public Client(String clientName, Config config, InputStream userRequestStream, PrintStream userResponseStream, PrintStream userErrorStream) {
		setClientName(clientName);
		setConfig(config);
		setUserRequestStream(userRequestStream);
		setUserResponseStream(userResponseStream);
		setUserErrorStream(userErrorStream);

		/*
		 * create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		setShell(new AdvancedShell(clientName, userRequestStream, userResponseStream, userErrorStream));
		
	}

	/**
	 * @return the clientName
	 */
	@SuppressWarnings("unused")
	private synchronized String getClientName() {
		return clientName;
	}

	/**
	 * @param clientName the clientName to set
	 */
	private synchronized void setClientName(String clientName) {
		this.clientName = clientName;
	}

	/**
	 * @return the config
	 */
	private synchronized Config getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	private synchronized void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @return the userRequestStream
	 */
	@SuppressWarnings("unused")
	private synchronized InputStream getUserRequestStream() {
		return userRequestStream;
	}

	/**
	 * @param userRequestStream the userRequestStream to set
	 */
	private synchronized void setUserRequestStream(InputStream userRequestStream) {
		this.userRequestStream = userRequestStream;
	}

	/**
	 * @return the userResponseStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserResponseStream() {
		return userResponseStream;
	}

	/**
	 * @param userResponseStream the userResponseStream to set
	 */
	private synchronized void setUserResponseStream(PrintStream userResponseStream) {
		this.userResponseStream = userResponseStream;
	}

	/**
	 * @return the userErrorStream
	 */
	@SuppressWarnings("unused")
	private synchronized PrintStream getUserErrorStream() {
		return userErrorStream;
	}

	/**
	 * @param userErrorStream the userErrorStream to set
	 */
	private synchronized void setUserErrorStream(PrintStream userErrorStream) {
		this.userErrorStream = userErrorStream;
	}

	/**
	 * @return the shell
	 */
	private synchronized AdvancedShell getShell() {
		return shell;
	}

	/**
	 * @param shell the shell to set
	 */
	private synchronized void setShell(AdvancedShell shell) {
		this.shell = shell;
	}

	/**
	 * @return the loggedInUser
	 */
	private synchronized String getLoggedInUser() {
		return loggedInUser;
	}

	/**
	 * @param loggedInUser the loggedInUser to set
	 */
	private synchronized void setLoggedInUser(String loggedInUser) {
		this.loggedInUser = loggedInUser;
	}

	/**
	 * @return the controllerSocket
	 */
	private synchronized Socket getControllerSocket() {
		return controllerSocket;
	}

	/**
	 * @param controllerSocket the socket to set
	 */
	private synchronized void setControllerSocket(Socket controllerSocket) {
		this.controllerSocket = controllerSocket;
	}

	/**
	 * @return the acceptCommands
	 */
	private synchronized boolean isAcceptCommands() {
		return acceptCommands;
	}

	/**
	 * @param acceptCommands the acceptCommands to set
	 */
	private synchronized void setAcceptCommands(boolean acceptCommands) {
		this.acceptCommands = acceptCommands;
	}

	/**
	 * @return the message
	 */
	private synchronized Message getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	@SuppressWarnings("unused")
	private synchronized void setMessage(Message message) {
		this.message = message;
	}

	/**
	 * @return the shellThread
	 */
	private synchronized Thread getShellThread() {
		return shellThread;
	}

	/**
	 * @param shellThread the shellThread to set
	 */
	private synchronized void setShellThread(Thread shellThread) {
		this.shellThread = shellThread;
	}

	/**
	 * @return the controllerPublicKey
	 */
	private synchronized PublicKey getControllerPublicKey() {
		return controllerPublicKey;
	}

	/**
	 * @param controllerPublicKey the controllerPublicKey to set
	 */
	private synchronized void setControllerPublicKey(PublicKey controllerPublicKey) {
		this.controllerPublicKey = controllerPublicKey;
	}

	/**
	 * @return the userPrivateKey
	 */
	private synchronized PrivateKey getUserPrivateKey() {
		return userPrivateKey;
	}

	/**
	 * @param userPrivateKey the userPrivateKey to set
	 */
	private synchronized void setUserPrivateKey(PrivateKey userPrivateKey) {
		this.userPrivateKey = userPrivateKey;
	}

	/**
	 * @return the secretKey
	 */
	private synchronized SecretKey getSecretKey() {
		return secretKey;
	}

	/**
	 * @param secretKey the secretKey to set
	 */
	private synchronized void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * @return the initVector
	 */
	private synchronized IvParameterSpec getInitVector() {
		return initVector;
	}

	/**
	 * @param initVector the initVector to set
	 */
	private synchronized void setInitVector(IvParameterSpec initVector) {
		this.initVector = initVector;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in, System.out, System.err);
		// Start the instance in a new thread
		new Thread((Runnable) client).start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		/*
		 * register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		getShell().register(this);

		/*
		 * make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface. Thus, you can run the Shell asynchronously by
		 * starting a new Thread:
		 * 
		 * Thread shellThread = new Thread(shell); shellThread.start();
		 * 
		 * In that case, do not forget to terminate the Thread ordinarily.
		 * Otherwise, the program will not exit.
		 */
		setShellThread(new Thread(getShell())); 
		getShellThread().start();
		
		getShell().printLine("'" + getClass().getName() + "' is up! Enter command.");

	}
	
	/**
	 * check if user is logged in
	 * 
	 * @return true if the user is logged in
	 */
	public Boolean loggedIn() {
		return (getLoggedInUser() != null);
	}
	
	/**
	 * Wrappers for 'byte[] request(Encryption encryption, byte[] data, boolean loggedIn)'
	 * 
	 * @param encryption default to AES
	 * @param data
	 * @param loggedIn defaults to true
	 * @return
	 */
	private String sendRequest(String data) {
		String result= null;
		byte[] request = null;
		if (data != null) {
			request = data.getBytes();
		}
		byte[] response = sendRequest(Encryption.AES, request, true);
		if (response != null) {
			result = new String(response);
		}
		return result;
	}
	private String sendRequest(String data, boolean loggedIn) {
		String result= null;
		byte[] request = null;
		if (data != null) {
			request = data.getBytes();
		}
		byte[] response = sendRequest(Encryption.AES, request, loggedIn);
		if (response != null) {
			result = new String(response);
		}
		return result;
	}
	private String sendRequest(Encryption encryption, String data) {
		String result= null;
		byte[] request = null;
		if (data != null) {
			request = data.getBytes();
		}
		byte[] response = sendRequest(encryption, request, true);
		if (response != null) {
			result = new String(response);
		}
		return result;
	}
	private String sendRequest(Encryption encryption, String data, boolean loggedIn) {
		String result= null;
		byte[] request = null;
		if (data != null) {
			request = data.getBytes();
		}
		byte[] response = sendRequest(encryption, request, loggedIn);
		if (response != null) {
			result = new String(response);
		}
		return result;
	}
	private byte[] sendRequest(byte[] data) {
		return sendRequest(Encryption.AES, data, true);
	}
	private byte[] sendRequest(byte[] data, boolean loggedIn) {
		return sendRequest(Encryption.AES, data, loggedIn);
	}
	private byte[] sendRequest(Encryption encryption, byte[] data) {
		return sendRequest(encryption, data, true);
	}
	/**
	 * @brief crypto-Wrapper for sending and receiving data using 'response()'
	 * 
	 * @param encryption
	 * @param data
	 * @param loggedIn
	 * @return
	 */
	private byte[] sendRequest(Encryption encryption, byte[] data, boolean loggedIn) {
		byte[] encryptData;
		byte[] decryptData;
		byte[] encodeData;
		byte[] decodeData;
		byte[] responseData;
		// getShell().printLine("Request(" + encryption + ", " + loggedIn + "): '" + new String(data) + "'.");
		if (data == null) {
			getShell().printLine("Error: No data to send!");
			return null;
		} else if (encryption == Encryption.RAW) {
			return recvResponse(data, loggedIn);
		} else if (encryption == Encryption.RSA) {
			// encrypt data
			encryptData = SecurityUtils.encryptRSA(data, getControllerPublicKey());
			if (encryptData == null) {
				getShell().printLine("Error: Can't RSA-encrypt request: '" + new String(data) + "'!");
				return null;
			}
			// encode Base64 (binary to char)
			encodeData = SecurityUtils.encodeB64(encryptData);
			if (encodeData == null) {
				getShell().printLine("Error: Can't Base64-encode request: '" + new String(data) + "'!");
				return null;
			}
			// send request
			responseData = recvResponse(encodeData, loggedIn);
			if (responseData == null) {
				getShell().printLine("Error: No data received!");
				return null;
			}
			if (new String(responseData).equals("?")) {
				// Mist auf der Leitung, wenn der Server den Socket abwürgt
				return null;
			}
			// decode Base64 (char to binary)
			decodeData = SecurityUtils.decodeB64(responseData);
			if (decodeData == null) {
				getShell().printLine("Error: Can't Base64-decode response: '" + new String(responseData) + "'!");
				return null;
			}
			// decrypt data
			decryptData = SecurityUtils.decryptRSA(decodeData, getUserPrivateKey());
			if (decryptData == null) {
				getShell().printLine("Error: Can't RSA-decrypt response: '" + new String(responseData) + "'!");
				return null;
			}
			// getShell().printLine("Response(" + encryption + ", " + loggedIn + "): '" + new String(decryptData) + "'.");
			return decryptData; 
		} else if (encryption == Encryption.AES) {
			// encrypt data
			encryptData = SecurityUtils.encryptAES(data, getSecretKey(), getInitVector());
			if (encryptData == null) {
				getShell().printLine("Error: Can't AES-encrypt request: '" + new String(data) + "'!");
				return null;
			}
			// encode Base64 (binary to char)
			encodeData = SecurityUtils.encodeB64(encryptData);
			if (encodeData == null) {
				getShell().printLine("Error: Can't Base64-encode request: '" + new String(data) + "'!");
				return null;
			}
			// send request
			responseData = recvResponse(encodeData, loggedIn);
			if (responseData == null) {
				getShell().printLine("Error: No data received!");
				return null;
			}
			// decode Base64 (char to binary)
			decodeData = SecurityUtils.decodeB64(responseData);
			if (decodeData == null) {
				getShell().printLine("Error: Can't Base64-decode response: '" + new String(responseData) + "'!");
				return null;
			}
			// decrypt data
			decryptData = SecurityUtils.decryptAES(decodeData, getSecretKey(), getInitVector());
			if (decryptData == null) {
				getShell().printLine("Error: Can't AES-decrypt response: '" + new String(responseData) + "'!");
				return null;
			}
			// getShell().printLine("Response(" + encryption + ", " + loggedIn + "): '" + new String(decryptData) + "'.");
			return decryptData; 
		} else {
			return ("/* TODO: implement code in 'sendRequest()' */").getBytes();
		}
	}
	
	/**
	 * Wrappers for 'byte[] response(byte[] request, boolean loggedIn)'
	 * 
	 * @param request is mandatory
	 * @param loggedIn defaults to true
	 * @return response
	 */
	private String recvResponse(String request) {
		return new String(recvResponse(request.getBytes(), true));
	}
	private String recvResponse(String request, boolean loggedIn) {
		return new String(recvResponse(request.getBytes(), loggedIn));
	}
	private byte[] recvResponse(byte[] request) {
		return recvResponse(request, true);
	}
	/**
	 * Sends request to controller and waits for response
	 * 
	 * @param request
	 * @param loggedIn
	 * @return response
	 */
	private byte[] recvResponse(byte[] request, boolean loggedIn) {
		String inString = "";
		String requestString = new String(request);

		if (isAcceptCommands()) {
			if (loggedIn && ! loggedIn()) {
				getShell().printLine("You have to login first!");
				return null;
			}
			if (! loggedIn && loggedIn()) {
				getShell().printLine("Already logged in!");
				return null;
			}

			try {
				// create a writer to send messages to the server
				PrintWriter serverWriter = new PrintWriter(getControllerSocket().getOutputStream(), true);
				if (serverWriter.checkError()) {
					getShell().printErrLine(getMessage().controller_disconnected);
					closeTCP();
					return null;
				}

				// write provided user input to the socket
				serverWriter.println(requestString);

				// create a reader to retrieve messages send by the server
				BufferedReader serverReader = new BufferedReader(new InputStreamReader(getControllerSocket().getInputStream()));
				// read server response
				// inString = serverReader.readLine();
				// not only read 1 line, read all
				inString = null;
				Character myChar = null;
				if ((myChar = (char) serverReader.read()) != -1) {
					inString = myChar.toString();
					while (serverReader.ready()) {
						if ((myChar = (char) serverReader.read()) == -1) {
							// ready (data available) but unexpected end of input found 
							getShell().printErrLine("!Error: Read-Problems!");
							closeTCP();
							try {
								return (byte[]) getShell().invoke(requestString);
							} catch (Throwable e) {
								return null;
							}
						}
						inString += myChar.toString();
					}
				}
				if (inString == null && requestString.trim().equals("!exit")) {
					// disconnected, wie von "!exit" geplant
					inString = getMessage().controller_disconnected;
				} else if (inString == null || inString.contains(getMessage().controller_disconnected)) {
					// sonst immer unerwartet disconnected
					getShell().printErrLine(getMessage().controller_disconnected);
					closeTCP();
					try {
						return (byte[]) getShell().invoke(requestString);
					} catch (Throwable e) {
						return null;
					}
				}
				// strip all trailing \n or \r
				while (inString.endsWith("\n") || inString.endsWith("\r")) {
					inString = inString.substring(0, inString.length() - 1);
				}
			} catch (SocketException e) {
				getShell().printErrLine(getMessage().controller_disconnected);
				closeTCP();
				try {
					return (byte[]) getShell().invoke(requestString);
				} catch (Throwable e1) {
					return null;
				}
			} catch (IOException e) {
				getShell().printErrLine("!Error: Unknown communication problem!");
				closeTCP();
				try {
					return (byte[]) getShell().invoke(requestString);
				} catch (Throwable e1) {
					return null;
				}
			}
			// return the server response, which will be written on console
			return inString.getBytes();
		} else {
			getShell().printErrLine(getMessage().controller_disconnected);
			// closeTCP();
			return null;
		}
	}
	
	/**
	 * Logs user out and closes all resources, like sockets, shell, ... 
	 */
	synchronized public void close() {
		// disable further commands
		if (isAcceptCommands()) {
			setAcceptCommands(false);
			// log out
			setLoggedInUser(null);

			closeTCP();
			
			// Afterwards stop the Shell from listening for commands
			if (getShell() != null) {
				boolean wasInterrupted = Thread.currentThread().isInterrupted();
				getShell().close();
				if (wasInterrupted) {
					// set Interrupted-Flag again
					Thread.currentThread().interrupt();
				} else {
					// clear Interrupted-Flag
					Thread.interrupted();
				}
			}
	
			// TODO more clean up?

			// finally set the Interrupted-Flag to stop the shell
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes TCP socket
	 */
	public void closeTCP() {
		if (getControllerSocket() != null && ! getControllerSocket().isClosed()) {
			try {
				getControllerSocket().close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
			// getShell().printLine("Socket closed.");
		} else {
			// getShell().printErrLine("Socket closed.");
		}
		setControllerSocket(null);
		setLoggedInUser(null);
	}

// ----------------------------------------------------- Local commands -----------------------------------------	
	
	/* (non-Javadoc)
	 * @see client.IClientCli#login(java.lang.String, java.lang.String)
	 */
	@Override
	@Command
	public String login(String username, String password) throws IOException {

		/*
		 * create a new tcp socket at specified host and port - make sure
		 * you specify them correctly in the client properties file(see
		 * client.properties)
		 */
	// --- not supported any longer --- //
		/*
		String port = "";
		int portNr = 0;
		try {
			// get port term and calculate expression 
			port = getConfig().getString("controller.tcp.port");
		    ScriptEngineManager mgr = new ScriptEngineManager();
		    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		    portNr = ((Double) engine.eval(port)).intValue();

		    setControllerSocket(new Socket(getConfig().getString("controller.host"), portNr));
		} catch (ScriptException e) {
			return "!Error: Invalid port expression '" + port + "'.";
			// return "!Error: Invalid port expression '" + port + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (UnknownHostException | ConnectException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (SocketException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (IOException e) {
			close();
			throw new RuntimeException(e.getClass().getSimpleName(), e);
		}
		
		String response = response("!login " + username + " " + password, false);
		
		if (response != null && response.equals(getMessage().login_success)) {
			this.setLoggedInUser(username);
			return response;
		}
		// unsuccessful
		closeTCP();
		return response;
		*/
		
		return message.unknown_command;
	}

	/* (non-Javadoc)
	 * @see client.IClientCli#logout()
	 */
	@Override
	@Command
	public String logout() throws IOException {

		// just inform the Controller
		String response  = sendRequest("!logout");
		closeTCP();

		// release all User specific state information
		setLoggedInUser(null);

		if (response == null && isAcceptCommands()) {
			return "Already logged out!";
		}
		return response;
	}

	/* (non-Javadoc)
	 * @see client.IClientCli#exit()
	 */
	@Override
	@Command
	public String exit() throws IOException {
		
		String response = "";
		
		// First try to logout in case a user is still logged in
		if (loggedIn()) {
			response = logout();
			if (response != null && response.trim().length() > 0 && ! response.trim().equals("\n")) {
				getShell().printLine(response);
			}
		}

		// also try to terminate Controller's ClientThread
		if (getControllerSocket() != null && ! getControllerSocket().isClosed()) {
			response = sendRequest("!exit", false);
		}

		// close all Sockets, Threads, Timer, ...
		close();

		if (! response.equals(getMessage().shutdown)) {
			return getMessage().shutdown;
		}
		return null;
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/* (non-Javadoc)
	 * @see client.IClientCli#authenticate(java.lang.String)
	 */
	@Override
	@Command
	public String authenticate(String username) throws IOException {

		if (loggedIn()) {
			return "Error: You are already logged in!";
		}
		
		/*
		 * create a new tcp socket at specified host and port - make sure
		 * you specify them correctly in the client properties file(see
		 * client.properties)
		 */
		String port = "";
		int portNr = 0;
		try {
			// get port term and calculate expression 
			port = getConfig().getString("controller.tcp.port");
		    ScriptEngineManager mgr = new ScriptEngineManager();
		    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		    portNr = ((Double) engine.eval(port)).intValue();

		    setControllerSocket(new Socket(getConfig().getString("controller.host"), portNr));
		} catch (ScriptException e) {
			return "!Error: Invalid port expression '" + port + "'.";
			// return "!Error: Invalid port expression '" + port + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (UnknownHostException | ConnectException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (SocketException e) {
			closeTCP();
			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.";
//			return "!Error: Cannot connect to controller at '" + getConfig().getString("controller.host") + "'.\n" + e.getMessage() + "\n" +e.getStackTrace().toString();
		} catch (IOException e) {
			close();
			throw new RuntimeException(e.getClass().getSimpleName(), e);
		}

		// Secure Authentication:
		// 0.  Prepare system
		// 0.a Make sure BouncyCastle is registred
		SecurityUtils.registerBouncyCastle();

		// 0.b RSA cipher: set cloud-controller's public key
		String keyPath = System.getProperty("user.dir") + File.separator + getConfig().getString("controller.key");
		keyPath = keyPath.replace("/", File.separator);
		File keyFile = new File(keyPath);
		if (! keyFile.exists()) {
			// unsuccessful: no controller key file
			closeTCP();
			getShell().printLine("!Error: Can't find controller-key file '" + keyPath + "'!");
			return getMessage().authentication_failed;
		}
		setControllerPublicKey(Keys.readPublicPEM(keyFile));

		// 0.c RSA cipher: set users private key
		keyPath = System.getProperty("user.dir") + File.separator + getConfig().getString("keys.dir") +
				File.separator + username + ".pem";
		keyPath = keyPath.replace("/", File.separator);
		keyFile = new File(keyPath);
		if (! keyFile.exists()) {
			// unsuccessful: no user key file
			closeTCP();
			getShell().printLine("!Error: Can't find user-key file '" + keyPath + "'!");
			return getMessage().authentication_failed;
		}
		setUserPrivateKey(Keys.readPrivatePEM(keyFile));

		// 0.d RSA cipher: generate 32-byte client-challenge
		byte[] clientChallenge = SecurityUtils.encodeB64(SecurityUtils.randomNumber(32));


		// 1.  1st Message: send RSA request
		// 1.a RSA cipher: send encrypted !authenticate-message
		byte[] message = SecurityUtils.concat(("!authenticate " + username + " ").getBytes(), clientChallenge);

		// encrypt entire message, send message and decrypt response
		byte[] response = sendRequest(Encryption.RSA, message, false);

		// 2.  2nd Message: receive RSA-encoded authentication response
		// 2.a check response
		if (response == null) {
			// unsuccessful: no response
			closeTCP();
			getShell().printLine("!Error: No response to Message 1 '" + message + "'!");
			return getMessage().authentication_failed;
		}
		String responseString = new String(response);
		String[] parts = responseString.split("\\s+");
		if (parts.length != 5) {
			// unsuccessful: wrong number of arguments
			closeTCP();
			getShell().printLine("!Error: wrong number of arguments in '" + responseString + "'!");
			return getMessage().authentication_failed;
		}
		if (! parts[0].equals("!ok")) {
			// unsuccessful: missing '!ok'
			closeTCP();
			getShell().printLine("!Error: missing '!ok' keyword in '" + responseString + "'!");
			return getMessage().authentication_failed;
		}
		// check challenge
		byte[] testChallenge = SecurityUtils.subarray(response, responseString.indexOf(parts[1]), parts[1].length()); 
		// getShell().printLine("Test challenge '" + new String(testChallenge) + "' against '" + new String(clientChallenge) + "'.");
		if (! new String(testChallenge).equals(new String(clientChallenge))) {
			// unsuccessful: wrong client-challenge
			closeTCP();
			getShell().printLine("!Error: wrong client-challenge in '" + responseString + "' instead of '" + 
						new String(SecurityUtils.encodeB64(clientChallenge)) + "'!");
			return getMessage().authentication_failed;
		}

		// 2.b keep controllerChallenge Bas64-encoded
		byte[] controllerChallenge = SecurityUtils.subarray(response, responseString.indexOf(parts[2]), parts[2].length());

		// 2.c save secret key
		byte[] secretKey = SecurityUtils.decodeB64(
				SecurityUtils.subarray(response, responseString.indexOf(parts[3]), parts[3].length()));
		if ((secretKey.length * 8) != 256) {		// 256 bits!
			// unsuccessful: wrong secret key length
			closeTCP();
			getShell().printLine("!Error: wrong secret key length in '" + response + "'!");
			return getMessage().authentication_failed;
		}
		setSecretKey(new SecretKeySpec(secretKey, 0, secretKey.length, "AES"));

		// 2.d save initialization vector
		byte[] initVector = SecurityUtils.decodeB64(
				SecurityUtils.subarray(response, responseString.indexOf(parts[4]), parts[4].length()));
		if (initVector.length != 16) {
			// unsuccessful: wrong initialization vector length
			closeTCP();
			getShell().printLine("!Error: wrong initialization vector length in '" + response + "'!");
			return getMessage().authentication_failed;
		}
		setInitVector(new IvParameterSpec(initVector));

		// from now on use AES-encryption

		// 3.  3rd Message: return (AES) controller-challenge
		response = sendRequest(controllerChallenge, false);
		// check response
		if (response == null) {
			// unsuccessful: no response
			closeTCP();
			getShell().printLine("!Error: No response to Message 3 '" + controllerChallenge + "'!");
			return getMessage().authentication_failed;
		}
		responseString = new String(response);
		if (! responseString.equals(getMessage().login_success)) {
			// unsuccessful: no success
			closeTCP();
			getShell().printLine("!Error: Incorrect answer '" + responseString + "' to Message 3 '" + new String(controllerChallenge) + "'!");
			return getMessage().authentication_failed;
		}

		// finally successful authenticated
		this.setLoggedInUser(username);
		return responseString;
	}

	// --- additional Commands not requested in assignment

	// TODO Remove additional commands

	/* (non-Javadoc)
	 * @see client.IClientCli#help()
	 */
	@Override
	@Command
	public String help() throws IOException {

		// ensure the user is logged in
		if (! loggedIn()) {
			// return local commands only
			String usage = "\navailable Commands:\n";
			// usage += "\t!login <userid> <password>\n";
			usage += "\t!authenticate <userid>\n";
			usage += "\t!exit\n";
			usage += "\t!help\n";

			return usage;
		}

		// get remote client help information
		return sendRequest("!help");
	}

	// Try command on remote site, not executable by the client shell 
	/* (non-Javadoc)
	 * @see client.IClientCli#remoteCommand(java.lang.String)
	 */
	@Override
	public String remoteCommand(String command) throws IOException {

		// only if logged in pass command to controller 
		if (loggedIn() && isAcceptCommands()) {
			return sendRequest(command);
		}
		if (isAcceptCommands()) {
			// Command not found
			String result = getMessage().unknown_command;
			try {
				result += "\n" + (String) getShell().invoke("!help");
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return result;
		}
		return null;

	}
}
