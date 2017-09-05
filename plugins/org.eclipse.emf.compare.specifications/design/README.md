<!--
Title: Design Documents README File  
Author: Christian W. Damus  
Affiliation: Eclipse.org  
Version: 1.0  
Date: 9 August, 2017  
Copyright: Copyright © 2017 Christian W. Damus and others.  
           All rights reserved.  
           This program and the accompanying materials  
           are made available under the terms of the Eclipse Public License v1.0  
           which accompanies this distribution, and is available at  
           http://www.eclipse.org/legal/epl-v10.html  
-->

## Design Documents

This folder contains design documentation that is not intended for end users.
Accordingly, it is not included in the `build.properties` neither for binary
distribution nor for the source bundle.  It is expected that developers that
wish to make reference to this documentation will have checked out the source
from git anyways and can use it in the workspace.

### Documentation Generation

Only the source files (including design models) are maintained in git.
Generated artifacts such as PNGs/SVGs (from Papyrus diagrams), PDF or HTML
rendered from the Markdown sources, etc. are expected to be generated either
in an automatic build process or manually by the developer.
