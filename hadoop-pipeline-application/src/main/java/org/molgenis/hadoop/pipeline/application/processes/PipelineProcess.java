package org.molgenis.hadoop.pipeline.application.processes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

/**
 * Generic command line process using an executable, in stream, out stream and error stream.
 */
public abstract class PipelineProcess implements Callable<Object>
{
	/**
	 * Logger to write information to.
	 */
	private static final Logger logger = Logger.getLogger(BwaAligner.class);

	/**
	 * Contains the command line to be executed.
	 */
	protected ArrayList<String> commandLineArguments;
	/**
	 * Contains the data to be written to the input stream of the process.
	 */
	protected byte[] processInputData;

	/**
	 * The container type which will be used to store the process output stream in.
	 */
	protected PipelineInFactory pipelineInFactory;

	public byte[] getProcessInputData()
	{
		return processInputData;
	}

	public void setProcessInputData(byte[] inputData)
	{
		this.processInputData = inputData;
	}

	/**
	 * Executes the process.
	 */
	public InContainer call() throws IOException, InterruptedException
	{
		ProcessBuilder builder = new ProcessBuilder(commandLineArguments);

		final Process process = builder.start();

		// Create and start the writing to the process input stream.
		new OutStreamHandler(process.getOutputStream(), processInputData).start();

		// Create containers to store the error/output stream results into.
		LinesInContainer errContainer = new LinesInContainer();
		InContainer inContainer = pipelineInFactory.getContainer();

		// Create and start the reading of the error/output stream.
		new LinesInStreamHandler(process.getErrorStream(), errContainer).start();
		pipelineInFactory.getInStreamHandler(process.getInputStream(), inContainer).start();

		// Makes the application wait for the process to finish before continuing.
		process.waitFor();

		// Prints any errors to the log.
		logger.error(errContainer.toString());

		return inContainer;
	}
}
