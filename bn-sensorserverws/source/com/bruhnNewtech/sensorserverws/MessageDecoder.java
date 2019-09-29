package com.bruhnNewtech.sensorserverws;
/***********************************************************************
 *
 *                      Copyright (c) Bruhn NewTech A/S 
 *                           All rights reserved.
 *
 ***********************************************************************
 *                      MessageDecoder.java
 ***********************************************************************
     $Header: MessageDecoder.java $
 **********************************************************************/

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */
public class MessageDecoder implements Decoder.Text<Message> 
{
    /**
     * FIXME: Document
     *
     */
    public MessageDecoder()
    {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public Message decode(final String s) throws DecodeException 
    {
        final Gson gson = new Gson();
        final Message message = gson.fromJson(s, Message.class);
        return message;
    }

    @Override
    public boolean willDecode(final String s) 
    {
        return (s != null);
    }

    @Override
    public void init(final EndpointConfig endpointConfig) 
    {
        // Custom initialization logic
    }

    @Override
    public void destroy() 
    {
        // Close resources
    }
}

/***********************************************************************
    $Log: $
***********************************************************************/
