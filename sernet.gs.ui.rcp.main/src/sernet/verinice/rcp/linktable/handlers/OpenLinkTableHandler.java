/*******************************************************************************
 * Copyright (c) 2016 Daniel Murygin <dm{a}sernet{dot}de>.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Daniel Murygin <dm{a}sernet{dot}de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.rcp.linktable.handlers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import sernet.verinice.rcp.linktable.LinkTableEditorInput;
import sernet.verinice.rcp.linktable.LinkTableUtil;
import sernet.verinice.rcp.linktable.Messages;
import sernet.verinice.service.linktable.vlt.VeriniceLinkTable;
import sernet.verinice.service.linktable.vlt.VeriniceLinkTableIO;

/**
 * Handler class for opening existing report queries. This handler is configured
 * in plugin.xml.
 *
 * @author Daniel Murygin <dm{a}sernet{dot}de>
 */
public class OpenLinkTableHandler extends LinkTableHandler {

    public OpenLinkTableHandler() {
        super();
    }

    @Override
    protected LinkTableEditorInput createLinkTable() {
        final String filePath = LinkTableUtil.createVltFilePath(
                Display.getCurrent().getActiveShell(), Messages.OpenLinkTableHandler_0, SWT.OPEN,
                null);
        LinkTableEditorInput linkTableEditorInput = null;
        if (filePath != null) {
            VeriniceLinkTable veriniceLinkTable = VeriniceLinkTableIO.read(filePath);
            linkTableEditorInput = new LinkTableEditorInput(veriniceLinkTable);
            linkTableEditorInput.setFilePath(filePath);
        }
        return linkTableEditorInput;
    }
}
