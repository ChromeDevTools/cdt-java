#!/usr/bin/python

#  Copyright (c) 2012 The Chromium Authors. All rights reserved.
#  Use of this source code is governed by a BSD-style license that can be
#  found in the LICENSE file.

#  A script that helps Ant build system to recognize current plugin versions numbers --
#  a task that Ant can't seem to do itself. Input: built chromedevtools plugins
#  and features. Output: .properties file that has version numbers extracted.
#  These numbers are going to be used to generate additional zip/tar deployment (standalone
#  libraries and update site zip).

import sys
import os
import re

buildDirectory = sys.argv[1]
outputDir = sys.argv[2]

items = os.listdir("%s/plugins" % buildDirectory)
# print(items)

def findRegExp(nameList, pattern):
    result = []
    for n in nameList:
        r = pattern.match(n)
        if r:
            result.append(r)
    if len(result) != 1:
        raise Error("Exactly one file name should match %s" % pattern)
    return result[0]

sdkMatch = findRegExp(items, re.compile(r'org\.chromium\.sdk_(\d+\.\d+\.\d+)\.(.+)\.jar'))
backendSourceMatch = findRegExp(items,
    re.compile(r'org\.chromium\.sdk\.wipbackends\.source_(\d+\.\d+\.\d+)\.(.+)\.jar'))

propertiesFile = open("%s/pluginVersion.properties" % outputDir, mode='w')

propertiesFile.write("mainVersion=%s\n" % sdkMatch.group(1))
propertiesFile.write("backendVersion=%s\n" % backendSourceMatch.group(1))
propertiesFile.write("mainBuilderVersion=%s\n" % sdkMatch.group(2))
propertiesFile.write("backendBuilderVersion=%s\n" % backendSourceMatch.group(2))

propertiesFile.close()
