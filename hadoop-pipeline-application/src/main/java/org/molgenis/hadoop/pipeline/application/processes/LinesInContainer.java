package org.molgenis.hadoop.pipeline.application.processes;

import java.util.ArrayList;

/**
 * Container for storing individual {@String}{@code s}.
 */
public class LinesInContainer implements InContainer
{
	/**
	 * Stores individual lines.
	 */
	private ArrayList<String> lines = new ArrayList<String>();

	/**
	 * Add a line to the container. If successful, returns true. If {@code item} could not be cast to a {@link String},
	 * returns false instead.
	 */
	@Override
	public boolean add(Object item)
	{
		try
		{
			lines.add((String) item);
			return true;
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}

	/**
	 * Retrieve an {@link ArrayList} containing the lines as {@link String}.
	 * 
	 * @return {@link ArrayList }{@code <}{@link String}{@code >}
	 */
	@Override
	public ArrayList<String> get()
	{
		return lines;
	}

	/**
	 * Returns the stored data as a single {@link String}. If {@code true} is given for {@code newlineSeperator}, a line
	 * separator will be placed between the individual data items within the returned String.
	 * 
	 * @param newlineSeperator
	 * @return {@link String}
	 */
	public String getAsString(boolean newlineSeperator)
	{
		StringBuilder sb = new StringBuilder();

		for (String line : lines)
		{
			sb.append(line);
			if (newlineSeperator)
			{
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

	/**
	 * Returns the stored data as a single {@link String} with added newlines between the different data items.
	 * 
	 * @return {@link String}
	 */
	@Override
	public String toString()
	{
		return getAsString(true);
	}

	@Override
	public void clear()
	{
		lines.clear();
	}
}
