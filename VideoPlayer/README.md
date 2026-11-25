# COMP3011 Assignment 2 - Java Local Project

# Author: Venika Thakur 

# a1970789

## Overview:

This project is an extension to the already supplied JavaFX video player with command-line configurable frames and multithreaded frame-processing architecture. 

The main goal was to improve video playback performance when there are multiple frames being used, whilst also keeping the original video player features working. 

The following effects can be added from the command line:
- 'n' or --number-frames 
- 's' or --scratch-frames
- 'f' or --flicker-frames
- 'w' or --black-and-white
- 'y' or --yellow-frames 
- 'v' or --vignette-frames 
- 'd' or --dust-frames 
- 'j' or --jitter-frames 
- 'm' or --mottle-frames 
- 'b' or --bleed-frames 
- 'p' or --pepper-frames 

The effects are applied in the same order they are written in the command line. 

For Example:
- j -f -f -f -n

FrameProcessingPipeline is responsible for the background frame processing. 
Each selected FrameProcessor is one stage of the pipeline and has a FIFO queue. Once a frame is submitted, and the effect is finished, it is placed into the next queue. This happens until every effect has been applied. 

- Concurrency Correctness 
Each FrameProcessor has its own worker thread and frames move through the pipeline in the same order as the command-line effects. 
BlockingQueue is used so the frames can safely pass between different threads. The frames are duplicated before entering the pipeline so it does not intefer with the decoder. 
When playback is stopped, the worker threads are stopped and hence are joined and the queues are cleared. 

The number of frames also allowed in the pipeline is limited so the effect workers do not use all available capacity, leave  capacity for JavaFX rendering. 

- GUI Responsiveness 
JavaFX remains responsible for playback timing, displaying completed frames, audio and user interface actions inlcuding pausing, resizing the window, and seeking. 
This allows the interface to remain responsive while multiple effects are being processed. 

- Performance + Testing 
This project includes automated tests for command-line controller and frame-processing pipeline. 
The tests can be run using:

1. mvn verify

The Maven test run should be completed with 10 tests, 0 failures and 0 errors. 

TITAN Part 1 testing reported 38/38 successfully implemented features,
including command-line parsing and correct frame output.

TITAN Part 2 reported:

- Performance: 386.04%
- Startup speed: 77%
- Use of cores: 7.43 out of 8

- Maven 
The project can be built and tested using:

mvn validate
mvn compile
mvn package
mvn verify
