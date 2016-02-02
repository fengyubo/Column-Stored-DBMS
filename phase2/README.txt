CS 2550 Project Phase 2
Submitted by:
Alaa Alaerjan	(asa90)
Yubo Feng	(yuf24)
Longhao Li	(lol16)
Nathan Ong	(nro5)

Notes:
------
 * Ensure that the machine you run on has Java 8 installed.
 * Ensure that your scripts and table files are in the same directory as the jar file, or specify the path in the command line.

Running the script:
-------------------
To run the jar file, change directory into the folder where the jar is located, and type:

java -jar CPU.jar <buffer_size_bytes> <seed> <tableName.txt> [<tableName2.txt> ...] X <script.txt> [<script2.txt> ...]

where:
<buffer_size_bytes> 	is the buffer size in bytes,
<seed> 			is the random seed that you want to give to the scheduler,
				(Note, giving a seed of 0 will initiate the Round Robin scheduler)
<tableName.txt> 	is the contents of tableName at the start of the execution of the program,
X			is the delimiter between tables and scripts (REQUIRED),
<script.txt>		is the name of the script file.
				(Note, having more than one script will run them simultaneously)

Contents:
---------
CPU.jar 		The jar file to run the program.
src/			The folder containing all of our source material.
table\ /files		The folder containing all of our table files.
test\ /cases		The folder containing all of our test scripts.
dataManager.log		A sample data manager log file (run on bufferSize = 10000, seed = 1, Tables: X, Y, Z; Scripts: t1 - t16).
afterImage.log		A sample after image log file (run on bufferSize = 10000, seed = 1, Tables: X, Y, Z; Scripts: t1 - t16).
