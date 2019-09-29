package com.bruhnNewtech.sensorserverws;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import javax.ws.rs.Path;
/***********************************************************************
 *
 *                      Copyright (c) Bruhn NewTech A/S 
 *                           All rights reserved.
 *
 ***********************************************************************
 *                      NotificationEndpoint.java
 ***********************************************************************
     $Header: NotificationEndpoint.java $
 **********************************************************************/

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */
//@ServerEndpoint(value = "/chat/{username}", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
@Path("/chat/{username}")
public class NotificationEndpoint
{
    private static Set<NotificationEndpoint> chatEndpoints = new CopyOnWriteArraySet<>();
    private static HashMap<String, String> users = new HashMap<>();
    private Session m_session;
    
    NotificationEndpoint()
    {
    }
 
    /**
     * FIXME: Document
     *
     * @param session
     * @param username
     * @throws IOException
     * @throws EncodeException 
     */
    @OnOpen
    public void onOpen(
      final Session session, 
      @PathParam("username") final String username) throws IOException, EncodeException 
    {
  
        this.m_session = session;
        chatEndpoints.add(this);
        users.put(session.getId(), username);
 
        final Message message = new Message();
        message.setFrom(username);
        message.setContent("Connected!");
        broadcast(message);
    }
 
    /**
     * FIXME: Document
     *
     * @param session
     * @param message
     * @throws IOException
     * @throws EncodeException 
     */
    @OnMessage
    public void onMessage(final Session session, final Message message) 
      throws IOException, EncodeException 
    {
  
        message.setFrom(users.get(session.getId()));
        broadcast(message);
    }
 
    /**
     * FIXME: Document
     *
     * @param session
     * @throws IOException
     * @throws EncodeException 
     */
    @OnClose
    public void onClose(final Session session) throws IOException, EncodeException 
    {
  
        chatEndpoints.remove(this);
        final Message message = new Message();
        message.setFrom(users.get(session.getId()));
        message.setContent("Disconnected!");
        broadcast(message);
    }
 
    /**
     * FIXME: Document
     *
     * @param session
     * @param throwable
     */
    @OnError
    public void onError(final Session session, final Throwable throwable) 
    {
        System.out.println("WE have a problem"); //$NON-NLS-1$
        System.out.println(throwable);
    }
 
    private static void broadcast(final Message message) 
      throws IOException, EncodeException 
    {
  
        chatEndpoints.forEach(endpoint -> 
        {
            synchronized (endpoint) 
            {
                try 
                {
                    endpoint.m_session.getBasicRemote().
                      sendObject(message);
                } 
                catch (final IOException | EncodeException e) 
                {
                    e.printStackTrace();
                }
            }
        });
    }
}

/***********************************************************************
    $Log: $
***********************************************************************/
