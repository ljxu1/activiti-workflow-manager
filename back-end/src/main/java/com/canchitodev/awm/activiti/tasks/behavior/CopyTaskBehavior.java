/**
 * This content is released under the MIT License (MIT)
 *
 * Copyright (c) 2017, canchito-dev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * @author 		José Carlos Mendoza Prego
 * @copyright	Copyright (c) 2017, canchito-dev (http://www.canchito-dev.com)
 * @license		http://opensource.org/licenses/MIT	MIT License
 * @link		https://github.com/canchito-dev/activiti-workflow-manager
 **/
package com.canchitodev.awm.activiti.tasks.behavior;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.canchitodev.awm.domain.FileHandler;
import com.canchitodev.awm.service.FolderHandlerService;

@Service("copyTaskBehavior")
@Scope("prototype")
public class CopyTaskBehavior extends AbstractTaskActivityBehavior {

	private static final long serialVersionUID = -4740654158860004620L;
	
	@Autowired
	private FolderHandlerService folderHandlerService;
	
	public Expression sourceFileHandler;
	public Expression destinationFileHandler;
	public Expression destinationFolderHandler;
	public Expression filenamePattern;

	@Override
	public void execute(DelegateExecution execution) {
		try {
			// Validate task's parameters
			this.validateParameters(execution);
			
			// Get the source file handler
			FileHandler sfh = (FileHandler) execution.getVariable((String) this.sourceFileHandler.getValue(execution));
			
			// Set destination file handler
			FileHandler dfh = new FileHandler(
				(String) this.destinationFileHandler.getValue(execution),
				(this.filenamePattern == null) ? sfh.getFilename() : (String) this.filenamePattern.getValue(execution),
				this.folderHandlerService.findByNameAndTenantId((String) this.destinationFolderHandler.getValue(execution), Long.parseLong(execution.getTenantId()))
			);
			
			// Add the destination file handler as Activiti's variable
			execution.setVariable(dfh.getName(), dfh);
			
			// Create generic task entity that will be used in the runnable
			this.submitTask(
					execution, 
					this.setDetails(sfh.getName(), dfh.getName()), 
					"copyTaskRunnable"
			);
		} catch (Exception e) {
			this.throwException(execution, 
					"There was a problem when trying to execute task 'copyTaskRunnable'"
			);
		}
	}
	
	private JSONObject setDetails(String sourceFileHandler, String destinationFileHandler) throws JSONException {
		JSONObject details = new JSONObject();
		details.put("sourceFileHandler", sourceFileHandler);
		details.put("destinationFileHandler", destinationFileHandler);
		return details;
	}

	@Override
	protected void validateParameters(DelegateExecution execution) throws IllegalArgumentException {
		if(this.sourceFileHandler == null)
			throw new IllegalArgumentException("Invalid or missing source file handler");
		else {
			if(!execution.hasVariable((String) this.sourceFileHandler.getValue(execution)))
				throw new IllegalArgumentException("Source file handler not found");
		}
		
		if(this.destinationFileHandler == null)
			throw new IllegalArgumentException("Invalid or missing destination file handler");
		
		if(this.destinationFolderHandler == null)
			throw new IllegalArgumentException("Invalid or missing destination folder handler");
		else {
			if(this.folderHandlerService.findByNameAndTenantId((String) this.destinationFolderHandler.getValue(execution), Long.parseLong(execution.getTenantId())) == null)
				throw new IllegalArgumentException("Destination folder handler does not exists");
		}
	}
}