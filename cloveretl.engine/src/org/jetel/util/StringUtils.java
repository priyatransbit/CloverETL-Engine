/*
 *  jETeL/Clover - Java based ETL application framework.
 *  Copyright (C) 2002  David Pavlis
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

/**
 *  Helper class with some useful methods regarding string/text manipulation
 *
 *@author     dpavlis
 *@since      July 25, 2002
 */

public class StringUtils {

	/**
	 *  Converts control characters into textual representation<br>
	 *  Note: This code handles only \n, \r and \t special chars
	 *
	 *@param  controlString  string containing control characters
	 *@return                string where control characters are replaced by their
	 *      text representation (i.e.\n -> "\n" )
	 *@since                 July 25, 2002
	 */
	public static String specCharToString(String controlString) {
		StringBuffer copy = new StringBuffer();
		char character;
		for (int i = 0; i < controlString.length(); i++) {
			character = controlString.charAt(i);
			switch (character) {
				case '\n':
					copy.append("\\n");
					break;
				case '\t':
					copy.append("\\t");
					break;
				case '\r':
					copy.append("\\r");
					break;
				default:
					copy.append(character);
			}
		}
		return copy.toString();
	}


	/**
	 *  Converts textual representation of control characters into control
	 *  characters<br>
	 *  Note: This code handles only \n, \r and \t special chars
	 *
	 *@param  controlString  Description of the Parameter
	 *@return                String with control characters
	 *@since                 July 25, 2002
	 */
	public static String stringToSpecChar(String controlString) {
		StringBuffer copy = new StringBuffer();
		char character;
		boolean isBackslash=false;
		for (int i = 0; i < controlString.length(); i++) {
			character = controlString.charAt(i);
			if (isBackslash){
				switch(character){
					case '\\': copy.append('\\');
					break;
					case 'n' : copy.append('\n');
					break;
					case 't' : copy.append('\t');
					break;
					case 'r' : copy.append('\r');
					break;
					default: copy.append('\\'); copy.append(character);
					break;
				}
			     isBackslash=false;
			}else{
				if (character=='\\')
					isBackslash=true;
				else
					copy.append(character);
			}
		}
		return copy.toString();
	}


	/**
	 *  Formats string from specified messages and their lengths.<br>
	 *  Negative (&lt;0) length means justify to the left, positive (&gt;0) to the right<br>
	 *  If message is longer than specified size, it is trimmed; if shorter, it is padded with blanks.
	 *
	 *@param  messages  array of objects with toString() implemented methods
	 *@param  sizes     array of desired lengths (+-) for every message specified
	 *@return           Formatted string
	 */
	public static String formatString(Object[] messages, int[] sizes) {
		int formatSize;
		int stringStart;
		int counter;
		String message;
		StringBuffer strBuf = new StringBuffer(100);
		for (int i = 0; i < messages.length; i++) {
			message = messages[i].toString();
			// left or right justified ?
			if (sizes[i]<0){
				formatSize=sizes[i]*(-1);
				fillString(strBuf,message,0,formatSize);
			}else{
				formatSize=sizes[i];
				if (message.length()<formatSize){
					fillBlank(strBuf,formatSize-message.length());
					fillString(strBuf,message,0,message.length());
				}else{
					fillString(strBuf,message,0,formatSize);
				}
			}
		}
		return strBuf.toString();
	}
	
	private static void fillString(StringBuffer strBuf,String source,int start,int length){
		int srcLength=source.length();
		for(int i=start;i<start+length;i++){
			if (i<srcLength){
				strBuf.append(source.charAt(i));
			}else{
				strBuf.append(' ');
			}
		}
	}
	
	private static void fillBlank(StringBuffer strBuf,int length){
		for(int i=0;i<length;i++){
			strBuf.append(' ');
		}
	}

}
/*
 *  End class StringUtils
 */

