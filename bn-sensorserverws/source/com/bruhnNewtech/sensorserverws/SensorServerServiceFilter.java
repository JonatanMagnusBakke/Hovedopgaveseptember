package com.bruhnNewtech.sensorserverws;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.DeploymentException;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.bruhnNewtech.commons.logging.BNLogger;
import com.bruhnNewtech.commons.logging.BNLoggerFactory;
import com.bruhnNewtech.commons.util.StringUtil;
import com.bruhnNewtech.commons.util.XMLUtil;
import com.bruhnNewtech.filterbuilder.filter.FilterEvent;
import com.bruhnNewtech.filterbuilder.filter.PipeException;
import com.bruhnNewtech.filterbuilder.filter.RequestResponseUtility;
import com.bruhnNewtech.filterbuilder.filter.ThreadedFilter;

//import org.glassfish.tyrus.server.Server;

/**
/**
 * The filter can be configured with following optional configuration options: 
 * <p>
 * <pre>
 * {@code
 * <Config>
 *      <WebService clientResponseTimeOut="5000"                    - default 10000 milliseconds
 *                  serverAddress="http://0.0.0.0:11010"            - default http://0.0.0.0:11008
 *                  discoveryDisable="false"                        - default false 
 *                  discoveryTypeName="CustomerService"             - default scim
 *                  discoveryScopeId="MyScope"                      - default host name of computer
 *                  discoveryURL = "http://192.168.109.126:11010"/> - default http://<default host IP address>:<port from serverAddress>
 * </Config>
 * }
 * </pre>
 * <p>
 *
 * <pre>
 * Copyright © 2016 Bruhn NewTech A/S. All rights reserved.
 * </pre>
 *
 */
public class SensorServerServiceFilter extends ThreadedFilter
{
    ////////////////////////////
    // Config constants
    private static final String ATTR_CLIENT_RESPONSE_TIME_OUT = "clientResponseTimeOut"; //$NON-NLS-1$
    private static final String ATTR_DISCOVERY_DISABLE        = "discoveryDisable";      //$NON-NLS-1$
    private static final String ATTR_SERVER_ADDRESS           = "serverAddress";         //$NON-NLS-1$
    private static final String ATTR_DISCOVERY_URL            = "discoveryURL";          //$NON-NLS-1$
    private static final String ATTR_DISCOVERY_SCOPE_ID       = "discoveryScopeId";      //$NON-NLS-1$
    private static final String ATTR_DISCOVERY_TYPE_NAME      = "discoveryTypeName";     //$NON-NLS-1$

    ////////////////////////////
    // Constants
    private static final String TAG_WEB_SERVICE         = "WebService";      //$NON-NLS-1$

    /** Private logger for this class */
    private static BNLogger logger = BNLoggerFactory.getLogger(SensorServerServiceFilter.class);

    ////////////////////////////
    // Configuration Parameters - start
    private String  discoveryTypeName    = "scim";        //$NON-NLS-1$
    private String  discoveryScopeId     = null;
    private String  discoveryURL         = null;
    //private String  serverAddress        = "http://0.0.0.0:11008"; //$NON-NLS-1$
    private String  serverAddress        = "ws://0.0.0.0:11008"; //$NON-NLS-1$
    private Boolean discoveryDisable     = false;
    // Response timeout in milliseconds
    private long    cbrnaResponseTimeOut = 10000;
    // Configuration Parameters - end
    ////////////////////////////

    ////////////////////////////
    // Internal
    private Server   messageServer     = null;
    // Host fields
    private int      hostRandomPort    = 0;
    private String   hostAddr          = null;
    @SuppressWarnings("unused")
    private String   hostFQDN          = null;
    private String   hostName          = null;
    private int      serverPort        = 0;
    // Cache the latest sensor reading on sensor id
    private HashMap<String, Element> cacheSensorData = new HashMap<>();
    // Cache the latest group status' (AggregatedState)
    private Element cacheGroupsStatus = null;
    
    /**
     *
     */
    public SensorServerServiceFilter()
    {
        // Empty
    }

    /**
     *
     * @param id
     */
    public SensorServerServiceFilter(final String id)
    {
        super(id);
    }

    /**
     * @return the cacheSensorData
     */
    public HashMap<String, Element> getCacheSensorData()
    {
        return cacheSensorData;
    }
    
    /**
     * @return the cacheGroupsStatus
     */
    public Element getCacheGroupsStatus()
    {
        return cacheGroupsStatus;
    }

    /**
     * @see com.bruhnNewtech.filterbuilder.filter.ThreadedFilter#processEvent(com.bruhnNewtech.filterbuilder.filter.ThreadedFilter.QueueElement)
     */
    @Override
    protected void processEvent(final QueueElement event)
    {
        if (RequestResponseUtility.isBroadCast(event.getEvent().getEventData()))
        {
            final Document doc = event.getEvent().getEventData();
            Element payload = XMLUtil.getFirstElementByTagName(doc, "Payload"); //$NON-NLS-1$
            if (payload != null)
            {
                final Element sensorData = XMLUtil.getFirstChildElement(payload);
                final String sensorId = sensorData.getAttribute("id"); //$NON-NLS-1$
                cacheSensorData.put(sensorId, sensorData);
            }
            else
            {
                payload = XMLUtil.getFirstElementByTagName(doc, "AggregatedState"); //$NON-NLS-1$
                if (payload != null)
                {
                    //Add notification here to tell the frontend when to update
                    cacheGroupsStatus  = payload;
                }
            }
        }
        try
        {
            if(event.getDirection().equals(Direction.NEXT))
            {
                dispatchNext(event.getEvent(), true);
            }
            else
            {
                dispatchPrevious(event.getEvent(), true);
            }
        }
        catch(final PipeException e)
        {
            if(logger.isFatalEnabled())
            {
                logger.fatal("Pipe error discovery message:\n" + "Explanation: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    void submitRequest(final Document request)
    {
        try
        {
            dispatchPrevious(new FilterEvent(request), true);
        }
        catch(final PipeException e)
        {
            if(logger.isFatalEnabled())
            {
                logger.fatal("Pipe error discovery message:\n" + "Explanation: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
    
    /**
     * @see com.bruhnNewtech.filterbuilder.filter.Filter#configure(java.lang.String, org.w3c.dom.Node)
     */
    @Override
    public void configure(final String id, final Node configNode)
    {
        super.configure(id, configNode);

        initHostInfo();

        final Element webElm = XMLUtil.getElementByTagName((Element)configNode, TAG_WEB_SERVICE);

        if(webElm != null)
        {
            discoveryTypeName = readConfigAttr(webElm, ATTR_DISCOVERY_TYPE_NAME, discoveryTypeName);
            discoveryScopeId = readConfigAttr(webElm, ATTR_DISCOVERY_SCOPE_ID, discoveryScopeId);
            discoveryURL = readConfigAttr(webElm, ATTR_DISCOVERY_URL, discoveryURL);
            serverAddress = readConfigAttr(webElm, ATTR_SERVER_ADDRESS, serverAddress);
            discoveryDisable = readConfigAttrBoolean(webElm, ATTR_DISCOVERY_DISABLE, discoveryDisable);
            cbrnaResponseTimeOut = readConfigAttrLong(webElm, ATTR_CLIENT_RESPONSE_TIME_OUT, cbrnaResponseTimeOut);
        }

        if(discoveryScopeId == null)
        {
            discoveryScopeId = hostName;
        }
        if(discoveryURL == null)
        {
            discoveryURL = "http://" + hostAddr; //$NON-NLS-1$
        }

        serverPort = getHostPort(serverAddress);
        serverAddress = setHostPort(serverAddress, serverPort);
        discoveryURL = setHostPort(discoveryURL, serverPort);
        
        if(logger.isDebugEnabled())
        {
            logger.debug(configToString());
        }
        
    }

    private final String configToString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Current config:\n"); //$NON-NLS-1$
        sb.append("\tserverAddress       : " + serverAddress + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\tdiscoveryTypeName   : " + discoveryTypeName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\tdiscoveryScopeId    : " + discoveryScopeId + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\tdiscoveryURL        : " + discoveryURL + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\tdiscoveryDisable    : " + discoveryDisable + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("\tcbrnaResponseTimeOut: " + cbrnaResponseTimeOut + " milliseconds\n"); //$NON-NLS-1$ //$NON-NLS-2$
        return sb.toString();
    }

    
    /**
     *
     * @param srvAddress
     * @param srvPort
     * @return srvAddress overwritten or added with srvPort
     */
    private String setHostPort(final String srvAddress, final int srvPort)
    {
        final String[] adrLst = srvAddress.split(":"); //$NON-NLS-1$
        return adrLst[0] + ":" + adrLst[1] + ":" + srvPort; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Extract hostPort from address
     *
     * @param srvAddress
     * @return the host port of the address or if not set a random host port
     */
    private int getHostPort(final String srvAddress)
    {
        int port = hostRandomPort;
        final String[] adrLst = srvAddress.split(":"); //$NON-NLS-1$
        if(adrLst.length > 2)
        {
            try
            {
            port = Integer.valueOf(adrLst[2]);
            }
            catch(final NumberFormatException e)
            {
                // Ignore
            }
        }
        return port;
    }
    
    
    /**
     * Get WebService config attribute
     *
     * @param webElm
     * @param attr name of attribute to get
     * @param defaultValue 
     * @return the value of the attribute
     */
    private String readConfigAttr(final Element webElm, final String attr, final String defaultValue)
    {
        String val = webElm.getAttribute(attr);
        if(StringUtil.isNullOrEmpty(val))
        {
            val = defaultValue;
        }
        return val;
    }

    /**
     * Get WebService config attribute as a long
     *
     * @param webElm
     * @param attr name of attribute to get
     * @param defaultValue 
     * @return the value of the attribute
     */
    private long readConfigAttrLong(final Element webElm, final String attr, final long defaultValue)
    {
        final String val = webElm.getAttribute(attr);
        long valLong = defaultValue;
        if(!StringUtil.isNullOrEmpty(val))
        {
            try
            {
                valLong = Integer.valueOf(val);
            }
            catch(final NumberFormatException e)
            {
                if(logger.isFatalEnabled())
                {
                    logger.fatal(attr + ": " + val + " is not a legal value"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

        }

        return valLong;
    }

    /**
     * Get WebService config attribute as a long
     *
     * @param webElm
     * @param attr name of attribute to get
     * @param defaultValue 
     * @return the value of the attribute
     */
    private Boolean readConfigAttrBoolean(final Element webElm, final String attr, final Boolean defaultValue)
    {
        final String val = webElm.getAttribute(attr);
        Boolean valLong = defaultValue;
        if(!StringUtil.isNullOrEmpty(val))
        {
            try
            {
                valLong = Boolean.valueOf(val);
            }
            catch(final NumberFormatException e)
            {
                if(logger.isFatalEnabled())
                {
                    logger.fatal(attr + ": " + val + " is not a legal value"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

        }

        return valLong;
    }

    /**
     * Get initial host information for the computer the service is running on.
     */
    private void initHostInfo()
    {

        try (ServerSocket sock = new ServerSocket())
        {
            final InetSocketAddress s = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            sock.bind(s);
            hostRandomPort = sock.getLocalPort();
            final InetAddress addr = sock.getInetAddress();
            hostAddr = addr.getHostAddress();
            hostFQDN = addr.getCanonicalHostName();
            hostName = addr.getHostName();
        }
        catch(final IOException e)
        {
            //
        }
    }

    /**
     * @see com.bruhnNewtech.filterbuilder.filter.Filter#prepareStart()
     */
    @Override
    public void prepareStart()
    {
        super.prepareStart();

        // Start Message Service
        
        
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(NotificationEndpoint.class);
        sf.setResourceProvider(NotificationEndpoint.class, new SingletonResourceProvider(new NotificationEndpoint()));
        //sf.setAddress(serverAddress);
        sf.setAddress("ws://localhost:11008/scim"); //$NON-NLS-1$
        
        /*
        final Map<String, Object> prop = new HashMap<>();
        prop.put("ws-discovery-types", discoveryTypeName); //$NON-NLS-1$
        prop.put("ws-discovery-scopes", discoveryScopeId); //$NON-NLS-1$
        prop.put("ws-discovery-published-url", discoveryURL); //$NON-NLS-1$
        prop.put("ws-discovery-disable", discoveryDisable.toString()); //$NON-NLS-1$
        */
        
        messageServer = sf.create();
        
        /*
        final NotificationEndpoint end = new NotificationEndpoint();
        messageServer = new Server("localhost", 11008, "/websockets", end); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            messageServer.start();
            System.out.println("Oppe og kører"); //$NON-NLS-1$
        }
        catch(final DeploymentException e)
        {
            System.out.println(e);
        }
        */
        
    }

    /**
     * @see com.bruhnNewtech.filterbuilder.filter.Filter#prepareDestroy()
     */
    @Override
    public void prepareDestroy()
    {
        super.prepareDestroy();
        if(messageServer != null)
        {
            messageServer.destroy();
            //messageServer.stop();
        }
    }

    /**
     * @return the logger
     */
    static BNLogger getLogger()
    {
        return logger;
    }
}
