package client;

import java.io.IOException;

public interface IClientCli {

	// --- Local (logged-out) commands needed for Lab 1 ---

	/**
	 * Authenticates the client with the provided username and password.
	 * <p/>
	 * <b>Request</b>:<br/>
	 * {@code !login &lt;username&gt; &lt;password&gt;}<br/>
	 * <b>Response:</b><br/>
	 * {@code !login success}<br/>
	 * or<br/>
	 * {@code !login wrong_credentials}
	 *
	 * @param username
	 *            the name of the user
	 * @param password
	 *            the password
	 * @return status whether the authentication was successful or not
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String login(String username, String password) throws IOException;

	/**
	 * Local part of remote command 'logout'
	 * 
	 * Performs a logout if necessary and closes open connections between client
	 * and cloud controller.
	 * <p/>
	 * <b>Request</b>:<br/>
	 * {@code !logout}<br/>
	 * <b>Response:</b><br/>
	 * {@code !logout &lt;message&gt;}<br/>
	 *
	 * @return message stating whether the logout was successful
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String logout() throws IOException;

	/**
	 * Performs a shutdown of the client and release all resources.<br/>
	 * Shutting down an already terminated client has no effect.
	 * <p/>
	 * Logout the user if necessary and be sure to release all resources, stop
	 * all threads and close any open sockets.
	 * <p/>
	 * E.g.:
	 * 
	 * <pre>
	 * &gt; !exit
	 * Shutting down client now
	 * </pre>
	 *
	 * @return exit message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String exit() throws IOException;

	// --- Local commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	/**
	 * Authenticates the client with the provided username and key.
	 * <p/>
	 * <b>Request</b>:<br/>
	 * {@code !login &lt;username&gt; &lt;client-challenge&gt;}<br/>
	 * <b>Response:</b><br/>
	 * {@code !ok &lt;client-challenge&gt; &lt;controller-challenge&gt; &lt; secret-key&gt; &lt;iv-parameter&gt;}
	 * <br/>
	 * <b>Request</b>:<br/>
	 * {@code &lt;controller-challenge&gt;}
	 *
	 * @param username
	 *            the name of the user
	 * @return status whether the authentication was successful or not
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String authenticate(String username) throws IOException;

	// --- remote Commands support

	/**
	 * Not available as an interactive command 
	 * this method forwards an unknown command 
	 * that was not found locally 
	 * to the remote controller 
	 * but only if logged in.
	 *
	 * @return usage message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String remoteCommand(String command) throws IOException;

	// --- additional Commands not requested in assignment

	// TODO Remove additional commands

	/**
	 * Show usage information of local (logged-out) available commands.
	 * <p/>
	 * E.g.:
	 * 
	 * <pre>
	 * &gt; !help
	 * (usage information shown)
	 * </pre>
	 *
	 * @return usage message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String help() throws IOException;

}
