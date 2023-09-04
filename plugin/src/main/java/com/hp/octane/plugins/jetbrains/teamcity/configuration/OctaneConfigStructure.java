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

package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

import java.util.Objects;


@JacksonXmlRootElement(localName = "octane-config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OctaneConfigStructure implements Cloneable {

	public static String PASSWORD_REPLACER = "___PLAIN_PASSWORD___";

	@JacksonXmlProperty(localName ="identity")
	private String identity;
	@JacksonXmlProperty(localName ="identityFrom" )
	private String identityFrom;
	@JacksonXmlProperty(localName ="uiLocation" )
	private String uiLocation;
	@JacksonXmlProperty(localName ="api-key" )
	private String username;
	@JacksonXmlProperty(localName ="secret" )
	private String secretPassword;
	@JacksonXmlProperty(localName ="impersonatedUser" )
	private String impersonatedUser;
	@JacksonXmlProperty(localName ="location" )
	private String location;
	@JacksonXmlProperty(localName ="sharedSpace" )
	private String sharedSpace;

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getIdentityFrom() {
		return identityFrom;
	}

	public void setIdentityFrom(String identityFrom) {
		this.identityFrom = identityFrom;
	}

	public String getUiLocation() {
		return uiLocation;
	}

	public void setUiLocation(String uiLocation) {
		this.uiLocation = uiLocation;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getSecretPassword() {
		return secretPassword;
	}

	public String unscramblePassword() {
		if (secretPassword != null && !secretPassword.isEmpty() && EncryptUtil.isScrambled(secretPassword)) {
			return EncryptUtil.unscramble(secretPassword);
		} else {
			return secretPassword;
		}
	}

	public void setSecretPassword(String secretPassword) {
		if (secretPassword == null || PASSWORD_REPLACER.equals(secretPassword)) {
			this.secretPassword = secretPassword;
		} else if (!EncryptUtil.isScrambled(secretPassword)) {
			this.secretPassword = EncryptUtil.scramble(secretPassword);
		} else {
			this.secretPassword = secretPassword;
		}
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSharedSpace() {
		return sharedSpace;
	}

	public void setSharedSpace(String sharedSpace) {
		this.sharedSpace = sharedSpace;
	}

	@Override
	public String toString() {
		return "OctaneConfigStructure { " +
				"identity: " + identity +
				", identityFrom: " + identityFrom +
				", uiLocation: " + uiLocation +
				", apiKey: " + username +
				", location: " + location +
				", impersonatedUser: " + impersonatedUser +
				", sharedSpace: " + sharedSpace + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OctaneConfigStructure that = (OctaneConfigStructure) o;
		return Objects.equals(location, that.location) &&
				Objects.equals(username, that.username) &&
				Objects.equals(getSecretPassword(), that.getSecretPassword()) &&
				Objects.equals(impersonatedUser, that.impersonatedUser) &&
				Objects.equals(sharedSpace, that.sharedSpace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uiLocation, username, getSecretPassword(), impersonatedUser, sharedSpace);
	}

	public String getImpersonatedUser() {
		return impersonatedUser;
	}

	public void setImpersonatedUser(String impersonatedUser) {
		this.impersonatedUser = impersonatedUser;
	}

	public OctaneConfigStructure cloneWithoutSensitiveFields() {
		try {
			OctaneConfigStructure cloned = (OctaneConfigStructure)this.clone();
			cloned.setSecretPassword(PASSWORD_REPLACER);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("failed to cloneWithoutSensitiveFields");
		}
	}
}
