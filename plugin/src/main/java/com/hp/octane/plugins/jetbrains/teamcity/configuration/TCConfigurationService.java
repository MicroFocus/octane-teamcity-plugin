/*
 *     2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.exceptions.OctaneConnectivityException;
import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import jetbrains.buildServer.serverSide.BuildServerEx;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.*;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 * Created by lazara.
 * Created by gadiel.
 */

public class TCConfigurationService {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(TCConfigurationService.class);

	private static final String CONFIG_FILE = "octane-config.xml";

	@Autowired
	private SBuildServer buildServer;
	@Autowired
	private PluginDescriptor pluginDescriptor;
	@Autowired
	private BuildServerEx buildServerEx;

	public String checkConfiguration(OctaneConfiguration octaneConfiguration, String impersonatedUser) {

		String resultMessage = setMessageFont("Connection succeeded for " + octaneConfiguration.getLocationForLog(), "green");

		try {
			OctaneSDK.testOctaneConfigurationAndFetchAvailableWorkspaces(octaneConfiguration.getUrl(),
					octaneConfiguration.getSharedSpace(),
					octaneConfiguration.getClient(),
					octaneConfiguration.getSecret(),
					TeamCityPluginServicesImpl.class);
		} catch (OctaneConnectivityException octaneConnEx){
			resultMessage = setMessageFont(octaneConnEx.getErrorMessageVal(), "red");
		} catch (Exception e) {
			return buildResponseStringEmptyConfigsWithError("Connection failed: " + e.getMessage());
		}

		resultMessage = checkImpersonatedUser(resultMessage, impersonatedUser);
		return buildResponseStringEmptyConfigs(resultMessage);
	}

	private String checkImpersonatedUser(String resultMessage, String impersonatedUser) {
		if (impersonatedUser == null || impersonatedUser.isEmpty()) {
			return resultMessage;
		}
		UserModel userModel = buildServer.getUserModel();
		List<SUser> users = new ArrayList<>(userModel.getAllUsers().getUsers());
		SUser user = users.stream().filter(u -> impersonatedUser.equals(u.getUsername())).findAny()
				.orElse(null);
		if (user == null) {
			resultMessage = resultMessage + "<br><font color=\"red\">Warning! </font>User '" + impersonatedUser + "' is not defined in TeamCity";
		}
		return resultMessage;
	}

	public List<OctaneConfigStructure> readConfig() {
		OctaneConfigMultiSharedSpaceStructure multiSharedSpaceStructure;
		try {
			XmlMapper xmlMapper = DTOFactory.getInstance().getXMLMapper();
			multiSharedSpaceStructure = xmlMapper.readValue(getConfigurationResource(), OctaneConfigMultiSharedSpaceStructure.class);
		} catch (IOException e) {
			logger.error("failed to read Octane configuration", e);
			return null;
		}
		return multiSharedSpaceStructure.getMultiConfigStructure();
	}

	public String saveConfig(OctaneConfigMultiSharedSpaceStructure configs) {
		try {
			DTOFactory.getInstance().getXMLMapper().writeValue(getConfigurationResource(), configs);

			//handle response
			int index = 0;
			String result = "{\"configs\":{";
			for (OctaneConfigStructure conf : configs.getMultiConfigStructure()) {
				result += "\"" + index + "\" : \"" + conf.getIdentity() + "\",";
				index++;
			}
			result = configs.getMultiConfigStructure().isEmpty() ? result : result.substring(0, result.length() - 1);
			result += "}, \"status\":\"" + escapeHtml4(setMessageFont("Configurations updated successfully", "green"))+ "\"}";
			return result;
		} catch (IOException e) {
			logger.error("failed to save Octane configurations", e);
			return buildResponseStringEmptyConfigsWithError("failed to save Octane configurations");
		} catch (IllegalStateException e) {
			logger.error("failed to publish Octane configurations", e);
			return buildResponseStringEmptyConfigsWithError("failed to publish Octane configurations");
		}
	}

	private File getConfigurationResource() {
		File parentFolder = new File(TeamCityPluginServicesImpl.getAllowedOctaneStorage(buildServerEx), "nga");
		parentFolder.mkdirs();
		return new File(parentFolder, CONFIG_FILE);
	}

	public boolean isEmptyConfig() {
		File configFile = getConfigurationResource();

		//copy configuration to new location in teamCityHome
		if (!configFile.exists()) {
			File oldConfigFile = new File(buildServer.getServerRootPath() + pluginDescriptor.getPluginResourcesPath(CONFIG_FILE));
			if (oldConfigFile.exists()) {
				try {
					FileUtils.copyFile(oldConfigFile, configFile);
					logger.info(String.format("Octane configuration copied successfully from %s to new location %s",
							oldConfigFile.getAbsolutePath(), configFile.getAbsolutePath()));
				} catch (IOException e) {
					logger.error(String.format("*****  Failed to copy octane configuration from %s to new location %s : %s",
							oldConfigFile.getAbsolutePath(), configFile.getAbsolutePath(), e.getMessage()), e);
				}
			}
		}
		return !configFile.exists() || configFile.length() == 0;
	}
}
