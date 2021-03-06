package org.molgenis.hadoop.pipeline.application.cachedigestion;

import java.io.IOException;
import java.util.Map;

import org.molgenis.hadoop.pipeline.application.Tester;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXParseException;

import htsjdk.samtools.SAMProgramRecord;

/**
 * Tester for the {@link HadoopXmlReader}.
 */
public class HadoopToolsXmlReaderTester extends Tester
{
	/**
	 * The reader that is being tested.
	 */
	private HadoopToolsXmlReader reader;

	/**
	 * Creates a {@link HadoopToolsXmlReader} needed for testing.
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public void beforeClass() throws IOException
	{
		reader = new HadoopToolsXmlReader();
	}

	/**
	 * Tests when an xml is given that contains a single tool node, which is valid.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSingleValidTool() throws IOException
	{
		Map<String, SAMProgramRecord> tools = reader
				.read(getClassLoader().getResource("tools_archive_xml_files/single_valid_tool.xml").getFile());

		Assert.assertEquals(tools.containsKey("bwa"), true);
		Assert.assertEquals(tools.get("bwa").getId(), "bwa");
		Assert.assertEquals(tools.get("bwa").getProgramName(), "bwa");
		Assert.assertEquals(tools.get("bwa").getProgramVersion(), "0.7.12-r1039");
	}

	/**
	 * Tests when an xml is given that contains a single tool node with optional information, which is valid.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSingleValidToolWithOptionalInfo() throws IOException
	{
		Map<String, SAMProgramRecord> tools = reader.read(getClassLoader()
				.getResource("tools_archive_xml_files/single_valid_tool_with_optional_info.xml").getFile());

		Assert.assertEquals(tools.containsKey("bwa"), true);
		Assert.assertEquals(tools.get("bwa").getId(), "bwa");
		Assert.assertEquals(tools.get("bwa").getProgramName(), "bwa");
		Assert.assertEquals(tools.get("bwa").getProgramVersion(), "0.7.12-r1039");
	}

	/**
	 * Tests when an xml is given that contains a multiple tool nodes, all of them being valid.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMultipleValidTools() throws IOException
	{
		Map<String, SAMProgramRecord> tools = reader
				.read(getClassLoader().getResource("tools_archive_xml_files/multiple_valid_tools.xml").getFile());

		Assert.assertEquals(tools.containsKey("tool.sh"), true);
		Assert.assertEquals(tools.get("tool.sh").getId(), "id1");
		Assert.assertEquals(tools.get("tool.sh").getProgramName(), "myTool");
		Assert.assertEquals(tools.get("tool.sh").getProgramVersion(), "8.88");

		Assert.assertEquals(tools.containsKey("program.exe"), true);
		Assert.assertEquals(tools.get("program.exe").getId(), "id2");
		Assert.assertEquals(tools.get("program.exe").getProgramName(), "another tool name");
		Assert.assertEquals(tools.get("program.exe").getProgramVersion(), "revision42");
	}

	/**
	 * Tests when an xml is given that contains a no tool nodes.
	 * 
	 * @throws Throwable
	 */
	@Test(expectedExceptions = SAXParseException.class)
	public void testNoToolPresent() throws Throwable
	{
		try
		{
			reader.read(getClassLoader().getResource("tools_archive_xml_files/no_tools_present.xml").getFile());
		}
		catch (IOException e)
		{
			// Retrieve the underlying exception.
			throw e.getCause();
		}
	}

	/**
	 * Tests when a xml is given that contains a single tool node, which is missing a required child node.
	 * 
	 * @throws Throwable
	 */
	@Test(expectedExceptions = SAXParseException.class)
	public void testSingleToolMissingChild() throws Throwable
	{
		try
		{
			reader.read(
					getClassLoader().getResource("tools_archive_xml_files/single_tool_missing_child.xml").getFile());
		}
		catch (IOException e)
		{
			// Retrieve the underlying exception.
			throw e.getCause();
		}

	}

	/**
	 * Tests when a xml is given that contains a single tool node, which is missing a required node attribute.
	 * 
	 * @throws Throwable
	 */
	@Test(expectedExceptions = SAXParseException.class)
	public void testSingleToolMissingAttribute() throws Throwable
	{
		try
		{
			reader.read(getClassLoader().getResource("tools_archive_xml_files/single_tool_missing_attribute.xml")
					.getFile());
		}
		catch (IOException e)
		{
			// Retrieve the underlying exception.
			throw e.getCause();
		}
	}

	/**
	 * Tests when a xml is given that contains a single tool node, which is missing a required child node in an optional
	 * child node (so this required subchild is only required when the optional child is present).
	 * 
	 * @throws Throwable
	 */
	@Test(expectedExceptions = SAXParseException.class)
	public void testSingleToolMissingSubChild() throws Throwable
	{
		try
		{
			reader.read(
					getClassLoader().getResource("tools_archive_xml_files/single_tool_missing_subchild.xml").getFile());
		}
		catch (IOException e)
		{
			// Retrieve the underlying exception.
			throw e.getCause();
		}
	}
}
