package com.imaginarymachines.confluence.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.core.ListBuilder;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.AttachmentUtils;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.pages.actions.PageAware;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpacesQuery;
import com.opensymphony.webwork.ServletActionContext;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;
import com.atlassian.plugin.webresource.UrlMode;
import java.util.List;

/**
* This Confluence action allows to copy a page including 
*/
public class TreecopyAction extends ConfluenceActionSupport implements PageAware {

	private static final long serialVersionUID = 7584904352458114888L;
	
    private static final Logger LOG = Logger.getLogger(TreecopyAction.class);

	private SpaceManager spaceManager;
	private PageManager pageManager;
	private AttachmentManager attachmentManager;
	private LabelManager labelManager;
	private SettingsManager settingsManager;
	private AbstractPage page;
	private Page currPage;
	private List<CopyPage> descendants;
	private List<Space> spaces;
	private String staticResourcePrefix;

	public String executeSetnames() {
		
		currPage = (Page) this.getWebInterfaceContext().getPage();
		
		CopyPage currCopy = createCopyPage(currPage,0);
		
		descendants = new ArrayList<CopyPage>();
		currCopy.readChildHierarchy(descendants);
		
		spaces = getSpacesEditableByUser(AuthenticatedUserThreadLocal.getUser());
		
		WebResourceUrlProvider webResourceUrlProvider = (WebResourceUrlProvider)ContainerManager.getComponent("webResourceUrlProvider");
		staticResourcePrefix = webResourceUrlProvider.getStaticResourcePrefix(UrlMode.ABSOLUTE);

		return "setnames";
	}
	
	public String executeCopy() {
		
		currPage = (Page) this.getWebInterfaceContext().getPage();
		CopyPage currCopy = createCopyPage(currPage,1);
		
		HttpServletRequest r = ServletActionContext.getRequest();
		
		Enumeration<String> enumParams = r.getParameterNames();
		while (enumParams.hasMoreElements()) {
			String name = enumParams.nextElement();
			String value = r.getParameter(name);
			
			if (name.startsWith("toggle-") && name.endsWith(value)) {
				
				CopyPage copypage = currCopy.getCopyPageById(Long.parseLong(value));
				if (copypage!=null) {
					// Sprawdzamy czy zalaczniki istnieja na filesystemie.
					if (!areAttachmentsOk(pageManager.getPage(Long.parseLong(value)))) {
						String pageTitle = pageManager.getPage(Long.parseLong(value)).getDisplayTitle();
						this.addActionError("copypagetree.error.missing.attachment", new Object[]{pageTitle} );
						return executeSetnames();
					}
					
					copypage.setToggle(true);
					copypage.setNewtitle(r.getParameter("title-"+value));
					if (LOG.isDebugEnabled()) {
						LOG.debug("COPY id=" + value+" newtitle="+copypage.getNewtitle());
					}
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("CANT FIND id=" + value);
					}
				}
				
			}
		}
		
		Space space = spaceManager.getSpace(r.getParameter("targetspace"));
		Page parentpage = pageManager.getPage(r.getParameter("targetspace"), r.getParameter("parenttitle"));
		List<Space> allspaces = getSpacesEditableByUser(AuthenticatedUserThreadLocal.getUser());

		if ( parentpage != null ) {
			if (allspaces.contains(space)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("SAVE under \""+parentpage.getTitle()+"\" in \""+space.getDisplayTitle()+"\"");
				}
				currCopy.storeCopyPages(space, pageManager, attachmentManager, labelManager, parentpage);			
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("NOT SAVED under \""+parentpage.getTitle()+"\" due to insufficient permissions in Space " + space.getKey());
				}
			}
		} else {
			if (allspaces.contains(space)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("SAVE under Space \""+space.getDisplayTitle()+"\"");
				}
				currCopy.storeCopyPages(space, pageManager, attachmentManager, labelManager, null);			
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("NOT SAVED due to insufficient permissions in Space " + space.getKey());
				}
			}
		}
		

		        
		return "success";
	}


	private CopyPage createCopyPage(Page org, int depth) {
		
		int position = 0;
		if (org.getPosition()!=null) position = org.getPosition();
		
		CopyPage copypage = new CopyPage(org.getId(), position, depth, org.getTitle());
		
		if (org.getChildren().size()>0) {
			for (int i=0; i<org.getChildren().size(); i++) {
				CopyPage childcopy = createCopyPage((Page)org.getChildren().get(i), depth+1);
				copypage.addChild(childcopy);
			}
		}
		return copypage;
	}
	
	public List<Space> getSpaces() {
		return spaces;
	}

	public Page getCurrPage() {
		return currPage;
	}
	
	public List<CopyPage> getDescendants() {
		return descendants;
	}
	
	/**
	* Implementation of PageAware
	*/
	public AbstractPage getPage() {
		return page;
	}

	/**
	* Implementation of PageAware
	*/
	public void setPage(AbstractPage page) {
		this.page = page;
	}
 
	/**
	* Implementation of PageAware:
	* Returning 'true' ensures that the
	* page is set before the action commences.
	*/
	public boolean isPageRequired() {
		return true;
	}
	 
	/**
	* Implementation of PageAware:
	* Returning 'true' ensures that the
	* current version of the page is used.
	*/
	public boolean isLatestVersionRequired() {
		return true;
	}
	 
	/**
	* Implementation of PageAware:
	* Returning 'true' ensures that the user
	* requires page view permissions.
	*/
	public boolean isViewPermissionRequired() {
		return true;
	}
	 
	/**
	* Dependency-injection of the Confluence LabelManager.
	*/
	public void setLabelManager(LabelManager labelManager) {
		this.labelManager = labelManager;
	}
	
	public void setPageManager(PageManager pageManager) {
		this.pageManager = pageManager;
	}

    public void setSpaceManager(SpaceManager spaceManager) {
        this.spaceManager = spaceManager;
    }
    
    public void setAttachmentManager(AttachmentManager attachmentManager) {
        this.attachmentManager = attachmentManager;
    }

    public String getStaticResourcePrefix() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("staticResourcePrefix: "+this.staticResourcePrefix);
		}
    	return this.staticResourcePrefix;
    }
    
    private List<Space> getSpacesEditableByUser(User user){
		// Szukamy przestrzeni w ktorych user moze tworzyc/edytowac strony.
		// Beda to potencjalne przestrzenie do ktorych user moze skopiowac page tree.
		SpacesQuery spacesQuery = SpacesQuery.newQuery()
				.forUser(user)
				.withPermission(SpacePermission.CREATEEDIT_PAGE_PERMISSION)
				.build();
		ListBuilder<Space> spacesBuilder = spaceManager.getSpaces(spacesQuery);
		
		// Wyciagamy cala liste naraz.
		// Teoretycznie nie powinnismy tego robic, ale chodzi o obiekty Space, ktorych jest stosunkowo malo.
		return spacesBuilder.getRange(0, spacesBuilder.getAvailableSize() - 1);
    	
    }
    
	private boolean areAttachmentsOk(Page pageToCheck) {
		
	
		//System.out.println("Attachment DataStore: "+settingsManager.getGlobalSettings().getAttachmentDataStore());
		//System.out.println("Attachment DataStore: "+getGlobalSettings().getAttachmentDataStore());

		String attachmentDir = AttachmentUtils.getConfluenceAttachmentDirectory();
		List<Attachment> attachments = pageToCheck.getLatestVersionsOfAttachments();
		
		if (attachments != null) {
			for (Attachment attachment : attachments) {
				
				// Tworzymy sciezke do zalacznika wg atlassianowego algorytmu.
				long attachmentId = attachment.getId();
				long attachmentContentId = attachment.getContent().getId();
				long attachmentContentIdLsd1 = attachmentContentId % 250;
				long attachmentContentIdLsd2 = ((attachmentContentId - (attachmentContentId %1000)) / 1000) % 250;
				String spaceSubdirs  = "";
			    if (attachment.getSpace() != null ) {
					long spaceId = attachment.getSpace().getId();
					long spaceIdLsd1 = spaceId % 250;
					long spaceIdLsd2 = ((spaceId - (spaceId %1000)) / 1000) % 250;
					spaceSubdirs = spaceIdLsd1 + "/" + spaceIdLsd2 + "/" + spaceId;
			    }
			    else {
			    	spaceSubdirs = "nonspaced";
			    }
				File expectedName = new File(attachmentDir + 
											 "/ver003/" + 
											 spaceSubdirs +
											 "/" + 
											 attachmentContentIdLsd1 + 
											 "/" + 
											 attachmentContentIdLsd2 + 
											 "/" + 
											 attachmentContentId + 
											 "/" + 
											 attachmentId + 
											 "/" + 
											 attachment.getVersion());
				
				//System.out.println("Copy Attachment. Check for "+expectedName.getAbsolutePath());
				if (!expectedName.exists()) 
				{
					if (LOG.isDebugEnabled()) {
						LOG.debug("Attachment " + expectedName.getAbsolutePath() + " does not exist.");
					}
					return false;
				}
			}
		}
		return true;
	}
	
}
