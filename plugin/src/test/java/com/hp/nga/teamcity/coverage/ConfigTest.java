package com.hp.nga.teamcity.coverage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigMultiSharedSpaceStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationService;
import org.junit.Test;
import org.testng.Assert;

import java.util.List;

public class ConfigTest {

    @Test
    public void configTest() throws JsonProcessingException {

        String configStr = "<configs><octane-config><identity>3b26839b-c314-42ac-9668-d218eb2dedb9</identity><identityFrom>1618170742415</identityFrom><uiLocation>http://localhost:8080/dev/ui/?admin</uiLocation><api-key>sa@nga</api-key><secret>scrambled:V2VsY29tZTE=</secret><impersonatedUser>admin</impersonatedUser><location>http://localhost:8080/dev</location><sharedSpace>1001</sharedSpace></octane-config></configs>";
        XmlMapper xmlMapper = new XmlMapper();
        OctaneConfigMultiSharedSpaceStructure multiStructure = xmlMapper.readValue(configStr,OctaneConfigMultiSharedSpaceStructure.class);
        Assert.assertEquals(multiStructure.getMultiConfigStructure().size(),1);
        OctaneConfigStructure conf1 = multiStructure.getMultiConfigStructure().get(0);

        Assert.assertEquals("3b26839b-c314-42ac-9668-d218eb2dedb9",conf1.getIdentity());
        Assert.assertEquals("1618170742415",conf1.getIdentityFrom());
        Assert.assertEquals("http://localhost:8080/dev/ui/?admin",conf1.getUiLocation());
        Assert.assertEquals("sa@nga",conf1.getUsername());
        Assert.assertEquals("scrambled:V2VsY29tZTE=",conf1.getSecretPassword());
        Assert.assertEquals("admin",conf1.getImpersonatedUser());
        Assert.assertEquals("http://localhost:8080/dev",conf1.getLocation());
        Assert.assertEquals("1001",conf1.getSharedSpace());

        String converted = xmlMapper.writeValueAsString(multiStructure);
        Assert.assertEquals(configStr,converted);
    }
}
