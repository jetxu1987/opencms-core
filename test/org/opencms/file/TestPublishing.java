/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/file/TestPublishing.java,v $
 * Date   : $Date: 2004/11/24 15:57:25 $
 * Version: $Revision: 1.7 $
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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.main.CmsException;
import org.opencms.main.I_CmsConstants;
import org.opencms.report.CmsShellReport;
import org.opencms.test.OpenCmsTestPropertiesSingleton;
import org.opencms.test.OpenCmsTestCase;
import org.opencms.test.OpenCmsTestResourceFilter;

/**
 * Unit tests for OpenCms publishing.<p>
 * 
 * @author Michael Emmerich (m.emmerich@alkacon.com)
 * 
 * @version $Revision: 1.7 $
 */
public class TestPublishing extends OpenCmsTestCase {
  
    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */    
    public TestPublishing(String arg0) {
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
        suite.setName(TestPublishing.class.getName());
        
        suite.addTest(new TestPublishing("testPublishNewFiles"));
        suite.addTest(new TestPublishing("testPublishNewFilesInNewFolder"));
        suite.addTest(new TestPublishing("testPublishChangedFiles"));
        suite.addTest(new TestPublishing("testPublishLockedFiles"));
        suite.addTest(new TestPublishing("testPublishDeletedFiles"));
        suite.addTest(new TestPublishing("testPublishProjectLastmodified"));
        
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
     * Test publishing new files.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishNewFiles() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing publish new files");
        
        String source = "/folder2/subfolder21/image1.gif";
        String destination1 = "/folder2/image1_new.gif";
        String destination2 = "/folder2/image1_sibling1.gif";
        String destination3 = "/folder2/image1_sibling2.gif";
        String destination4 = "/folder2/image1_sibling3.gif";
        
        CmsProject onlineProject  = cms.readProject("Online");
        CmsProject offlineProject  = cms.readProject("Offline");

        // make four copies of a file to be published later
        cms.copyResource(source, destination1, I_CmsConstants.C_COPY_AS_NEW);
        cms.copyResource(source, destination2, I_CmsConstants.C_COPY_AS_SIBLING);
        cms.copyResource(source, destination3, I_CmsConstants.C_COPY_AS_SIBLING);
        cms.copyResource(source, destination4, I_CmsConstants.C_COPY_AS_SIBLING);
        
        storeResources(cms, destination1);
        storeResources(cms, destination2);
        storeResources(cms, destination3);
        storeResources(cms, destination4);
        
        // unlock all new resources
        // do not neet do unlock destination3 as it is a sibling of destination2     
        cms.unlockResource(destination1);
        cms.unlockResource(destination2);    
        
        // publish a new resource
        //
        cms.publishResource(destination1);

        // the file must be now available in the online project
        cms.getRequestContext().setCurrentProject(onlineProject);
        try {
            cms.readResource(destination1);
        } catch (CmsException e) {
            fail("Resource " + destination1 + " not found in online project:" +e);
        }    
        assertFilter(cms, destination1, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
        
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, destination1, I_CmsConstants.C_STATE_UNCHANGED);     
 
        // publish a sibling without publishing other siblings
        //
        cms.publishResource(destination2);
        
        // the file must be now available in the online project
        cms.getRequestContext().setCurrentProject(onlineProject);
        try {
            cms.readResource(destination2);
        } catch (CmsException e) {
            fail("Resource " + destination2 + " not found in online project:" +e);
        }
        // the other siblings must not be available in the online project yet
        try {
            cms.readResource(destination3);
            fail("Resource " + destination3+ " should not available online yet");
        } catch (CmsException e) {
            if (e.getType() != CmsException.C_NOT_FOUND) {
                fail("Resource " + destination3 + " error:" +e);
            }
        }
        try {
            cms.readResource(destination4);
            fail("Resource " + destination4+ " should not available online yet");
        } catch (CmsException e) {
            if (e.getType() != CmsException.C_NOT_FOUND) {
                fail("Resource " + destination4 + " error:" +e);
            }
        }
        
        assertFilter(cms, destination2, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);          
        
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, destination2, I_CmsConstants.C_STATE_UNCHANGED);
        // the other siblings in the offline project must still be shown as new
        assertState(cms, destination3, I_CmsConstants.C_STATE_NEW);
        assertState(cms, destination4, I_CmsConstants.C_STATE_NEW);
        
        // publish a sibling and all other siblings of it
        //
        cms.publishResource(destination3, true, new CmsShellReport());
        // the file and its siblings must be now available in the online project
        cms.getRequestContext().setCurrentProject(onlineProject);
        try {
            cms.readResource(destination3);
        } catch (CmsException e) {
            fail("Resource " + destination3 + " not found in online project:" +e);
        }
        try {
            cms.readResource(destination4);
        } catch (CmsException e) {
            fail("Resource " + destination4 + " not found in online project:" +e);
        }
        assertFilter(cms, destination3, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
        assertFilter(cms, destination4, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
        
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, destination3, I_CmsConstants.C_STATE_UNCHANGED);
        assertState(cms, destination4, I_CmsConstants.C_STATE_UNCHANGED);
    }  
    
    /**
     * Test publishing files within a new unpublished folder.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishNewFilesInNewFolder() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing publishing new files in new folder");
        
        String source = "/folder1/image1.gif";
        String newFolder = "/new_folder/";
        String newFile = newFolder + "new_file";
        String newSibling = newFolder + "new_sibling";
        
        cms.createResource(newFolder, CmsResourceTypeFolder.C_RESOURCE_TYPE_ID);
        cms.unlockResource(newFolder);
        storeResources(cms, newFolder);
        
        // change to test project
        CmsProject project = getTestProject(cms);
        cms.getRequestContext().setCurrentProject(project);
        
        cms.copyResource(source, newFile, I_CmsConstants.C_COPY_AS_NEW);
        cms.unlockResource(newFile);
        
        cms.copyResource(source, newSibling, I_CmsConstants.C_COPY_AS_SIBLING);
        cms.unlockResource(newSibling);
        
        // direct publish of the new file will not publish the new file
        echo ("Publishing the resource directly");
        
        storeResources(cms, newFile);
        cms.publishResource(newFile);
        assertFilter(cms, newFile, OpenCmsTestResourceFilter.FILTER_EQUAL);
        
        // direct publish of another sibling will not publish the new sibling
        echo ("Publishing another sibling");
        cms.lockResource(source);
        cms.touch(source, System.currentTimeMillis(), I_CmsConstants.C_DATE_UNCHANGED, I_CmsConstants.C_DATE_UNCHANGED, false);
        cms.unlockResource(source);
        
        storeResources(cms, newSibling);
        cms.publishResource(source, true, new CmsShellReport());
        assertFilter(cms, newSibling, OpenCmsTestResourceFilter.FILTER_EQUAL);
        
        // publishing the test project will not publish the new file or the new sibling
        echo ("Publishing the test project");
        cms.publishProject();
        
        assertFilter(cms, newFile, OpenCmsTestResourceFilter.FILTER_EQUAL);
        assertFilter(cms, newSibling, OpenCmsTestResourceFilter.FILTER_EQUAL);
        
        // publishing the offline project will publish the folder but not the resources
        echo ("Publishing the offline project");
        
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        cms.publishProject();
        
        assertFilter(cms, newFolder, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);
        assertState(cms, newFolder, I_CmsConstants.C_STATE_UNCHANGED);
        
        assertFilter(cms, newFile, OpenCmsTestResourceFilter.FILTER_EQUAL);
        assertFilter(cms, newSibling, OpenCmsTestResourceFilter.FILTER_EQUAL);        
        
        // publishing the test proejct again will now publish the resources
        echo ("Publishing the test project again");
        cms.getRequestContext().setCurrentProject(project);
        cms.publishProject();
        
        assertFilter(cms, newFile, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);
        assertState(cms, newFile, I_CmsConstants.C_STATE_UNCHANGED);
        
        assertFilter(cms, newSibling, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);
        assertState(cms, newSibling, I_CmsConstants.C_STATE_UNCHANGED);
        
    }   
    
    /**
     * Test publishing changed files.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishChangedFiles() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing publish changed files");
        
        String resource1 = "/folder2/image1_new.gif";
        String resource2 = "/folder2/image1_sibling1.gif";
        String resource3 = "/folder2/image1_sibling2.gif";
        String resource4 = "/folder2/image1_sibling3.gif";
        
        CmsProject onlineProject  = cms.readProject("Online");
        CmsProject offlineProject  = cms.readProject("Offline");
        
        CmsProperty prop1;
        CmsProperty prop2;
        CmsProperty prop3;
        CmsProperty prop4;

        
        // make changes to the resources 
        // do not need to make any changes to resource3 and resource4 as they are
        // siblings of resource2!
 
        cms.lockResource(resource1);
        cms.lockResource(resource2);
        
        cms.writePropertyObject(resource1, new CmsProperty("Title", resource1, null));
        cms.writePropertyObject(resource2, new CmsProperty("Title", resource2, null));
        cms.writePropertyObject(resource3, new CmsProperty("Title", resource3, null));
        cms.writePropertyObject(resource4, new CmsProperty("Title", resource4, null));
           
        // unlock all resources
        cms.unlockResource(resource1);
        cms.unlockResource(resource2);
                      
        storeResources(cms, resource1);
        storeResources(cms, resource2);
        storeResources(cms, resource3);
        storeResources(cms, resource4);
       
        // publish a modified resource without siblings
        //
        cms.publishResource(resource1);

        // the online file must the offline changes
        cms.getRequestContext().setCurrentProject(onlineProject);
        prop1 = cms.readPropertyObject(resource1, "Title", false);

        
        if (!prop1.getValue().equals((resource1))) {
            fail("Property not published for " + resource1);
        }
        assertFilter(cms, resource1, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
        
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, resource1, I_CmsConstants.C_STATE_UNCHANGED);

        // publish a modified resource with siblings, but keeping the siblings unpublished
        //
        cms.publishResource(resource2);

        // the online file must the offline changes
        cms.getRequestContext().setCurrentProject(onlineProject);
        prop2 = cms.readPropertyObject(resource2, "Title", false);
        prop3 = cms.readPropertyObject(resource3, "Title", false);
        prop4 = cms.readPropertyObject(resource4, "Title", false);
        
        if (!prop2.getValue().equals((resource2))) {
            fail("Property not published for " + resource2);
        }
        if (prop3.getValue().equals((resource3))) {
            fail("Property  published for " + resource3);
        }
        if (prop4.getValue().equals((resource4))) {
            fail("Property  published for " + resource4);
        }
        
        assertFilter(cms, resource2, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);          
        
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, resource2, I_CmsConstants.C_STATE_UNCHANGED);
        assertState(cms, resource3, I_CmsConstants.C_STATE_CHANGED);
        assertState(cms, resource4, I_CmsConstants.C_STATE_CHANGED);

        // publish a modified resource with siblings, publish the siblings as well
        //
        cms.publishResource(resource3, true, new CmsShellReport());

        // the online file must the offline changes
        cms.getRequestContext().setCurrentProject(onlineProject);
        prop3 = cms.readPropertyObject(resource3, "Title", false);
        prop4 = cms.readPropertyObject(resource4, "Title", false);
        
        if (!prop3.getValue().equals((resource3))) {
            fail("Property  not published for " + resource3);
        }
        if (!prop4.getValue().equals((resource4))) {
            fail("Property  not published for " + resource4);
        }
        
        assertFilter(cms, resource3, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
        assertFilter(cms, resource4, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);  
                
        // check if the file in the offline project is unchancged now
        cms.getRequestContext().setCurrentProject(offlineProject);
        assertState(cms, resource3, I_CmsConstants.C_STATE_UNCHANGED);
        assertState(cms, resource4, I_CmsConstants.C_STATE_UNCHANGED);
    }  

    /**
     * Test publishing changed files.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishLockedFiles() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing publish locked files");
        
        String source = "/folder2/subfolder21/image1.gif";
        String resource1 = "/folder2/image1_new.gif";
        String resource2 = "/folder2/image1_sibling1.gif";
        
        CmsProject onlineProject  = cms.readProject("Online");
        
        CmsProperty prop0;
        CmsProperty prop1;
        CmsProperty prop2;
  
        // make changes to the resources 
        // do not need to make any changes to resource3 and resource4 as they are
        // siblings of resource2!
 
        cms.lockResource(source);
        cms.lockResource(resource1);
        cms.lockResource(resource2);
        
        cms.writePropertyObject(source, new CmsProperty("Title", source + " modified", null));
        cms.writePropertyObject(resource1, new CmsProperty("Title", resource1 + " modified", null));
        cms.writePropertyObject(resource2, new CmsProperty("Title", resource2 + " modified", null));

        storeResources(cms, source);
        storeResources(cms, resource1);
        storeResources(cms, resource2);
       
        // publish a modified resource without siblings
        cms.publishProject();

        // ensure that all changed resources are still changed in the offline project
        assertState(cms, source, I_CmsConstants.C_STATE_CHANGED);
        assertState(cms, resource1, I_CmsConstants.C_STATE_CHANGED);
        assertState(cms, resource2, I_CmsConstants.C_STATE_CHANGED);
                
        // ensure that all changed resources are NOT published
        cms.getRequestContext().setCurrentProject(onlineProject);
        prop0 = cms.readPropertyObject(source, "Title", false);
        prop1 = cms.readPropertyObject(resource1, "Title", false);
        prop2 = cms.readPropertyObject(resource2, "Title", false);
        
        if (prop0.getValue().equals((source + " modified"))) {
            fail("Property published for " + source);
        }
        if (prop1.getValue().equals((resource1 + " modified"))) {
            fail("Property published for " + resource1);
        }        
        if (prop2.getValue().equals((resource2 + " modified"))) {
            fail("Property published for " + resource2);
        }
    }  
    
    /**
     * Test publishing deelted files.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishDeletedFiles() throws Throwable {
        
        CmsObject cms = getCmsObject();     
        echo("Testing publish deleted files");
        
        String resource1 = "/folder2/image1_new.gif";
        String resource2 = "/folder2/image1_sibling1.gif";
        String resource3 = "/folder2/image1_sibling2.gif";
        String resource4 = "/folder2/image1_sibling3.gif";
        
        CmsProject onlineProject  = cms.readProject("Online");
        CmsProject offlineProject  = cms.readProject("Offline");
        
        // publish a deleted resource without siblings
        //
        
        // delete the resources 
        cms.lockResource(resource1);
        cms.deleteResource(resource1, I_CmsConstants.C_DELETE_OPTION_PRESERVE_SIBLINGS);
        cms.unlockResource(resource1);
       
        cms.publishResource(resource1);

        // the online file must be deleted
        cms.getRequestContext().setCurrentProject(onlineProject);
        try {
            cms.readResource(resource1);
            fail ("Resource " + resource1 + " was not deleted online");
       } catch (CmsException e) {
            if (e.getType() != CmsException.C_NOT_FOUND) {
                fail("Resource " + resource1 + " error:" +e);
            }
        }
       
       cms.getRequestContext().setCurrentProject(offlineProject);
             
       // publish a deleted resource with siblings, 
       // delete the siblings also, but publish only the resource itself
       
       // delete the resources 
       cms.lockResource(resource2);
       cms.deleteResource(resource2, I_CmsConstants.C_DELETE_OPTION_DELETE_SIBLINGS);
       cms.unlockResource(resource2);
       
       // this test makes only sense when siblings are published
       cms.publishResource(resource2, false, new CmsShellReport());

       // the online file must be deleted
       cms.getRequestContext().setCurrentProject(onlineProject);
       try {
           cms.readResource(resource2);
           fail ("Resource " + resource2 + " was not deleted online");
      } catch (CmsException e) {
           if (e.getType() != CmsException.C_NOT_FOUND) {
               fail("Resource " + resource2 + " error:" +e);
           }
       }    
       // the other siblings must still be there
      try {
          cms.readResource(resource3);
      } catch (CmsException e) {
              fail("Resource " + resource3 + " error:" +e);
      }
      try {
          cms.readResource(resource4);
      } catch (CmsException e) {
            fail("Resource " + resource4 + " error:" +e);
      }
      
      cms.getRequestContext().setCurrentProject(offlineProject);
      // in the offline project, the siblings must be still marked as deleted
      assertState(cms, resource3, I_CmsConstants.C_STATE_DELETED);
      assertState(cms, resource4, I_CmsConstants.C_STATE_DELETED);
      
      // publish a deleted resource with siblings, delete the siblings
      //
      cms.publishResource(resource3, true, new CmsShellReport());
      
      // the online files must be deleted
      cms.getRequestContext().setCurrentProject(onlineProject);
      try {
          cms.readResource(resource3);
          fail ("Resource " + resource3 + " was not deleted online");
      } catch (CmsException e) {
          if (e.getType() != CmsException.C_NOT_FOUND) {
              fail("Resource " + resource3 + " error:" +e);
          }
      } 
      try {
          cms.readResource(resource4);
          fail ("Resource " + resource4 + " was not deleted online");
      } catch (CmsException e) {
          if (e.getType() != CmsException.C_NOT_FOUND) {
              fail("Resource " + resource4 + " error:" +e);
          }
      } 
      
      cms.getRequestContext().setCurrentProject(offlineProject);

    }
    
    /**
     * Tests publishing resources within a distinct project.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testPublishProjectLastmodified() throws Throwable {

        CmsObject cms = getCmsObject();     
        echo("Testing publishing a distinct project");
        
        String path = "/folder1";
        String res1 = path + "/page1.html";
        String res2 = path + "/page2.html";
        
        long timestamp = System.currentTimeMillis();

        cms.getRequestContext().setCurrentProject(cms.readProject("Online"));
        storeResources(cms, res1);
        
        // change first resource in the offline project
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        cms.lockResource(res1);
        cms.touch(res1, timestamp, I_CmsConstants.C_DATE_UNCHANGED, I_CmsConstants.C_DATE_UNCHANGED, false);
        
        cms.unlockProject(cms.getRequestContext().currentProject().getId());
        
        // create a new project
        CmsProject project = getTestProject(cms);
        cms.getRequestContext().setCurrentProject(project);
        cms.copyResourceToProject(path);
        
        // and change another resource in this project
        cms.lockResource(res2);
        cms.touch(res2, timestamp, I_CmsConstants.C_DATE_UNCHANGED, I_CmsConstants.C_DATE_UNCHANGED, false);
        cms.unlockProject(cms.getRequestContext().currentProject().getId());
        storeResources(cms, res2);
        
        // when the project is published, only the second resource will be published
        cms.publishProject();
        
        cms.getRequestContext().setCurrentProject(cms.readProject("Online"));
        this.assertFilter(cms, res1, OpenCmsTestResourceFilter.FILTER_EQUAL);
        this.assertFilter(cms, res2, OpenCmsTestResourceFilter.FILTER_PUBLISHRESOURCE);
        this.assertDateLastModified(cms, res2, timestamp);
        
        echo("Testing publishing in different projects");
        
        // change back to the test project, create a new resource
        String res3 = path + "/testPublishProjectLastmodified.txt";
        cms.getRequestContext().setCurrentProject(project);
        cms.createResource(res3, CmsResourceTypePlain.C_RESOURCE_TYPE_ID);
        
        // change to offline project, copy resource as sibling
        // this will also change the project in which the source was lastmodified (now "Offline")
        String res4 = "/testPublishProjectLastmodified.txt";
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        cms.copyResource(res3, res4, I_CmsConstants.C_COPY_AS_SIBLING);
        
        // check the current state of the resources
        assertState(cms, res3, I_CmsConstants.C_STATE_NEW);
        assertState(cms, res4, I_CmsConstants.C_STATE_NEW);
        
        // publish the test project
        cms.getRequestContext().setCurrentProject(project);
        cms.unlockProject(project.getId());
        cms.publishProject();
        
        // the resource inside the test project must not be published (not in test project)
        assertState(cms, res3, I_CmsConstants.C_STATE_NEW);
        
        // the sibling outside the test project must not be published (not in test project)
        assertState(cms, res4, I_CmsConstants.C_STATE_NEW);
        
        // publish the root folder of the test project within the test project
        cms.publishResource(path, true, new CmsShellReport());
        
        // the resource inside the test project must not be published (still not in test project)
        assertState(cms, res3, I_CmsConstants.C_STATE_NEW);
        
        // the sibling outside the test project must not be published (still not in test project)
        assertState(cms, res4, I_CmsConstants.C_STATE_NEW);        
    }
    
    /**
     * Returns a project for testing purposes.<p>
     * 
     * @param cms the cms object
     * @return a project for testing purposes
     */
    CmsProject getTestProject(CmsObject cms) {
        
        CmsProject project = null;
        
        try {
            project = cms.readProject("Test");
        } catch (Exception e) {
            try {
                project = cms.createProject("Test", "", "Users", "Administrators");
            } catch (Exception ee) {
                // noop
            }
        }
        
        return project;
    }
}
