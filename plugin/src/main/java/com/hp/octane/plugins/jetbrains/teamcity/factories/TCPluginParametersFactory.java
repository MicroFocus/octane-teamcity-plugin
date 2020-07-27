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

package com.hp.octane.plugins.jetbrains.teamcity.factories;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;

import java.util.*;

/**
 * Created by gullery on 22/03/2016.
 */

public class TCPluginParametersFactory {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public List<CIParameter> obtainFromBuildType(SBuildType buildType) {
		List<CIParameter> result = new LinkedList<>();
		CIParameter tmp;

		if (buildType != null && !buildType.getParameters().isEmpty()) {
			Set<String> paramsNames = getParametersNameSet(buildType.getParameters());
			for (String paramName : paramsNames) {
				String name = getOriginalParamName(paramName);
				tmp = dtoFactory.newDTO(CIParameter.class)
						.setType(CIParameterType.STRING)
						.setName(name)
						.setDescription(buildType.getParameters().get(paramName))
						.setDefaultValue(buildType.getParameters().get(name))
						.setValue( buildType.getParameters().get(paramName));
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
}
