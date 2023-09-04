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

import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import jetbrains.buildServer.serverSide.BuildServerEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by lazara on 07/02/2016.
 */

public class GenericOctaneActionsController implements Controller {

    @Autowired
    private BuildServerEx buildServerEx;

    /**
     * Support url : http://localhost:8093/nga/logs/?page=1
     * @param req
     * @param res
     * @return
     * @throws Exception
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        boolean handled = false;
        if ("get".equalsIgnoreCase(req.getMethod())) {
            if (req.getRequestURI().equalsIgnoreCase("/nga/logs/")) {
                String name = "nga";
                String pageStr= req.getParameter("page");
                Integer pageId = null;
                if (pageStr != null) {
                    try {
                        pageId = Integer.parseInt(pageStr);
                    } catch (Exception e) {
                        //do nothing
                    }
                }

                java.nio.file.Path path = Paths.get(
                        TeamCityPluginServicesImpl.getAllowedOctaneStorage(buildServerEx).getAbsolutePath(),
                        "nga",
                        "logs",
                        pageId == null ? name + ".log" : String.format("%s-%s.log", name, pageId));
                if (path.toFile().exists()) {
                    String returnStr = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    PrintWriter writer = res.getWriter();
                    writer.write(returnStr);
                    handled = true;
                }
            }
        }
        if (!handled) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }
}
