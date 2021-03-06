package org.molgenis.hadoop.pipeline.application.inputdigestion;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.fs.FileSystem;

/**
 * Parser to digest command line arguments.
 */
public class CommandLineInputParser extends InputParser
{
	/**
	 * Variable for generating & digesting the command line options.
	 */
	private Options options = new Options();

	/**
	 * Variable for digesting the command line.
	 */
	private CommandLine commandLine;

	/**
	 * Initiates parsing of the command line.
	 * 
	 * @param fileSys
	 *            {@link FileSystem} The current {@link FileSystem}.
	 * @param args
	 *            {@link String}{@code []} User input.
	 * @throws ParseException
	 * @throws IOException
	 */
	public CommandLineInputParser(FileSystem fileSys, String[] args) throws ParseException, IOException
	{
		setFileSys(requireNonNull(fileSys));
		requireNonNull(args);

		createOptions();
		retrieveParser(args);
		digestCommandLine();

		// If the user input arguments aren't all valid, a ParseException is thrown together with the help message.
		if (!checkValidityArguments())
		{
			printHelpMessage();
			throw new ParseException("Input arguments are invalid (see error messages for more information).");
		}
	}

	/**
	 * Prints the help message to stdout.
	 */
	public void printHelpMessage()
	{
		String cmdSyntax = "yarn jar HadoopPipelineApplicationWithDependencies.jar [-D <property>=<value>]... -t <tools> -i <input> [-i <input>]... -o <output> -r <reference> -s <samples> -b <bed>";
		String helpHeader = "";
		String helpFooter = "Molgenis hadoop-pipeline";

		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(80, cmdSyntax, helpHeader, this.options, helpFooter, false);
	}

	/**
	 * Creates the command line option.
	 */
	@SuppressWarnings("static-access")
	private void createOptions()
	{
		options.addOption(OptionBuilder.withArgName("tools").hasArg().isRequired(true)
				.withDescription(
						"A tar.gz archive containing the required executables (see the readme for more information).")
				.create("t"));

		options.addOption(OptionBuilder.withArgName("input").hasArg().isRequired(true)
				.withDescription(
						"Directory containing the input files. It is possible to use multiple -i arguments to add more"
								+ " than 1 input directory or use subdirectories by adding"
								+ " \"-D mapreduce.input.fileinputformat.input.dir.recursive=true\"")
				.create("i"));

		options.addOption(OptionBuilder.withArgName("output").hasArg().isRequired(true)
				.withDescription(
						"Directory in which the results should be stored. Note that the directory itself should not exist,"
								+ " though the parent directory should.")
				.create("o"));

		options.addOption(OptionBuilder.withArgName("reference").hasArg().isRequired(true)
				.withDescription(
						"Burrows-Wheeler Alignment reference fasta file. Other BWA index file should be present as well"
								+ " using the same prefix.")
				.create("r"));

		options.addOption(OptionBuilder.withArgName("bed").hasArg().isRequired(true)
				.withDescription(
						"BED formatted file describing how to group the aligned SAMRecords during the shuffle/sort phase.")
				.create("b"));

		options.addOption(OptionBuilder.withArgName("samples").hasArg().isRequired(true)
				.withDescription(
						"The samplesheet file containing information about (only) the samples that are being used. This file"
								+ " should be a csv file that is comma-seperated and contains a header line with at least"
								+ " the following fields; externalSampleID, sequencer, sequencingStartDate, run, flowcell & lane."
								+ " The order of these fields does not matter and they are case-insensitive.")
				.create("s"));
	}

	/**
	 * Creates a parser for command line parsing.
	 * 
	 * @param args
	 *            {@link String}{@code []}
	 * @throws ParseException
	 */
	private void retrieveParser(String[] args) throws ParseException
	{
		// Creates parser.
		CommandLineParser parser = new BasicParser();

		// Execute the parsing.
		commandLine = parser.parse(options, args);
	}

	/**
	 * Digests the command line for arguments and appoints these to variables.
	 */
	private void digestCommandLine()
	{
		if (commandLine.hasOption("t"))
		{
			setToolsArchiveLocation(commandLine.getOptionValue("t"));
		}
		if (commandLine.hasOption("i")) // Supports multiple input values.
		{
			setInputDirs(commandLine.getOptionValues("i"));
		}
		if (commandLine.hasOption("o"))
		{
			setOutputDir(commandLine.getOptionValue("o"));
		}
		if (commandLine.hasOption("r"))
		{
			setAlignmentReferenceFastaFiles(commandLine.getOptionValue("r"));
		}
		if (commandLine.hasOption("b"))
		{
			setBedFile(commandLine.getOptionValue("b"));
		}
		if (commandLine.hasOption("s"))
		{
			setSamplesInfoFile(commandLine.getOptionValue("s"));
		}
	}
}
