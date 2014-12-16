package node.tcp;

import java.io.IOException;

public interface ITcpNodeControllerCli {

	/**
	 * Process compute command.
	 *
	 * @return compute result
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String compute(int num1, String operation, int num2) throws IOException;

}
