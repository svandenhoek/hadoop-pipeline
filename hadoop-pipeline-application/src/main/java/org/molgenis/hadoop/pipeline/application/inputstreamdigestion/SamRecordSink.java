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
				digestStreamItem(samIterator.next());
			}
			finishStreamProcessing();
		}
		finally
		{
			IOUtils.closeQuietly(samReader);
		}
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
	 * Allows for some final processes after the last {@link SAMRecord} is digested. Defaults to no behavior.
	 */
	protected void finishStreamProcessing() throws IOException
	{
	}
}
