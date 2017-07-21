#!/bin/bash
#
# Copyright (c) 2017 Christian W. Damus and others.
# 
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Christian W. Damus - Initial API and implementation
#   

#
# A script meant for execution in the Travis CI environment
# in which the verbosity of log output is a problem.
#

set -eux -o pipefail

basedir=$(dirname "$0")/..

mvn -f "${basedir}/pom.xml" -q \
	-Dsurefire.printSummary=false \
	$* | grep -E -v -e 'Time elapsed: [0-9.]+ sec\s*$'
