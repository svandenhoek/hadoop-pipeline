package org.molgenis.hadoop.pipeline.application.cachedigestion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.molgenis.hadoop.pipeline.application.Tester;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tester for {@link HadoopSamplesInfoFileReader}.
 */
public class HadoopSamplesInfoFileReaderTester extends Tester
{
	/**
	 * The reader that is being tested.
	 */
	private HadoopSamplesInfoFileReader reader;

	/**
	 * The expected valid samples.
	 */
	private ArrayList<Sample> expectedValidSamples;

	/**
	 * Creates a {@link HadoopToolsXmlReader} needed for testing.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public void beforeClass() throws IOException
	{
		reader = new HadoopSamplesInfoFileReader();

		expectedValidSamples = new ArrayList<Sample>();
		expectedValidSamples.add(new Sample("sample1", "SN163", 150616, 648, "AHKYLMADXX", 1));
		expectedValidSamples.add(new Sample("sample2", "SN163", 150616, 648, "AHKYLMADXX", 2));
		expectedValidSamples.add(new Sample("sample3", "SN163", 150702, 649, "BHJYNKADXX", 5));
	}

	/**
	 * Test when a samplesheet contains many columns (so including unused ones).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testValidSamplesheet() throws IOException
	{
		List<Sample> actualSamples = reader
				.read(getClassLoader().getResource("samplesheets/samplesheet.csv").getFile());

		Assert.assertEquals(actualSamples, expectedValidSamples);
	}

	/**
	 * Test when the samplesheet only contains the vital columns with data.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testValidMinimalSamplesheet() throws IOException
	{
		List<Sample> actualSamples = reader
				.read(getClassLoader().getResource("samplesheets/valid_minimal.csv").getFile());

		Assert.assertEquals(actualSamples, expectedValidSamples);
	}

	/**
	 * Test when the header of the csv file is missing a field.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testHeaderMissingRunColumn() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/header_missing_run_column.csv").getFile());
	}

	/**
	 * Test when the header of the csv file is an empty line.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testHeaderIsAnEmptyLine() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/header_empty_line.csv").getFile());
	}

	/**
	 * Test when the header of the csv file is missing.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testHeaderLineIsMissing() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/missing_header_line.csv").getFile());
	}

	/**
	 * Test when a sample is missing a field.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testSampleMissingRun() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/sample_missing_run.csv").getFile());
	}

	/**
	 * Test when a sample is missing the last field.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testSampleMissingLastField() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/sample_missing_last_field.csv").getFile());
	}

	/**
	 * Test when a sample is missing the last field but with a comma ending the previous field.
	 * 
	 * @throws IOException
	 */
	@Test(expectedExceptions = IOException.class)
	public void testSampleMissingLastFieldEndingWithAComma() throws IOException
	{
		reader.read(getClassLoader().getResource("samplesheets/sample_missing_last_field_comma-ended.csv").getFile());
	}

	/**
	 * Test when the run value and date are swapped (so columns do not have a correct seeming value but is of the same
	 * type). Currently, the expected behavior is not checking on this and it should pass as long as the type of the
	 * value is correct.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSampleRunAndSequencingStartDateSwapped() throws IOException
	{
		@SuppressWarnings("unchecked")
		List<Sample> expectedSamples = (List<Sample>) expectedValidSamples.clone();
		expectedSamples.remove(1);
		expectedSamples.add(1, new Sample("sample2", "SN163", 648, 150616, "AHKYLMADXX", 2));

		List<Sample> actualSamples = reader.read(
				getClassLoader().getResource("samplesheets/sample_run_sequencingStartDate_swapped.csv").getFile());

		Assert.assertEquals(actualSamples, expectedSamples);
	}
}
