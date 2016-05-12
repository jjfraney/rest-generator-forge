/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jjflyboy.forge.addon.rest.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.rest.RestFacet;
import org.jboss.forge.addon.javaee.rest.ui.RestSetupWizard;
import org.jboss.forge.addon.parser.java.beans.ProjectOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.stacks.annotations.StackConstraint;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.text.Inflector;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.util.Lists;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;

@FacetConstraint(JavaSourceFacet.class)
@StackConstraint(RestFacet.class)
public class RestResourceClassFromPojoCommand extends AbstractProjectCommand implements PrerequisiteCommandsProvider
{
	@Inject
	@WithAttributes(label = "Content Type", defaultValue = MediaType.APPLICATION_JSON, required = true)
	private UIInputMany<String> contentTypes;

	@Inject
	@WithAttributes(label = "Targets", required = true)
	private UISelectMany<JavaResource> targets;

	@Inject
	@WithAttributes(label = "Target Package Name", required = true, type = InputType.JAVA_PACKAGE_PICKER)
	private UIInput<String> packageName;

	@Inject
	@WithAttributes(label = "Overwrite existing classes?", enabled = false, defaultValue = "false")
	private UIInput<Boolean> overwrite;

	@Inject
	@WithAttributes(label = "Generator", required = true)
	private UISelectOne<ResourceClassGenerator> generator;

	@Inject
	private DecoupledResourceClassGenerator defaultResourceGenerator;

	@Inject
	private Inflector inflector;

	@Inject
	private ProjectOperations projectOperations;

	@Override
	public UICommandMetadata getMetadata(UIContext context)
	{
		return Metadata.from(super.getMetadata(context), getClass())
				.name("REST: Generate Endpoints From Resource Representations")
				.description("Generate REST endpoints from Resource Representations")
				.category(Categories.create(super.getMetadata(context).getCategory(), "JAX-RS"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initializeUI(UIBuilder builder) throws Exception
	{
		UIContext context = builder.getUIContext();
		Project project = getSelectedProject(context);
		// List<JavaClassSource> supportedEntities = new ArrayList<>();
		// targets.setValueChoices(supportedEntities);
		// targets.setItemLabelConverter((source) -> source.getQualifiedName());

		packageName.setDefaultValue(project.getFacet(JavaSourceFacet.class).getBasePackage() + ".rest");

		contentTypes.setCompleter(
				(uiContext, input, value) -> Arrays.asList(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON));
		generator.setDefaultValue(defaultResourceGenerator);
		if (context.getProvider().isGUI())
		{
			generator.setItemLabelConverter((source) -> source.getDescription());
		}
		else
		{
			generator.setItemLabelConverter((source) -> source.getName());
		}
		setupTargetsSelector(context);

		builder.add(targets)
		.add(generator)
		.add(contentTypes)
		.add(packageName)
		.add(overwrite);
	}

	private void setupTargetsSelector(UIContext context) {

		Project project = getSelectedProject(context);
		List<JavaResource> resourceRepresentationClasses = new ArrayList<>();
		for(JavaResource jr: projectOperations.getProjectClasses(project)) {
			if (jr.getName().endsWith("RR.java")) {
				resourceRepresentationClasses.add(jr);
			}
		}
		targets.setValueChoices(resourceRepresentationClasses);
	}

	@Override
	public Result execute(final UIExecutionContext context) throws Exception
	{
		UIContext uiContext = context.getUIContext();
		GenerationContext generationContext = createContextFor(uiContext);
		Set<JavaSource<? extends JavaSource<?>>> sources = generateResourceClasses(generationContext);
		Project project = generationContext.getProject();
		JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		List<JavaResource> selection = new ArrayList<>();

		for (JavaSource<? extends JavaSource<?>> source : sources)
		{
			selection.add(javaSourceFacet.saveJavaSource(source));
		}
		uiContext.setSelection(selection);
		return Results.success("Resource classes created");
	}

	private Set<JavaSource<? extends JavaSource<?>>> generateResourceClasses(GenerationContext context)
			throws Exception
	{
		ResourceClassGenerator selectedGenerator = generator.getValue();
		Set<JavaSource<? extends JavaSource<?>>> classes = new HashSet<>();
		for (JavaResource target : targets.getValue())
		{
			context.setRrClass(target.getJavaType());

			String resourceName = target.getJavaType().getName().replaceAll("RR$", "");
			context.setResourceName(resourceName);

			String resourcePath = hyphenate(inflector.pluralize(context.getResourceName()));
			context.setResourcePath(resourcePath);

			JavaInterfaceSource resourceClassInterface = selectedGenerator.generateResourceClassInterfaceFrom(context);
			classes.add(resourceClassInterface);

			JavaInterfaceSource resourcesClassInterface = selectedGenerator
					.generateResourcesClassInterfaceFrom(context);
			classes.add(resourcesClassInterface);

			JavaClassSource resourceClass = selectedGenerator.generateResourceClassFrom(context);
			classes.add(resourceClass);

			JavaInterfaceSource controllerInterface = selectedGenerator
					.generateResourceControllerInterfaceFrom(context);
			classes.add(controllerInterface);

			JavaClassSource controllerClass = selectedGenerator.generateResourceControllerFrom(context);
			classes.add(controllerClass);
		}
		return classes;
	}

	private GenerationContext createContextFor(final UIContext context)
	{
		GenerationContext generationContext = new GenerationContext();
		generationContext.setProject(getSelectedProject(context));

		List<String> ct = Lists.toList(contentTypes.getValue())
				.stream()
				.map((s) -> {
					// double quotes if not present
					return s.startsWith("\"") ? s : "\"" + s + "\"";
				})
				.collect(Collectors.toList());

		generationContext.setContentTypes(ct);
		generationContext.setTargetPackageName(packageName.getValue());
		generationContext.setInflector(inflector);
		return generationContext;
	}

	@Override
	public NavigationResult getPrerequisiteCommands(UIContext context)
	{
		NavigationResultBuilder builder = NavigationResultBuilder.create();
		Project project = getSelectedProject(context);
		if (project != null)
		{
			if (!project.hasFacet(RestFacet.class))
			{
				builder.add(RestSetupWizard.class);
			}
		}
		return builder.build();
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

	/**
	 * convert a camelcase name to a hyphenated compound: camelCaseWord ->
	 * camel-case-word
	 *
	 * @param camelCaseWord
	 * @return
	 */
	private String hyphenate(String camelCaseWord) {
		if (camelCaseWord == null)
			return null;
		String result = camelCaseWord.trim();
		if (result.length() == 0)
			return "";
		result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2");
		result = result.replaceAll("([a-z\\d])([A-Z])", "$1-$2");
		return result.toLowerCase();
	}

}
