/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2002 Jan Blok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.mypanels;

import com.izforge.izpack.api.data.ResourceManager;
import com.izforge.izpack.installer.base.InstallerFrame;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.panels.path.PathInputPanel;
import java.io.File;

/**
 * The Hello panel class.
 *
 * @author Julien Ponge
 */
public class MyTargetPanel extends PathInputPanel
{
    /**
     * Called when the panel becomes active.
     */
    @Override
    public boolean isValidated()
    {
        // Standard behavior of PathInputPanel.
        if (!super.isValidated())
        {
            return (false);
        }
        this.installData.setInstallPath(pathSelectionPanel.getPath()+File.separatorChar+"PhaidraImporter");
        return (true);
    }
    
    @Override
    public void panelActivate()
    {
        // Resolve the default for chosenPath
        super.panelActivate();

        loadDefaultInstallDir();
        pathSelectionPanel.setPath(System.getProperty("user.home")+File.separatorChar+"PhaidraImporter");
                
        //if (getDefaultInstallDir() != null)
        //{
        //}
    }
    
    public MyTargetPanel(InstallerFrame parent, GUIInstallData idata, ResourceManager resourceManager)
    {
        super(parent, idata, resourceManager);
    }
}