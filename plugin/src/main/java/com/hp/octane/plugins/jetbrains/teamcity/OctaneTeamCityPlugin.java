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

package com.hp.octane.plugins.jetbrains.teamcity;

/**
 * Created by lazara on 23/12/2015.
 */

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.services.configurationparameters.OctaneRootsCacheAllowedParameter;
import com.hp.octane.integrations.services.configurationparameters.factory.ConfigurationParameterFactory;
import com.hp.octane.plugins.jetbrains.teamcity.actions.ConfigurationActionsController;
import com.hp.octane.plugins.jetbrains.teamcity.actions.GenericOctaneActionsController;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigMultiSharedSpaceStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationService;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import jetbrains.buildServer.serverSide.BuildServerEx;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.stream.XMLOutputFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OctaneTeamCityPlugin implements ServerExtension {
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(OctaneTeamCityPlugin.class);
    public static final String PLUGIN_NAME = OctaneTeamCityPlugin.class.getSimpleName().toLowerCase();


    @Autowired
    private GenericOctaneActionsController genericController;
    @Autowired
    private ConfigurationActionsController configurationController;
    @Autowired
    private TCConfigurationService configurationService;

    @Autowired
    private WebControllerManager webControllerManager;
    @Autowired
    private TCConfigurationHolder holder;
    @Autowired
    private SBuildServer buildServer;
    @Autowired
    BuildServerEx buildServerEx;

    private static String rootServerUrl = null;
    @PostConstruct
    private void initPlugin() throws Exception {
        buildServer.registerExtension(ServerExtension.class, PLUGIN_NAME, this);
        SDKBasedLoggerProvider.configure(TeamCityPluginServicesImpl.getAllowedOctaneStorage(buildServerEx));

        //Common Xml mapper factory that used on recent Team city versions
        DTOFactory.getInstance().initXmlMapper(new WstxInputFactory(),new WstxOutputFactory());

        logger.info("");
        logger.info("**********************************************************************");
        logger.info("********************STARTING TEAMCITY *********************************");
        logger.info("**********************************************************************");
        logger.info("CI SDK version " + OctaneSDK.SDK_VERSION);

        registerControllers();
        if (configurationService.isEmptyConfig()) {
            logger.info("ALM Octane CI Plugin configuration is empty");
            return;
        }

        List<OctaneConfigStructure> configs = configurationService.readConfig();
        if (configs == null || configs.isEmpty()) {
            logger.info("ALM Octane CI Plugin initialized; no valid configurations were found");
            return;
        }
        configs = validateLoadedConfigurations(configs);
        holder.setConfigs(configs);
        ensureServerInstanceID();
        for (OctaneConfigStructure config : holder.getConfigs()) {
               OctaneConfiguration octaneConfiguration = OctaneConfiguration.create(config.getIdentity(), config.getLocation(),
                       config.getSharedSpace());
               octaneConfiguration.setClient(config.getUsername());
               octaneConfiguration.setSecret(config.unscramblePassword());
               try{
                   ConfigurationParameterFactory.addParameter(octaneConfiguration, OctaneRootsCacheAllowedParameter.KEY, "false");
                   OctaneSDK.addClient(octaneConfiguration, TeamCityPluginServicesImpl.class);
                   holder.getOctaneConfigurations().put(config.getIdentity(), octaneConfiguration);
               } catch (Exception e) {
                   logger.error(e.getMessage(),e);
               }
        }
        logger.info("ALM Octane CI Plugin initialized; current configurations: " + holder.getConfigs().stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    @PreDestroy
    public void cleanUp() {
        logger.info("ALM Octane CI Plugin destroyed; current configurations: " + holder.getConfigs().stream().map(Object::toString).collect(Collectors.joining(",")));
        for (OctaneConfiguration config : holder.getOctaneConfigurations().values()) {
            OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(config.getInstanceId()));
        }
    }

    private List<OctaneConfigStructure> validateLoadedConfigurations(List<OctaneConfigStructure> configs) {
        for (OctaneConfigStructure octaneConfigStructure : new ArrayList<>(configs)) {
            if (octaneConfigStructure.getLocation() == null || octaneConfigStructure.getLocation().isEmpty()) {
                configs.remove(octaneConfigStructure);
            }
        }
        return configs;
    }

    private void registerControllers() {
        webControllerManager.registerController("/nga/**", genericController);
        webControllerManager.registerController("/octane-rest/**", configurationController);
    }

    private void ensureServerInstanceID() {
        if (holder.getConfigs() == null || holder.getConfigs().isEmpty()) {
            return;
        }
        boolean shouldSave = false;
        for (OctaneConfigStructure config : holder.getConfigs()) {
            String identity = config.getIdentity();
            if (identity == null || identity.isEmpty()) {
                config.setIdentity(UUID.randomUUID().toString());
                config.setIdentityFrom(String.valueOf(new Date().getTime()));
                shouldSave = true;
            }
        }
        if (shouldSave) {
            OctaneConfigMultiSharedSpaceStructure confs = new OctaneConfigMultiSharedSpaceStructure();
            confs.setMultiConfigStructure(holder.getConfigs());
            configurationService.saveConfig(confs);
        }
    }

    public static void setRootURL(String rootUrl) {
        if (rootUrl != null && !rootUrl.isEmpty()) {
            rootServerUrl = rootUrl;
            if (rootUrl.endsWith("/")) {
                rootServerUrl = rootUrl.substring(0, rootUrl.length() - 1);
            }
        }
    }

    public String getServerUrl(){
        return rootServerUrl;
    }
}
