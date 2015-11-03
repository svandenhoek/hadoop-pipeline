/**
 * The Javadoc from Oracle's java.lang.RuntimeException was used to create the Javadoc present in this file.
 */
package org.molgenis.hadoop.pipeline.application.exceptions;

/**
 * An <em>unchecked exception</em> thrown to indicate something went wrong during the execution of a pipeline containing
 * 1 or more {@link Process}{@code es}.
 */
public class ProcessPipeException extends RuntimeException
{
	private static final long serialVersionUID = -3951771900588889532L;

	/**
	 * Constructs a new process pipe exception with the specified cause and a detail message of
	 * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
	 * <tt>cause</tt>).
	 *
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
	 *            value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public ProcessPipeException(Throwable cause)
	{
		super(cause);
	}
}