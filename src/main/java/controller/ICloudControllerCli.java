package controller;

import java.io.IOException;

public interface ICloudControllerCli {

	// --- Commands needed for Lab 1 ---

	/**
	 * Prints out some information about each known node in the cloud, running
	 * or idle.<br/>
	 * 
	 * @return information about the nodes
	 * @throws IOException
	 */
	String nodes() throws IOException;

	/**
	 * Prints out some information about each user, containing username, login
	 * status (online/offline) and credits.<br/>
	 * 
	 * @return the user information
	 * @throws IOException
	 */
	String users() throws IOException;

	/**
	 * Performs a shutdown of the cloud controller and release all resources. <br/>
	 * Shutting down an already terminated cloud controller has no effect.
	 * <p/>
	 * Do not forget to logout each logged in user.
	 * <p/>
	 * E.g.:
	 * 
	 * <pre>
	 * &gt; !exit
	 * Shutting down cloud controller now
	 * </pre>
	 *
	 * @return any message indicating that the cloud controller is going to
	 *         terminate
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String exit() throws IOException;

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
