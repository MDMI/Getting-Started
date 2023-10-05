package org.mdmi.core.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeServiceISOMain {

	private static Logger logger = LoggerFactory.getLogger(RuntimeServiceISOMain.class);

	public static void main(String[] args) {

		try {

			Options options = new Options();
			Option sourceMap = new Option("sourceMap", true, "source map");
			sourceMap.setRequired(true);
			options.addOption(sourceMap);
			Option sourceModel = new Option("sourceModel", true, "source map");
			sourceModel.setRequired(true);
			options.addOption(sourceModel);
			Option targetFolder = new Option("targetFolder", true, "source map");
			targetFolder.setRequired(true);
			options.addOption(targetFolder);
			Option targetMessage = new Option("targetMessage", true, "source map");
			targetMessage.setRequired(true);
			options.addOption(targetMessage);

			Option elements = new Option("elements", "elements");
			elements.setRequired(true);
			elements.setArgs(Option.UNLIMITED_VALUES);
			options.addOption(elements);

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			new HashMap<String, String>();
			new HashMap<String, String>();

			String result = RuntimeService.runTransformationISO(
				cmd.getOptionValue("sourceMap"), cmd.getOptionValue("sourceModel"), cmd.getOptionValues("elements"),
				cmd.getOptionValue("targetFolder"));

			logger.trace(result);

			Path targetMessageFile = Paths.get(cmd.getOptionValue("targetMessage"));
			Files.write(targetMessageFile, result.getBytes("UTF-8"));

		} catch (Exception e) {

		}

	}

}
