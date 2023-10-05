package org.mdmi.core.engine.preprocessors;

import java.util.ArrayList;

import org.mdmi.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurablePreProcessor implements IPreProcessor {

	private static Logger logger = LoggerFactory.getLogger(ConfigurablePreProcessor.class);

	String name;

	ArrayList<String> groups;

	Object arguments;

	public void setName(String name) {
		this.name = name;
	}

	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}

	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}

	@Override
	public boolean canProcess(MessageModel messageModel) {
		logger.trace("canProcess " + messageModel.getGroup().getName());
		if (groups.contains(messageModel.getGroup().getName())) {
			logger.trace("canProcess true");
			return true;
		}
		logger.trace("canProcess false");
		return false;
	}

	@Override
	public String getName() {
		return this.name;
	}
}
