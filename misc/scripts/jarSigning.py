#!/usr/bin/env python

# Copyright 2017-2023 Open Text.

# The only warranties for products and services of Open Text and
# its affiliates and licensors (“Open Text”) are as may be set forth
# in the express warranty statements accompanying such products and services.
# Nothing herein should be construed as constituting an additional warranty.
# Open Text shall not be liable for technical or editorial errors or
# omissions contained herein. The information contained herein is subject
# to change without notice.

# Except as specifically indicated otherwise, this document contains
# confidential information and a valid license is required for possession,
# use or copying. If this work is provided to the U.S. Government,
# consistent with FAR 12.211 and 12.212, Commercial Computer Software,
# Computer Software Documentation, and Technical Data for Commercial Items are
# licensed to the U.S. Government under vendor's standard commercial license.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import zipfile
import contextlib
import os
import subprocess
import sys
import shutil


#Global params
#zipname = '../target/teamcity-ci-plugin.zip'
#extractionFolder = './target/extracted'
#tempTarget = './target/temp'
#jarFolder = extractionFolder + '/server/'
total = len(sys.argv)
cmdargs = str(sys.argv)
print ("The total numbers of args passed to the script: %d " % total)
print ("Args list: %s " % cmdargs)

# map script parameters with maven properties
zipname = sys.argv[1]
print "zipname:" +  zipname

extractionFolder = sys.argv[2]
print "extractionFolder:" +  extractionFolder

tempTarget = sys.argv[3]
print "tempTarget:" + tempTarget

jarFolder = sys.argv[4]
print "jarFolder:" + jarFolder


# unzip teamcity.zip into target/extracted folder
def unzipTargetFile():

    print("extracting " + zipname + " and placing it in " + extractionFolder)
    with contextlib.closing(zipfile.ZipFile(zipname , "r")) as z:
            z.extractall(extractionFolder)
    print "Extracted : " + zipname +  " to: " + extractionFolder

# sign each jar inside server folder (under extracted folder) into target/temp folder	
# verify each signed jar and fail teamcity-ci-plugin-build in case of unsign
def signJars():

    print 'starting to sign jars'
    for filename in os.listdir(jarFolder):
        print 'starting to sign ' + str(filename)
        subprocess.call(["/opt/HPCSS/HPSignClient/HPSign.sh", "-r jarAgm", "-c HPSign.conf", "-i ", jarFolder + filename, "-o ", tempTarget + "/server/" ,"-obj jarfile_batch_sign_local_timestamp" ])
        output = subprocess.Popen(['/usr/bin/jarsigner -verify ' + tempTarget + '/server/' + str(filename)], stdout=subprocess.PIPE, shell=True).communicate()[0]
        print output
        if 'jar verified.' in output:
            print str(filename) + ' has been verified'
        else:
            print str(filename) + ' is not verified '
            print 'Killing this job due to file not being signed'
            sys.exit(1)

    print 'finished to sign jars'

# Copy teamcity-plugin.xml under target/temp folder
def copyXML():
    print 'copying teamcity-plugin.xml'
    shutil.copy2( extractionFolder + '/teamcity-plugin.xml', tempTarget + '/teamcity-plugin.xml')
    print 'finished copying teamcity-plugin.xml'

# Pack all files under target/temp to zip 
def packFiles():
    old_Working_directory = os.getcwd()
    os.chdir(tempTarget)
    zipf = zipfile.ZipFile('../../../target/hpe-alm-octaine-teamcity-ci-plugin.zip', 'w', zipfile.ZIP_DEFLATED)
    for root, dirs, files in os.walk("./"):
        for file in files:
            zipf.write(os.path.join(root, file))

    os.chdir(old_Working_directory)

if __name__ == "__main__":

    unzipTargetFile()
    signJars()
    copyXML()
    packFiles()