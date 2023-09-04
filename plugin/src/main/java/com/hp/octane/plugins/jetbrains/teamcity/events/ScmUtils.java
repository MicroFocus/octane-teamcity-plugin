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

package com.hp.octane.plugins.jetbrains.teamcity.events;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.scm.*;
import com.hp.octane.integrations.dto.scm.impl.SCMChangeType;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SDKBasedLoggerProvider;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.vcs.*;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class ScmUtils {

	private static final Logger logger = SDKBasedLoggerProvider.getLogger(ScmUtils.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	static SCMData getScmData(@NotNull SRunningBuild build) {

		SCMData scmData = null;
		try {
			if (build.getVcsRootEntries() != null && !build.getVcsRootEntries().isEmpty() &&
					build.getRevisions() != null && !build.getRevisions().isEmpty()) {
				if (build.getVcsRootEntries().size() > 1) {
					logger.warn("found " + build.getVcsRootEntries().size() + " repositories for the " + build + ", currently supporting only the first one and omitting the rest");
				}
				if (build.getRevisions().size() > 1) {
					logger.warn("found " + build.getRevisions().size() + " revisions for the " + build + ", currently supporting only the first one and omitting the rest");
				}
				scmData = dtoFactory.newDTO(SCMData.class);
				scmData.setCommits(getScmCommits(build.getContainingChanges()));
				scmData.setRepository(setRepositoryFromVcsRoot(build.getVcsRootEntries().get(0).getVcsRoot()));
				scmData.setBuiltRevId(build.getBuildNumber());
			}
		} catch (Exception e) {
			logger.error("failed to extract SCM data from " + build, e);
		}

		return scmData;
	}

	private static SCMRepository setRepositoryFromVcsRoot(VcsRootInstance vcsRootInstance) {
		return dtoFactory.newDTO(SCMRepository.class)
			.setUrl(vcsRootInstance.getProperty("url"))
			.setBranch(vcsRootInstance.getProperty("branch"))
			.setType(scmTypeFromVcsRoot(vcsRootInstance));
	}

	private static List<SCMCommit> getScmCommits(List<SVcsModification> containingChanges) {
		List<SCMCommit> scmCommitList = new ArrayList<>();
		for (SVcsModification change : containingChanges) {
			scmCommitList.add(createScmCommit(change));
		}
		return scmCommitList;
	}

	private static SCMCommit createScmCommit(SVcsModification change) {
		return dtoFactory.newDTO(SCMCommit.class)
			.setRevId(change.getDisplayVersion())
			.setComment(change.getDescription().trim())
			.setUser(change.getUserName())
			.setUserEmail(change.getCommitters().isEmpty() ? null : change.getCommitters().iterator().next().getEmail())
			.setTime(change.getVcsDate().getTime())
			.setChanges(getFileChangeList(change.getChanges()));
	}

	private static SCMType scmTypeFromVcsRoot(VcsRootInstance vcsRootInstance) {
		String repositoryType = vcsRootInstance.getVcsDisplayName();
		if (repositoryType.equalsIgnoreCase("git")) {
			return SCMType.GIT;
		} else if (repositoryType.equalsIgnoreCase("Subversion")) {
			return SCMType.SVN;
		}
		return SCMType.UNKNOWN;
	}



	private static List<SCMChange> getFileChangeList(List<VcsFileModification> fileList) {
		List<SCMChange> scmChangesList = new ArrayList<>();
		for (VcsFileModification fileChange : fileList) {
			SCMChange scmChange = dtoFactory.newDTO(SCMChange.class)
				.setFile(fileChange.getFileName())
				.setType(scmChangeTypeFromChange(fileChange).toString());
			scmChangesList.add(scmChange);
		}
		return scmChangesList;
	}

	@NotNull
	private static SCMChangeType scmChangeTypeFromChange(VcsFileModification fileChange) {
		SCMChangeType result;
		switch (fileChange.getType()) {
			case ADDED:
			case DIRECTORY_ADDED:
				result = SCMChangeType.ADD;
				break;
			case CHANGED:
			case DIRECTORY_CHANGED:
			case DIRECTORY_COPIED:
				result = SCMChangeType.EDIT;
				break;
			case REMOVED:
			case DIRECTORY_REMOVED:
				result = SCMChangeType.DELETE;
				break;
			default:
				result = SCMChangeType.UNKNOWN;
		}
		return result;
	}

}
