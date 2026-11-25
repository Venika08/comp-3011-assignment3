/*
 * Created for Adelaide University COMP3011 Assignment 2. 
 * Author:
 * Venika Thakur - studentId: a1970789
 */

/**
 * Processes video frames using background processing stages. 

 *
 * <p>
 * Each {@link FrameProcessor} owns one worker thread and one pipeline stage.
 * Frames move through the processors in the same order FIFO that the effects were given on the command line. 
 * This allows different frames to be worked on at the same time without changing the order of effects on each frame. 
 * </p>
 *
 * <p>
 * The queues are kept small so the program does not process too many frames ahead of playback. When the pipeline is stopped, the worker threads are interrupted
 * * and the queues are cleared so old frames are not displayed after seeking or restarting playback. 
 * far ahead of rendering. {@link #stop()} interrupts and joins all workers and
 * clears queued frames, which allows playback seeking and shutdown to restart
 * from a clean state without stale frames.
 * </p>
 */


package comp3011;

import java.util.List;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.bytedeco.javacv.Frame;

public class FrameProcessingPipeline {
	
	private static final int QUEUE_CAPACITY = 1;
	
	private static final int MAX_FRAMES_IN_FLIGHT = 
			Math.max(2,  Runtime.getRuntime().availableProcessors() - 1);
	
	private final List<FrameProcessor> frameProcessors;
	private final Consumer<Exception> errorHandler;
	
	
	private final List<BlockingQueue<FrameTask>> stageQueues = 
			new ArrayList<>();
	
	private final List<Thread> workerThreads =
			new ArrayList<>();
	
	private volatile boolean running;
	
	private final AtomicInteger framesInFlight = 
			new AtomicInteger();
	
	public FrameProcessingPipeline(
			List<FrameProcessor> frameProcessors, 
			Consumer<Exception> errorHandler) {
		
		this.frameProcessors = List.copyOf(frameProcessors);
		this.errorHandler = errorHandler;
		
		for (int i = 0; i <= frameProcessors.size(); i++) {
			stageQueues.add(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
		}
		
	}
	
	public void initialise(InfoVideo videoInfo) throws Exception {
		for (FrameProcessor processor : frameProcessors) {
			processor.initialise(videoInfo);
		}
	}
	
	public boolean canAcceptFrame() {
		return running 
				&& framesInFlight.get() < MAX_FRAMES_IN_FLIGHT
				&& stageQueues.get(0).remainingCapacity() > 0;
	}
	
	public void start() {
		if (running) {
			return;
		}
		
		running = true;
		
		for (int i = 0; i < frameProcessors.size(); i++) {
			final int stageIndex = i;
			
			Thread worker = new Thread(
					() -> runStage(stageIndex), 
					"frame-processor-" + stageIndex + "-"
					+ frameProcessors.get(stageIndex).getClass().getSimpleName());
			
			worker.setDaemon(true);
			workerThreads.add(worker);
			worker.start();
					
		}
	}
	
	public void stop() {
		running = false;
		
		for (Thread worker : workerThreads) {
			worker.interrupt();
		}
		
		Thread currentThread = Thread.currentThread();
		
		for (Thread worker : workerThreads) {
			if (worker == currentThread) {
				continue;
			}
			
			try {
				worker.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		
		workerThreads.clear();
		
		for (BlockingQueue<FrameTask> queue : stageQueues) {
			queue.clear();
		}
		
		framesInFlight.set(0);
	}
	
	private void runStage(int stageIndex) {
		FrameProcessor processor = frameProcessors.get(stageIndex);
		
		BlockingQueue<FrameTask> inputQueue =
				stageQueues.get(stageIndex);
		
		BlockingQueue<FrameTask> outputQueue = 
				stageQueues.get(stageIndex + 1);
		
		
		while (running) {
			try {
				FrameTask task = inputQueue.take();
				
				processor.process(
						task.frame(),
						task.info());
				
				outputQueue.put(task);
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			
			} catch (Exception e) {
				running = false;
				errorHandler.accept(e);
				return;
			}
		}
	}
	
	public boolean submit(Frame frame, InfoFrame info) {
		if (!canAcceptFrame()) {
			return false;
		}
		
		Frame frameCopy = frame.clone();
		FrameTask task = new FrameTask(frameCopy, info);
		
		boolean accepted = stageQueues.get(0).offer(task);
		
		if (accepted) {
			framesInFlight.incrementAndGet();
		}
		
		return accepted;
	}
	
	public FrameTask pollCompleted() {
		FrameTask completed = 
				stageQueues.get(stageQueues.size() - 1).poll();
		
		if (completed != null) {
			framesInFlight.decrementAndGet();
		}
		
		return completed;

}
	
	public boolean hasPendingFrames() {
		return framesInFlight.get() > 0;
	}
	
	public record FrameTask(
			Frame frame, 
			InfoFrame info) {
	
}
	
}


