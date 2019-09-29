package com.bruhnNewtech.sensorserverws;
/***********************************************************************
 *
 *                      Copyright (c) Bruhn NewTech A/S 
 *                           All rights reserved.
 *
 ***********************************************************************
 *                      MessageEncoder.java
 ***********************************************************************
     $Header: MessageEncoder.java $
 **********************************************************************/

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import com.google.gson.Gson;

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */
public class MessageEncoder implements Encoder.Text<Message> 
{
    /**
     * FIXME: Document
     *
     */
    public MessageEncoder()
    {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public String encode(final Message message) throws EncodeException 
    {
        final Gson gson = new Gson();
        final String json = gson.toJson(message);
        return json;
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
