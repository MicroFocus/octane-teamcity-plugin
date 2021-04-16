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

    @Override
    public ModelAndView handleRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        boolean handled = false;
        if ("get".equalsIgnoreCase(req.getMethod())) {
            if (req.getRequestURI().equalsIgnoreCase("/nga/logs/")) {
                String name = "nga";
                Integer id = null;
                java.nio.file.Path path = Paths.get(
                        TeamCityPluginServicesImpl.getAllowedOctaneStorage(buildServerEx).getAbsolutePath(),
                        "nga",
                        "logs",
                        id == null ? name + ".log" : String.format("%s-%s.log", name, id));
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
