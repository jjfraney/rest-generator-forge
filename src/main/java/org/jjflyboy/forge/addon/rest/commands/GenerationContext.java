package org.jjflyboy.forge.addon.rest.commands;

import java.util.List;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.text.Inflector;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class GenerationContext {
	private Project project;
	private JavaClassSource rrClass;
	private String targetPackageName;
	private List<String> contentType;
	private String resourcePath;
	private String resourceName;

	private Inflector inflector;

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public JavaClassSource getRrClass() {
		return rrClass;
	}

	public void setRrClass(JavaClassSource rrClass) {
		this.rrClass = rrClass;
	}

	public String getTargetPackageName() {
		return targetPackageName;
	}

	public void setTargetPackageName(String targetPackageName) {
		this.targetPackageName = targetPackageName;
	}

	public List<String> getContentType() {
		return contentType;
	}

	public void setContentType(List<String> contentType) {
		this.contentType = contentType;
	}

	public Inflector getInflector() {
		return inflector;
	}

	public void setInflector(Inflector inflector) {
		this.inflector = inflector;
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

}