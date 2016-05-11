package org.jjflyboy.forge.addon.rest.commands;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.parser.java.beans.ProjectOperations;
import org.jboss.forge.addon.parser.java.converters.PackageRootConverter;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceException;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.PropertySource;

public class XNewConverters extends AbstractProjectCommand {

	@Inject
	@WithAttributes(label = "sourcePojo", required = true)
	private UISelectOne<JavaResource> sourcePojo;

	@Inject
	@WithAttributes(label = "named", required = true)
	private UIInput<String> named;

	@Inject
	@WithAttributes(type = InputType.JAVA_PACKAGE_PICKER, label = "target package", required = false)
	private UIInput<String> targetPackage;

	@Inject
	@WithAttributes(name = "converter", label = "create converter?", defaultValue = "true")
	private UIInput<Boolean> createConverter;

	@Inject
	private ProjectOperations projectOperations;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(XNewConverters.class).name("xConverters: new").category(Categories.create("POJO"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		targetPackage.getFacet(HintsFacet.class).setInputType(InputType.JAVA_PACKAGE_PICKER);
		targetPackage.setValueConverter(new PackageRootConverter(getProjectFactory(), builder));
		setupPojo(sourcePojo, builder.getUIContext());
		builder.add(sourcePojo).add(named).add(targetPackage).add(createConverter);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		// if the target.named does not exist, create it
		UIContext uiContext = context.getUIContext();
		Project project = getSelectedProject(uiContext);
		JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		JavaClassSource source = buildJavaSource(javaSourceFacet);

		JavaResource newClassSource;
		boolean classAlreadyExists;
		try {
			newClassSource = javaSourceFacet.getJavaResource(source);
			classAlreadyExists = newClassSource != null && newClassSource.exists();
		} catch (ResourceException ex) {
			classAlreadyExists = false;
		}

		if (!classAlreadyExists) {

			JavaClassSource targetClass = sourcePojo.getValue().getJavaType();
			for (PropertySource<JavaClassSource> p : targetClass.getProperties()) {
				source.addProperty(p.getType().getName(), p.getName());
			}
			newClassSource = javaSourceFacet.saveJavaSource(source);

		}

		return Results.success(source.getQualifiedName() + " was " + (classAlreadyExists ? "preserved" : "created"));

	}

	private JavaClassSource buildJavaSource(JavaSourceFacet java) {
		if (!named.hasValue() && !named.hasDefaultValue()) {
			return null;
		}

		JavaClassSource source = Roaster.create(JavaClassSource.class).setName(named.getValue());

		if (targetPackage.hasValue() || targetPackage.hasDefaultValue()) {
			source.setPackage(targetPackage.getValue());
		} else {
			source.setPackage(java.getBasePackage());
		}
		return source;
	}

	@Override
	public void validate(UIValidationContext validator) {
		// TODO: implement or remove (placeholding)
		super.validate(validator);
	}

	private void setupPojo(UISelectOne<JavaResource> pojo, UIContext context) {

		UISelection<FileResource<?>> selection = context.getInitialSelection();
		Project project = getSelectedProject(context);
		final List<JavaResource> entities = projectOperations.getProjectClasses(project);
		pojo.setValueChoices(entities);
		int idx = -1;
		if (!selection.isEmpty()) {
			idx = entities.indexOf(selection.get());
		}
		if (idx != -1) {
			pojo.setDefaultValue(entities.get(idx));
		}
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	@Inject
	private ProjectFactory projectFactory;

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}

}