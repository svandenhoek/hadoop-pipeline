package org.molgenis.hadoop.pipeline.application.inputstreamdigestion;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

/**
 * Sink for digesting SAM-formatted {@link InputStream}{@code s}.
 */
public abstract class SamRecordSink extends Sink<SAMRecord>
{
	/**
	 * Digests a SAM-formatted {@link InputStream}. For each {@link SAMRecord} present in the {@link InputStream},
	 * {@link #digestStreamItem(SAMRecord)} is called.
	 */
	@Override
	public void handleInputStream(InputStream inputStream) throws IOException
	{
		SamReader samReader = null;
		try
		{
			SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault()
					.validationStringency(ValidationStringency.LENIENT);
			samReader = samReaderFactory.open(SamInputResource.of(inputStream));
			SAMRecordIterator samIterator = samReader.iterator();
			while (samIterator.hasNext())
			{
				digestStreamItems(samIterator.next(), samIterator.next());
			}
		}
		finally
		{
			IOUtils.closeQuietly(samReader);
		}
	}

	protected void digestStreamItems(SAMRecord first, SAMRecord second) throws IOException
	{
		digestStreamItem(first);
		digestStreamItem(second);
	}

	/**
	 * Digests a single {@link SAMRecord} from the {@link InputStream}. Be sure to create a custom {@code @Override}
	 * implementation that defines what should be done with each {@link SAMRecord}!
	 * 
	 * @param item
	 *            {@link SAMRecord}
	 */
	@Override
	protected abstract void digestStreamItem(SAMRecord item) throws IOException;

	/**
	 * Quick validation whether two {@link SAMRecord}{@code s} are mates using (some) supplied {@code getters} from the
	 * instances themself.
	 * 
	 * @param first
	 *            {@link SAMRecord}
	 * @param second
	 *            {@link SAMRecord}
	 * @return {@code boolean} - True if mates, false if not.
	 */
	protected boolean validateIfMates(SAMRecord first, SAMRecord second)
	{
		if (first.getMateReferenceName() == second.getReferenceName()
				&& first.getReferenceName() == second.getMateReferenceName()
				&& first.getMateAlignmentStart() == second.getStart()
				&& first.getStart() == second.getMateAlignmentStart()
				&& first.getMateNegativeStrandFlag() == second.getReadNegativeStrandFlag()
				&& first.getReadNegativeStrandFlag() == second.getMateNegativeStrandFlag())
		{
			return true;
		}
		return false;
	}
}
