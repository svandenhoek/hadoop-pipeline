package org.molgenis.hadoop.pipeline.application.mapreduce;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.molgenis.hadoop.pipeline.application.Tester;
import org.molgenis.hadoop.pipeline.application.cachedigestion.ContigRegionsMap;
import org.molgenis.hadoop.pipeline.application.cachedigestion.ContigRegionsMapBuilder;
import org.molgenis.hadoop.pipeline.application.cachedigestion.Region;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

/**
 * Tester for {@link SamRecordGroupsRetriever}.
 */
public class SamRecordGroupsRetrieverTester extends Tester
{
	/**
	 * Writer to catch logger output with.
	 */
	private StringWriter stringWriter;

	/**
	 * Grouper to be tested.
	 */
	private SamRecordGroupsRetriever grouper;

	/**
	 * A test {@link SAMRecord}.
	 */
	private SAMRecord record1;

	/**
	 * Another test {@link SAMRecord}.
	 */
	private SAMRecord record2;

	/**
	 * {@link List} with {@link Region}{@code s} used as input for the {@link SamRecordGroupsRetriever}.
	 */
	private List<Region> inputRegions;

	/**
	 * Builder used to create the {@link ContigRegionsMap}.
	 */
	private ContigRegionsMapBuilder builder;

	/**
	 * Stores the expected output.
	 */
	private List<Region> expectedOutputGroups;

	/**
	 * Loads/generates general data needed for testing.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public void beforeClass() throws IOException
	{
		// Initiates builder used to create the SamRecordGroupsRetriever input.
		builder = new ContigRegionsMapBuilder();

		// Sets a record on contig 1 with range 100-200 (inclusive start/end).
		record1 = generateTestRecord("1", 100, "101M", new SAMSequenceRecord("1:1-301", 300));

		// Sets a record on contig 2 with range 100-200 (inclusive start/end).
		record2 = generateTestRecord("2", 100, "101M", new SAMSequenceRecord("1:1-301", 300),
				new SAMSequenceRecord("2:1-301", 300));
	}

	/**
	 * Prepares for running a test.
	 */
	@BeforeMethod
	public void beforeMethod()
	{
		// Generate clean logger writer.
		stringWriter = new StringWriter();
		Logger.getRootLogger().addAppender(new WriterAppender(new SimpleLayout(), stringWriter));

		// Creates new Lists to be filled with Regions to serve as grouper input and for generating the expected output.
		inputRegions = new ArrayList<>();
		expectedOutputGroups = new ArrayList<Region>();
	}

	/**
	 * Cleans up after a test and writes data to stdout for manual reviewing.
	 */
	@AfterMethod
	public void afterMethod(Method method)
	{
		// Write logger data to stdout.
		System.out.println(method.getName().toString());
		System.out.println(stringWriter.toString());

		// Clears the grouper.
		grouper = null;

		// Clears the ContigRegionsMapBuilder.
		builder.clear();
	}

	/**
	 * Test with a {@link Region} that is just before the {@link SAMRecord}.
	 */
	@Test
	public void testWithSingleRegionJustBeforeRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 89, 99));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with a {@link Region} that is matches exactly with the start of the {@link SAMRecord}.
	 */
	@Test
	public void testWithSingleRegionJustOnStartOfRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 90, 100));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with a {@link Region} that is just after the {@link SAMRecord}.
	 */
	@Test
	public void testWithSingleRegionJustAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 201, 211));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with a {@link Region} that matches exactly with the end of the {@link SAMRecord}.
	 */
	@Test
	public void testWithSingleRegionJustOnEndOfRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 200, 210));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with a {@link Region} that is in the middle of the {@link SAMRecord}.
	 */
	@Test
	public void testWithSingleRegionWithinRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 150, 160));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with a {@link Region} that overlaps the {@link SAMRecord} completely.
	 */
	@Test
	public void testWithSingleRegionCompletelyOverlappingRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 90, 210));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which all are in range of the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthAllWithinRange()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 98, 123));
		inputRegions.add(new Region("1", 124, 148));
		inputRegions.add(new Region("1", 149, 173));
		inputRegions.add(new Region("1", 174, 198));
		inputRegions.add(new Region("1", 199, 223));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which all are in range of the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthAllWithinRange()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 91, 110));
		inputRegions.add(new Region("1", 111, 130));
		inputRegions.add(new Region("1", 131, 150));
		inputRegions.add(new Region("1", 151, 170));
		inputRegions.add(new Region("1", 171, 190));
		inputRegions.add(new Region("1", 191, 220));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// All input groups should be returned, so output is equal to input.
		expectedOutputGroups = inputRegions;

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which all are before the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthAllBeforeRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 11, 20));
		inputRegions.add(new Region("1", 21, 30));
		inputRegions.add(new Region("1", 31, 40));
		inputRegions.add(new Region("1", 41, 50));
		inputRegions.add(new Region("1", 51, 60));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which all are before the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthAllBeforeRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 11, 20));
		inputRegions.add(new Region("1", 21, 30));
		inputRegions.add(new Region("1", 31, 40));
		inputRegions.add(new Region("1", 41, 50));
		inputRegions.add(new Region("1", 51, 60));
		inputRegions.add(new Region("1", 61, 70));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which all are after the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthAllAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 211, 220));
		inputRegions.add(new Region("1", 221, 230));
		inputRegions.add(new Region("1", 231, 240));
		inputRegions.add(new Region("1", 241, 250));
		inputRegions.add(new Region("1", 251, 260));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which all are after the
	 * {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthAllAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 211, 220));
		inputRegions.add(new Region("1", 221, 230));
		inputRegions.add(new Region("1", 231, 240));
		inputRegions.add(new Region("1", 241, 250));
		inputRegions.add(new Region("1", 251, 260));
		inputRegions.add(new Region("1", 261, 270));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which some are on range of the
	 * {@link SAMRecord} while others are before or after it.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthSomeBeforeInAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 10, 60));
		inputRegions.add(new Region("1", 61, 110));
		inputRegions.add(new Region("1", 111, 160));
		inputRegions.add(new Region("1", 161, 210));
		inputRegions.add(new Region("1", 211, 260));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(1, 4));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which some are on range of the
	 * {@link SAMRecord} while others are before or after it.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthSomeBeforeInAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 61, 90));
		inputRegions.add(new Region("1", 91, 120));
		inputRegions.add(new Region("1", 121, 150));
		inputRegions.add(new Region("1", 151, 180));
		inputRegions.add(new Region("1", 181, 210));
		inputRegions.add(new Region("1", 211, 240));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(1, 5));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which only the first {@link Region} is
	 * within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthOnlyFirstWithinRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 196, 205));
		inputRegions.add(new Region("1", 206, 215));
		inputRegions.add(new Region("1", 216, 225));
		inputRegions.add(new Region("1", 226, 235));
		inputRegions.add(new Region("1", 236, 245));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Only first item of input should be returned.
		expectedOutputGroups.add(inputRegions.get(0));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which only the first {@link Region}
	 * is within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthOnlyFirstWithinRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 196, 205));
		inputRegions.add(new Region("1", 206, 215));
		inputRegions.add(new Region("1", 216, 225));
		inputRegions.add(new Region("1", 226, 235));
		inputRegions.add(new Region("1", 236, 245));
		inputRegions.add(new Region("1", 246, 255));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Only first item of input should be returned.
		expectedOutputGroups.add(inputRegions.get(0));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an odd sized {@link List} containing {@link Region}{@code s}, of which only the last {@link Region} is
	 * within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthOnlyLastWithinRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 56, 65));
		inputRegions.add(new Region("1", 66, 75));
		inputRegions.add(new Region("1", 76, 85));
		inputRegions.add(new Region("1", 86, 95));
		inputRegions.add(new Region("1", 96, 105));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Only last item of input should be returned.
		expectedOutputGroups.add(inputRegions.get(inputRegions.size() - 1));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which only the last {@link Region} is
	 * within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthOnlyLastWithinRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 46, 55));
		inputRegions.add(new Region("1", 56, 65));
		inputRegions.add(new Region("1", 66, 75));
		inputRegions.add(new Region("1", 76, 85));
		inputRegions.add(new Region("1", 86, 95));
		inputRegions.add(new Region("1", 96, 105));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Only last item of input should be returned.
		expectedOutputGroups.add(inputRegions.get(inputRegions.size() - 1));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an {@link List} containing {@link Region}{@code s}, of which some {@link Region}{@code s} match a
	 * different contig (so should not match even though the positions might be within range). There are an odd number
	 * of {@link Region} {@code s} that match the contig and all are within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthThreeOnSameContigOfWhichAllInRange()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 91, 110));
		inputRegions.add(new Region("1", 111, 130));
		inputRegions.add(new Region("1", 131, 150));
		inputRegions.add(new Region("1", 151, 170));
		inputRegions.add(new Region("1", 171, 190));
		inputRegions.add(new Region("1", 191, 220));
		inputRegions.add(new Region("2", 131, 150));
		inputRegions.add(new Region("2", 151, 170));
		inputRegions.add(new Region("2", 171, 190));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(6, 9));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record2);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an {@link List} containing {@link Region}{@code s}, of which some {@link Region}{@code s} match a
	 * different contig (so should not match even though the positions might be within range). There are an even number
	 * of {@link Region} {@code s} that match the contig and all are within range of the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthTwoOnSameContigOfWhichAllInRange()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 91, 110));
		inputRegions.add(new Region("1", 111, 130));
		inputRegions.add(new Region("1", 131, 150));
		inputRegions.add(new Region("1", 151, 170));
		inputRegions.add(new Region("1", 171, 190));
		inputRegions.add(new Region("1", 191, 220));
		inputRegions.add(new Region("2", 131, 150));
		inputRegions.add(new Region("2", 151, 170));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(6, 8));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record2);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an {@link List} containing {@link Region}{@code s}, of which some {@link Region}{@code s} match a
	 * different contig (so should not match even though the positions might be within range). There are an odd number
	 * of {@link Region} {@code s} that match the contig. Some of these are within range of the {@link SAMRecord}, while
	 * others are before or after it.
	 */
	@Test
	public void testWithMultipleRegionsOddArrayLengthSixOnSameContigOfWhichSomeBeforeInAfterRecord()
	{
		// Prepares/executes region with record matching.
		for (int i = 1; i < 3; i++)
		{
			String iStr = Integer.toString(i);

			inputRegions.add(new Region(iStr, 10, 60));
			inputRegions.add(new Region(iStr, 61, 110));
			inputRegions.add(new Region(iStr, 111, 160));
			inputRegions.add(new Region(iStr, 161, 210));
			inputRegions.add(new Region(iStr, 211, 260));
		}
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(6, 9));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record2);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an {@link List} containing {@link Region}{@code s}, of which some {@link Region}{@code s} match a
	 * different contig (so should not match even though the positions might be within range). There are an even number
	 * of {@link Region} {@code s} that match the contig. Some of these are within range of the {@link SAMRecord}, while
	 * others are before or after it.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthSixOnSameContigOfWhichSomeBeforeInAfterRecord()
	{
		// Prepares/executes region with record matching.
		for (int i = 1; i < 3; i++)
		{
			String iStr = Integer.toString(i);

			inputRegions.add(new Region(iStr, 61, 90));
			inputRegions.add(new Region(iStr, 91, 120));
			inputRegions.add(new Region(iStr, 121, 150));
			inputRegions.add(new Region(iStr, 151, 180));
			inputRegions.add(new Region(iStr, 181, 210));
			inputRegions.add(new Region(iStr, 211, 240));
		}
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Sublist of input should be returned.
		expectedOutputGroups.addAll(inputRegions.subList(7, 11));

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record2);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test for when all {@link Region}{@code s} are from a different contig than the {@link SAMRecord}.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthNoneOnSameContig()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 211, 220));
		inputRegions.add(new Region("1", 221, 230));
		inputRegions.add(new Region("1", 231, 240));
		inputRegions.add(new Region("1", 241, 250));
		inputRegions.add(new Region("1", 251, 260));
		inputRegions.add(new Region("1", 261, 270));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record2);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test with an even sized {@link List} containing {@link Region}{@code s}, of which the {@link SAMRecord} are
	 * either before or after it.
	 */
	@Test
	public void testWithMultipleRegionsEvenArrayLengthBeforeOrAfterRecord()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 61, 90));
		inputRegions.add(new Region("1", 211, 240));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test when an unmapped read is used.
	 */
	@Test
	public void testWithUnalignedRecord()
	{
		// Generates unmapped record.
		SAMRecord record = generateTestRecord(null, null, null, new SAMSequenceRecord("1:1-301", 300));

		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 150, 160));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Expected output should be empty, so no additional adjustments are made to the expected output.

		// Executes and runs comparison.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test where the first round of recursion takes lower half (resulting in uneven subset), the next round takes the
	 * higher half (resulting in an even subset of 2 elements) which are the border case of interest.
	 */
	@Test
	public void testRecursionWithMultipleRegionsEvenArrayLowerToHigherToBorder()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 1, 40));
		inputRegions.add(new Region("1", 41, 80)); // border
		inputRegions.add(new Region("1", 81, 120)); // border
		inputRegions.add(new Region("1", 121, 160));
		inputRegions.add(new Region("1", 161, 200));
		inputRegions.add(new Region("1", 201, 240));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Generate expected recursion positions. Each position represents a round of recursion.
		int[] expectedLows =
		{ 0, 0, 1 };
		int[] expectedMiddles =
		{ 3, 1, 2 };
		int[] expectedHighs =
		{ 5, 2, 2 };

		// Sublist of input should be returned.
		expectedOutputGroups = inputRegions.subList(2, 5);

		// Executes and runs comparisons.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		compareRecursionPositions(stringWriter.toString(), expectedLows, expectedMiddles, expectedHighs);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test where the first round of recursion takes higher half (resulting in even subset), the next round takes the
	 * lower half (resulting in an even subset of 2 elements) which are the border case of interest.
	 */
	@Test
	public void testRecursionWithMultipleRegionsEvenArrayHigherToLowerToBorder()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 1, 10));
		inputRegions.add(new Region("1", 11, 20));
		inputRegions.add(new Region("1", 21, 40));
		inputRegions.add(new Region("1", 41, 60));
		inputRegions.add(new Region("1", 61, 80)); // border
		inputRegions.add(new Region("1", 81, 120)); // border
		inputRegions.add(new Region("1", 121, 160));
		inputRegions.add(new Region("1", 161, 200));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Generate expected recursion positions. Each position represents a round of recursion.
		int[] expectedLows =
		{ 0, 4, 4 };
		int[] expectedMiddles =
		{ 4, 6, 5 };
		int[] expectedHighs =
		{ 7, 7, 5 };

		// Sublist of input should be returned.
		expectedOutputGroups = inputRegions.subList(5, 8);

		// Executes and runs comparisons.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		compareRecursionPositions(stringWriter.toString(), expectedLows, expectedMiddles, expectedHighs);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test where the first round of recursion takes lower half (resulting in uneven subset), the next round takes the
	 * higher half (resulting in an even subset of 2 elements) which are the border case of interest.
	 */
	@Test
	public void testRecursionWithMultipleRegionsUnEvenArrayLowerToHigherToBorder()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 1, 20));
		inputRegions.add(new Region("1", 21, 80)); // border
		inputRegions.add(new Region("1", 81, 120)); // border
		inputRegions.add(new Region("1", 121, 160));
		inputRegions.add(new Region("1", 161, 200));
		inputRegions.add(new Region("1", 201, 240));
		inputRegions.add(new Region("1", 241, 280));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Generate expected recursion positions. Each position represents a round of recursion.
		int[] expectedLows =
		{ 0, 0, 1 };
		int[] expectedMiddles =
		{ 3, 1, 2 };
		int[] expectedHighs =
		{ 6, 2, 2 };

		// Sublist of input should be returned.
		expectedOutputGroups = inputRegions.subList(2, 5);

		// Executes and runs comparisons.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		compareRecursionPositions(stringWriter.toString(), expectedLows, expectedMiddles, expectedHighs);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Test where the first round of recursion takes higher half (resulting in even subset), the next round takes the
	 * lower half (resulting in an even subset of 2 elements) which are the border case of interest.
	 */
	@Test
	public void testRecursionWithMultipleRegionsUnEvenArrayHigherToLowerToBorder()
	{
		// Prepares/executes region with record matching.
		inputRegions.add(new Region("1", 11, 20));
		inputRegions.add(new Region("1", 21, 40));
		inputRegions.add(new Region("1", 41, 60));
		inputRegions.add(new Region("1", 61, 80)); // border
		inputRegions.add(new Region("1", 81, 120)); // border
		inputRegions.add(new Region("1", 121, 160));
		inputRegions.add(new Region("1", 161, 200));
		grouper = new SamRecordGroupsRetriever(builder.addAll(inputRegions).build());

		// Generate expected recursion positions. Each position represents a round of recursion.
		int[] expectedLows =
		{ 0, 3, 3 };
		int[] expectedMiddles =
		{ 3, 5, 4 };
		int[] expectedHighs =
		{ 6, 6, 4 };

		// Sublist of input should be returned.
		expectedOutputGroups = inputRegions.subList(4, 7);

		// Executes and runs comparisons.
		List<Region> actualOutputGroups = grouper.retrieveGroupsWithinRange(record1);
		compareRecursionPositions(stringWriter.toString(), expectedLows, expectedMiddles, expectedHighs);
		Assert.assertEquals(actualOutputGroups, expectedOutputGroups);
	}

	/**
	 * Generates a new {@link SAMRecord} that can be used for testing.
	 * 
	 * @param recordContig
	 *            {@link String} - If {@code null}, {@link SAMRecord#setReferenceName(String)} will not be done.
	 * @param recordStart
	 *            {@link Integer} - If {@code null}, {@link SAMRecord#setAlignmentStart(String)} will not be done.
	 * @param recordCigar
	 *            {@link String} - If {@code null}, {@link SAMRecord#setCigarString(String)} will not be done.
	 * @param recordSeqs
	 *            1 or more {@link SAMSequenceRecord} - Used for the {@link SAMFileHeader}{@code 's}
	 *            {@link SAMSequenceDictionary}.
	 * @return {@link SAMRecord}
	 */
	private SAMRecord generateTestRecord(String recordReferenceName, Integer recordStart, String recordCigar,
			SAMSequenceRecord... recordSeqs)
	{
		// Creates header objects.
		SAMFileHeader header = new SAMFileHeader();
		SAMSequenceDictionary seqDict = new SAMSequenceDictionary();

		// Adds/sets sequence records.
		for (int i = 0; i < recordSeqs.length; i++)
		{
			seqDict.addSequence(recordSeqs[i]);
		}
		header.setSequenceDictionary(seqDict);

		// Creates SAMRecord.
		SAMRecord record = new SAMRecord(header);
		if (recordReferenceName != null) record.setReferenceName(recordReferenceName);
		if (recordStart != null) record.setAlignmentStart(recordStart);
		if (recordCigar != null) record.setCigarString(recordCigar);

		return record;
	}

	/**
	 * Compares the debug message containing the recursion positions to what is expected. Assumes debug messages about
	 * recursion positions start with "DEBUG - Entered recursion." and consist of a single line. Furthermore, it is
	 * assumed that these recursion position messages are in chronological order. Lines that start with something
	 * different are ignored.
	 * 
	 * @param debugOutput
	 *            {@link String}
	 * @param expectedLow
	 *            {@code int[]}
	 * @param expectedMiddle
	 *            {@code int[]}
	 * @param expectedHigh
	 *            {@code int[]}
	 */
	private void compareRecursionPositions(String debugOutput, int[] expectedLow, int[] expectedMiddle,
			int[] expectedHigh)
	{
		// Recursion counter.
		int counter = 0;

		// Goes through each logger line and if it starts with "DEBUG - Entered recursion.", compares it to the expected
		// message at that recursion leve.
		for (String line : debugOutput.split(System.lineSeparator()))
		{
			if (line.startsWith("DEBUG - Entered recursion."))
			{
				Assert.assertEquals(line, "DEBUG - Entered recursion. Low=" + expectedLow[counter] + ", middle="
						+ expectedMiddle[counter] + ", high=" + expectedHigh[counter]);
				counter++;
			}
		}
	}
}
