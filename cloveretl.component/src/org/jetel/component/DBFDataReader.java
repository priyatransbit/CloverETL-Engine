/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.data.DataRecord;
import org.jetel.data.DataRecordFactory;
import org.jetel.data.Defaults;
import org.jetel.database.dbf.DBFDataParser;
import org.jetel.exception.AttributeNotFoundException;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.ConfigurationProblem;
import org.jetel.exception.ConfigurationStatus;
import org.jetel.exception.ConfigurationStatus.Priority;
import org.jetel.exception.ConfigurationStatus.Severity;
import org.jetel.exception.IParserExceptionHandler;
import org.jetel.exception.ParserExceptionHandlerFactory;
import org.jetel.exception.PolicyType;
import org.jetel.exception.XMLConfigurationException;
import org.jetel.graph.Node;
import org.jetel.graph.Result;
import org.jetel.graph.TransformationGraph;
import org.jetel.metadata.DataFieldMetadata;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.ExceptionUtils;
import org.jetel.util.MultiFileReader;
import org.jetel.util.SynchronizeUtils;
import org.jetel.util.property.ComponentXMLAttributes;
import org.jetel.util.property.RefResFlag;
import org.jetel.util.string.StringUtils;
import org.w3c.dom.Element;

/**
 *  <h3>dBase Table/Data Reader Component</h3>
 *
 * <!-- Parses specified input data file (in form of dBase table) and broadcasts the records to all connected out ports -->
 *
 * <table border="1">
 *  <th>Component:</th>
 * <tr><td><h4><i>Name:</i></h4></td>
 * <td>DBFDataReader</td></tr>
 * <tr><td><h4><i>Category:</i></h4></td>
 * <td></td></tr>
 * <tr><td><h4><i>Description:</i></h4></td>
 * <td>Reads records from specified dBase data file and broadcasts the records to all connected out ports.</td></tr>
 * <tr><td><h4><i>Inputs:</i></h4></td>
 * <td></td></tr>
 * <tr><td><h4><i>Outputs:</i></h4></td>
 * <td>At least one output port defined/connected.</td></tr>
 * <tr><td><h4><i>Comment:</i></h4></td>
 * <td>This component needs metadata specified as fix-length - type="fixed"<br>
 * Also, first field in metadata must be String field with length 1 which is used as
 * indicator of deleted records in DBF.<br>
 * Such metadata can be automatically generated by Clover's utility <code>DBFAnalyzer</code>. Its main
 * class can be executed as <code>java -cp "clover.engine.jar" org.jetel.database.dbf.DBFAnalyzer</code><br>
 * <i>Note: DBFAnalyzer generates additional information from DBF file (<code>dataOffset</code> and <code>recordSize</code>), but these are
 * not neccessary.</i></td></tr>
 * </table>
 *  <br>
 *  <table border="1">
 *  <th>XML attributes:</th>
 *  <tr><td><b>type</b></td><td>"DBF_DATA_READER"</td></tr>
 *  <tr><td><b>id</b></td><td>component identification</td>
 *  <tr><td><b>fileURL</b></td><td>path to the input table file</td>
 *  <tr><td><b>dataPolicy</b><br><i>optional</i></td><td>specifies how to handle misformatted or incorrect data.  'Strict' (default value) aborts processing, 'Controlled' logs the entire record while processing continues, and 'Lenient' attempts to set incorrect data to default values while processing continues.</td>
 *  <tr><td><b>charset</b><br><i>optional</i></td><td>Which character set to use for decoding field's data.  Default value is deduced from DBF table header. If it is
 *  specified as part of metadata at record level, then it is take from there.</td>
 *  <tr><td><b>skipRows</b><br><i>optional</i></td><td>specifies how many records/rows should be skipped from the source file. Good for handling files where first rows is a header not a real data. Dafault is 0.</td>
 *  <tr><td><b>numRecords</b><br><i>optional</i></td><td>max number of parsed records</td>
 *  </tr>
 *  </table>
 *
 *  <h4>Example:</h4>
 *  <pre>&lt;Node type="DBF_DATA_READER" id="InputFile" fileURL="/tmp/customers.dbf" /&gt;</pre>
 *  <h5>Example metadata:</h5>
 *  <pre>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;Record charset=&quot;windows-1252&quot; dataOffset=&quot;456&quot; name=&quot;feature_dbf&quot; recordSize=&quot;35&quot; type=&quot;fixed&quot;&gt;
 *       &lt;Field name=&quot;_IS_DELETED_&quot; nullable=&quot;true&quot; size=&quot;1&quot; type=&quot;string&quot;/&gt;
 *       &lt;Field name=&quot;INDEX&quot; nullable=&quot;true&quot; size=&quot;7&quot; type=&quot;numeric&quot;/&gt;
 *       &lt;Field name=&quot;TYPE&quot; nullable=&quot;true&quot; size=&quot;9&quot; type=&quot;string&quot;/&gt;
 *       &lt;Field name=&quot;NAME&quot; nullable=&quot;true&quot; size=&quot;11&quot; type=&quot;numeric&quot;/&gt;
 *       &lt;Field name=&quot;STREET&quot; nullable=&quot;true&quot; size=&quot;1&quot; type=&quot;string&quot;/&gt;
 *       &lt;Field name=&quot;CITY&quot; nullable=&quot;true&quot; size=&quot;6&quot; type=&quot;numeric&quot;/&gt;
 * &lt;/Record&gt;
 *  </pre>
 *
 * @author      dpavlis
 * @since       June 28, 2004
 * @see         org.jetel.database.dbf.DBFDataParser
 */

public class DBFDataReader extends Node {

	private static final String XML_DATAPOLICY_ATTRIBUTE = "dataPolicy";
	public static final String XML_FILEURL_ATTRIBUTE = "fileURL";
	private static final String XML_CHARSET_ATTRIBUTE = "charset";
    private static final String XML_RECORD_SKIP_ATTRIBUTE = "skipRows";
    private static final String XML_NUMRECORDS_ATTRIBUTE = "numRecords";
	private static final String XML_SKIP_SOURCE_ROWS_ATTRIBUTE = "skipSourceRows";
	private static final String XML_NUM_SOURCE_RECORDS_ATTRIBUTE = "numSourceRecords";
	private static final String XML_INCREMENTAL_FILE_ATTRIBUTE = "incrementalFile";
	private static final String XML_INCREMENTAL_KEY_ATTRIBUTE = "incrementalKey";

	/**  Description of the Field */
	public final static String COMPONENT_TYPE = "DBF_DATA_READER";

	private static Log logger = LogFactory.getLog(DBFDataReader.class);

	private final static int INPUT_PORT = 0;
	private final static int OUTPUT_PORT = 0;
    private MultiFileReader reader;
    private String policyTypeStr;
    private PolicyType policyType = PolicyType.STRICT;
	private String fileURL;
    private int skipRows=-1; // do not skip rows by default
    private int numRecords = -1;
	private int skipSourceRows = -1;
	private int numSourceRecords = -1;
    private String incrementalFile;
    private String incrementalKey;

	private DBFDataParser parser;
	private String charset;

	IParserExceptionHandler exceptionHandler = null;

	/**
	 *Constructor for the DBFDataReader object
	 *
	 * @param  id       Description of the Parameter
	 * @param  fileURL  Description of the Parameter
	 */
	public DBFDataReader(String id, String fileURL) {
		super(id);
		this.fileURL = fileURL;
	}


	/**
	 *Constructor for the DBFDataReader object
	 *
	 * @param  id       Description of the Parameter
	 * @param  fileURL  Description of the Parameter
	 * @param  charset  Description of the Parameter
	 */
	public DBFDataReader(String id, String fileURL, String charset) {
		super(id);
		this.fileURL = fileURL;
		this.charset = charset;
	}
	
	@Override
	public void preExecute() throws ComponentNotReadyException {
		super.preExecute();
		reader.preExecute();
	}


	@Override
	public Result execute() throws Exception {
		// we need to create data record - take the metadata from first output port
		DataRecord record = DataRecordFactory.newRecord(getOutputPort(OUTPUT_PORT).getMetadata());

		// till it reaches end of data or it is stopped from outside
		try {
			while (record != null && runIt) {
				try {
					if ((record = reader.getNext(record)) != null) {
						// broadcast the record to all connected Edges
						writeRecordBroadcast(record);
					}
				} catch (RuntimeException bdfe) {
					if (policyType == PolicyType.STRICT) {
						throw bdfe;
					} else {
						logger.info(ExceptionUtils.getMessage(bdfe));
					}
				}
				SynchronizeUtils.cloverYield();
			}
		} catch (Exception e) {
			throw e;
		} finally {
			broadcastEOF();
		}
		return runIt ? Result.FINISHED_OK : Result.ABORTED;
	}

	@Override
	public void postExecute() throws ComponentNotReadyException {
		super.postExecute();
		
		reader.postExecute();
	}
	
	@Override
	public void commit() {
		super.commit();
		storeValues();
	}


	/*
	 * (non-Javadoc)
	 * @see org.jetel.graph.GraphElement#free()
	 */
	@Override
	public synchronized void free() {
		super.free();
    	if (reader != null) {
			try {
    			reader.close();
			} catch (IOException e) {
				logger.error(e);
			}
	    }
	}

    /**
     * Stores all values as incremental reading.
     */
	private void storeValues() {
		try {
			Object dictValue = getGraph().getDictionary().getValue(Defaults.INCREMENTAL_STORE_KEY);
			if (Boolean.FALSE.equals(dictValue)) {
				return;
			}
			reader.storeIncrementalReading();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
    
	/**
	 *  Description of the Method
	 *
	 * @exception  ComponentNotReadyException  Description of the Exception
	 * @since                                  April 4, 2002
	 */
	@Override
	public void init() throws ComponentNotReadyException {
        if(isInitialized()) return;
		super.init();
		
		policyType = PolicyType.valueOfIgnoreCase(policyTypeStr);

		prepareMultiFileReader();
	}

	private void prepareMultiFileReader() throws ComponentNotReadyException {
		DataRecordMetadata metadata = getOutputPort(OUTPUT_PORT).getMetadata();
		if( charset == null ){
			parser = new DBFDataParser(metadata);
		} else {
			parser = new DBFDataParser(metadata, charset);
		}
		parser.setExceptionHandler(ParserExceptionHandlerFactory.getHandler(policyType));
		
		TransformationGraph graph = getGraph();

        // initialize multifile reader based on prepared parser
        reader = new MultiFileReader(parser, getContextURL(), fileURL);
        reader.setLogger(logger);
        reader.setSkip(skipRows);
        reader.setNumSourceRecords(numSourceRecords);
        // skip source rows
        if (skipSourceRows == -1) {
            for (DataRecordMetadata dataRecordMetadata: getOutMetadata()) {
            	int ssr = dataRecordMetadata.getSkipSourceRows();
            	if (ssr > 0) {
                    skipSourceRows = ssr;
                    break;
            	}
            }
        }
        reader.setSkipSourceRows(skipSourceRows);
        reader.setNumRecords(numRecords);
        reader.setIncrementalFile(incrementalFile);
        reader.setIncrementalKey(incrementalKey);
        reader.setInputPort(getInputPort(INPUT_PORT)); //for port protocol: ReadableChannelIterator reads data
        reader.setCharset(charset);
        reader.setPropertyRefResolver(getPropertyRefResolver());
        reader.setDictionary(graph.getDictionary());
		reader.init(getOutputPort(OUTPUT_PORT).getMetadata());
	}

	@Override
	public String[] getUsedUrls() {
		return new String[] { fileURL };
	}

	/**
	 *  Description of the Method
	 *
	 * @param  nodeXML  Description of Parameter
	 * @return          Description of the Returned Value
	 * @throws AttributeNotFoundException 
	 * @since           May 21, 2002
	 */
    public static Node fromXML(TransformationGraph graph, Element xmlElement) throws XMLConfigurationException, AttributeNotFoundException {
		DBFDataReader dbfDataReader = null;
		ComponentXMLAttributes xattribs = new ComponentXMLAttributes(xmlElement, graph);
		
		if (xattribs.exists(XML_CHARSET_ATTRIBUTE)) {
			dbfDataReader = new DBFDataReader(xattribs.getString(XML_ID_ATTRIBUTE),
					xattribs.getStringEx(XML_FILEURL_ATTRIBUTE, null, RefResFlag.URL),
					xattribs.getString(XML_CHARSET_ATTRIBUTE));
		} else {
			dbfDataReader = new DBFDataReader(xattribs.getString(XML_ID_ATTRIBUTE),
					xattribs.getStringEx(XML_FILEURL_ATTRIBUTE, null, RefResFlag.URL));
		}
		if (xattribs.exists(XML_DATAPOLICY_ATTRIBUTE)) {
			dbfDataReader.setPolicyType(xattribs.getString(XML_DATAPOLICY_ATTRIBUTE));
		}
        if (xattribs.exists(XML_RECORD_SKIP_ATTRIBUTE)){
        	dbfDataReader.setSkipRows(xattribs.getInteger(XML_RECORD_SKIP_ATTRIBUTE));
        }
        if (xattribs.exists(XML_NUMRECORDS_ATTRIBUTE)){
        	dbfDataReader.setNumRecords(xattribs.getInteger(XML_NUMRECORDS_ATTRIBUTE));
        }
		if (xattribs.exists(XML_INCREMENTAL_FILE_ATTRIBUTE)){
			dbfDataReader.setIncrementalFile(xattribs.getStringEx(XML_INCREMENTAL_FILE_ATTRIBUTE, RefResFlag.URL));
		}
		if (xattribs.exists(XML_INCREMENTAL_KEY_ATTRIBUTE)){
			dbfDataReader.setIncrementalKey(xattribs.getString(XML_INCREMENTAL_KEY_ATTRIBUTE));
		}
		if (xattribs.exists(XML_SKIP_SOURCE_ROWS_ATTRIBUTE)){
			dbfDataReader.setSkipSourceRows(xattribs.getInteger(XML_SKIP_SOURCE_ROWS_ATTRIBUTE));
		}
		if (xattribs.exists(XML_NUM_SOURCE_RECORDS_ATTRIBUTE)){
			dbfDataReader.setNumSourceRecords(xattribs.getInteger(XML_NUM_SOURCE_RECORDS_ATTRIBUTE));
		}

		return dbfDataReader;
	}

	/**  Description of the Method */
    @Override
    public ConfigurationStatus checkConfig(ConfigurationStatus status) {
        super.checkConfig(status);
        
        if(!checkInputPorts(status, 0, 1)
        		|| !checkOutputPorts(status, 1, Integer.MAX_VALUE)) {
        	return status;
        }
        
		if (!PolicyType.isPolicyType(policyTypeStr)) {
			status.add(MessageFormat.format("Invalid data policy: {0}", policyTypeStr), Severity.ERROR, this, Priority.NORMAL, XML_DATAPOLICY_ATTRIBUTE);
		} else {
			policyType = PolicyType.valueOfIgnoreCase(policyTypeStr);
		}

        if (charset != null && !Charset.isSupported(charset)) {
        	status.add(new ConfigurationProblem(
					MessageFormat.format("Charset {0} not supported!", charset), 
					ConfigurationStatus.Severity.ERROR, this, 
					ConfigurationStatus.Priority.NORMAL, XML_CHARSET_ATTRIBUTE));
        }
        
        checkMetadata(status);
        
        if (fileURL == null) {
        	status.add("File URL not defined.", Severity.ERROR, this, Priority.NORMAL, XML_FILEURL_ATTRIBUTE);
        	return status;
        }

        try { 
            // check inputs
            prepareMultiFileReader();
            DataRecordMetadata metadata = getOutputPort(OUTPUT_PORT).getMetadata();
    		if (!metadata.hasFieldWithoutAutofilling()) {
    			status.add(new ConfigurationProblem(
                		MessageFormat.format("No field elements without autofilling for ''{0}'' have been found!", getOutputPort(OUTPUT_PORT).getMetadata().getName()), 
                		ConfigurationStatus.Severity.ERROR, this, ConfigurationStatus.Priority.NORMAL));
    		}
            reader.checkConfig(metadata);
        } catch (ComponentNotReadyException e) {
            ConfigurationProblem problem = new ConfigurationProblem(
            		ExceptionUtils.getMessage(e), 
            		ConfigurationStatus.Severity.WARNING, 
            		this, 
            		ConfigurationStatus.Priority.NORMAL);
            if(!StringUtils.isEmpty(e.getAttributeName())) {
                problem.setAttributeName(e.getAttributeName());
            }
            status.add(problem);
        } finally {
        	free();
        }
        
        return status;
    }
    
    private ConfigurationStatus checkMetadata(ConfigurationStatus status) {
    	ConfigurationStatus newStatus = checkMetadata(status, null, getOutPorts());
    	List<DataRecordMetadata> metadata = getOutMetadata();
    	if (metadata != null && !metadata.isEmpty()) {
    		DataFieldMetadata[] fields = metadata.iterator().next().getFields();
			if (fields != null && fields.length > 0) {
				if (!fields[0].isFixed() || fields[0].getSize() != 1) {
					newStatus.add(new ConfigurationProblem(
							MessageFormat.format("Invalid output metadata {0}. Expected size of first field is 1.", metadata.iterator().next().getId()), 
							ConfigurationStatus.Severity.ERROR, 
							this, 
							ConfigurationStatus.Priority.NORMAL));
				}
			}
    	}
    	return newStatus;
    }
	
    /**
     * @param skipRows The skipRows to set.
     */
    public void setSkipRows(int skipRows) {
        this.skipRows = skipRows;
    }
    
    public void setNumRecords(int numRecords) {
        this.numRecords = Math.max(numRecords, 0);
    }

	/**
	 * @param how many rows to skip for every source
	 */
	public void setSkipSourceRows(int skipSourceRows) {
		this.skipSourceRows = Math.max(skipSourceRows, 0);
	}
	
	/**
	 * @param how many rows to process for every source
	 */
	public void setNumSourceRecords(int numSourceRecords) {
		this.numSourceRecords = Math.max(numSourceRecords, 0);
	}
    
    public void setPolicyType(String policyTypeStr) {
        this.policyTypeStr = policyTypeStr;
    }

    public void setIncrementalFile(String incrementalFile) {
    	this.incrementalFile = incrementalFile;
    }

    public void setIncrementalKey(String incrementalKey) {
    	this.incrementalKey = incrementalKey;
    }

}

