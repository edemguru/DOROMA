package node.udp;

import java.io.IOException;

public interface IUdpNodeSenderCli {

	/**
	 * Sends alive-informations.
	 *
	 * @return processing result
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String alive(int udpPort, String operations, String address, int tcpPort) throws IOException;

}
