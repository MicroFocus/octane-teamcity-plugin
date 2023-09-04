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

package com.hp.octane.plugins.jetbrains.teamcity.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.services.configurationparameters.OctaneRootsCacheAllowedParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.integrations.utils.OctaneUrlParser;
import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigMultiSharedSpaceStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationService;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import com.hp.octane.plugins.jetbrains.teamcity.utils.Utils;
import jetbrains.buildServer.serverSide.auth.Permission;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.buildResponseStringEmptyConfigsWithError;

/**
 * Created by lazara on 14/02/2016.
 */

public class ConfigurationActionsController implements Controller {
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(ConfigurationActionsController.class);

	@Autowired
	private TCConfigurationService configurationService;
	@Autowired
	private TCConfigurationHolder holder;

	@Override
	public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
		if (!Utils.hasPermission(httpServletRequest, Permission.CHANGE_SERVER_SETTINGS)) {
			httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}

		String returnStr = "";
		String action = httpServletRequest.getParameter("action");

		if (!"post".equalsIgnoreCase(httpServletRequest.getMethod()) && (action == null || action.isEmpty())) {
			returnStr = reloadConfiguration();
		} else {
			try {
				if ("test".equalsIgnoreCase(action)) {
					String server = httpServletRequest.getParameter("server");
					String instanceId = httpServletRequest.getParameter("instanceId");
					try {
						String apiKey = httpServletRequest.getParameter("username");
						String secret = httpServletRequest.getParameter("password");
						if(OctaneConfigStructure.PASSWORD_REPLACER.equals(secret) && holder.getOctaneConfigurations().containsKey(instanceId)){
							secret = holder.getOctaneConfigurations().get(instanceId).getSecret();
						}
						OctaneConfiguration testedOctaneConfiguration = OctaneConfiguration.createWithUiLocation(UUID.randomUUID().toString(),
								server);
						testedOctaneConfiguration.setClient(apiKey);
						testedOctaneConfiguration.setSecret(secret);
						String impersonatedUser = httpServletRequest.getParameter("impersonatedUser");
						returnStr = configurationService.checkConfiguration(testedOctaneConfiguration, impersonatedUser);
					} catch (Exception error) {
						returnStr = buildResponseStringEmptyConfigsWithError(error.getMessage());
					}
				} else {
					//save configuration
					ObjectMapper objectMapper = new ObjectMapper();
					TypeFactory typeFactory = objectMapper.getTypeFactory();
					CollectionType collectionType = typeFactory.constructCollectionType(
							List.class, OctaneConfigStructure.class);
					List<OctaneConfigStructure> configs = objectMapper.readValue(httpServletRequest.getInputStream(), collectionType);
					handleDeletedConfigurations(configs);
					restorePasswords(configs);

					returnStr = updateConfiguration(configs);
				}
			} catch (Exception e) {
				logger.error("failed to process configuration request (" + (action == null ? "save" : action) + ")", e);
				returnStr = e.getMessage() + ". Failed to process configuration request (" + (action == null ? "save" : action) + ")";
				returnStr = buildResponseStringEmptyConfigsWithError(returnStr);
			}
		}

		PrintWriter writer;
		try {
			writer = httpServletResponse.getWriter();
			writer.write(returnStr);
		} catch (IOException ioe) {
			logger.error("failed to write response", ioe);
		}
		return null;
	}

	private void restorePasswords(List<OctaneConfigStructure> configs) {
		configs.forEach(c->{
			if(OctaneConfigStructure.PASSWORD_REPLACER.equals(c.unscramblePassword()) && holder.getOctaneConfigurations().containsKey(c.getIdentity())){
				c.setSecretPassword(holder.getOctaneConfigurations().get(c.getIdentity()).getSecret());
			}
		});
	}

	private void handleDeletedConfigurations(List<OctaneConfigStructure> newConfigs) {
		Map<String, OctaneConfiguration> origConfigs = holder.getOctaneConfigurations();
		Set<String> newConfigIdentities = new HashSet<>();
		for (OctaneConfigStructure conf : newConfigs) {
			String identity = conf.getIdentity() == null || conf.getIdentity().isEmpty() ? "empty" : conf.getIdentity();
			newConfigIdentities.add(identity);
		}
		Set<String> configToRemove = new HashSet<>();
		for (String identity : origConfigs.keySet()) {
			if (!newConfigIdentities.contains(identity)) {
				logger.info("Removing client with instance Id: " + identity);
				try {
					OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(identity));
				} catch (Exception ex) {
					logger.error("Failed to remove SDK client. Trying to continue anyway ", ex);
				}
				configToRemove.add(identity);
			}
		}

		if (!configToRemove.isEmpty()) {
			configToRemove.forEach(instanceId -> origConfigs.remove(instanceId));
			//remove config before save
			for (OctaneConfigStructure octaneConfigStructure : new ArrayList<>(holder.getConfigs())) {
				if (configToRemove.contains(octaneConfigStructure.getIdentity())) {
					holder.getConfigs().remove(octaneConfigStructure);
				}
			}
			save();
		}
	}

	private String save() {
		logger.info("Saving ALM Octane configurations...");
		OctaneConfigMultiSharedSpaceStructure confs = new OctaneConfigMultiSharedSpaceStructure();
		confs.setMultiConfigStructure(holder.getConfigs());
		return configurationService.saveConfig(confs);
	}

	public String updateConfiguration(List<OctaneConfigStructure> newConfigs) {
		List<OctaneConfigStructure> originalConfigs = holder.getConfigs();
		for (OctaneConfigStructure newConf : newConfigs) {
			OctaneConfigStructure result = originalConfigs.stream()
					.filter(or_conf -> or_conf.getIdentity().equals(newConf.getIdentity()))
					.findAny()
					.orElse(null);

			if (result == null || holder.getOctaneConfigurations().get(result.getIdentity()) == null) {
				OctaneUrlParser octaneUrlParser;
				try {
					octaneUrlParser = checkAndUpdateIdentityAndLocationIfNotTheSame(newConf);
				} catch (Exception error) {
					return buildResponseStringEmptyConfigsWithError(error.getMessage());

				}
				OctaneConfiguration octaneConfiguration = OctaneConfiguration.create(newConf.getIdentity(), newConf.getLocation(),
						octaneUrlParser.getSharedSpace());
				octaneConfiguration.setClient(newConf.getUsername());
				octaneConfiguration.setSecret(newConf.unscramblePassword());

				try {
					ConfigurationParameterFactory.addParameter(octaneConfiguration, OctaneRootsCacheAllowedParameter.KEY, "false");
					OctaneSDK.addClient(octaneConfiguration, TeamCityPluginServicesImpl.class);
				} catch (Exception e) {
					return buildResponseStringEmptyConfigsWithError(e.getMessage());
				}
				holder.getOctaneConfigurations().put(newConf.getIdentity(), octaneConfiguration);
				newConf.setSharedSpace(octaneUrlParser.getSharedSpace());
				holder.getConfigs().add(newConf);
			} else {
				//update existing configuration
				OctaneConfiguration octaneConfiguration = holder.getOctaneConfigurations().get(result.getIdentity());

				OctaneUrlParser octaneUrlParser;
				try {
					octaneUrlParser = OctaneUrlParser.parse(newConf.getUiLocation());
				} catch (Exception error) {
					return buildResponseStringEmptyConfigsWithError(error.getMessage());

				}

				String sp = octaneUrlParser.getSharedSpace();
				String location = octaneUrlParser.getLocation();
				octaneConfiguration.setUiLocation(newConf.getUiLocation());
				result.setUiLocation(newConf.getUiLocation());
				octaneConfiguration.setClient(newConf.getUsername());
				result.setUsername(newConf.getUsername());
				octaneConfiguration.setSecret(newConf.unscramblePassword());
				result.setSecretPassword(newConf.getSecretPassword());
				result.setSharedSpace(sp);
				result.setLocation(location);
				result.setImpersonatedUser(newConf.getImpersonatedUser());
			}
		}

		return save();
	}

	private OctaneUrlParser checkAndUpdateIdentityAndLocationIfNotTheSame(OctaneConfigStructure newConf) {

		String identity = newConf.getIdentity();
		OctaneUrlParser octaneUrlParser = OctaneUrlParser.parse(newConf.getUiLocation());
		newConf.setLocation(octaneUrlParser.getLocation());

		if (holder.getConfigs().contains(newConf)) {
			OctaneConfigStructure matchingObject = holder.getConfigs().stream().
					filter(c -> c.equals(newConf)).
					findAny().orElse(null);
			if (matchingObject != null) {
				newConf.setIdentity(matchingObject.getIdentity());
				newConf.setIdentityFrom(matchingObject.getIdentityFrom());
				return octaneUrlParser;
			}
		}
		if (identity == null || identity.equals("") || "undefined".equalsIgnoreCase(identity)) {
			newConf.setIdentity(UUID.randomUUID().toString());
			newConf.setIdentityFrom(String.valueOf(new Date().getTime()));
		}
		return octaneUrlParser;
	}

	public String reloadConfiguration() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			List<OctaneConfigStructure> cfg = holder.getConfigs().stream().map(c -> c.cloneWithoutSensitiveFields()).collect(Collectors.toList());
			return mapper.writeValueAsString(cfg);
		} catch (JsonProcessingException jpe) {
			logger.error("failed to reload configuration", jpe);
			return Utils.buildResponseStringEmptyConfigsWithError("failed to reload configuration");
		}
	}
}
