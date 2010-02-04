/*
 * BigBlueButton - http://www.bigbluebutton.org
 * 
 * Copyright (c) 2008-2009 by respective authors (see below). All rights reserved.
 * 
 * BigBlueButton is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 3 of the License, or (at your option) any later 
 * version. 
 * 
 * BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with BigBlueButton; if not, If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: $
 */

package org.bigbluebutton.conference.service.archive.record

import org.ho.yaml.YamlEncoderimport org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.red5.logging.Red5LoggerFactory

public class FileRecorder implements IRecorder{
	private static Logger log = Red5LoggerFactory.getLogger( FileRecorder.class, "bigbluebutton" )
	
	private final String conference
	private final String room
	private final String recordingsDirectory
	private final File recordingFile
	private final File roomDir
	
	public FileRecorder(String conference, String room) {
		this.conference = conference
		this.room = room
	}
	
	public void initialize() {
		roomDir = new File("$recordingsDirectory/$conference/$room")
		if (! roomDir.exists())
			roomDir.mkdirs()
		// Comment out for now..let's use yaml at the moment (ralam 4/17/2009)
		//recordingFile = new File(roomDir.canonicalPath + File.separator + "recordings.xml" )
		//recordingFile = new File(roomDir.canonicalPath + File.separator + "recordings.yaml" )
		/**
		 * We do not actually want to delete the file. We just want to append to it.
		 */
		 //deleteRecording()
	}
	
	public void deleteRecording() {
		if (recordingFile.exists()) {
			// delete the file so we start fresh
			recordingFile.delete()
			recordingFile = new File(roomDir.canonicalPath + File.separator + "recordings.yaml" )
		}		
	}
	
	public void recordEvent(Map event) {
		log.debug("Recording event to file ${recordingFile.absolutePath}.")
		FileOutputStream fout = new FileOutputStream(recordingFile, true /*append*/)
		log.debug("Recording event to file - got output stream.")
		if (fout == null) {
			log.debug("Outputstream is null")
		} else {
			log.debug("Outputstream is NOT null")
		}
		
		Thread.start {
			YamlEncoder enc = new YamlEncoder(fout)
			log.debug("Recording event to file - setting up encoder.")
	        enc.writeObject(event)
	        log.debug("Recording event to file - writing the event to file.")
	        enc.close()  
	        log.debug("Recorded event to file - closed encoder.")
		}        
	}
	
	def recordXmlEvent(event) {		
		Thread.start {
			recordingFile.append(event)
		}  
	}
	
	public void setRecordingsDirectory(String directory) {
		recordingsDirectory = directory
	}
}
