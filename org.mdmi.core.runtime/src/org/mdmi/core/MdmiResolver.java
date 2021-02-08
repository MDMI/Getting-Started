/*******************************************************************************
* Copyright (c) 2012, 2017, 2018 MDIX Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     MDIX Inc - initial API and implementation
*
* Author:
*     Gabriel Oancea
*
*******************************************************************************/
package org.mdmi.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//import org.apache.xml.utils.TreeWalker;
import org.mdmi.MessageGroup;
import org.mdmi.MessageModel;
import org.mdmi.core.Mdmi.MapInfo;
import org.mdmi.core.engine.SimplifiedSemanticParser;
import org.mdmi.util.MDMIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Resolver for MDMI maps, syntax and semantic parsers. It loads and parses the maps, and the parsers. This
 * implementation caches all message groups.
 */
public class MdmiResolver {

	private static Logger logger = LoggerFactory.getLogger(MdmiResolver.class);

	static protected HashMap<String, MI> themaps = new HashMap<String, MI>();

	public synchronized HashMap<String, MI> getMaps() {
		return themaps;
	}

	public static class Map {

		/**
		 * @param name
		 * @param display
		 */
		public Map(String name, String display) {
			super();
			this.name = name;
			this.display = display;
		}

		String name;

		String display;

		/**
		 * @return the family
		 */

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the display
		 */
		public String getDisplay() {
			return display;
		}

		/**
		 * @param display
		 *            the display to set
		 */
		public void setDisplay(String display) {
			this.display = display;
		}

	};

	public String getActiveMaps() {
		return getActiveMaps(null);
	}

	public String getActiveMaps(ArrayList<String> filter) {
		ArrayList<Map> maps = new ArrayList<Map>();
		for (String key : Mdmi.INSTANCE().getResolver().getMaps().keySet()) {
			if (filter == null || (filter != null && filter.contains(key))) {
				MI mi = Mdmi.INSTANCE().getResolver().getMaps().get(key);
				for (MessageModel mm : mi.messageGroup.getModels()) {
					if (mm.getDescription() != null) {
						maps.add(new Map(key, mm.getDescription()));
					} else {
						maps.add(new Map(key, key));
					}
				}
			}
		}
		Gson gsonBuilder = new GsonBuilder().create();

		return gsonBuilder.toJson(maps);

	}

	/**
	 * Create a new instance and resolve the MDMI maps specified in the configuration file.
	 *
	 * @param config
	 *            Configuration data, used to locate the MDMI maps and providers.
	 */
	public MdmiResolver() {

	}

	/**
	 * Resolve (load and parse) the MDMI maps specified in the configuration file.
	 *
	 * @param config
	 *            Configuration data, used to locate the MDMI maps and providers.
	 */
	// public void resolveConfig(MdmiConfig config) {
	// Collection<MdmiConfig.MapInfo> mes = config.getAllMapInfos();
	// for (Iterator<MdmiConfig.MapInfo> iterator = mes.iterator(); iterator.hasNext();) {
	// MdmiConfig.MapInfo me = iterator.next();
	// resolveOne(me);
	// }
	// }

	static HashMap<String, List<MessageGroup>> loadedGroups = new HashMap<String, List<MessageGroup>>();

	private InputStream createInputStreamfromURI(String uri) {

		File file = new File(uri);

		try {
			FileInputStream fileStream = new FileInputStream(file);
			return new BufferedInputStream(fileStream);
		} catch (FileNotFoundException exc) {
			throw new MdmiException(exc, "MapBuilder: file {0} not found!", file.getAbsolutePath());
		} catch (Throwable t) {
			throw new MdmiException(t, "MapBuilder: file {0} not found!", file.getAbsolutePath());
		}
	}

	public void resolve(InputStream inputStream) throws IOException {

		synchronized (themaps) {

			MessageGroup messageGroup = MDMIUtil.load(inputStream);

			// for (MessageGroup messageGroup : mgs) {
			logger.debug("Loaded message group " + messageGroup.getName());
			for (MessageModel messageModel : messageGroup.getModels()) {
				logger.debug("Loaded message model " + messageModel.getMessageModelName());
				MapInfo mapInfo = new MapInfo();
				mapInfo.mapName = createKey(messageGroup.getName(), messageModel.getMessageModelName());
				MI mi = new MI(mapInfo, messageGroup);
				themaps.put(createKey(messageGroup.getName(), messageModel.getMessageModelName()), mi);
			}
		}
		// MapInfo mapInfo = new MapInfo();
		// // mapInfo.mapFileName;
		// mapInfo.mapName = mg.getName();
		//
		// MI mi = new MI(mapInfo, mg);
		//
		// logger.debug("Loaded message group " + mg.getName());
		// // if (m_maps.containsKey(mi.messageGroup.getName())) {
		// // m_maps.remove(mi.messageGroup.getName());
		// // }
		// m_maps.put(mi.messageGroup.getName() + mi.messageGroup.getName(), mi);
		// }

	}

	/**
	 * Resolve (load and parse) the specified MDMI map.
	 *
	 * @param mapInfo
	 *            The map info structure containing map location information.
	 * @return The message groups in the given map, if it is valid.
	 */
	public void resolveOne(Mdmi.MapInfo mapInfo) {

		synchronized (themaps) {
			MessageGroup mg = MDMIUtil.load(mapInfo.mapFileName);
			MI mi = new MI(mapInfo, mg);
			logger.debug("Loaded message group " + mg.getName());

			for (MessageModel mm : mg.getModels()) {
				String s = createKey(mg.getName(), mm.getMessageModelName());
				logger.debug("Loaded message model " + s);
				themaps.put(s, mi);
			}
		}

	}

	public String createKey(String group, String model) {
		return group + "." + model;
	}

	/**
	 * Get the requested message model by group and message name.
	 *
	 * @param messageGroup
	 *            The message group name.
	 * @param messageModel
	 *            The message model name.
	 * @return The model if found, null otherwise.
	 */
	public MessageModel getModel(String messageGroup, String messageModel) {
		// if (messageGroup == null || messageModel == null) {
		// throw new IllegalArgumentException("Null argument");
		// }
		synchronized (themaps) {
			MI mi = themaps.get(createKey(messageGroup, messageModel));
			if (mi == null) {
				logger.error("cant find " + createKey(messageGroup, messageModel));
				return null;
			}
			return mi.messageGroup.getModel(messageModel);
		}
	}

	/**
	 * Get the requested message group by group name.
	 *
	 * @param messageGroup
	 *            The message group name.
	 * @return The message group if found, null otherwise.
	 */
	// public MessageGroup getMessageGroup(String messageGroup) {
	// if (messageGroup == null) {
	// throw new IllegalArgumentException("Null argument");
	// }
	// MI mi = m_maps.get(messageGroup);
	// if (mi == null) {
	// return null;
	// }
	// return mi.messageGroup;
	// }

	/**
	 * Get the message group for the specified MdmiValueSetsHandler.
	 *
	 * @param vsh
	 *            The value set handler.
	 * @return The message group if found, null otherwise.
	 */
	public MessageGroup getMessageGroup(MdmiValueSetsHandler vsh) {

		synchronized (themaps) {
			if (vsh == null) {
				throw new IllegalArgumentException("Null argument");
			}
			Collection<MI> c = themaps.values();
			for (Iterator<MI> it = c.iterator(); it.hasNext();) {
				MI mi = it.next();
				if (mi.valueSetsHandler == vsh) {
					return mi.messageGroup;
				}
			}
			return null;
		}
	}

	/**
	 * Get a list of all known message groups.
	 *
	 * @return A list of all known message groups.
	 */
	public Collection<MessageGroup> getMessageGroups() {
		synchronized (themaps) {
			ArrayList<MessageGroup> a = new ArrayList<MessageGroup>();
			Collection<MI> c = themaps.values();
			for (Iterator<MI> it = c.iterator(); it.hasNext();) {
				MI mi = it.next();
				a.add(mi.messageGroup);
			}
			return a;
		}
	}

	/**
	 * Get the syntax parser for the specified message group.
	 *
	 * @param messageGroup
	 *            The message group name to look for.
	 * @return The syntax parser for the specified group, or null if the group is not found.
	 */
	public ISyntacticParser getSyntacticParser(String messageGroup, String messageModel) {
		synchronized (themaps) {
			if (messageGroup == null) {
				throw new IllegalArgumentException("Null argument");
			}
			MI mi = themaps.get(createKey(messageGroup, messageModel));
			if (mi == null) {
				return null;
			}
			return mi.getSyntaxParser();
		}
	}

	/**
	 * Get the value sets handler for the specified message group.
	 *
	 * @param messageGroup
	 *            The message group name to look for.
	 * @return The enumeration converter for the specified message group, or null if the group is not found.
	 */
	public MdmiValueSetsHandler getValueSetsHandler(String messageGroup, String messageModel) {
		synchronized (themaps) {
			if (messageGroup == null) {
				throw new IllegalArgumentException("Null argument");
			}

			MI mi = themaps.get(createKey(messageGroup, messageModel));
			if (mi == null) {
				return null;
			}
			return mi.valueSetsHandler;
		}
	}

	/**
	 * Get the semantic parser for the specified message group.
	 *
	 * @param messageGroup
	 *            The message group name to look for.
	 * @param string
	 * @return The semantic parser for the specified group, or null if the group is not found.
	 */
	public ISemanticParser getSemanticParser(String messageGroup, String messageModel) {
		synchronized (themaps) {
			if (messageGroup == null) {
				throw new IllegalArgumentException("Null argument");
			}
			MI mi = themaps.get(createKey(messageGroup, messageModel));
			if (mi == null) {
				return null;
			}
			return mi.getSemanticParser();
		}
	}

	// private void handleErrors(File f, List<ModelInfo> errors) {
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < errors.size(); i++) {
	// ModelInfo mi = errors.get(i);
	// sb.append(mi.getMessage());
	// sb.append("\n");
	// }
	// // TODO for now log the exceptions, we should throw on invalid maps!
	// // Mdmi.INSTANCE().logger().severe("MDMI map in file {0} has errors: {1}", f.getAbsolutePath(), sb.toString());
	// }
	//
	// private void handleWarnings(File f, List<ModelInfo> errors) {
	// StringBuffer sb = new StringBuffer();
	// for (int i = 0; i < errors.size(); i++) {
	// ModelInfo mi = errors.get(i);
	// sb.append(mi.getMessage());
	// sb.append("\n");
	// }
	// // Mdmi.INSTANCE().logger().severe("MDMI map in file {0} has warnings: {1}", f.getAbsolutePath(), sb.toString());
	// }

	public final class MI {
		Mdmi.MapInfo mapInfo;

		public MessageGroup messageGroup;

		ISemanticParser semanticSvcProvider;

		MdmiValueSetsHandler valueSetsHandler;

		public MI(Mdmi.MapInfo mapInfo, MessageGroup messageGroup) {
			this.mapInfo = mapInfo;
			this.messageGroup = messageGroup;
		}

		ISyntacticParser getSyntaxParser() {

			if (messageGroup != null) {
				if (messageGroup.getDefaultLocationExprLang() != null) {
					try {
						switch (messageGroup.getDefaultLocationExprLang()) {
							case "JSON":
								return new org.mdmi.core.engine.json.JsonSyntacticParser();
							case "HL7":
								return (ISyntacticParser) Class.forName(
									"org.openhealthtools.mdht.mdmiplugins.parsers.HL7Parser").newInstance();
							case "QUERY":
								return (ISyntacticParser) Class.forName(
									"org.mdmi.engine.parsers.QuerySyntaxParser").newInstance();
							default:
								// TreeWalker tw;
								return new org.mdmi.core.engine.xml.DOMSAXSyntacticParser(messageGroup.getName());
						}
					} catch (Exception e) {

					}

				}

			}

			return new org.mdmi.core.engine.xml.DOMSAXSyntacticParser(messageGroup.getName());

		}

		ISemanticParser getSemanticParser() {

			return new SimplifiedSemanticParser(messageGroup);

		}
	} // MdmiMapResolver$MT

	/**
	 * @param instance
	 */
	public void load(Mdmi instance) {
		Collection<Mdmi.MapInfo> mes = Mdmi.INSTANCE().getAllMapInfos();
		for (Iterator<Mdmi.MapInfo> iterator = mes.iterator(); iterator.hasNext();) {
			Mdmi.MapInfo me = iterator.next();
			resolveOne(me);
		}
	}
} // MdmiMapResolver
