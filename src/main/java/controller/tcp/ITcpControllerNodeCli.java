package controller.tcp;

import java.io.IOException;

public interface ITcpControllerNodeCli {

	/**
	 * Evaluates the given mathematical term and returns the result.
	 * <p/>
	 * <b>Request</b>:<br/>
	 * {@code !compute 5 + 5}<br/>
	 * <b>Response:</b><br/>
	 * {@code 10}<br/>
	 *
	 * @param val1
	 *            the first integer operand
	 * @param operator
	 *            the operator
	 * @param val2
	 *            the second integer operand
	 * @return the result of the evaluation
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	String compute(int val1, String operator, int val2) throws IOException;

}
