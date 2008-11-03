package org.jetel.util.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.data.Defaults;
import org.jetel.exception.JetelException;

/**
 * Execute multiple produer threads, consumer threads and procces.
 * 
 * <b>
 * IMPORTANT: not tested, write by Cyril for obsolute Subgraph implemantation via Dictionary
 * </b>
 * 
 * @see ProcBox 
 *
 */
public class ProducerConsumerExecutor {

	private List<ProducerThread> producerThreads = new LinkedList<ProducerThread>();
	private List<ConsumerThread> consumerThreads = new LinkedList<ConsumerThread>();
	private List<Process> processes = new LinkedList<Process>();
	
	static Log logger = LogFactory.getLog(ProducerConsumerExecutor.class);


	public ProducerConsumerExecutor() {

	}

	public void addProducer(DataProducer producer, OutputStream outStream) {
		final ProducerThread thread = new ProducerThread(producer, outStream);
		producerThreads.add(thread);
	}

	public void addConsumer(DataConsumer consumer, InputStream inStream) {
		if (consumer == null) {
			consumer = new WasteDataConsumer();
		}
		final ConsumerThread thread = new ConsumerThread(consumer, inStream);
		consumerThreads.add(thread);
	}

	public void addProcess(Process process) {
		processes.add(process);
	}

	/**
	 * Starts all producer and consumer threads.
	 */
	public void start() {
		for (ProducerThread thread : producerThreads) {
			thread.start();
		}

		for (ConsumerThread thread : consumerThreads) {
			thread.start();
		}
	}

	
	/**
	 * Joins the process and all slave threads. 
	 * @return Return value of last finished process or 0 if no process defined
	 * @throws InterruptedException 
	 */
	public int join() throws InterruptedException {
		int ret = 0;

		for (ProducerThread thread : producerThreads) {
			thread.join();
			try {
				thread.stream.close();
			} catch (IOException e) {
				logger.warn("Cannot close output", e);
			}
		}

		for( Process process : processes){
			ret = process.waitFor();
		}
		
		for (ConsumerThread thread : consumerThreads) {
			thread.join();
			try {
				thread.stream.close();
			} catch (IOException e) {
				logger.warn("Cannot close input", e);
			}
		}
		
		return ret;
	}

	/**
	 * Nomen omen.
	 * 
	 * @author Jan Hadrava, Javlin Consulting (www.javlinconsulting.cz)
	 * 
	 */
	private static class ProducerThread extends Thread {
		private boolean runIt = true;
		private DataProducer producer;
		private OutputStream stream;

		/**
		 * Sole ctor. Creates thread which uses specified producer to supply data to specified stream, which is supposed
		 * to be connected to process' input.
		 * 
		 * @param producer
		 * @param stream
		 */
		public ProducerThread(DataProducer producer, OutputStream stream) {
			super(Thread.currentThread().getName() + ".ProducerThread");
			this.producer = producer;
			this.stream = stream;
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				producer.setOutput(stream);
				while (runIt && producer.produce())
					;
				producer.close();
			} catch (JetelException e) {
				logger.error("Data producer failed: " + e.getMessage());
			}
		}
	}

	/**
	 * Nomen omen.
	 * 
	 * @author Jan Hadrava, Javlin Consulting (www.javlinconsulting.cz)
	 * 
	 */
	private static class ConsumerThread extends Thread {
		private boolean runIt = true;
		private DataConsumer consumer;
		private InputStream stream;

		/**
		 * Sole ctor. Creates thread which uses specified consumer to process data from specified stream, which is
		 * supposed to be connected either to process' output or to process' error output.
		 * 
		 * @param consumer
		 * @param stream
		 */
		public ConsumerThread(DataConsumer consumer, InputStream stream) {
			super(Thread.currentThread().getName() + ".ConsumerThread");
			this.consumer = consumer;
			this.stream = stream;
		}

		/**
		 * @see java.lan.Thread#run()
		 */
		public void run() {
			try {
				consumer.setInput(stream);
				while (runIt && consumer.consume())
					;
				consumer.close();
			} catch (JetelException e) {
				logger.error("Data producer failed: " + e.getMessage());
			}
		}
	}

	/**
	 * A consumer which is used whenever user doesn't specify consumer or error consumer. It reads data from input
	 * stream and instantly discards them.
	 * 
	 * @author Jan Hadrava, Javlin Consulting (www.javlinconsulting.cz)
	 * 
	 */
	private static class WasteDataConsumer implements DataConsumer {
		private InputStream stream;
		private byte buf[] = new byte[Defaults.DEFAULT_INTERNAL_IO_BUFFER_SIZE];

		public void setInput(InputStream stream) {
			this.stream = stream;
		}

		public boolean consume() throws JetelException {
			try {
				return stream.read(buf) > -1;
			} catch (IOException e) {
				throw new JetelException("Error while reading input buffer", e);
			}
		}

		public void close() {
		}
	}

}
