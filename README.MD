# ALM Octane server-side plugin for Teamcity

Project status: [![Build status](https://ci.appveyor.com/api/projects/status/mby6wig2w6x5j22o?svg=true)](https://ci.appveyor.com/project/OctaneCIPlugins/octane-teamcity-plugin)

Master release branch status: [![Build status](https://ci.appveyor.com/api/projects/status/mby6wig2w6x5j22o/branch/master?svg=true)](https://ci.appveyor.com/project/OctaneCIPlugins/octane-teamcity-plugin/branch/master)


## Relevant links
-	**Download the most recent LTS version of the plugin** at [Jetbrains plugins marketplace](https://plugins.jetbrains.com/plugin/9542-alm-octane-ci-integration)
-	**Check the open issues (and add new issues)** at [Github issues](https://github.com/MicroFocus/octane-teamcity-plugin/issues)
 
 
## Install
In your TeamCity server's user interface, open the plugin management area and upload the plugin:

1. Go to Administration > Plugins List and click Upload plugin zip.
2. Browse to the plugin file that you downloaded and click Upload.
3. Restart the TeamCity server.


## Configure

In your TeamCity server's user interface, locate the configuration area for the ALM Octane CI plugin:

1. Go to Administration.
2. Under Server Administration, click ALM Octane CI Plugin.

For further configuration information please see the ["Configure the ALM Octane CI plugin to access ALM Octane"](https://admhelp.microfocus.com/octane/en/16.0.100-16.0.400/Online/Content/AdminGuide/how_config_CI_plugin.htm#mt-item-2) in our help center

### Advanced configuration

Each ALM Octane configuration includes a collapsible **Advanced configuration** section (click **Advanced configuration** to expand it). It currently contains a single option:

- **Send SCM data** *(enabled by default)*

  Controls whether the plugin sends SCM (commit) data events to ALM Octane. When enabled, on every build the plugin reports the associated repository, branch, and commit/change details. When cleared, the plugin stops sending SCM events for that configuration, while continuing to send all other events (pipeline lifecycle events, test results, etc.).

  This setting is **strictly optional** and should be left enabled in almost all cases, as SCM data is part of the standard integration flow. It is provided only for the rare situation in which SCM event processing needs to be turned off (for example, to work around a specific issue with SCM data on a given environment). Disabling it does not affect pipeline or test-result reporting.


## Upgrade

To upgrade the plugin to a newer version, upload the new plugin zip over the existing installation:

1. Go to **Administration > Plugins**.
2. If the currently installed ALM Octane CI plugin is listed, you may optionally remove it first (**Delete plugin**). Uploading a new zip with the same plugin name replaces the previous one.
3. Click **Upload plugin zip**, browse to the new plugin file, and click **Upload**.
4. Restart the TeamCity server to activate the upgraded plugin. (or use the experimental plugin reload option)

After the restart, verify the new version under **Administration > Plugins List**, and confirm your existing ALM Octane configurations are intact under **Administration > ALM Octane CI Plugin**.

> **Latest tested TeamCity version:** 2026.1.3. This is the most recent TeamCity version against which the plugin has been tested.

#### Disclaimer

Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company. This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText. Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.