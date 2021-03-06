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
package org.jetel.component.tree.writer.portdata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.jetel.data.DataRecord;
import org.jetel.data.Defaults;
import org.jetel.exception.TempFileCreationException;
import org.jetel.graph.ContextProvider;
import org.jetel.graph.runtime.IAuthorityProxy;
import org.jetel.util.bytes.CloverBuffer;

/**
 * Class implementing DynamicRecordBuffer backed by temporary file - i.e. unlimited size<br>
 * 
 * Life cycle of this class is not really straightforward. Two separate options:
 * 1) init() ((writeRaw() | read())* [clear()])* close()
 * 2) init() (write()* flushBuffer() loadData() next()* [clear()])* close()
 * 
 * For mode details, look at the junit test {@link DirectDynamicRecordBufferTest}.
 * 
 * @author lkrejci (info@cloveretl.com) (c) Javlin, a.s. (www.cloveretl.com)
 * 
 * @created 5 Jan 2011
 */
public class DirectDynamicRecordBuffer {
	
	private FileChannel tmpFileChannel;
	private File tmpFile;
	
	private List<DiskSlot> fileBuffers = new ArrayList<DiskSlot>();
	private DiskSlot lastDiskSlot;
	
	private CloverBuffer recordBuffer;
	private CloverBuffer dataBuffer;
	
	private int currentWritePosition;
	private int writeRecordCount = 0;
	private int readRecordCount = 0;
	private int nextSlot = 0;
	
	private final static String TMP_FILE_PREFIX = "fbufddrb";
	// prefix of temporary file generated by system
	private final static String TMP_FILE_SUFFIX = ".tmp";
	// suffix of temporary file generated by system
	private final static String TMP_FILE_MODE = "rw";
	
	public DirectDynamicRecordBuffer() {
	}
	
	public void init() throws IOException {
		try {
			tmpFile = IAuthorityProxy.getAuthorityProxy(ContextProvider.getGraph()).newTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX, -1);
		} catch (TempFileCreationException e) {
			throw new IOException("Could not create temp file.", e);
		}
		tmpFileChannel = new RandomAccessFile(tmpFile, TMP_FILE_MODE).getChannel();
		dataBuffer = CloverBuffer.allocateDirect(Defaults.Record.RECORDS_BUFFER_SIZE);
		recordBuffer = CloverBuffer.allocateDirect(Defaults.Record.RECORD_INITIAL_SIZE, Defaults.Record.RECORD_LIMIT_SIZE);
	}
	
	public IndexKey writeRaw(DataRecord record) throws IOException {
        try {
            record.serializeUnitary(recordBuffer);
        } catch (BufferOverflowException ex) {
            throw new IOException("Internal buffer is not big enough to accomodate data record ! (See Record.RECORD_LIMIT_SIZE parameter)");
        }
        recordBuffer.flip();
		
		int recordSize = recordBuffer.remaining();
		
		tmpFileChannel.write(recordBuffer.buf());
		recordBuffer.clear();
        int oldCurrentWritePosition = currentWritePosition;
        currentWritePosition += recordSize; 
		
		return new IndexKey(oldCurrentWritePosition, recordSize);
	}
	
	public void read(CloverBuffer record, int position) throws IOException {
		tmpFileChannel.read(record.buf(), position);
		record.flip();
	}

	public void write(DataRecord record) throws IOException {
		try {
            record.serializeUnitary(recordBuffer);
        } catch (BufferOverflowException ex) {
            throw new IOException("Internal buffer is not big enough to accomodate data record ! (See RECORD_LIMIT_SIZE parameter)");
        }
        recordBuffer.flip();
        
        int recordSize = recordBuffer.remaining();
        
        if (dataBuffer.remaining() < recordSize && dataBuffer.position() > 0) {
        	//buffer is not flushed if is empty - dynamicity of buffer is used
        	flushBuffer();
        }
        dataBuffer.put(recordBuffer);
        recordBuffer.clear();
        writeRecordCount++;
	}
	
	public void flushBuffer() throws IOException {
		dataBuffer.flip();
		int size = dataBuffer.limit();
		long offset = lastDiskSlot != null ? lastDiskSlot.getOffset() + lastDiskSlot.getSize() : 0;
		DiskSlot diskSlot = new DiskSlot(size, offset);
		tmpFileChannel.write(dataBuffer.buf(), offset);

		dataBuffer.clear();
		fileBuffers.add(diskSlot);
		lastDiskSlot = diskSlot;
	}

	public boolean next(DataRecord record) throws IOException {
		if (readRecordCount < writeRecordCount) {
			if (dataBuffer.remaining() == 0) {
				if (!loadData()) {
					return false;
				}
			}

			record.deserializeUnitary(dataBuffer);

			readRecordCount++;

			return true;
		}
		return false;
	}
	
	public boolean loadData() throws IOException {
		if (nextSlot < fileBuffers.size()) {
			DiskSlot slot = fileBuffers.get(nextSlot++);
			dataBuffer.clear();
			dataBuffer.limit(slot.getSize());
			tmpFileChannel.read(dataBuffer.buf(), slot.getOffset());
			dataBuffer.flip();
			return true;
		}
		return false;
		
	}

	public void close() throws IOException {
		if (tmpFileChannel != null) {
			tmpFileChannel.close();
		}
		tmpFile.delete();
	}
	
	public void clear() {
		fileBuffers.clear();
		dataBuffer.clear();
		
		writeRecordCount = 0;
		readRecordCount = 0;
		nextSlot = 0;
		lastDiskSlot = null;
		
		currentWritePosition = 0;
		
//		try {
//			tmpFileChannel.truncate(0);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		try {
//			if (tmpFileChannel != null) {
//				tmpFileChannel.close();
//			}
//			tmpFile.delete();
//	
//			tmpFile = File.createTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX, tempDirectory != null ? new File(tempDirectory) : null);
//			tmpFile.deleteOnExit();
//			tmpFileChannel = new RandomAccessFile(tmpFile, TMP_FILE_MODE).getChannel();
//		} catch (Exception e) {
//			throw new JetelRuntimeException(e);
//		}
	}
	
	public void reset() {
		readRecordCount = 0;
		nextSlot = 0;
		lastDiskSlot = null;
	}
	
	private static class DiskSlot {
        int size;
        long offset;
        
        DiskSlot(int size, long offset) {
            this.size = size;
            this.offset = offset;
        }

        int getSize() {
        	return size;
        }

        long getOffset() {
             return offset;
        }
    }

}
