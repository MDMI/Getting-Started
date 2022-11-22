package org.mdmi.core.engine.preprocessors;

import java.util.ArrayList;

import org.mdmi.MessageModel;

public abstract class ConfigurablePreProcessor implements IPreProcessor {

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
		if (groups.contains(messageModel.getGroup().getName())) {
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return this.name;
	}
}
