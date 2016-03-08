package org.molgenis.hadoop.pipeline.application.mapreduce;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.molgenis.hadoop.pipeline.application.DistributedCacheHandler;
import org.molgenis.hadoop.pipeline.application.HadoopPipelineApplication;
import org.molgenis.hadoop.pipeline.application.cachedigestion.HadoopBedFormatFileReader;
import org.molgenis.hadoop.pipeline.application.cachedigestion.HadoopSamplesInfoFileReader;
import org.molgenis.hadoop.pipeline.application.cachedigestion.Region;
import org.molgenis.hadoop.pipeline.application.cachedigestion.Sample;
import org.molgenis.hadoop.pipeline.application.inputstreamdigestion.SamRecordSink;
import org.molgenis.hadoop.pipeline.application.processes.PipeRunner;
import org.molgenis.hadoop.pipeline.application.writables.RegionSamRecordStartWritable;
import org.seqdoop.hadoop_bam.SAMRecordWritable;

import htsjdk.samtools.SAMRecord;

/**
 * Hadoop MapReduce Job mapper.
 */
public class HadoopPipelineMapper extends Mapper<Text, BytesWritable, RegionSamRecordStartWritable, SAMRecordWritable>
{
	/**
	 * Logger to write information to.
	 */
	private static final Logger logger = Logger.getLogger(HadoopPipelineMapper.class);

	/**
	 * BwaTool executable location.
	 */
	private String bwaTool;

	/**
	 * Alignment reference fasta file location (with index file having the same prefix and being in the same directory.
	 */
	private String alignmentReferenceFastaFile;

	/**
	 * Allows retrieval of the groups to which a specific {@link SAMRecord} belongs to (based upon the area the
	 * {@link SAMRecord} was aligned to on the reference data compared to a BED file stored start/end regions).
	 */
	private SamRecordGroupsRetriever groupsRetriever;

	/**
	 * The possible samples an input split can belong to.
	 */
	private List<Sample> samples;

	/**
	 * Function called at the beginning of a task.
	 */
	@Override
	protected void setup(Context context) throws IOException, InterruptedException
	{
		digestCache(context);
	}

	/**
	 * Function run on individual chunks of the data.
	 */
	@Override
	public void map(final Text key, BytesWritable value, final Context context) throws IOException, InterruptedException
	{
		// Only digests an input split if it is an ".fq.gz" file that starts with "halvade_" in the filename.
		// Non-".fq.gz" files will simply be ignored while ".fq.gz" files that start with a different name will cause an
		// IOException.
		if (validateInputFileType(key.toString()))
		{
			// Retrieve the sample belonging to the input split.
			Sample sample = retrieveCorrectSample(key.toString());

			SamRecordSink sink = new SamRecordSink()
			{
				Set<Region> regionsSet;

				@Override
				protected void digestStreamItems(SAMRecord first, SAMRecord second) throws IOException
				{
					// Valides whether the 2 SAMRecords are a read pair.
					if (!validateIfMates(first, second))
					{
						throw new IOException(
								"No valid read pair: " + first.getSAMString() + " - " + second.getSAMString());
					}

					// Retrieves the regions the paired reads align to.
					List<Region> regions1 = groupsRetriever.retrieveGroupsWithinRange(first);
					List<Region> regions2 = groupsRetriever.retrieveGroupsWithinRange(second);

					// Generates a set containing the unique regions only.
					regionsSet = new HashSet<>();
					regionsSet.addAll(regions1);
					regionsSet.addAll(regions2);

					// Runs the basic digestStreamItem(SAMRecord item) for both reads.
					super.digestStreamItems(first, second);
				}

				@Override
				public void digestStreamItem(SAMRecord item) throws IOException
				{
					try
					{
						// Creates value.
						SAMRecordWritable samWritable = new SAMRecordWritable();
						samWritable.set(item);

						// Writes a key-value pair for each key present in the regionsSet (so this or the complement
						// read aligned to).
						for (Region key : regionsSet)
						{
							context.write(new RegionSamRecordStartWritable(key, item), samWritable);
						}
					}
					catch (InterruptedException e)
					{
						throw new RuntimeException(e);
					}
				}
			};

			logger.debug("Executing pipeline with input split: \"" + key.toString() + "\" and read group line \""
					+ sample.getReadGroupLine() + "\".");
			PipeRunner.startPipeline(value.getBytes(), sink, new ProcessBuilder(bwaTool, "mem", "-p", "-M", "-R",
					sample.getSafeReadGroupLine(), alignmentReferenceFastaFile, "-").start());
		}
	}

	/**
	 * Digests the cache files that are needed into the required formats.
	 * 
	 * IMPORTANT: Be sure the exact same array order is used as defined in {@link HadoopPipelineApplication}!
	 * 
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private void digestCache(Context context) throws IllegalArgumentException, IOException
	{
		DistributedCacheHandler cacheHandler = new DistributedCacheHandler(context);

		bwaTool = cacheHandler.getBwaToolFromToolsArchive();
		alignmentReferenceFastaFile = cacheHandler.getReferenceFastaFile();

		// Retrieves the groups stored in the bed-file which can be used for SAMRecord grouping.
		String bedFile = cacheHandler.getBedFile();
		List<Region> possibleGroups = new HadoopBedFormatFileReader().read(bedFile);
		groupsRetriever = new SamRecordGroupsRetriever(possibleGroups);

		// Retrieves the samples stored in the samples information file.
		String samplesInfoFile = cacheHandler.getSamplesInfoFile();
		samples = new HadoopSamplesInfoFileReader().read(samplesInfoFile);
	}

	/**
	 * Checks whether an input split is a valid halvade input chunk.
	 * 
	 * @param inputSplitPath
	 *            {@link String}
	 * @return {@code true} if input split is a file with a name that starts with "halvade_" and ends with ".fq.gz",
	 *         false if it has a file extension different to ".fq.gz".
	 * @throws IOException
	 *             if the given input split is a ".fq.gz" file but does not start with "halvade_", throws an
	 *             {@link Exception) as safety measure as the to-be-digested could be invalid due to being wrongly
	 *             uploaded (or some other reason that should result in the file not being processed).
	 */
	private boolean validateInputFileType(String inputSplitPath) throws IOException
	{
		// Retrieves the file name.
		String fileName = FilenameUtils.getName(inputSplitPath);

		// If a .fq.gz file is found that starts with a different name than expected, throws an Exception.
		if (fileName.endsWith(".fq.gz") && !fileName.startsWith("halvade_"))
		{
			throw new IOException("Invalid .fq.gz file found: " + inputSplitPath);
		}

		// Non-".fq.gz" files return false.
		if (!fileName.endsWith(".fq.gz"))
		{
			return false;
		}

		//  Otherwise returns true.
		return true;
	}

	/**
	 * Returns the first found {@link Sample} that matches to the current input split (only one match should be present
	 * in {@code this.samples}).
	 * 
	 * @param inputSplitPath
	 *            {@link String}
	 * @return {@link Sample} the {@link Sample} that matches with the {@code inputSplitPath}.
	 * @throws IOException
	 *             if no {@link Sample} could be found within {@code this.samples} that matches the
	 *             {@code inputSplitPath}.
	 */
	private Sample retrieveCorrectSample(String inputSplitPath) throws IOException
	{
		// Retrieves the parent directory name from the input split file.
		String sampleDirName = FilenameUtils.getName(FilenameUtils.getPathNoEndSeparator(inputSplitPath));

		// Goes through each available sample for comparison.
		for (Sample sample : samples)
		{
			// If the sample comparison name equals the last directory in the input split path,
			// returns the sample.
			if (sample.getComparisonName().equals(sampleDirName))
			{
				return sample;
			}
		}
		// If no matching sample was found, throws an Exception.
		throw new IOException("Incorrectly named path or samplesheet missing information about: " + inputSplitPath);
	}
}
