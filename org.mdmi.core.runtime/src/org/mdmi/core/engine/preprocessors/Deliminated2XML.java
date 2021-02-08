/*******************************************************************************
 * Copyright (c) 2018 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package org.mdmi.core.engine.preprocessors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.mdmi.Bag;
import org.mdmi.MessageModel;
import org.mdmi.core.MdmiMessage;

/**
 * @author seanmuir
 *
 */
public class Deliminated2XML implements IPreProcessor {

	// private static final String CSV2XML = "CSV2XML";

	private String name;

	private String delim;

	/**
	 * @param name
	 * @param delim
	 */
	public Deliminated2XML(String name, String delim) {
		super();
		this.name = name;
		this.delim = delim;

		// ParseException asdfasfd;

		// EcoreResourceFactoryImpl asdf;

		// TreeWalker l;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.IPreProcessor#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.IPreProcessor#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {
		if (messageModel != null && messageModel.getGroup() != null) {
			if (name.equals(messageModel.getGroup().getDefaultFormatExpressionLanguage())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.IPreProcessor#processMessage(org.mdmi.core.MdmiMessage)
	 */
	@Override
	public void processMessage(MessageModel messageModel, MdmiMessage message) {

		try {
			Reader inputString = new StringReader(message.getDataAsString());
			BufferedReader inputReader = new BufferedReader(inputString);
			List<String> lines = new ArrayList<>();
			for (;;) {
				String line;
				line = inputReader.readLine();
				if (line == null)
					break;
				lines.add(line);
			}
			if (messageModel.getSyntaxModel() != null && messageModel.getSyntaxModel().getRoot() instanceof Bag) {
				Bag rootBag = (Bag) messageModel.getSyntaxModel().getRoot();
				if (!rootBag.getNodes().isEmpty()) {
					String root = rootBag.getLocation();
					String element = rootBag.getNodes().get(0).getLocation();
					message.setData(toXML(lines, delim, root, element).getBytes());
				}

			}

		} catch (IOException e) {
			/**
			 * @TODO Add proper exception handling
			 */
		}

	}

	private String toXML(List<String> inputLines, String delim, String root, String elementName) {
		List<String> header = new ArrayList<>(
			Arrays.asList(inputLines.get(0).replaceAll("\"", "").split(delim))).stream().map(String::trim).collect(
				Collectors.toList());

		String output = "<" + root + ">" + inputLines.stream().skip(1).map(line -> {
			List<String> cells = Arrays.asList(line.split(delim));
			return "<" + elementName + ">" +
					IntStream.range(0, cells.size()).mapToObj(
						i -> "<" + header.get(i) + ">" + cells.get(i).replaceAll("\"", "") + "</" + header.get(i) +
								">").collect(Collectors.joining()) +
					"</" + elementName + ">";
		}).collect(Collectors.joining(System.lineSeparator())).replaceAll("&", "&amp;") + "</" + root + ">";
		return "<?xml version=\"1.0\" ?>" + System.lineSeparator() + output + System.lineSeparator();
	}

}
