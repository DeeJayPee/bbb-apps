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

package org.bigbluebutton.conference.service.voice.asterisk

import org.bigbluebutton.conference.service.voice.IVoiceServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.asteriskjava.live.AsteriskServer
import org.asteriskjava.live.AsteriskServerListener
import org.asteriskjava.live.DefaultAsteriskServer
import org.asteriskjava.live.ManagerCommunicationException
import org.asteriskjava.manager.ManagerConnectionimport org.asteriskjava.manager.AuthenticationFailedException
import org.asteriskjava.manager.ManagerConnection
import org.asteriskjava.manager.TimeoutException
import org.asteriskjava.live.AbstractAsteriskServerListener
import org.asteriskjava.live.MeetMeUser
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import org.asteriskjava.live.MeetMeUser
import org.asteriskjava.live.MeetMeUserState
import org.asteriskjava.live.MeetMeRoomimport org.bigbluebutton.conference.service.voice.IConferenceServerListenerimport org.red5.logging.Red5LoggerFactory
import org.asteriskjava.manager.ManagerConnectionState
public class AsteriskVoiceServer /*extends AbstractAsteriskServerListener*/ implements IVoiceServer, PropertyChangeListener{
	private static Logger log = Red5LoggerFactory.getLogger( AsteriskVoiceServer.class, "bigbluebutton" )

	private ManagerConnection managerConnection;	
	private AsteriskServer asteriskServer = new DefaultAsteriskServer();
	private IConferenceServerListener conferenceServerListener
	
	/**
	 * This sends pings to our Asterisk server so Asterisk won't close the connection if there
	 * is no traffic.
	 */
	private PingThread pingThread;
	private boolean waitForMessage = true;
	def amiThread
	
	
	def start(){
		amiThread = new Thread() {
	        log.info("Staring AMI Asterisk service...");
	        
			try {
				log.debug("Logging at " + managerConnection.getHostname() + ":" + 
						managerConnection.getPort())
				
				managerConnection.login()
				((DefaultAsteriskServer)asteriskServer).setManagerConnection(managerConnection)	
				asteriskServer.addAsteriskServerListener(this)
				((DefaultAsteriskServer)asteriskServer).initialize()
				
				pingThread = new PingThread(managerConnection)
				pingThread.setTimeout(40000)
				pingThread.start()
			} catch (IOException e) {
				log.error("IOException while connecting to Asterisk server.")
			} catch (TimeoutException e) {
				log.error("TimeoutException while connecting to Asterisk server.")
			} catch (AuthenticationFailedException e) {
				log.error("AuthenticationFailedException while connecting to Asterisk server.")
			} catch (ManagerCommunicationException e) {
				log.error(e.printStackTrace())
			}
		}
		amiThread.start()
	}
	

	def stop(){
		try {
			pingThread.die()
			managerConnection.logoff()
		} catch (IllegalStateException e) {
			log.error("Logging off when Asterisk Server is not connected.")
		} finally {
			amiThread.stop()
		}
	}
	
	def mute(user, conference, mute) {
		log.debug("mute: $user $conference $mute")
		MeetMeRoom room = getMeetMeRoom(conference)
		
		if (room == null) return
		
		Collection<MeetMeUser> users = room.getUsers()
		
		for (Iterator it = users.iterator(); it.hasNext();) {
    		MeetMeUser muser = (MeetMeUser) it.next();
    		if (user == muser.getUserNumber()) {
    			if (mute) {
    				muser.mute()
    			} else {
    				muser.unmute()
    			}
    		}
    	}
	}
	
	def kick(user, conference) {
		log.debug("Kick: $user $conference")
		MeetMeRoom room = getMeetMeRoom(conference)
		
		if (room == null) return
		
		Collection<MeetMeUser> users = room.getUsers()
		
		for (Iterator it = users.iterator(); it.hasNext();) {
    		MeetMeUser muser = (MeetMeUser) it.next();
    		if (user == muser.getUserNumber()) {
    			muser.kick()
    		}
    	}
	}
	
	def mute(conference, mute) {
		log.debug("Mute: $conference $mute")
		MeetMeRoom room = getMeetMeRoom(conference)
		
		if (room == null) return
		
		Collection<MeetMeUser> users = room.getUsers()
		
		for (Iterator it = users.iterator(); it.hasNext();) {
    		MeetMeUser muser = (MeetMeUser) it.next();    		
    		if (mute) {
    			muser.mute()
    		} else {
    			muser.unmute()
    		}
    	}
	}
	
	def kick(conference){
		log.debug("Kick: $conference")
		MeetMeRoom room = getMeetMeRoom(conference)
		
		if (room == null) return
		
		Collection<MeetMeUser> users = room.getUsers()
		
		for (Iterator it = users.iterator(); it.hasNext();) {
    		MeetMeUser muser = (MeetMeUser) it.next();    		
    		muser.kick()
    	}
	}
	
	def initializeRoom(conference){
		log.debug("initialize $conference")
		MeetMeRoom room = getMeetMeRoom(conference)
		
		if (room == null) return
		
		if (room.empty) {
			log.debug("$conference is empty.")
			return
		}
		
		Collection<MeetMeUser> users = room.getUsers()
		
		for (Iterator it = users.iterator(); it.hasNext();) {
    		MeetMeUser muser = (MeetMeUser) it.next();    		
    		newUserJoined(muser)
    	}
	}
	
    public void onNewMeetMeUser(MeetMeUser user)
    {
		log.info("New user joined meetme room: " + user.getRoom() + 
				" " + user.getChannel().getCallerId().getName());
		// add a listener for changes to this user
		user.addPropertyChangeListener(this)
		newUserJoined(user)
    }
    
    private newUserJoined(MeetMeUser user) {	
		String room = user.getRoom().getRoomNumber();
		String userid = user.getUserNumber().toString()
		String username = user.getChannel().callerId.name
		Boolean muted = user.isMuted()
		Boolean talking = user.isTalking()
		conferenceServerListener.joined(room, userid, username, muted, talking)
    }
    
	public void propertyChange(PropertyChangeEvent evt) {
		MeetMeUser changedUser = (MeetMeUser) evt.getSource();
	
		log.debug("Received property changed event for " + evt.getPropertyName() +
				" old = '" + evt.getOldValue() + "' new = '" + evt.getNewValue() +
				"' room = '" + ((MeetMeUser) evt.getSource()).getRoom() + "'");	
		
		if (evt.getPropertyName().equals("muted")) {				
			conferenceServerListener.mute(changedUser.userNumber.toString(), changedUser.room.roomNumber, changedUser.muted)
		} else if (evt.getPropertyName().equals("talking")) {				
			conferenceServerListener.talk(changedUser.userNumber.toString(), changedUser.room.roomNumber, changedUser.talking)
		} else if ("state".equals(evt.getPropertyName())) {
			if (MeetMeUserState.LEFT == (MeetMeUserState) evt.getNewValue()) {
				conferenceServerListener.left(changedUser.room.roomNumber, changedUser.userNumber.toString())
			}
		}			
	}    
	
	private MeetMeRoom getMeetMeRoom(String room) {
		if (managerConnection.getState() != ManagerConnectionState.CONNECTED) {
			log.error("No connection to the Asterisk server. Connection state is {}", managerConnection.getState().toString())
			return null
		}
		try {
			MeetMeRoom mr = asteriskServer.getMeetMeRoom(room)
			return mr
		} catch (ManagerCommunicationException e) {
			log.error("Exception error when trying to get conference ${room}")
		}
		return null
	}
	
	public void setManagerConnection(ManagerConnection connection) {
		log.debug('setting manager connection')
		this.managerConnection = connection
		log.debug('setting manager connection DONE')
	}
	
	public void setConferenceServerListener(IConferenceServerListener l) {
		log.debug('setting conference listener')
		conferenceServerListener = l
		log.debug('setting conference listener DONE')
	}
}
