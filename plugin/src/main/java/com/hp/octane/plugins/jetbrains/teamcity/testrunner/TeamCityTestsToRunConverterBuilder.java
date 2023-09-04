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

package com.hp.octane.plugins.jetbrains.teamcity.testrunner;

import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.executor.TestsToRunConvertersFactory;
import com.hp.octane.integrations.executor.TestsToRunFramework;

import java.util.HashMap;

public class TeamCityTestsToRunConverterBuilder {
	public final static String TESTS_TO_RUN_PARAMETER = "testsToRun";
	public final static String TESTING_FRAMEWORK_PARAMETER = "Testing_framework";

	TestsToRunFramework testsToRunFramework;

	public TeamCityTestsToRunConverterBuilder(String testsToRunFramework){
		this.testsToRunFramework = TestsToRunFramework.fromValue(testsToRunFramework);
	}
	public TeamCityTestsToRunConverterBuilder(){
		this.testsToRunFramework = TestsToRunFramework.fromValue("mvnSurefire");
	}

	public TestsToRunConverterResult convert(String rawTests, String executingDirectory){
		return TestsToRunConvertersFactory.createConverter(testsToRunFramework).convert(rawTests, executingDirectory,new HashMap<>());
	}
}
