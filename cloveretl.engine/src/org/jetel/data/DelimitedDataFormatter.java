/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002  David Pavlis
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version.
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

// FILE: c:/projects/jetel/org/jetel/data/DelimitedDataFormatter.java

package org.jetel.data;
import java.io.*;
import org.jetel.metadata.*;
/**
 * Outputs delimited data record. Handles encoding of characters.
 *
 * @author     D.Pavlis
 * @since    March 28, 2002
 * @see        OtherClasses
 */
public class DelimitedDataFormatter implements DataFormatter {

	// Attributes
	private DataRecordMetadata metadata;
	private PrintStream writer;
	private String delimiters[];
	private String encoding;

	private final static int OUTPUT_STREAM_BUFFER_SIZE = 2048; // size of the buffer
	private final static String DEFAULT_CHARSET_ENCODING = "ISO-8859-1";	
	// Associations

	// Operations
	
	public DelimitedDataFormatter(){
		encoding=null;
	}
	
	public DelimitedDataFormatter(String encoding){
		this.encoding=encoding;
	}
	
	/**
	 *  Description of the Method
	 *
	 * @param  out        Description of Parameter
	 * @param  _metadata  Description of Parameter
	 * @since             March 28, 2002
	 */
	public void open(OutputStream out, DataRecordMetadata _metadata) {
		this.metadata = _metadata;

		// create buffered input stream reader 
		if (encoding!=null){
			try{
				writer = new PrintStream(new BufferedOutputStream(out,OUTPUT_STREAM_BUFFER_SIZE),false,encoding);
			}catch(UnsupportedEncodingException ex){
				throw new RuntimeException(ex);
			}
		}else{
			writer = new PrintStream(new BufferedOutputStream(out,OUTPUT_STREAM_BUFFER_SIZE),false);
		}
		
		// create array of delimiters & initialize them
		delimiters = new String[metadata.getNumFields()];
		for (int i = 0; i < metadata.getNumFields(); i++) {
			delimiters[i] = metadata.getField(i).getDelimiter();
		}

	}


	/**
	 *  Description of the Method
	 *
	 * @since    March 28, 2002
	 */
	public void close() {
		writer.close();
	}


	/**
	 *  Description of the Method
	 *
	 * @since    March 28, 2002
	 */
	public void flush() {
		writer.flush();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  record           Description of Parameter
	 * @exception  IOException  Description of Exception
	 * @since                   March 28, 2002
	 */
	public void write(DataRecord record) throws IOException {
		String str;
		for (int i = 0; i < metadata.getNumFields(); i++) {
			str=record.getField(i).toString();
			writer.print(str);
			writer.print(delimiters[i]);
		}
	}

}
/*
 *  end class DelimitedDataFormatter
 */

