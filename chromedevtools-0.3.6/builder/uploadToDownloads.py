#  Copyright (c) 2012 The Chromium Authors. All rights reserved.
#  Use of this source code is governed by a BSD-style license that can be
#  found in the LICENSE file.

# This script is expected to be called from build.xml uploadToDownloads target

import sys
import getpass
import googlecode_upload

resultDir = sys.argv[1]
user_name = sys.argv[2]

main_version = sys.argv[3]
backend_version = sys.argv[4]

pwd = getpass.getpass(prompt="Google SVN password: ")

def uploadFile(file, summary):
    googlecode_upload.upload(file=file, project_name="chromedevtools", user_name=user_name, password=pwd, summary=summary)

uploadFile("%s/org.chromium.sdk-wipbackends-%s-%s.tar" % (resultDir, main_version, backend_version), "ChromeDevTools SDK WIP backends v. %s/%s" % (main_version, backend_version))
uploadFile("%s/org.chromium.sdk-lib-%s.tar" % (resultDir, main_version), "ChromeDevTools SDK library as tar archive v. %s" % main_version)
uploadFile("%s/chromedevtools-%s-wipbackends-%s-site.zip" % (resultDir, main_version, backend_version), "ChromeDevTools %s/WIP backends %s update site in zip archive" % (main_version, backend_version))
