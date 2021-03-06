/**
 * Copyright (C) 2014 Umbrew (Flemming.Harms@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.db_bootstrap;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.db_bootstrap.databasebootstrap.DatabaseBootstrapTester;

/**
 * @author Flemming Harms 
 */
@RunWith(Arquillian.class)
public class BootstrapDatabaseITCase {

    private static final String ARCHIVE_NAME = "bootstrap_test";

    private static final String hibernate_cfg_xml =
            "<!DOCTYPE hibernate-configuration SYSTEM "+
            "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"+
            "<hibernate-configuration>"+
            "   <session-factory>"+
            "        <property name=\"hibernate.connection.driver_class\">org.h2.Driver</property>"+
            "        <property name=\"hibernate.connection.url\">jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MVCC=true</property>"+
            "        <property name=\"hibernate.connection.username\">sa</property>"+
            "        <property name=\"hibernate.connection.password\">sa</property>"+
            "        <property name=\"javax.persistence.validation.mode\">none</property>"+
            "        <property name=\"hbm2ddl.auto\">validate</property>"+
            "    </session-factory>"+
            "</hibernate-configuration>";

    @Deployment
    public static Archive<?> deploy() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "bootstrap.jar");
        lib.addClasses(DatabaseBootstrapTester.class);
        lib.addClasses(BootstrapDatabaseITCase.class);
        lib.addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
        lib.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        lib.addAsManifestResource(new StringAsset(hibernate_cfg_xml), "hibernate.cfg.xml");
        ear.addAsLibraries(lib);


        // add application dependency on H2 JDBC driver, so that the Hibernate classloader (same as app classloader)
        // will see the H2 JDBC driver.
        // equivalent hack for use of shared Hiberante module, would be to add the H2 dependency directly to the
        // shared Hibernate module.
        // also add dependency on org.slf4j
        ear.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure>" +
                        " <deployment>" +
                        "  <dependencies>" +
                        "   <module name=\"com.h2database.h2\"/>" +
                        "   <module name=\"org.slf4j\"/>" +
                        "  </dependencies>" +
                        " </deployment>" +
                        "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        return ear;
    }

    @PersistenceContext
    EntityManager em;

    @Test
    public void testRunBootstrap() throws Exception {
        Query query = em.createNativeQuery("select * from person where PersonId = '1'");
        List<?> resultList = query.getResultList();
        assertEquals(1, resultList.size());
        Object[] result = (Object[]) resultList.get(0);
        assertEquals("John",result[1]);
        assertEquals("Doe",result[2]);
    }

}
