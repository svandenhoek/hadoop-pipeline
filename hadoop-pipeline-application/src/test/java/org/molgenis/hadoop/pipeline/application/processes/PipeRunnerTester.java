package org.molgenis.hadoop.pipeline.application.processes;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.molgenis.hadoop.pipeline.application.Tester;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import htsjdk.samtools.SAMRecord;

public class PipeRunnerTester extends Tester
{
	StringWriter stringWriter;
	String bwaTool;
	String referenceData;

	@AfterClass
	public void afterClass()
	{
	}

	/**
	 * Creates an empty {@link StringWriter} to which logger information can be written to.
	 */
	@BeforeMethod
	public void beforeMethod()
	{
		stringWriter = new StringWriter();
		Logger.getRootLogger().addAppender(new WriterAppender(new SimpleLayout(), stringWriter));
	}

	/**
	 * Prints the logger information.
	 */
	@AfterMethod
	public void afterMethod()
	{
		System.out.println(stringWriter.toString());
	}

	/**
	 * Tests the {@link PipeRunner} by calling a simple python character replacing script.
	 * 
	 * @throws Exception
	 */
	@Test
	public void runSinkWithSingleProces() throws Exception
	{
		// Reads in the intertwined fastq file as a binary array.
		byte[] inputData = new String("Hello world?" + System.lineSeparator() + "This is 1 demo!")
				.getBytes(StandardCharsets.UTF_8);

		// Creates a StringBuilder to store the stream in.
		final StringBuffer sb = new StringBuffer();

		// Describes the sink functionality (what has to be done for each line).
		StringSink sink = new StringSink()
		{
			@Override
			public void digestStreamItem(String item)
			{
				sb.append(item + System.lineSeparator());
			}
		};

		// Runs the pipeline.
		PipeRunner.startPipeline(inputData, sink,
				new ProcessBuilder("python", getClassLoader().getResource("CharacterReplacer.py").getPath(), "1", "a")
						.start());

		// Splits the StringBuilder results into a String array with each line being one element.
		String[] lines = sb.toString().split(System.lineSeparator());
		Assert.assertEquals(lines[0], "Hello world?");
		Assert.assertEquals(lines[1], "This is a demo!");
	}

	/**
	 * Tests the {@link PipeRunner} by calling a simple python character replacing script multiple times.
	 * 
	 * @throws Exception
	 */
	@Test
	public void runSinkWithMultipleProcesses() throws Exception
	{
		// Reads in the intertwined fastq file as a binary array.
		byte[] inputData = new String("Hello world?" + System.lineSeparator() + "This is 1 demo!")
				.getBytes(StandardCharsets.UTF_8);

		// Creates a StringBuilder to store the stream in.
		final StringBuffer sb = new StringBuffer();

		// Describes the sink functionality (what has to be done for each line).
		StringSink sink = new StringSink()
		{
			@Override
			public void digestStreamItem(String item)
			{
				sb.append(item + System.lineSeparator());
			}
		};

		// Runs the pipeline.
		PipeRunner.startPipeline(inputData, sink,
				new ProcessBuilder("python", getClassLoader().getResource("CharacterReplacer.py").getPath(), "?", ".")
						.start(),
				new ProcessBuilder("python", getClassLoader().getResource("CharacterReplacer.py").getPath(), "1", "a")
						.start(),
				new ProcessBuilder("python", getClassLoader().getResource("CharacterReplacer.py").getPath(), "!", ".")
						.start());

		// Splits the StringBuilder results into a String array with each line being one element.
		String[] lines = sb.toString().split(System.lineSeparator());
		Assert.assertEquals(lines[0], "Hello world.");
		Assert.assertEquals(lines[1], "This is a demo.");
	}

	/**
	 * Tests the {@link PipeRunner} using the bwa binary and the {@link StringSink}. The header information is compared
	 * for each record, but as a bwa alignment aligns reads randomly if a read can be aligned to multiple locations,
	 * reads aligning to the first 100 bases of the reference are printed for manual checking.
	 *
	 * @throws Exception
	 */
	@Test
	public void runBwaAlignmentWithStringSink() throws Exception
	{
		// Reads in the intertwined fastq file as a binary array.
		byte[] inputData = readFileAsByteArray("input_fastq/halvade_0_0.fq.gz");

		// Creates a StringBuilder to store the stream in.
		final StringBuffer sb = new StringBuffer();

		// Describes the sink functionality (what has to be done for each line).
		StringSink sink = new StringSink()
		{
			@Override
			public void digestStreamItem(String item)
			{
				sb.append(item + System.lineSeparator());
			}
		};

		// Runs the pipeline.
		PipeRunner.startPipeline(inputData, sink,
				new ProcessBuilder(getClassLoader().getResource("tools/bwa").getPath(), "mem", "-p", "-M",
						getClassLoader().getResource("reference_data/chr1_20000000-21000000.fa").getPath(), "-")
								.start());

		// Splits the StringBuilder results into a String array with each line being one element.
		String[] lines = sb.toString().split(System.lineSeparator());
		// Compares the @SQ line.
		Assert.assertEquals(lines[0], "@SQ\tSN:1:20000000-21000000\tLN:1000001");

		// Splits the program line on the tab for easier comparison of individual items.
		String[] pgLine = lines[1].split("\\t");
		// Compares the @PG line (excluded "CL:" as this is defined outside the PipeRunner class itself).
		Assert.assertEquals(pgLine[0], "@PG");
		Assert.assertEquals(pgLine[1], "ID:bwa");
		Assert.assertEquals(pgLine[2], "PN:bwa");
		Assert.assertEquals(pgLine[3], "VN:0.7.12-r1039");

		// Writes the records to stdout that align to the first 100 nucleotides of the reference genome.
		// IMPORTANT: This is a 1-based leftmost mapping position!
		System.out.println("### StringSink bwa output ###");
		for (String line : lines)
		{
			// Skips lines starting with an "@".
			if (line.startsWith("@"))
			{
				continue;
			}
			if (Integer.parseInt(line.split("\\t")[3]) <= 100)
			{
				System.out.println(line);
			}
		}
	}

	/**
	 * Tests the {@link PipeRunner} using the bwa binary and the {@link SAMRecordSink}. The header information is
	 * compared for each record, but as a bwa alignment aligns reads randomly if a read can be aligned to multiple
	 * locations, reads aligning to the first 100 bases of the reference are printed for manual checking.
	 *
	 * @throws Exception
	 */
	@Test
	public void runBwaAlignmentWithSAMRecordSink() throws Exception
	{
		// Reads in the intertwined fastq file as a binary array.
		byte[] inputData = readFileAsByteArray("input_fastq/halvade_0_0.fq.gz");

		// Describes the sink functionality (what has to be done for each record).
		SAMRecordSink sink = new SAMRecordSink()
		{
			@Override
			public void digestStreamItem(SAMRecord item)
			{
				// Compares the @SQ line.
				Assert.assertEquals("1:20000000-21000000",
						item.getHeader().getSequenceDictionary().getSequence(0).getSequenceName());
				Assert.assertEquals(1000001,
						item.getHeader().getSequenceDictionary().getSequence(0).getSequenceLength());

				// Compares the @PG line (excluded "CL:" as this is defined outside the PipeRunner class itself).
				Assert.assertEquals("bwa", item.getHeader().getProgramRecords().get(0).getId());
				Assert.assertEquals("bwa", item.getHeader().getProgramRecords().get(0).getProgramName());
				Assert.assertEquals("0.7.12-r1039", item.getHeader().getProgramRecords().get(0).getProgramVersion());

				// Writes the records to stdout that align to the first 100 nucleotides of the reference genome.
				// IMPORTANT: getAlignmentStart() is 1-based inclusive!
				if (item.getAlignmentStart() <= 100)
				{
					System.out.println(item.getSAMString().trim());
				}
			}
		};

		// Runs the pipeline.
		System.out.println("### SAMRecordSink bwa output ###");
		PipeRunner.startPipeline(inputData, sink,
				new ProcessBuilder(getClassLoader().getResource("tools/bwa").getPath(), "mem", "-p", "-M",
						getClassLoader().getResource("reference_data/chr1_20000000-21000000.fa").getPath(), "-")
								.start());
	}
}