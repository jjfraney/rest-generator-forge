package org.jjflyboy.forge.addon.rest.commands;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.javaee.JavaEEPackageConstants;
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
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.PropertySource;

public class XNewConverters extends AbstractProjectCommand {

	@Inject
	@WithAttributes(label = "source", required = true)
	private UISelectOne<JavaResource> source;

	@Inject
	@WithAttributes(name = "converter", label = "create converter?", defaultValue = "true")
	private UIInput<Boolean> createConverter;

	@Inject
	@WithAttributes(type = InputType.JAVA_PACKAGE_PICKER, label = "target package", required = false)
	private UIInput<String> targetPackage;

	@Inject
	private ProjectOperations projectOperations;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(XNewConverters.class).name("xConverters: new").category(Categories.create("POJO"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		targetPackage.setValueConverter(new PackageRootConverter(getProjectFactory(), builder));
		setupPojo(source, builder.getUIContext());
		builder.add(source).add(createConverter).add(targetPackage);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		// if the target.named does not exist, create it
		UIContext uiContext = context.getUIContext();
		Project project = getSelectedProject(uiContext);

		JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		JavaClassSource entityClass = source.getValue().getJavaType();
		JavaClassSource rrc = createResourceRepresentationClass(entityClass, javaSourceFacet);

		JavaResource newClassSource;
		boolean classAlreadyExists;
		try {
			newClassSource = javaSourceFacet.getJavaResource(rrc);
			classAlreadyExists = newClassSource != null && newClassSource.exists();
		} catch (ResourceException ex) {
			classAlreadyExists = false;
		}

		if (!classAlreadyExists) {

			for (PropertySource<JavaClassSource> p : entityClass.getProperties()) {
				String type = stringifyType(p.getType(), project);
				rrc.addProperty(type, p.getName());
			}
			newClassSource = javaSourceFacet.saveJavaSource(rrc);

		}

		return Results.success(rrc.getQualifiedName() + " was " + (classAlreadyExists ? "preserved" : "created"));
	}

	private String stringifyType(Type<JavaClassSource> type, Project project) {
		StringBuilder b = new StringBuilder();
		if (type.isParameterized()) {
			b.append(type.getName()).append('<');
			String sep = "";
			for (Type<JavaClassSource> t : type.getTypeArguments()) {
				String tname = stringifyType(t, project);
				b.append(sep).append(tname);
			}
			b.append('>');
		} else if (type.getQualifiedName().startsWith(project.getFacet(JavaSourceFacet.class).getBasePackage())) {
			b.append(type.getName() + "Reference");
		} else {
			b.append(type.getName());
		}
		return b.toString();
	}

	/**
	 * create resource representation where package name is same as entity,
	 * where entity package is replaced with rest package, and RR is at tail of
	 * resource representation class name
	 *
	 * @param javaResource
	 * @param java
	 * @return
	 */
	private JavaClassSource createResourceRepresentationClass(JavaClassSource javaResource, JavaSourceFacet java) {
		String targetPackage = getTargetRestPackage(javaResource, java);

		JavaClassSource source = Roaster.create(JavaClassSource.class).setName(javaResource.getName() + "RR");
		source.setPackage(targetPackage);
		return source;
	}

	private String getTargetRestPackage(JavaClassSource javaResource, JavaSourceFacet java) {
		String tp = targetPackage.getValue();
		if (tp == null) {
			String entityPackage = java.getBasePackage() + "." + JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE;
			String restPackage = java.getBasePackage() + "." + JavaEEPackageConstants.DEFAULT_REST_PACKAGE;
			tp = javaResource.getPackage().replace(entityPackage, restPackage);
		}
		return tp;
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