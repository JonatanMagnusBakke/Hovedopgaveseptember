package test.bruhnNewtech.sensorserverws;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.bruhnNewtech.commons.logging.BNLoggerFactory;
import com.bruhnNewtech.commons.util.StringUtil;
import com.bruhnNewtech.commons.util.XMLUtil;
import com.bruhnNewtech.filterbuilder.filter.RequestResponseUtility;
import com.bruhnNewtech.filterbuilder.testutils.BasicTestFilter;
import com.bruhnNewtech.sensorserver.filter.sensoroverview.SensorOverviewManager;
import com.bruhnNewtech.sensorserver.server.ConfigurationProvider;
import com.bruhnNewtech.sensorserver.server.SensorAttributes;
import com.bruhnNewtech.sensorserver.server.SensorType;
import com.bruhnNewtech.sensorserverws.SensorServerServiceFilter;
import com.bruhnNewtech.testutils.DocumentComparisonUtil;

/**
 * 
 *
 * <pre>
 * Copyright © 2015 Bruhn NewTech A/S. All rights reserved.
 * </pre>
 *
 */
public class SensorServerServiceFilterTest extends BasicTestFilter
{
    private static final String      PACKAGE_DIRECTORY_NAME  = "./source/" + SensorServerServiceFilterTest.class.getPackage().getName().replace('.', '/'); //$NON-NLS-1$

    private static final String FILTER_ID = "SCIMService"; //$NON-NLS-1$

    private static ArrayList<String> ignoreList;

    public SensorServerServiceFilterTest()
    {
        // Empty
    }
    
    /**
     * FIXME: Document
     *
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        // Set log properties
        System.setProperty(BNLoggerFactory.BN_LOG4J_PROPERTY, PACKAGE_DIRECTORY_NAME + "/inp/log4j.properties"); //$NON-NLS-1$
        
        ignoreList = new ArrayList<>();
        ignoreList.add(".*/@time"); //$NON-NLS-1$
        ignoreList.add(".*/Message\\[.*\\]"); //$NON-NLS-1$         
    }

    /**
     * FIXME: Document
     *
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        // Empty
    }

    /**
     * FIXME: Document
     *
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        final SensorAttributes sensor1 = new SensorAttributes("id1", "RAID-XP Chem", SensorType.CHEM, "RAID-XP", false);   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        sensor1.setGroup("Vehicle"); //$NON-NLS-1$
        sensor1.addCapability("Power", null); //$NON-NLS-1$
        final SensorAttributes sensor2 = new SensorAttributes("id2", "RAID-XP Rad", SensorType.NUC, "RAID-XP", false);   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        sensor2.setGroup("Vehicle"); //$NON-NLS-1$
        ConfigurationProvider.getInstance().addSensor(sensor1);
        ConfigurationProvider.getInstance().addSensor(sensor2);
        if(!SensorOverviewManager.getInstance().isInitialized())
        {
            SensorOverviewManager.getInstance().initSensorList();
        }        
    }

    /**
     * FIXME: Document
     *
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        // Empty
    }

    @Test
    public void testReceiveMsg() throws Exception
    {
        final SensorServerServiceFilter filter = new SensorServerServiceFilter();
        setFilterToBeTested(filter);
        RequestResponseUtility.resetMessageId();

        final Document config = XMLUtil.parseDocument(new File(getInputDirectory() + "/config.xml")); //$NON-NLS-1$
          
        filter.configure(FILTER_ID, config.getDocumentElement());
        filter.prepareStart();

        ///////////////////////////////////////////////////////////////////////////
        // 
        ///////////////////////////////////////////////////////////////////////////
        final ClientRunnable client1 = new ClientRunnable("", "abc-16262", "configuration/sensors"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        client1.start();
        
        Thread.sleep(1000);

        client1.join();
        assertEquals(200, client1.getStatusCode());
        System.out.println(XMLUtil.convertDomToString(client1.getResponseDocument(), true, true));
        DocumentComparisonUtil.compareDocuments(getReferenceDocument("sensors"), client1.getResponseDocument());    //$NON-NLS-1$

        ///////////////////////////////////////////////////////////////////////////
        // 
        ///////////////////////////////////////////////////////////////////////////
        final ClientRunnable client2 = new ClientRunnable("", "abc-16262", "sensor/status"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        client2.start();
        
        Thread.sleep(1000);

        client2.join();
        assertEquals(200, client2.getStatusCode());
        System.out.println(XMLUtil.convertDomToString(client2.getResponseDocument(), true, true));
        DocumentComparisonUtil.compareDocuments(getReferenceDocument("status"), client2.getResponseDocument(), null, true);    //$NON-NLS-1$        
       
    }

    private class ClientRunnable extends Thread
    {
        private String testName;
        private int statusCode;
        private String port = "11008"; //$NON-NLS-1$
        private String key = ""; //$NON-NLS-1$
        private String resource = "sensors"; //$NON-NLS-1$
        private Document responseDoc = null;
        

        public ClientRunnable(final String test, final String key, final String resource)
        {
            setTestName(test);
            if(!StringUtil.isNullOrEmpty(key))
            {
                this.key  = key;
            }
            if(!StringUtil.isNullOrEmpty(resource))
            {
                this.resource   = resource;
                if (!StringUtil.isNullOrEmpty(key))
                {
                    this.resource = this.resource + "?key=" + key; //$NON-NLS-1$
                }
            }
        }
        
        /**
         * @see java.lang.Thread#run()
         */
        @SuppressWarnings("synthetic-access")
        @Override
        public void run()
        {
            final Client myClient = ClientBuilder.newClient();
            
            final Response res;
            try
            {
                setStatusCode(0);
                responseDoc = myClient.target("http://localhost:" + port + "/scim/v1.0/" + resource).request(MediaType.APPLICATION_XML_TYPE).get(Document.class); //$NON-NLS-1$ //$NON-NLS-2$
                setStatusCode(200);
            }
            catch(final ProcessingException e)
            {
                System.out.println(e.getMessage());
                setStatusCode(-2);
            }
            catch(final Exception e)
            {
                System.out.println(e.getMessage());
                setStatusCode(-1);
            }
            System.out.println("Closing test client"); //$NON-NLS-1$
            myClient.close();
        }

        public int getStatusCode()
        {
            return statusCode;
        }

        public void setStatusCode(final int statusCode)
        {
            this.statusCode = statusCode;
        }

        public Document getResponseDocument()
        {
            return responseDoc;
        }
        
        /**
         * @return the testName
         */
        String getTestName()
        {
            return testName;
        }

        /**
         * @param testName the testName to set
         */
        void setTestName(final String testName)
        {
            this.testName = testName;
        }
    }
    
}
