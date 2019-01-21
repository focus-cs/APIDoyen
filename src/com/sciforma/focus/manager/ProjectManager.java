/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sciforma.focus.manager;

/**
 *
 * @author lahoudie
 */
import com.sciforma.focus.exeception.TechnicalException;
import com.sciforma.focus.filter.Filter;
import com.sciforma.psnext.api.Project;


public interface ProjectManager {

	public abstract class ProjectFilter implements Filter<Project> {
		public boolean onlyActived() {
			return true;
		}
	}

	Project findProjectById(String ID) throws TechnicalException;

}