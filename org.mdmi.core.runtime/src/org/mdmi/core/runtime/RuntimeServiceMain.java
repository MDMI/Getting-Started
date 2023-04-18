package org.mdmi.core.runtime;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.mdmi.core.engine.terminology.FHIRTerminologyTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeServiceMain {

	private static Logger logger = LoggerFactory.getLogger(RuntimeServiceMain.class);

	public static void main(String[] args) {

		Properties p = System.getProperties();
		Enumeration<Object> keys = p.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = (String) p.get(key);
			logger.trace(key + ": " + value);
		}

		for (String arg : args) {
			logger.trace(arg);
		}

		Options options = new Options();
		Option sourceMap = new Option("sourceMap", true, "Source File Path");
		sourceMap.setArgName("pathToFile");
		sourceMap.setRequired(true);
		options.addOption(sourceMap);
		Option sourceModel = new Option("sourceModel", true, "source map");
		sourceModel.setRequired(true);
		sourceModel.setArgName("messagegroupname.messagemodelname");
		options.addOption(sourceModel);
		Option targetMap = new Option("targetMap", true, "source map");
		targetMap.setRequired(true);
		targetMap.setArgName("pathToFile");
		options.addOption(targetMap);
		Option targetModel = new Option("targetModel", true, "source map");
		targetModel.setRequired(true);
		options.addOption(targetModel);
		targetModel.setArgName("messagegroupname.messagemodelname");
		Option sourceMessage = new Option("sourceMessage", true, "source map");
		sourceMessage.setRequired(true);
		sourceMessage.setArgName("pathToFile");
		options.addOption(sourceMessage);
		Option targetFolder = new Option("targetFolder", true, "source map");
		targetFolder.setRequired(true);
		targetFolder.setArgName("pathToFolder");
		options.addOption(targetFolder);
		Option targetFormat = new Option("targetFormat", true, "targetFormat map");
		targetFormat.setRequired(true);
		targetFormat.setArgName("XML|JSON|???");
		options.addOption(targetFormat);

		Option useTerminology = new Option("useTerminology", true, "useTerminology");
		useTerminology.setRequired(false);
		useTerminology.setArgName("true|false");
		options.addOption(useTerminology);

		Option terminologyUrl = new Option("terminologyUrl", true, "terminologyUrl");
		useTerminology.setRequired(false);
		useTerminology.setArgName("url");
		options.addOption(terminologyUrl);

		Option terminologyUserName = new Option("terminologyUserName", true, "terminologyUserName");
		terminologyUserName.setRequired(false);
		terminologyUserName.setArgName("url");
		options.addOption(terminologyUserName);

		Option terminologyPassword = new Option("terminologyPassword", true, "terminologyPassword");
		terminologyPassword.setRequired(false);
		terminologyPassword.setArgName("url");
		options.addOption(terminologyPassword);

		try {

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = null;
			try {
				cmd = parser.parse(options, args);
			} catch (ParseException pe) {

				final PrintWriter writer = new PrintWriter(System.out);
				final HelpFormatter usageFormatter = new HelpFormatter();
				Comparator<Option> comparator = new Comparator<>() {

					@Override
					public int compare(Option o1, Option o2) {
						if (o1.getOpt().startsWith("source") && o2.getOpt().startsWith("target")) {
							return -1;
						}

						if (o2.getOpt().startsWith("source") && o1.getOpt().startsWith("target")) {
							return 1;
						}

						Integer one = null;
						Integer two = null;
						if (o1.getOpt().endsWith("Message")) {
							one = new Integer(3);
						}

						if (o1.getOpt().endsWith("Model")) {
							one = new Integer(2);
						}

						if (o1.getOpt().endsWith("Map")) {
							one = new Integer(1);
						}

						if (o1.getOpt().endsWith("Folder")) {
							one = new Integer(4);
						}

						if (o1.getOpt().endsWith("Format")) {
							one = new Integer(3);
						}

						if (o2.getOpt().endsWith("Message")) {
							two = new Integer(3);
						}

						if (o2.getOpt().endsWith("Model")) {
							two = new Integer(2);
						}

						if (o2.getOpt().endsWith("Map")) {
							two = new Integer(1);
						}

						if (o2.getOpt().endsWith("Folder")) {
							two = new Integer(4);
						}

						if (o2.getOpt().endsWith("Format")) {
							two = new Integer(3);
						}

						return one.compareTo(two);
					}
				};
				usageFormatter.setOptionComparator(comparator);
				usageFormatter.printUsage(writer, 80, "org.mdmi.core.runtime.RuntimeServiceMain", options);
				writer.close();
				return;
			}

			HashMap<String, String> sourceProperties = new HashMap<>();
			HashMap<String, String> targetProperties = new HashMap<>();

			String useTerminologyArg = cmd.getOptionValue("useTerminology");

			logger.trace("useTerminology " + useTerminologyArg);

			if (!StringUtils.isEmpty(useTerminologyArg) && "true".equalsIgnoreCase(useTerminologyArg)) {
				org.mdmi.core.engine.terminology.FHIRTerminologyTransform.processTerminology = true;
			} else {
				org.mdmi.core.engine.terminology.FHIRTerminologyTransform.processTerminology = false;
			}

			String terminologyUrlArg = cmd.getOptionValue("terminologyUrl");
			String terminologyUserNameArg = cmd.getOptionValue("terminologyUserName");
			String terminologyPasswordArg = cmd.getOptionValue("terminologyPassword");

			logger.trace("terminologyUrl " + terminologyUrlArg);

			if (FHIRTerminologyTransform.processTerminology && !StringUtils.isEmpty(terminologyUrlArg) &&
					"true".equalsIgnoreCase(useTerminologyArg)) {
				FHIRTerminologyTransform.setFHIRTerminologyURL(terminologyUrlArg);

				if (!StringUtils.isEmpty(terminologyUserNameArg) && !StringUtils.isEmpty(terminologyPasswordArg)) {
					FHIRTerminologyTransform.setUserName(terminologyUserNameArg);
					FHIRTerminologyTransform.setPassword(terminologyPasswordArg);
				}
			}

			logger.trace("USE TERMINOLOGY " + FHIRTerminologyTransform.processTerminology);

			String result = RuntimeService.runTransformation(
				cmd.getOptionValue("sourceMap"), cmd.getOptionValue("sourceModel"), cmd.getOptionValue("sourceMessage"),
				cmd.getOptionValue("targetMap"), cmd.getOptionValue("targetModel"), cmd.getOptionValue("targetFolder"),
				sourceProperties, targetProperties);

			logger.trace(result);

			Path targetMessageFile = Paths.get(
				cmd.getOptionValue("targetFolder") + "/result." + cmd.getOptionValue("targetFormat"));
			Files.write(targetMessageFile, result.getBytes("UTF-8"));

		} catch (Exception e) {

		}

	}

}
