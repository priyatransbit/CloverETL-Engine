/*
 *  jETeL/Clover - Java based ETL application framework.
 *  Copyright (C) 2004  David Pavlis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jetel.util;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetel.data.Defaults;
import org.jetel.graph.TransformationGraph;

/**
 *  Helper class for resolving references within string values to Properties´.<br>
 *  The reference is expected in form: ${..property_name..} <br> If property
 *  name is found in properties, then the whole ${...} substring is replaced by the property value.
 *
 * @author      dpavlis
 * @since       12. May 2004
 * @revision    $Revision$
 */
public class PropertyRefResolver {
	private Matcher regexMatcher;
	private Properties properties;


	/**Constructor for the PropertyRefResolver object */
	public PropertyRefResolver() {
		properties = TransformationGraph.getReference().getGraphProperties();
		if (properties != null) {
			Pattern pattern = Pattern.compile(Defaults.GraphProperties.PROPERTY_PLACEHOLDER_REGEX);
			regexMatcher = pattern.matcher("");
		}
	}


	/**
	 *Constructor for the PropertyRefResolver object
	 *
	 * @param  properties  Description of the Parameter
	 */
	public PropertyRefResolver(Properties properties) {
		this.properties = properties;
		if (this.properties != null) {
			Pattern pattern = Pattern.compile(Defaults.GraphProperties.PROPERTY_PLACEHOLDER_REGEX);
			regexMatcher = pattern.matcher("");
		}
	}


	/**
	 *  Adds a feature to the Properties attribute of the PropertyRefResolver object
	 *
	 * @param  properties  The feature to be added to the Properties attribute
	 */
	public void addProperties(Properties properties) {
		this.properties.putAll(properties);
	}


	/**
	 *  Looks for reference to global graph properties within string and
	 *  tries to resolve them - replace by the property's value.<br>
	 *  The format of reference 'call' is denoted by Defaults.GraphProperties.PROPERTY_PLACEHOLDER_REGEX -
	 *  usually in the form ${_property_name_}
	 *
	 * @param  value  String potentially containing one or more references to property
	 * @return        String with all references resolved
	 * @see           org.jetel.data.Defaults
	 */
	public String resolveRef(String value) {
		String reference;
		String resolvedReference;
		StringBuffer strBuf = new StringBuffer();
		if ((value != null) && (properties != null)) {
			regexMatcher.reset(value);
			while (regexMatcher.find()) {
				reference = regexMatcher.group(1);
				resolvedReference = properties.getProperty(reference);
				if (resolvedReference == null) {
					throw new RuntimeException("Can't resolve reference to graph property: " + reference);
				}
				regexMatcher.appendReplacement(strBuf, resolvedReference);
			}
			regexMatcher.appendTail(strBuf);
			return strBuf.toString();
		} else {
			return value;
		}
	}

	/*
	 *  Test/Debug code
	 *  public static void main(String args[]){
	 *  Properties prop=new Properties();
	 *  try{
	 *  InputStream inStream = new BufferedInputStream(new FileInputStream(args[0]));
	 *  prop.load(inStream);
	 *  }catch(IOException ex){
	 *  ex.printStackTrace();
	 *  }
	 *  PropertyRefResolver attr=new PropertyRefResolver(prop);
	 *  System.out.println("DB driver is: '{${dbDriver}}' ...");
	 *  System.out.println(attr.resolvePropertyRef("DB driver is: '{${dbDriver}}' ..."));
	 *  System.out.println("${user} is user");
	 *  System.out.println(attr.resolvePropertyRef("${user} is user"));
	 *  System.out.println("${usr}/${password} is user/password");
	 *  System.out.println(attr.resolvePropertyRef("${usr}/${password} is user/password"));
	 *  }
	 */
}

