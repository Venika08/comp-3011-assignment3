/*
 * Created for Adelaide University COMP3011 Assignment 2. 
 * Author:
 * Venika Thakur - a1970789
 */

package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;


class FrameProcessingPipelineTest {
	
	@Test
	void preservesProcessorOrderAndFrameOrder() throws Exception {
		List<String> events = 
				Collections.synchronizedList(new ArrayList<>());
		
		List<FrameProcessor> processors = List.of(
				new RecordingProcessor("A", events),
				new RecordingProcessor("B", events),
				new RecordingProcessor("C", events));
				
		AtomicReference<Exception> error = new AtomicReference<>();
		
		FrameProcessingPipeline pipeline = 
				new FrameProcessingPipeline(processors, error::set);
		
		pipeline.start();
		
		try {
			int submitted = 0;

			while (submitted < 3) {
			    if (pipeline.canAcceptFrame()) {
			        int frameNumber = submitted + 1;

			        if (pipeline.submit(
			                new Frame(),
			                new InfoFrame(frameNumber, frameNumber * 1_000L))) {

			            submitted++;
			        }
			    }

			    if (error.get() != null) {
			        fail(error.get());
			    }

			    Thread.sleep(1);
			}
			
			List<Integer> completedFrames = new ArrayList<>();
			
			for (int i = 0; i < 3; i++) {
				FrameProcessingPipeline.FrameTask completed = 
						waitForCompleted(pipeline, error);
				
				completedFrames.add(completed.info().frameNumber());
			}
			
			assertEquals(List.of(1, 2, 3), completedFrames);
			
			for (int frameNumber = 1; frameNumber <= 3; frameNumber++) {
				List<String> frameEvents;
				
				synchronized (events) {
					final int number = frameNumber;
					
					frameEvents = events.stream()
							.filter(event -> event.endsWith("-" + number))
							.toList();
				}
				
				assertEquals(
						List.of(
								"A-" + frameNumber, 
								"B-" + frameNumber, 
								"C-" + frameNumber),
						frameEvents);
			}
			
			assertNull(error.get());
			
		} finally {
			pipeline.stop();
		}
	}
	
	@Test
	void processorStagesRunOnDifferentWorkerThreads() throws Exception {
        AtomicReference<String> firstThread = new AtomicReference<>();
        AtomicReference<String> secondThread = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        List<FrameProcessor> processors = List.of(
                new ThreadRecordingProcessor(firstThread),
                new ThreadRecordingProcessor(secondThread));

        FrameProcessingPipeline pipeline =
                new FrameProcessingPipeline(processors, error::set);

        pipeline.start();
        
        try {
        	assertTrue(pipeline.submit(
        			new Frame(),
        			new InfoFrame(1, 1_000L)));
        	
        	waitForCompleted(pipeline, error);
        	
        	assertTrue(firstThread.get().startsWith("frame-processor-"));
        	assertTrue(secondThread.get().startsWith("frame-processor-"));
        	
        	assertTrue(
        			!firstThread.get().equals(secondThread.get()),
        			"Each processing stage have its own worker thread.");
        	
        	assertNull(error.get());
        
        } finally {
        	pipeline.stop();
        }
	}
	
	@Test
	void zeroProcessorsPassFramesThrough() throws Exception {
		AtomicReference<Exception> error = new AtomicReference<>();

        FrameProcessingPipeline pipeline =
                new FrameProcessingPipeline(List.of(), error::set);

        pipeline.start();

        try {
            assertTrue(pipeline.submit(
                    new Frame(),
                    new InfoFrame(7, 7_000L)));

            FrameProcessingPipeline.FrameTask completed =
                    waitForCompleted(pipeline, error);

            assertEquals(7, completed.info().frameNumber());
            assertNull(error.get());

        } finally {
            pipeline.stop();
        }
    }

    
    @Test
    void pipelineOutperformsSequentialBaselineUnderLoad() throws Exception {
        assumeTrue(
                Runtime.getRuntime().availableProcessors() >= 2,
                "Performance comparison requires at least two processors.");

        final int frameCount = 8;
        final int workIterations = 6_000_000;

        // Warm up the CPU-load code before either measurement so JIT compilation
        // does not unfairly favour one implementation.
        new CpuLoadProcessor(200_000).process(
                new Frame(),
                new InfoFrame(0, 0));

        /*
         * This loop represents the original single-threaded design:
         * every effect for one frame must finish before processing continues.
         */
        List<FrameProcessor> sequentialProcessors = List.of(
                new CpuLoadProcessor(workIterations),
                new CpuLoadProcessor(workIterations),
                new CpuLoadProcessor(workIterations));

        long sequentialStart = System.nanoTime();

        for (int frameNumber = 1;
                frameNumber <= frameCount;
                frameNumber++) {

            Frame frame = new Frame();
            InfoFrame info =
                    new InfoFrame(frameNumber, frameNumber * 1_000L);

            for (FrameProcessor processor : sequentialProcessors) {
                processor.process(frame, info);
            }
        }

        long sequentialTimeNs =
                System.nanoTime() - sequentialStart;

        /*
         * Run the same amount of processor work through the new pipeline.
         * Different stages can now work on different frames at the same time.
         */
        AtomicReference<Exception> error = new AtomicReference<>();

        List<FrameProcessor> concurrentProcessors = List.of(
                new CpuLoadProcessor(workIterations),
                new CpuLoadProcessor(workIterations),
                new CpuLoadProcessor(workIterations));

        FrameProcessingPipeline pipeline =
                new FrameProcessingPipeline(
                        concurrentProcessors,
                        error::set);

        pipeline.start();

        long pipelineStart = System.nanoTime();

        try {
            int submitted = 0;
            int completed = 0;

            long deadline =
                    System.nanoTime() + 10_000_000_000L;

            while (completed < frameCount) {
                if (System.nanoTime() > deadline) {
                    fail("Timed out during pipeline performance test.");
                }

                if (submitted < frameCount
                        && pipeline.canAcceptFrame()) {

                    int frameNumber = submitted + 1;

                    if (pipeline.submit(
                            new Frame(),
                            new InfoFrame(
                                    frameNumber,
                                    frameNumber * 1_000L))) {

                        submitted++;
                    }
                }

                while (pipeline.pollCompleted() != null) {
                    completed++;
                }

                if (error.get() != null) {
                    fail(error.get());
                }

                Thread.sleep(1);
            }

        } finally {
            pipeline.stop();
        }

        long pipelineTimeNs =
                System.nanoTime() - pipelineStart;

        assertTrue(
                pipelineTimeNs < sequentialTimeNs,
                "The concurrent pipeline should outperform the "
                        + "original sequential processing approach. "
                        + "Sequential: "
                        + sequentialTimeNs / 1_000_000
                        + " ms, pipeline: "
                        + pipelineTimeNs / 1_000_000
                        + " ms.");
    }

    private FrameProcessingPipeline.FrameTask waitForCompleted(
            FrameProcessingPipeline pipeline,
            AtomicReference<Exception> error)
            throws InterruptedException {

        long deadline =
                System.nanoTime() + 2_000_000_000L;

        while (System.nanoTime() < deadline) {
            if (error.get() != null) {
                fail(error.get());
            }

            FrameProcessingPipeline.FrameTask completed =
                    pipeline.pollCompleted();

            if (completed != null) {
                return completed;
            }

            Thread.sleep(1);
        }

        fail("Timed out waiting for a processed frame.");
        return null;
    }

    private static class RecordingProcessor
            extends FrameProcessor {

        private final String name;
        private final List<String> events;

        RecordingProcessor(
                String name,
                List<String> events) {

            this.name = name;
            this.events = events;
        }

        @Override
        public void process(
                Frame frame,
                InfoFrame info) {

            events.add(name + "-" + info.frameNumber());
        }
    }

    private static class ThreadRecordingProcessor
            extends FrameProcessor {

        private final AtomicReference<String> threadName;

        ThreadRecordingProcessor(
                AtomicReference<String> threadName) {

            this.threadName = threadName;
        }

        @Override
        public void process(
                Frame frame,
                InfoFrame info) {

            threadName.set(Thread.currentThread().getName());
        }
    }

    private static class CpuLoadProcessor
    extends FrameProcessor {

private final int iterations;

/*
 * Volatile keeps the calculated result observable so the JVM cannot
 * simply discard the CPU work during optimisation.
 */
private volatile long result;

CpuLoadProcessor(int iterations) {
    this.iterations = iterations;
}

@Override
public void process(
        Frame frame,
        InfoFrame info) {

    long value = info.frameNumber() + 1L;

    for (int i = 0; i < iterations; i++) {
        value ^= value << 13;
        value ^= value >>> 7;
        value ^= value << 17;
        value += i;
    }

    result = value;
}
}
}  
	
