/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/file/Attic/TestProperyDefinition.java,v $
 * Date   : $Date: 2004/11/24 15:57:25 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2004 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.file;

import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.test.OpenCmsTestPropertiesSingleton;
import org.opencms.test.OpenCmsTestCase;
import org.opencms.util.CmsUUID;

import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit test for the "createPropertyDefinition", "readPropertyDefiniton" and
 * "readAllPropertyDefintions" methods of the CmsObject.<p>
 * 
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * @version $Revision: 1.9 $
 */
public class TestProperyDefinition extends OpenCmsTestCase {

    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */
    public TestProperyDefinition(String arg0) {

        super(arg0);
    }

    /**
     * Test suite for this test class.<p>
     * 
     * @return the test suite
     */
    public static Test suite() {
        OpenCmsTestPropertiesSingleton.initialize(org.opencms.test.AllTests.TEST_PROPERTIES_PATH);

        TestSuite suite = new TestSuite();
        suite.setName(TestProperyDefinition.class.getName());

        suite.addTest(new TestProperyDefinition("testCreatePropertyDefinition"));
        suite.addTest(new TestProperyDefinition("testCreateReadDeletePropertyDefinition"));
        suite.addTest(new TestProperyDefinition("testGetResourcesWithProperty"));
        
        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() {

                setupOpenCms("simpletest", "/sites/default/");
            }

            protected void tearDown() {

                removeOpenCms();
            }
        };

        return wrapper;
    }
    
    /**
     * Test the createPropertyDefintion method.<p>
     * @param tc the OpenCmsTestCase
     * @param cms the CmsObject
     * @param propertyDefiniton1 the property definition to create
     * @throws Throwable if something goes wrong
     */
    public static void createPropertyDefinition(OpenCmsTestCase tc, CmsObject cms, String propertyDefiniton1)
    throws Throwable {

        // get all propertydefintions
        List allPropertydefintions = cms.readAllPropertydefinitions();
        // create a property defintion with a dummy id value (the real id is created by db)
        CmsPropertydefinition prop = new CmsPropertydefinition(
            CmsUUID.getNullUUID(),
            propertyDefiniton1);

        cms.createPropertydefinition(propertyDefiniton1);

        // check if the propertsdefintion was written
        tc.assertPropertydefinitionExist(cms, prop);
        // check if all other properties are still identical
        tc.assertPropertydefinitions(cms, allPropertydefintions, prop);
    }

    /**
     * Test the createPropertyDefintion method.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreatePropertyDefinition() throws Throwable {

        CmsObject cms = getCmsObject();
        echo("Testing createPropetyDefinition and readPropertyDefiniton");
        createPropertyDefinition(this, cms, "NewPropertyDefinition");
    }

    /**
     * Test to create, read and delete a property definition through the cache driver.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateReadDeletePropertyDefinition() throws Throwable {

        CmsObject cms = getCmsObject();

        echo("Testing creation of a new property definition");
        createReadDeletePropertyDefinition(cms);
    }

    /**
     * Test to create, read and delete a property definition through the cache driver.<p>
     * 
     * @param cms the CmsObject
     * @throws Throwable if something goes wrong
     */
    public static void createReadDeletePropertyDefinition(CmsObject cms) throws Throwable {

        CmsPropertydefinition propertyDefinition = null;
        String propertyDefinitionName = "locale-available";
                
        // 1) create a property definition

        try {
            // create a new property definition
            propertyDefinition = cms.createPropertydefinition(propertyDefinitionName);
        } catch (CmsException e) {
            fail("Error creating property definition " + propertyDefinitionName + ", " + e.toString());
        }

        assertEquals(propertyDefinition.getName(), propertyDefinitionName);

        // 2) read the created property definition
        
        try {
            // read the created property definition
            propertyDefinition = null;
            propertyDefinition = cms.readPropertydefinition(propertyDefinitionName);
        } catch (CmsException e) {
            fail("Error reading property definition " + propertyDefinitionName + ", " + e.toString());
        }

        assertEquals(propertyDefinition.getName(), propertyDefinitionName);

        // 3) check if the new created property is contained in the list of all property definitions
        
        List allPropertyDefinitions = null;
        try {
            // read all property definitions
            allPropertyDefinitions = cms.readAllPropertydefinitions();
        } catch (CmsException e) {
            fail("Error reading all property definitions, " + e.toString());
        }

        boolean found = false;
        for (int i = 0, n = allPropertyDefinitions.size(); i < n; i++) {
            if (((CmsPropertydefinition)allPropertyDefinitions.get(i)).equals(propertyDefinition)) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            fail("Property definition " + propertyDefinitionName + " is not in the list of all property definitions");
        }
        
        // 4) delete the new property definition again

        try {
            // delete the property definition again
            cms.deletePropertydefinition(propertyDefinitionName);
        } catch (CmsException e) {
            fail("Error deleting property definition " + propertyDefinitionName + ", " + e.toString());
        }
        
        // 5) re-read the delete property definition to check that it is not found

        try {
            // re-read the created property definition
            propertyDefinition = null;
            propertyDefinition = cms.readPropertydefinition(propertyDefinitionName);
        } catch (CmsException e) {
            // intentionally left blank
        }

        assertNull(propertyDefinition);
        
        // 6) re-read the list of all property definitions to check that it is not contained
        
        allPropertyDefinitions = null;
        try {
            // read all property definitions
            allPropertyDefinitions = cms.readAllPropertydefinitions();
        } catch (CmsException e) {
            fail("Error reading all property definitions, " + e.toString());
        }

        found = false;
        for (int i = 0, n = allPropertyDefinitions.size(); i < n; i++) {
            if (((CmsPropertydefinition)allPropertyDefinitions.get(i)).equals(propertyDefinition)) {
                found = true;
                break;
            }
        }  

        if (found) {
            fail("Property definition " + propertyDefinitionName + " is still in the list of all property definitions");
        }
        
    }

    /**
     * Tests reading all resources that have a specific property definition set.<p>
     * 
     * @throws Exception if something goes wrong
     */
    public void testGetResourcesWithProperty() throws Exception {
        
        echo("Testing reading all resources with a specific property");        
        CmsObject cms = getCmsObject();
        List result;
        
        result = cms.readResourcesWithProperty(I_CmsConstants.C_PROPERTY_TITLE);
        assertEquals(62, result.size());
        result = cms.readResourcesWithProperty(I_CmsConstants.C_PROPERTY_TEMPLATE);
        assertEquals(24, result.size());
    }
    
}