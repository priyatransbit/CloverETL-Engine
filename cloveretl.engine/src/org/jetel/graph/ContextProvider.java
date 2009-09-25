/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002-04  David Pavlis <david_pavlis@hotmail.com>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/
package org.jetel.graph;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class should be able to provide org.jetel.graph.Node corresponding to current thread.
 * All is provided via static methods. Every time you need the transformation graph instance just call
 * ContextProvider.getGraph().
 *
 * This functionality can work only when all threads inside components are registered in this class.
 *
 * For this purpose it is recommended to use CloverWorker class instead Runnable every time you want to create
 * separate thread inside a component.
 *
 * @author Martin Zatopek (martin.zatopek@javlinconsulting.cz)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 *
 * @created 24.9.2009
 */
public class ContextProvider {

    private final static Log logger = LogFactory.getLog(ContextProvider.class);

	static private Map<Thread, Node> nodesCache = new HashMap<Thread, Node>(); 
    
	static public TransformationGraph getGraph() {
    	Node node = nodesCache.get(Thread.currentThread());
    	if (node == null) {
			logger.warn("ContextProvider was not able to provide requested graph. Current thread is not registered.");
    	}
    	return node.getGraph();
    }

	static public Node getNode() {
    	Node node = nodesCache.get(Thread.currentThread());
    	if (node == null) {
			logger.warn("ContextProvider was not able to provide requested node. Current thread is not registered.");
    	}
    	return node;
    }

	static public void registerNode(Node node) {
		nodesCache.put(Thread.currentThread(), node);
	}
	
	static public void unregisterNode() {
		if (!nodesCache.containsKey(Thread.currentThread())) {
			logger.warn("Attempt to unregister non-registered thread in the ContextProvider.");
		} else {
			nodesCache.remove(Thread.currentThread());
		}
	}
	
}
