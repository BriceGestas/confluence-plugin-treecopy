package com.imaginarymachines.confluence.plugins;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.actions.PageAware;
import com.atlassian.confluence.spaces.SpaceManager;
import com.opensymphony.webwork.ServletActionContext;
 
/**
* This Confluence action allows to copy a page including 
*/
public class TreecopyAction extends ConfluenceActionSupport implements PageAware {

	private SpaceManager spaceManager;
	private PageManager pageManager;
	private AttachmentManager attachmentManager;
	private LabelManager labelManager;
	private AbstractPage page;
	private Page currPage;
	private ArrayList<CopyPage> descendants;
	
	public String executeSetnames() {
		
		currPage = (Page) this.getWebInterfaceContext().getPage();
		
		//System.out.println("CURRENT PAGE: "+currPage.getTitle());
		CopyPage currCopy = createCopyPage(currPage,0);
		
		descendants = new ArrayList<CopyPage>();
		currCopy.readChildHierarchy(descendants);
		
		for (int i=0; i<descendants.size(); i++) {
			CopyPage cp = descendants.get(i);
		}
		
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
					copypage.setToggle(true);
					copypage.setNewtitle(r.getParameter("title-"+value));
					System.out.println("COPY id=" + value+" newtitle="+copypage.getNewtitle());
				} else {
					System.out.println("CANT FIND id=" + value);
				}
				
			}
		}
		
		currCopy.storeCopyPages(currPage.getSpace(), pageManager, attachmentManager, labelManager, currPage.getParent());
		        
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
	
	
	public Page getCurrPage() {
		return currPage;
	}
	
	public ArrayList<CopyPage> getDescendants() {
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
}