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
package org.bigbluebutton.conference

import org.red5.server.api.Red5import org.bigbluebutton.conference.service.participants.ParticipantsApplicationimport org.bigbluebutton.conference.service.archive.ArchiveApplicationimport org.red5.logging.Red5LoggerFactory
import org.red5.server.adapter.ApplicationAdapter
import org.red5.server.adapter.IApplication
import org.red5.server.api.IClient
import org.red5.server.api.IConnection
import org.red5.server.api.IScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.red5.server.api.so.ISharedObject

public class BigBlueButtonApplication extends ApplicationAdapter{

	private static Logger log = Red5LoggerFactory.getLogger(BigBlueButtonApplication.class, "bigbluebutton")
	
	private static final String APP = "BigBlueButtonApplication"
	private ParticipantsApplication participantsApplication
	private ArchiveApplication archiveApplication
	
	private final String version
	
    public boolean appStart (IScope app )
    {
        log.debug "Starting BigBlueButton version $version" 
        return super.appStart(app)
    }
        
    public void appStop (IScope app )
    {
        log.debug( "${APP} - appStop" )
        super.appStop(app)
    }

    public boolean appConnect( IConnection conn , Object[] params )
    {
        log.debug( "${APP} - appConnect ")    	
        return super.appConnect(conn, params)
    }
    
    public void appDisconnect( IConnection conn)
    {
        log.debug( "${APP} - appDisconnect ")
        super.appDisconnect(conn)
    }
    
    public boolean roomStart(IScope room) {
    	log.debug( "${APP} - roomStart " )
    	assert participantsApplication != null
    	participantsApplication.createRoom(room.name)
    	return super.roomStart(room)
    }	
	
    public void roomStop(IScope room) {
    	log.debug( "${APP} - roomStop " )
    	super.roomStop(room)
    	assert participantsApplication != null
    	participantsApplication.destroyRoom(room.name)
    	BigBlueButtonSession bbbSession = getBbbSession()
    	assert bbbSession != null
    	if (Constants.PLAYBACK_MODE.equals(bbbSession.mode)) {
			log.debug( "${APP} - roomStop - destroying PlaybackSession ${bbbSession.sessionName}")
			assert archiveApplication != null
        	archiveApplication.destroyPlaybackSession(bbbSession.sessionName)
        	log.debug( "${APP} - roomStop - destroyed PlaybackSession ${bbbSession.sessionName}")
        } else {        	
			log.debug( "${APP} - roomStop - destroying RecordSession ${bbbSession.sessionName}")
			assert archiveApplication != null
        	archiveApplication.destroyRecordSession(bbbSession.sessionName)
        	log.debug( "${APP} - roomStop - destroyed RecordSession ${bbbSession.sessionName}")
        }
    }
    
	public boolean roomConnect(IConnection connection, Object[] params) {
    	log.debug( "${APP} - roomConnect - ")

        String username = ((String) params[0]).toString()
        log.debug( "${APP} - roomConnect - $username")
        String role = ((String) params[1]).toString()
        log.debug( "${APP} - roomConnect - $role")
        String conference = ((String)params[2]).toString()
        log.debug( "${APP} - roomConnect - $conference")
        String mode = ((String) params[3]).toString()
        log.debug( "${APP} - roomConnect - $mode")
        /*
         * Convert the id to Long because it gets converted to ascii decimal
         * equivalent (i.e. zero (0) becomes 48) if we don't.
         */
        def userid = Red5.connectionLocal.client.id.toLong()
        log.debug( "${APP} - roomConnect - $userid")
        def sessionName = connection.scope.name
        log.debug( "${APP} - roomConnect - $sessionName")
        
        def room   
        String voiceBridge = ((String) params[5]).toString()
		Boolean record
		
        if (Constants.PLAYBACK_MODE.equals(mode)) {
        	room = ((String) params[4]).toString()   
        	assert archiveApplication != null
			log.debug( "${APP} - roomConnect - creating PlaybackSession $sessionName")
        	archiveApplication.createPlaybackSession(conference, room, sessionName)
        } else {
        	room = sessionName
        	assert archiveApplication != null			
			voiceBridge = ((String) params[5]).toString()
			record = ((String) params[6]).toBoolean()
        	log.debug ("Got params $voiceBridge and $record")
			if (record == true) {
				log.debug( "${APP} - roomConnect - creating RecordSession $sessionName")
				archiveApplication.createRecordSession(conference, room, sessionName)
			}
			
        }
    	log.debug( "${APP} - roomConnect - creating BigBlueButtonSession")
    	BigBlueButtonSession bbbSession = new BigBlueButtonSession(sessionName, userid,  username, role, 
    			conference, mode, room, voiceBridge, record)
    	log.debug( "${APP} - roomConnect - setting attribute BigBlueButtonSession")
        connection.setAttribute(Constants.SESSION, bbbSession)        
		log.debug("${APP} - roomConnect - [${username},${role},${conference},${room}]") 

        super.roomConnect(connection, params)
    	return true;
	}

	public String getMyUserId() {
		log.debug("Getting userid for connection.")
		BigBlueButtonSession bbbSession = Red5.connectionLocal.getAttribute(Constants.SESSION)
		assert bbbSession != null
		return bbbSession.userid;
	}
	
	public void setParticipantsApplication(ParticipantsApplication a) {
		log.debug("Setting participants application")
		participantsApplication = a
	}
	
	public void setArchiveApplication(ArchiveApplication a) {
		log.debug("Setting archive application")
		archiveApplication = a
	}
	
	public void setApplicationListeners(Set listeners) {
		log.debug("Setting application listeners")
		def count = 0
		Iterator iter = listeners.iterator()
		while (iter.hasNext()) {
			log.debug("Setting application listeners $count")
			super.addListener((IApplication) iter.next())
			count++
		}
		log.debug("Finished Setting application listeners")
	}
	
	public void setVersion(String v) {
		version = v
	}
	
	private BigBlueButtonSession getBbbSession() {
		return Red5.connectionLocal.getAttribute(Constants.SESSION)
	}
}
