/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.

 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. Venika Thakur - a1970789
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

/**
 * Created in the VideoPlayerApp's main function to receive and parse the video player's command-line arguments.
 *
 * <p>
 * Extracts the video file and supported launch options, reports invalid input
 * or help text, and exposes the resulting launch configuration via methods to the
 * {@link VideoPlayerApp}.
 * </p>
 */
public class CommandLineController {
    private final String[] args;

    private boolean helpRequested;
    private boolean audioRequested;
    private boolean maximiseRequested;
    private boolean subtitleRequested;
    private Integer displayId;
    private File videoFile; //stores the video file we give it 
    private String errorMessage;
    
    private final List<FrameProcessor> frameProcessors = new ArrayList<>();

    public CommandLineController(String[] args) {
        this.args = args.clone();
        parse();
        if (errorMessage != null) {
            System.out.println(errorMessage);
        }
        if (helpRequested) {
            printHelp();
        }
    }

    public File getVideoFile() {
        return videoFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getExitCode() {
        return errorMessage == null ? 0 : 1;
    }

    public Integer getDisplayId() {
        return displayId;
    }

    public boolean isAudioRequested() {
        return audioRequested;
    }

    public boolean isMaximiseRequested() {
        return maximiseRequested;
    }
    
    public boolean isSubtitleRequested() {
    	return subtitleRequested;
    }
    
    
    public List<FrameProcessor> getFrameProcessors() {
    	return List.copyOf(frameProcessors);
    }

    public boolean shouldLaunchApplication() {
        return errorMessage == null && videoFile != null;
    }

    private void parse() {
        List<String> videoFiles = new ArrayList<>();
        for (String arg : args) {
            if ("-h".equals(arg) || "--help".equals(arg)) {
                helpRequested = true;
            } else if ("-a".equals(arg) || "--audio".equals(arg)) {
                audioRequested = true;
            } else if ("-x".equals(arg) || "--maximise".equals(arg)) {
                maximiseRequested = true;
            } else if ("-1".equals(arg) || "--monitor-1".equals(arg)) {
                setDisplayId(1);
            } else if ("-2".equals(arg) || "--monitor-2".equals(arg)) {
            	setDisplayId(2);
            } else if ("-S".equals(arg) || "--subtitle-frames".equals(arg)) {
                subtitleRequested = true;
            } else if (parseFrameProcessorOption(arg)) {
            	// Frame processor option was recognised and added. 
            } else if (arg.startsWith("-")) {
            	errorMessage = "Unknown option: " + arg;
            } else {
                videoFiles.add(arg);
            }
        }

        if (videoFiles.size() > 1) {
            errorMessage = "Usage: VideoPlayer [options] [video-file]";
        } else if (videoFiles.size() == 1) {
            videoFile = new File(videoFiles.get(0));
            if (!videoFile.isFile()) {
                errorMessage = "File not found: " + videoFile.getPath();
                videoFile = null;
            }
        } else {
            if (!helpRequested) {
                errorMessage = "No video file specified.";
            }
        }
    }
    
    /**
     * Parses frame-processor command-line options while preserving the order.
     * 
     * To add a new FrameProcessor, add its long option in this method, add its short option in createFrameProcessor(char), and add the matching entry to printHelp().
     * 
     * Keeping processor creation separate prevents VideoPlayerModel from command-line syntax.
     */
    
    private boolean parseFrameProcessorOption(String arg) {
    	FrameProcessor processor = null;
    	
    	switch (arg) {
    	case "--number-frames":
    		processor = new FrameNumberer();
    		break;
    	case "--scratch-frames":
    		processor = new FrameScratcher();
    		break;
    	case "--flicker-frames":
    		processor = new FrameFlickerer();
    		break;
    	case "--black-and-white":
    		processor = new FrameBlackAndWhiter();
    		break;
    	case "--yellow-frames":
    		processor = new FrameYellower();
    		break;
    	case "--vignette-frames":
    		processor = new FrameVignetter();
    		break;
    	case "--dust-frames":
    		processor = new FrameDuster();
    		break;
    	case "--jitter-frames":
    		processor = new FrameJitterer();
    		break;
    	case "--mottle-frames":
    		processor = new FrameMottler();
    		break;
    	case "--bleed-frames":
    		processor = new FrameBleeder();
    		break;
    	case "--pepper-frames":
    		processor = new FramePepperer();
    		break;
    	default:
    		break;
    	
    }
    	
    if (processor != null) {
    	frameProcessors.add(processor);
    	return true;
    }
    
    if (arg.startsWith("-") && !arg.startsWith("--") && arg.length() > 1) {
    	List<FrameProcessor> stackedProcessors = new ArrayList<>();
    	
    	for (int i = 1; i < arg.length(); i++) {
    		FrameProcessor stackedProcessor = createFrameProcessor(arg.charAt(i));
    		
    			if (stackedProcessor == null) {
    				return false;
    			}
    			
    			stackedProcessors.add(stackedProcessor);
    		}
    		
    		frameProcessors.addAll(stackedProcessors);
    		return true;
    	}
    	
    	return false;
    }
    
    private FrameProcessor createFrameProcessor(char option) {
    	switch (option) {
    	case 'n':
    		return new FrameNumberer();
    	case 's':
    		return new FrameScratcher();
    	case 'f':
    		return new FrameFlickerer();
    	case 'w':
    		return new FrameBlackAndWhiter();
    	case 'y':
    		return new FrameYellower();
    	case 'v':
    		return new FrameVignetter();
    	case 'd':
    		return new FrameDuster();
    	case 'j':
    		return new FrameJitterer();
    	case 'm':
    		return new FrameMottler();
    	case 'b':
    		return new FrameBleeder();
    	case 'p':
    		return new FramePepperer();
    	default:
    		return null;
    	}
    }

    private void setDisplayId(int displayId) {
        if (this.displayId != null && this.displayId != displayId) {
            errorMessage = "Only one display option can be used";
            return;
        }
        this.displayId = displayId;
    }

    private void printHelp() {
        System.out.println("Usage: VideoPlayer [options] [video-file]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help         Show this help message");
        System.out.println("  -a, --audio        Play audio");
        System.out.println("  -x, --maximise     Open the player maximised");
        System.out.println("  -1, --monitor-1    Open the player on display 1");
        System.out.println("  -2, --monitor-2    Open the player on display 2");
        System.out.println();
        
        System.out.println("Frame processors:");
        System.out.println("  -n, --number-frames    Render the frame number onto each frame");
        System.out.println("  -s, --scratch-frames   Render vertical film scratches");
        System.out.println("  -f, --flicker-frames   Randomly dim frames");
        System.out.println("  -w, --black-and-white  Convert frames to black and white");
        System.out.println("  -y, --yellow-frames    Apply a warmer colour temperature");
        System.out.println("  -v, --vignette-frames  Darken the frame edges");
        System.out.println("  -d, --dust-frames      Render dust and hair marks");
        System.out.println("  -j, --jitter-frames    Randomly displace frames by a few pixels");
        System.out.println("  -m, --mottle-frames    Add cloudy emulsion mottling");
        System.out.println("  -b, --bleed-frames     Bleed light into frames");
        System.out.println("  -p, --pepper-frames    Pepper frames with dark spots/blotches");
        System.out.println("  -S, --subtitle-frames  Add speech-to-text subtitles to frames");
        System.out.println();

        System.out.println("Frame processors are applied in command-line order and may be repeated.");
        System.out.println("Example: -nssnfwyvdjmbp numbers, scratches twice, numbers again, flickers,");
        System.out.println("converts, warms, vignettes, dusts, jitters, mottles, bleeds, then peppers.");
    }
}
