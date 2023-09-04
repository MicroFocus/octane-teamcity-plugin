/**
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
