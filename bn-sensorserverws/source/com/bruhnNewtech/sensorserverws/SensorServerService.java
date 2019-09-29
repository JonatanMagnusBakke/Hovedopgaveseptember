package com.bruhnNewtech.sensorserverws;

import java.util.Vector;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.bruhnNewtech.commons.constants.api.SensorServerApi.ConfigurationProviderAPI;
import com.bruhnNewtech.commons.exception.XMLException;
import com.bruhnNewtech.commons.exception.XMLParseException;
import com.bruhnNewtech.commons.logging.BNLogger;
import com.bruhnNewtech.commons.logging.BNLoggerFactory;
import com.bruhnNewtech.commons.util.StringUtil;
import com.bruhnNewtech.commons.util.XMLUtil;
import com.bruhnNewtech.filterbuilder.filter.RequestResponseUtility;
import com.bruhnNewtech.sensorserver.filter.sensoroverview.SensorOverviewManager;
import com.bruhnNewtech.sensorserver.server.ConfigurationProvider;
import com.bruhnNewtech.sensorserver.server.SensorAttributes;
import com.bruhnNewtech.sensorserver.server.SensorType;

/**
 *
 *
 * <pre>
 * Copyright © 2015 Bruhn NewTech A/S. All rights reserved.
 * </pre>
 *
 */
@Path("/scim/v1.0")
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class SensorServerService
{
    /** Private logger for this class */
    private static BNLogger           logger = BNLoggerFactory.getLogger(SensorServerService.class);
    /**  */
    private SensorServerServiceFilter messageFilter;

    /**
     *
     * @param msgFilter
     */
    public SensorServerService(final SensorServerServiceFilter msgFilter)
    {
        this.messageFilter = msgFilter;
    }

    @SuppressWarnings("resource")
    private Response validateAPIKey(final String apiKey)
    {
        Response rsp = null;
        if(null == apiKey)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("status, APIKey not present"); //$NON-NLS-1$
            }
            rsp = Response.status(Status.BAD_REQUEST).entity("apikey header parameter is mandatory").build(); //$NON-NLS-1$
        }
        else
        {
            if(apiKey.startsWith("abc")) //$NON-NLS-1$
            {
                rsp = Response.status(Status.OK).build();
            }
            else
            {
                if(logger.isDebugEnabled())
                {
                    logger.debug("status, invalid APIKey:" + apiKey); //$NON-NLS-1$
                }
                rsp = Response.status(Status.UNAUTHORIZED).build();
            }
        }
        return rsp;
    }

    /**
     * Handle the group status 
     * @return response 
     * @throws XMLParseException 
     */
    @GET
    @Path("/groups/status")
    public Response groupsStatus(@HeaderParam("apikey") final String apiKey) throws XMLParseException
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            final Document doc = XMLUtil.createDocumentFromElement(messageFilter.getCacheGroupsStatus());
            rsp = Response.ok(doc).build();
        }
        return rsp;
    }
    
    
    /**
     * FIXME: Document
     *
     * @param apiKey
     * @return
     * @throws XMLParseException
     */
    @GET
    @Path("/groups/primary")
    public Response getPrimaryGroups(@HeaderParam("apikey") final String apiKey) throws XMLParseException
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            final Document responseDoc = XMLUtil.createDocument();
            final Element responseElem = responseDoc.createElement("Response"); //$NON-NLS-1$
            responseDoc.appendChild(responseElem);
            final Vector<String> grps = ConfigurationProvider.getInstance().getGroups();
            for(String s : grps)
            {
                
                if(ConfigurationProvider.getInstance().isGroupPrimary(s))
                {
                    final Element groupElement = responseDoc.createElement("Groups"); //$NON-NLS-1$
                    groupElement.setAttribute("group", s); //$NON-NLS-1$
                    responseElem.appendChild(groupElement);
               }
            }
            
            rsp = Response.ok(responseDoc).build();
        }
        
        return rsp;
    }

    /**
     * Handle the sensor list 
     * @return response 
     */
    @GET
    @Path("/sensors/configuration")
    public Response sensors(@HeaderParam("apikey") final String apiKey)
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            Document responseDoc = null;           
            final Vector<SensorAttributes> sensorList = ConfigurationProvider.getInstance().getSensors();
            try
            {
                responseDoc = XMLUtil.createDocument();
                final Element responseElem = responseDoc.createElement("Response"); //$NON-NLS-1$
                responseDoc.appendChild(responseElem);
                for(final SensorAttributes sensor : sensorList)
                {
                    final Element sensorElement = responseDoc.createElement(ConfigurationProviderAPI.TAG_SENSOR);
                    sensorElement.setAttribute("id", sensor.getId()); //$NON-NLS-1$
                    sensorElement.setAttribute("name", sensor.getName()); //$NON-NLS-1$
                    sensorElement.setAttribute("type", sensor.getType().name()); //$NON-NLS-1$
                    sensorElement.setAttribute("model", sensor.getModel()); //$NON-NLS-1$
                    if(!StringUtil.isNullOrEmpty(sensor.getGroup()))
                    {
                        sensorElement.setAttribute("group", sensor.getGroup()); //$NON-NLS-1$
                    }
                    if((sensor.getType() == SensorType.GPS) && (sensor.getDefault()))
                    {
                        sensorElement.setAttribute("isGpsDefault", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                        sensorElement.setAttribute("showSummary", "false"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    // Add secondary group(s)
                    for(String secGrp : sensor.getSecondaryGroups())
                    {
                        final Element grpElement = responseDoc.createElement(ConfigurationProviderAPI.TAG_SECONDARYGROUP);
                        grpElement.setTextContent(secGrp);
                        sensorElement.appendChild(grpElement);
                    }

                    // Add capability
                    for(String capaName : sensor.getCapabilityNames())
                    {
                        final Element capaElement = responseDoc.createElement(ConfigurationProviderAPI.TAG_CAPABILITY);
                        capaElement.setTextContent(capaName);
                        sensorElement.appendChild(capaElement);
                    }

                    responseElem.appendChild(sensorElement);
                }
            }
            catch(final XMLParseException e)
            {
                // TODO Handle Exception
            }
            if(logger.isDebugEnabled())
            {
                String responseXML = StringUtil.EMPTY_STRING;
                try
                {
                    responseXML = new String(XMLUtil.convertDomToBytes(responseDoc));
                }
                catch(final XMLException e)
                {
                    // Debug logging no action
                }
                logger.debug("sensors\n" + responseXML); //$NON-NLS-1$
            }

            rsp = Response.ok(responseDoc).build();
        }
        return rsp;
    }

    /**
     * Handle the sensor list 
     * @return response 
     */
    @GET
    @Path("/sensors/status")
    public Response status(@HeaderParam("apikey") final String apiKey)
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            Document responseDoc = null;
            try
            {
                responseDoc = XMLUtil.createDocument();
                final Element responseElem = responseDoc.createElement("Response"); //$NON-NLS-1$
                responseDoc.appendChild(responseElem);
                final Element sensorVierviewElem = SensorOverviewManager.getInstance().generateSensorOverview(responseDoc, "", false); //$NON-NLS-1$
                responseElem.appendChild(sensorVierviewElem);
            }
            catch(final XMLParseException e1)
            {
                // TODO Handle Exception
            }
            if(logger.isDebugEnabled())
            {
                String responseXML = StringUtil.EMPTY_STRING;
                try
                {
                    responseXML = new String(XMLUtil.convertDomToBytes(responseDoc));
                }
                catch(final XMLException e)
                {
                    // Debug logging no action
                }
                logger.debug("status\n" + responseXML); //$NON-NLS-1$
            }
            rsp = Response.ok(responseDoc).build();
        }
        return rsp;
    }

    /**
     * Handle the last sensor reading 
     * @return response 
     */
    @GET
    @Path("/sensors/{id}")
    public Response status(@HeaderParam("apikey") final String apiKey, @PathParam("id") final String id)
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            final Element sensorData = messageFilter.getCacheSensorData().get(id);
            if (sensorData != null)
            {
                Document responseDoc = null;
                try
                {
                    responseDoc = XMLUtil.createDocumentFromElement(sensorData);
                }
                catch(final XMLParseException e1)
                {
                    System.out.println(e1.getLocalizedMessage());
                }
                if(logger.isDebugEnabled())
                {
                    String responseXML = StringUtil.EMPTY_STRING;
                    try
                    {
                        responseXML = new String(XMLUtil.convertDomToBytes(responseDoc));
                    }
                    catch(final XMLException e)
                    {
                        // Debug logging no action
                    }
                    logger.debug("status\n" + responseXML); //$NON-NLS-1$
                }
                rsp = Response.ok(responseDoc).build();
            }
            else
            {
                rsp = Response.status(Status.NOT_FOUND).build();
            }
        }
        return rsp;
    }

    /**
     * Handle the last sensor reading 
     * @return response 
     * @throws XMLParseException 
     */
    @SuppressWarnings("resource")
    @PUT
    @Path("/sensors/{id}/request")
    public Response updateState(final Document request, @HeaderParam("apikey") final String apiKey, @PathParam("id") final String id) throws XMLParseException
    {
        Response rsp = validateAPIKey(apiKey);
        if(rsp.getStatus() == 200)
        {
            final Element sensorData = messageFilter.getCacheSensorData().get(id);
            if (sensorData != null)
            {
                final NodeList actions = request.getElementsByTagName("Action"); //$NON-NLS-1$
                final Element action = actions.getLength() == 1 ? (Element) actions.item(0) : null;
                if(action != null && !action.getTextContent().isEmpty() && !id.isEmpty())
                {
                    try
                    {
                        final Document req = RequestResponseUtility.createRequest("", "", action.getTextContent(), id);  //$NON-NLS-1$//$NON-NLS-2$
                        messageFilter.submitRequest(req);
                        rsp = Response.ok().build();
                    }
                    catch(final ParserConfigurationException e)
                    {
                        rsp = Response.serverError().build();
                    }
                }
                else
                {
                    rsp = Response.status(Status.BAD_REQUEST).build();
                }
            }
            else
            {
                rsp = Response.status(Status.NOT_FOUND).build();
            }
        }
        return rsp;
    }
}
