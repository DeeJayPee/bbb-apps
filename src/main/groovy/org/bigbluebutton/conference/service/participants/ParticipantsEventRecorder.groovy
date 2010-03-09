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

package org.bigbluebutton.conference.service.participants

import java.util.Map
import org.bigbluebutton.conference.service.archive.record.IEventRecorder
import org.bigbluebutton.conference.service.archive.record.IRecorder
import org.bigbluebutton.conference.IRoomListenerimport org.red5.server.api.so.ISharedObject
import org.bigbluebutton.conference.Participant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.red5.logging.Red5LoggerFactory
import groovy.xml.MarkupBuilder

public class ParticipantsEventRecorder implements IEventRecorder, IRoomListener {
	private static Logger log = Red5LoggerFactory.getLogger( ParticipantsEventRecorder.class, "bigbluebutton" )
	
	IRecorder recorder
	private ISharedObject so
	private final Boolean record
	
	def name = 'PARTICIPANT'
	
	def acceptRecorder(IRecorder recorder){
		log.debug("Accepting IRecorder")
		this.recorder = recorder
	}
	
	def getName() {
		return name
	}
	
	def recordEvent(Map event){
		if (record) {
			recorder.recordEvent(event)
		}
		
	}
	
	public ParticipantsEventRecorder(ISharedObject so, Boolean record) {
		this.so = so 
		this.record = record
	}
	
	public void participantStatusChange(Long userid, String status, Object value){
		log.debug("A participant's status has changed ${userid} $status $value.")
		so.sendMessage("participantStatusChange", [userid, status, value])

		}
	
	public void participantJoined(Participant p) {
		log.debug("A participant has joined ${p.userid}.")
		List args = new ArrayList()
		args.add(p.toMap())
		log.debug("Sending participantJoined ${p.userid} to client.")
		so.sendMessage("participantJoined", args)
		
	}
	
	public void participantLeft(Long userid) {		
		List args = new ArrayList()
		args.add(userid)
		so.sendMessage("participantLeft", args)
	}

	public void endAndKickAll() {
		so.sendMessage("logout", new ArrayList());
	}	
}
