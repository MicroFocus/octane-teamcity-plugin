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
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.pipelines.PipelinePhase;
import com.hp.octane.integrations.dto.snapshots.CIBuildResult;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.dependency.Dependency;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lazara on 04/01/2016.
 */

public class ModelCommonFactory {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final Logger logger = SDKBasedLoggerProvider.getLogger(ModelCommonFactory.class);

	@Autowired
	private TCPluginParametersFactory parametersFactory;
	@Autowired
	private ProjectManager projectManager;

	public CIJobsList createProjectList() {
		CIJobsList ciJobsList = dtoFactory.newDTO(CIJobsList.class);
		List<PipelineNode> list = new ArrayList<>();
		List<String> ids = new ArrayList<>();

		PipelineNode buildConf;
		for (SProject project : projectManager.getProjects()) {

			List<SBuildType> buildTypes = project.getBuildTypes();
			for (SBuildType buildType : buildTypes) {
				logger.info("Fetching data for project: " + buildType.getName());
				if (!ids.contains(buildType.getInternalId())) {
					ids.add(buildType.getInternalId());
					buildConf = dtoFactory.newDTO(PipelineNode.class)
							.setJobCiId(buildType.getExternalId())
							.setName(getFullNameFromBuildType(buildType))
							.setParameters(parametersFactory.obtainFromBuildType(buildType));
					list.add(buildConf);
				}
			}
		}

		ciJobsList.setJobs(list.toArray(new PipelineNode[list.size()]));
		return ciJobsList;
	}

	public PipelineNode createStructure(String buildConfigurationId) {
		SBuildType root = projectManager.findBuildTypeByExternalId(buildConfigurationId);
		PipelineNode treeRoot = null;
		if (root != null) {
			treeRoot = dtoFactory.newDTO(PipelineNode.class)
					.setJobCiId(root.getExternalId())
					.setName(getFullNameFromBuildType(root))
					.setParameters(parametersFactory.obtainFromBuildType(root));

			List<PipelineNode> pipelineNodeList = buildFromDependenciesFlat(root.getOwnDependencies());
			if (!pipelineNodeList.isEmpty()) {
				PipelinePhase phase = dtoFactory.newDTO(PipelinePhase.class)
						.setName("teamcity_dependencies")
						.setBlocking(true)
						.setJobs(pipelineNodeList);
				List<PipelinePhase> pipelinePhaseList = new ArrayList<PipelinePhase>();
				pipelinePhaseList.add(phase);
				treeRoot.setPhasesPostBuild(pipelinePhaseList);
			}
		} else {
			//should update the response?
		}
		return treeRoot;
	}

	private List<PipelineNode> buildFromDependenciesFlat(List<Dependency> dependencies) {
		List<PipelineNode> result = new LinkedList<>();
		if (dependencies != null) {
			for (Dependency dependency : dependencies) {
				SBuildType build = dependency.getDependOn();
				if (build != null) {
					PipelineNode buildItem = dtoFactory.newDTO(PipelineNode.class)
							.setJobCiId(build.getExternalId())
							.setName(getFullNameFromBuildType(build))
							.setParameters(parametersFactory.obtainFromBuildType(build));
					result.add(buildItem);
					result.addAll(buildFromDependenciesFlat(build.getOwnDependencies()));
				}
			}
		}
		return result;
	}

	public CIBuildResult resultFromNativeStatus(Status status, boolean isInterrupted) {
		CIBuildResult result = isInterrupted ? CIBuildResult.ABORTED : CIBuildResult.UNAVAILABLE;
		if (status == Status.ERROR || status == Status.FAILURE) {
			result = CIBuildResult.FAILURE;
		} else if (status == Status.WARNING) {
			result = CIBuildResult.UNSTABLE;
		} else if (status == Status.NORMAL) {
			result = CIBuildResult.SUCCESS;
		}
		return result;
	}

    private String getFullNameFromBuildType(SBuildType buildType) {
        return buildType.getProjectName() + " - " + buildType.getName();
    }

}
