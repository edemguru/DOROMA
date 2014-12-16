package controller.udp;

import java.io.IOException;

public interface IUdpControllerListenerCli {

	// --- Remote commands ---

	/**
	 * Process alive information send by a node
	 *
	 * @param port
	 * @param operations
	 * @param address
	 * @return result of processing
	 * @throws IOException
	 */
	String alive(String port, String operations, String address) throws IOException;

}
