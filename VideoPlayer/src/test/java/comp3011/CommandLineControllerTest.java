package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommandLineControllerTest {
	
	@TempDir
	Path tempDir;
	
	@Test 
	void stackedFrameProcessorOptionsPreseverOrderAndDuplicates() throws IOException {
		Path videoFile = Files.createFile(tempDir.resolve("test.mp4"));
		
		CommandLineController controller = 
				new CommandLineController(new String[] {
						"-jfddn",
						videoFile.toString()
				});
		
		List<FrameProcessor> processors = controller.getFrameProcessors();
		
		assertNull(controller.getErrorMessage());
		assertEquals(5, processors.size());
		
		assertInstanceOf(FrameJitterer.class, processors.get(0));
		assertInstanceOf(FrameFlickerer.class, processors.get(1));
		assertInstanceOf(FrameDuster.class, processors.get(2));
		assertInstanceOf(FrameDuster.class, processors.get(3));
		assertInstanceOf(FrameNumberer.class, processors.get(4));
		
	}
	
	@Test
	void longFrameProcessorOptionsPreserveOrderAndDuplicates() throws IOException {
		Path videoFile = Files.createFile(tempDir.resolve("long-options.mp4"));
		
		CommandLineController controller = 
				new CommandLineController(new String[] {
						"--jitter-frames",
						"--flicker-frames",
						"--dust-frames",
						"--dust-frames",
						"--number-frames",
						videoFile.toString()
				});
		
		List<FrameProcessor> processors = controller.getFrameProcessors();
		
		assertNull(controller.getErrorMessage());
		assertEquals(5, processors.size());
		
		assertInstanceOf(FrameJitterer.class, processors.get(0));
		assertInstanceOf(FrameFlickerer.class, processors.get(1));
		assertInstanceOf(FrameDuster.class, processors.get(2));
	    assertInstanceOf(FrameDuster.class, processors.get(3));
	    assertInstanceOf(FrameNumberer.class, processors.get(4));
	}
	
	@Test
	void mixedFrameProcessorOptionsAreParsedCorrectly() throws IOException {
		Path videoFile = Files.createFile(tempDir.resolve("mixed-options.mp4"));
		
		CommandLineController controller = 
				new CommandLineController(new String[] {
						"-jf",
						"--dust-frames",
						"-d",
						"--number-frames",
						videoFile.toString()
				});
		
		List<FrameProcessor> processors = controller.getFrameProcessors();
		
		assertNull(controller.getErrorMessage());
		assertEquals(5, processors.size());
		
		assertInstanceOf(FrameJitterer.class, processors.get(0));
		assertInstanceOf(FrameFlickerer.class, processors.get(1));
	    assertInstanceOf(FrameDuster.class, processors.get(2));
	    assertInstanceOf(FrameDuster.class, processors.get(3));
	    assertInstanceOf(FrameNumberer.class, processors.get(4));
	}
	
	@Test
	void invalidStackedOptionIsRejected() throws IOException {
		Path videoFile = Files.createFile(tempDir.resolve("invalid-option.mp4"));
		
		CommandLineController controller = 
				new CommandLineController(new String[] {
						"-nz",
						videoFile.toString()
				});
		
		assertEquals("Unknown option: -nz", controller.getErrorMessage());
		assertEquals(0, controller.getFrameProcessors().size());
	}
	
	@Test
	void existingAudioOptionsStillWorks() throws IOException {
		Path videoFile = Files.createFile(tempDir.resolve("audio-option.mp4"));
		
		CommandLineController controller = 
				new CommandLineController(new String[] {
						"-a",
						videoFile.toString()
				});
		
		assertNull(controller.getErrorMessage());
		assertEquals(true, controller.isAudioRequested());
	}
	
	@Test 
	void helpTextMatchesSpecificationExactly() {
	    PrintStream originalOut = System.out;
	    ByteArrayOutputStream output = new ByteArrayOutputStream();

	    try {
	        System.setOut(new PrintStream(output));

	        new CommandLineController(new String[] {
	                "-h"
	        });

	    } finally {
	        System.setOut(originalOut);
	    }

	    String expected = String.join(
	            System.lineSeparator(),
	            "Usage: VideoPlayer [options] [video-file]",
	            "",
	            "Options:",
	            "  -h, --help         Show this help message",
	            "  -a, --audio        Play audio",
	            "  -x, --maximise     Open the player maximised",
	            "  -1, --monitor-1    Open the player on display 1",
	            "  -2, --monitor-2    Open the player on display 2",
	            "",
	            "Frame processors:",
	            "  -n, --number-frames    Render the frame number onto each frame",
	            "  -s, --scratch-frames   Render vertical film scratches",
	            "  -f, --flicker-frames   Randomly dim frames",
	            "  -w, --black-and-white  Convert frames to black and white",
	            "  -y, --yellow-frames    Apply a warmer colour temperature",
	            "  -v, --vignette-frames  Darken the frame edges",
	            "  -d, --dust-frames      Render dust and hair marks",
	            "  -j, --jitter-frames    Randomly displace frames by a few pixels",
	            "  -m, --mottle-frames    Add cloudy emulsion mottling",
	            "  -b, --bleed-frames     Bleed light into frames",
	            "  -p, --pepper-frames    Pepper frames with dark spots/blotches",
	            "",
	            "Frame processors are applied in command-line order and may be repeated.",
	            "Example: -nssnfwyvdjmbp numbers, scratches twice, numbers again, flickers,",
	            "converts, warms, vignettes, dusts, jitters, mottles, bleeds, then peppers.",
	            ""
	    );

	    assertEquals(expected, output.toString());
	}
	

}
