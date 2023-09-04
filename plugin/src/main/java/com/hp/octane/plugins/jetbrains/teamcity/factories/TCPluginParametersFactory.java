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

package com.hp.octane.plugins.jetbrains.teamcity.factories;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by gullery on 22/03/2016.
 */

public class TCPluginParametersFactory {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private static final Logger logger = SDKBasedLoggerProvider.getLogger(TCPluginParametersFactory.class);

    public List<CIParameter> obtainFromBuildType(SBuildType buildType) {
        List<CIParameter> result = new LinkedList<>();
        CIParameter tmp;

        if (buildType != null && !buildType.getParameters().isEmpty()) {
            Set<String> paramsNames = getParametersNameSet(buildType.getParameters());
            for (String paramName : paramsNames) {
                String name = getOriginalParamName(paramName);
                //do not pass parameters of type password to Octane
                logger.debug("Param name: " + paramName);
                logger.debug("OriginalParamName: " + name);

                if (buildType.getOwnParameter(name) != null &&
                        buildType.getOwnParameter(name).getControlDescription() != null &&
                        "password".equals(buildType.getOwnParameter(name).getControlDescription().getParameterType())) {
                    continue;
                }
                tmp = dtoFactory.newDTO(CIParameter.class)
                        .setType(CIParameterType.STRING)
                        .setName(name)
                        .setDescription("")
                        .setDefaultValue(buildType.getParameters().get(name))
                        .setValue(buildType.getParameters().get(paramName));
                result.add(tmp);
            }
        }
        return result;
    }

    private String getOriginalParamName(String paramName) {
        String name = paramName;
        if (paramName.startsWith("build.my")) {
            name = paramName.substring("build.my.".length(), paramName.length());
        }
        return name;
    }


    private Set<String> getParametersNameSet(Map<String, String> parameters) {
        Set<String> paramsNames = new HashSet<>();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            String name = parameter.getKey();
            if (parameter.getKey().startsWith("build.my")) {
                name = parameter.getKey().substring("build.my.".length(), parameter.getKey().length());
            }
            if (isNotVisibleParam(name)) {
                continue;
            }
            if (paramsNames.contains(name) && parameter.getKey().startsWith("build.my")) {
                paramsNames.remove(name);
                paramsNames.add(parameter.getKey());
            } else if (!parameter.getKey().startsWith("build.my") && paramsNames.contains("build.my." + name)) {
                continue;
            } else {
                paramsNames.add(parameter.getKey());
            }
        }
        return paramsNames;
    }

    public List<CIParameter> obtainFromBuild(SBuild build) {
        List<CIParameter> result = new LinkedList<>();
        CIParameter tmp;
        if (build != null && !build.getBuildOwnParameters().isEmpty()) {
            Set<String> paramsNames = getParametersNameSet(build.getBuildOwnParameters());
            for (String paramName : paramsNames) {
                String name = getOriginalParamName(paramName);
                tmp = dtoFactory.newDTO(CIParameter.class)
                        .setType(CIParameterType.STRING)
                        .setName(name)
                        .setValue(build.getBuildOwnParameters().get(paramName));
                result.add(tmp);
            }
        }
        return result;
    }

    private boolean isNotVisibleParam(String name) {
        if (name.startsWith("system.") ||
                name.startsWith("build.") ||
                name.startsWith("env.") ||
                name.startsWith("teamcity.")) {
            return true;
        }
        return false;
    }

    public List<CIParameter> obtainFromQueuedBuild(SQueuedBuild build) {
        List<CIParameter> result = new LinkedList<>();
        CIParameter tmp;
        if (build != null && !build.getBuildPromotion().getParameters().isEmpty()) {
            Set<String> paramsNames = getParametersNameSet(build.getBuildPromotion().getParameters());
            for (String paramName : paramsNames) {
                String name = getOriginalParamName(paramName);
                tmp = dtoFactory.newDTO(CIParameter.class)
                        .setType(CIParameterType.STRING)
                        .setName(name)
                        .setValue(build.getBuildPromotion().getParameters().get(paramName));
                result.add(tmp);
            }
        }
        return result;
    }
}
