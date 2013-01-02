package com.imaginarymachines.confluence.plugins;

import com.atlassian.confluence.core.DefaultSaveContext;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceType;
import com.atlassian.confluence.spaces.SpacesQuery;
import com.atlassian.user.User;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StringBufferInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class PagesServlet extends HttpServlet {
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String actionName = request.getParameter("action");
		
		if (actionName!=null && actionName.equals("getpages")) {
		            
			WebResourceManager webResourceManager = (WebResourceManager)ContainerManager.getComponent("webResourceManager");
        	PageManager pageManager = (PageManager)ContainerManager.getComponent("pageManager");
            SpaceManager spaceManager = (SpaceManager)ContainerManager.getComponent("spaceManager");
            PermissionManager permissionManager = (PermissionManager)ContainerManager.getComponent("permissionManager");

            User user = AuthenticatedUserThreadLocal.getUser();
            String spacekey = request.getParameter("spacekey");
        	String term = request.getParameter("value");

            //SpacesQuery query = SpacesQuery.newQuery().withSpaceKey(spacekey).forUser(user).withPermission("EDIT").build();
            //SpacesQuery query = SpacesQuery.newQuery().withSpaceKey(spacekey).forUser(user).build();
            //List<Space> allspaces = spaceManager.getAllSpaces(query);
            //System.out.println("found "+allspaces.size()+" spaces"); 
            //Space space = allspaces.get(0);

            List<Space> allspaces = spaceManager.getSpacesEditableByUser(AuthenticatedUserThreadLocal.getUser());
            
            ArrayList<HashMap<String, String>> pagemap = new ArrayList<HashMap<String, String>>();
            
            if (term!=null && !term.equals("")) {
                Iterator<Space> itr = allspaces.iterator();
                    while (itr.hasNext()) {
                    Space space = itr.next();
                    if (space.getKey().equals(spacekey)) {
                        //ListBuilder<Page> pages = pageManager.getTopLevelPagesBuilder(space);
                        //List<Page> pages = pageManager.getTopLevelPages(space);
                        List<Page> pages = pageManager.getPagesStartingWith(space, term);
                        Iterator<Page> itr2 = pages.iterator();
                        while (itr2.hasNext()) {
                            Page page = itr2.next();
                            if(permissionManager.hasPermission(user, Permission.VIEW, page)) {
                                HashMap<String, String> entry = new HashMap<String, String>();
                                entry.put("value", page.getTitle());
                                pagemap.add(entry);                
                            }

                        }
                    }
                }
            }
            
            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(pagemap));
            
		}
		
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
}