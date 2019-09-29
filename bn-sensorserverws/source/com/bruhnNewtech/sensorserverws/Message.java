package com.bruhnNewtech.sensorserverws;
/***********************************************************************
 *
 *                      Copyright (c) Bruhn NewTech A/S 
 *                           All rights reserved.
 *
 ***********************************************************************
 *                      Message.java
 ***********************************************************************
     $Header: Message.java $
 **********************************************************************/

/**
 * FIXME: Class Documentation Here
 *
 * @version $Revision: $
 * @since   27 May 2019
 */

public class Message 
{
    
    
    private String from;
    private String to;
    private String content;
    
    Message()
    {
        
    }

    @Override
    public String toString() 
    {
        return super.toString();
    }

    /**
     * FIXME: Document
     *
     * @return
     */
    public String getFrom() 
    {
        return from;
    }

    /**
     * FIXME: Document
     *
     * @param from
     */
    public void setFrom(final String from) 
    {
        this.from = from;
    }

    public String getTo() 
    {
        return to;
    }

    /**
     * FIXME: Document
     *
     * @param to
     */
    public void setTo(final String to) 
    {
        this.to = to;
    }

    /**
     * FIXME: Document
     *
     * @return
     */
    public String getContent() 
    {
        return content;
    }

    /**
     * FIXME: Document
     *
     * @param content
     */
    public void setContent(final String content) 
    {
        this.content = content;
    }
}

/***********************************************************************
    $Log: $
***********************************************************************/
