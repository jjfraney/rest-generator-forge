package org.jjflyboy.forge.addon.rest.commands;

import java.util.List;
import java.util.Set;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.text.Inflector;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.PropertySource;

public class GenerationContext {
	private Project project;
	private JavaClassSource rrClass;
	private String keyName;
	private PropertySource<JavaClassSource> keyProperty;
	private String outputPackageName;
	private List<String> contentTypes;
	private String resourcePath;
	private String resourceName;
	private Set<RestMethod> methods;
	private String domainClassName;

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

	public String getOutputPackageName() {
		return outputPackageName;
	}

	public void setOutputPackageName(String outputPackageName) {
		this.outputPackageName = outputPackageName;
	}

	public List<String> getContentTypes() {
		return contentTypes;
	}

	public void setContentTypes(List<String> contentType) {
		this.contentTypes = contentType;
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

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public PropertySource<JavaClassSource> getKeyProperty() {
		return keyProperty;
	}

	public void setKeyProperty(PropertySource<JavaClassSource> keyProperty) {
		this.keyProperty = keyProperty;
	}

	public Set<RestMethod> getMethods() {
		return methods;
	}

	public void setMethods(Set<RestMethod> methods) {
		this.methods = methods;
	}

	public String getDomainClassName() {
		return domainClassName;
	}

	public void setDomainClassName(String domainClassName) {
		this.domainClassName = domainClassName;
	}

}
