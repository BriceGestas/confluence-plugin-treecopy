package com.imaginarymachines.confluence.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.Space;
import com.opensymphony.webwork.dispatcher.client.ClientException;

public class CopyPage implements Comparable {

	private int position;
	private String title;
	private String newtitle;
	private boolean toggle;
	private int depth;
	private long id;
	private long newid;
	private CopyPage parent;
	
	private ArrayList children = new ArrayList();
	
	public CopyPage(long id, int position, int depth, String title) {
		this.id=id;
		this.position = position;
		this.depth = depth;
		this.title = title;
	}
	
	public void addChild(CopyPage childcopy) {
		childcopy.setParent(this);
		this.children.add(childcopy);
	}
	
	public void readChildHierarchy(ArrayList<CopyPage> level) {		
		level.add(this);
		//System.out.println("read: "+this.title+" "+this.depth);
		List childs = this.getChildren();
		for (int i=0; i<childs.size(); i++) {
			CopyPage child = (CopyPage)childs.get(i);
			child.readChildHierarchy(level);
		}
	}

	private List getChildren() {
		Collections.sort(this.children);
		return this.children;
	}

	public CopyPage getCopyPageById(long id) {
		
		if (this.id==id) {
			return this;
		} else {
			List childs = this.getChildren();
			for (int i=0; i<childs.size(); i++) {
				CopyPage child = (CopyPage)childs.get(i);
				CopyPage match = child.getCopyPageById(id);
				if (match!=null) return match;
			}
		}
		
		return null;
	}
	
	public void storeCopyPages(Space space, PageManager pageManager, AttachmentManager attachmentManager, LabelManager labelManager, Page defaultParentPage) {
		
		if (this.toggle) this.storeCopyPage(space, pageManager, attachmentManager, labelManager, defaultParentPage);
		
		List childs = this.getChildren();
		for (int i=0; i<childs.size(); i++) {
			CopyPage child = (CopyPage)childs.get(i);
			child.storeCopyPages(space, pageManager, attachmentManager, labelManager, defaultParentPage);
		}
		
	}
	
	private void storeCopyPage(Space space, PageManager pageManager, AttachmentManager attachmentManager, LabelManager labelManager, Page defaultParentPage) {
		
		Page oldPage = pageManager.getPage(this.id);
		
		Page parentPage = defaultParentPage;
		
		if (this.getParent()!=null) {
			parentPage = pageManager.getPage(this.getParent().getNewid());
		}
		//System.out.println("parentPage "+parentPage.getTitle());
		
		final Page newPage = new Page();
        newPage.setTitle(this.getNewtitle());
        newPage.setSpace(space);
        //newPage.setContent(oldPage.getContent());
        newPage.setBodyAsString(oldPage.getBodyAsString());
        newPage.setPosition(oldPage.getPosition());
        pageManager.saveContentEntity(newPage, null);
        if (parentPage!=null) {
        	parentPage.addChild(newPage);
        } else {
        	System.out.println("no parentPage!");
        }
        this.setNewid(newPage.getId());
        
        List<Attachment> oldAttachments = oldPage.getAttachments();
        for (Attachment oldAttachment : oldAttachments) {
        	try {
        		System.out.println("oldAttachment="+oldAttachment.getFileName());
        		Attachment newAttachment = new Attachment();
        		newAttachment.setContentType(oldAttachment.getContentType());
        		newAttachment.setFileName(oldAttachment.getFileName());
        		newAttachment.setComment(oldAttachment.getComment());
        		newAttachment.setFileSize(oldAttachment.getFileSize());
	            newPage.addAttachment(newAttachment);
        		attachmentManager.saveAttachment(newAttachment, null, oldAttachment.getContentsAsStream());
        		System.out.println("newAttachment="+newAttachment.getFileName());
            } catch (Exception exception) {
            	exception.printStackTrace();
            }
            
        }
        
        List<Label> oldLabels = oldPage.getLabels();
        for (Label oldLabel : oldLabels) {
        	labelManager.addLabel(newPage, oldLabel);
        }
        
        
        
	}
	
	public int getPosition() {
		return position;
	}

	public String getTitle() {
		return title;
	}

	public int getDepth() {
		return depth;
	}

	public long getId() {
		return id;
	}
	
	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(boolean toggle) {
		this.toggle = toggle;
	}

	public String getNewtitle() {
		return newtitle;
	}

	public void setNewtitle(String newtitle) {
		this.newtitle = newtitle;
	}

	
	public long getNewid() {
		return newid;
	}

	public void setNewid(long newid) {
		this.newid = newid;
	}

	public CopyPage getParent() {
		return parent;
	}

	public void setParent(CopyPage parent) {
		this.parent = parent;
	}

	public int compareTo(Object arg0) {
		
		//a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
		
		CopyPage copyPage = (CopyPage)arg0;
		
		if (this.position<copyPage.position) return -1;
		if (this.position==copyPage.position) return 0;
		if (this.position>copyPage.position) return 1;
		return 0;
	}
	
	
	
}
