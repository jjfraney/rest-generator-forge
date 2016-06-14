/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jjflyboy.forge.addon.rest.commands;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.util.Lists;
import org.jboss.forge.roaster.model.Annotation;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.ValuePair;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.PropertySource;

@FacetConstraint(JavaSourceFacet.class)
@StackConstraint(RestFacet.class)
public class RestResourceClassFromPojoCommand extends AbstractProjectCommand implements PrerequisiteCommandsProvider
{
	@Inject
	@WithAttributes(label = "Content Type", defaultValue = MediaType.APPLICATION_JSON, required = false)
	private UIInputMany<String> contentTypes;

	@Inject
	@WithAttributes(label = "targets", required = true, type = InputType.JAVA_CLASS_PICKER)
	private UISelectMany<String> targets;
	// due to forge bug, cannot use: private UISelectMany<JavaResource> targets;
	// using this map to compensate for the forge bug above
	private Map<String, JavaResource> targetMap = new HashMap<>();

	@Inject
	@WithAttributes(label = "Target Package Name", required = false, type = InputType.JAVA_PACKAGE_PICKER)
	private UIInput<String> packageName;

	@Inject
	@WithAttributes(label = "Property name of targets' id property", required = false)
	private UIInput<String> idPropertyName;

	@Inject
	@WithAttributes(label = "Methods to generate")
	private UISelectMany<RestMethod> methods;

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

	@Override
	public void initializeUI(UIBuilder builder) throws Exception
	{
		UIContext context = builder.getUIContext();
		Project project = getSelectedProject(context);

		contentTypes.setCompleter(
				(uiContext, input, value) -> Arrays.asList(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON));
		setupTargetsSelector(context);

		methods.setDefaultValue(
				Arrays.asList(new RestMethod[] { RestMethod.GET, RestMethod.PUT, RestMethod.POST, RestMethod.DELETE }));

		builder.add(targets)
		.add(contentTypes)
		.add(packageName)
		.add(idPropertyName)
		.add(methods);
	}

	private void setupTargetsSelector(UIContext context) {

		Project project = getSelectedProject(context);
		for(JavaResource jr: projectOperations.getProjectClasses(project)) {
			if (jr.getName().endsWith("RR.java")) {
				try {
					targetMap.put(jr.getJavaType().getQualifiedName(), jr);
				} catch (FileNotFoundException e) {
					// this is ok, because we are using files found by
					// projectOperations
				}
			}
		}
		targets.setValueChoices(targetMap.keySet());
	}

	@Override
	public void validate(UIValidationContext validator) {
		super.validate(validator);
		for (String targetRRClassName : targets.getValue()) {
			JavaResource t = targetMap.get(targetRRClassName);

			// these ought to be classes (by how we created the filter list)
			JavaClassSource c;
			try {
				c = ((JavaClassSource) t.getJavaType());
			} catch (FileNotFoundException e) {
				validator.addValidationError(targets, "Java source file not found. " + t.getFullyQualifiedName());
				continue;
			}

			String idName = idPropertyName.getValue();
			if(idName != null) {
				if(c.getProperty(idName) == null) {
					String message = new StringBuilder()
							.append("id property does not exist in the target class.")
							.append("  id property name=").append(idName)
							.append(", target RR class=").append(c.getName())
							.toString();
					validator.addValidationError(idPropertyName, message);
				}
			} else {
				boolean keyFound = inferKeyName(getResourceNameFrom(c), buildPropertyMap(c)) != null;
				if(! keyFound) {
					String message = new StringBuilder()
							.append("target class does not have guessable id property.")
							.append(" target class=").append(c.getName())
							.append(" suggestions=").append(Arrays.asList(keyPossibilities(getResourceNameFrom(c))))
							.toString();
					validator.addValidationWarning(targets, message);
				}

				if (!keyFound) {
					Set<RestMethod> m = new HashSet<>();
					m.addAll(Lists.toList(methods.getValue()));
					if (m.contains(RestMethod.POST) || m.contains(RestMethod.DELETE) || m.contains(RestMethod.PUT)) {
						String message = new StringBuilder()
								.append("These methods cannot be generated without an id property. ")
								.append(" methods=")
								.append(m)
								.toString();
						validator.addValidationError(methods, message);
					}
					if (m.contains(RestMethod.GET)) {
						String message = "GET by id cannot be generated without an id property.";
						validator.addValidationWarning(methods, message);
					}
				}
			}
		}
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
		ResourceClassGenerator selectedGenerator = defaultResourceGenerator;
		Set<JavaSource<? extends JavaSource<?>>> classes = new HashSet<>();
		for (String rrClassName : targets.getValue())
		{
			JavaResource target = targetMap.get(rrClassName);

			context.setRrClass(target.getJavaType());
			String resourceName = getResourceNameFrom(target.getJavaType());

			Map<String, PropertySource<JavaClassSource>> map = buildPropertyMap(target.getJavaType());
			String keyName = inferKeyName(resourceName, map);

			context.setKeyName(keyName);
			context.setKeyProperty(keyName == null ? null : map.get(keyName));

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

	private String getResourceNameFrom(JavaType<?> javaType) {
		return javaType.getName().replaceAll("RR$", "");
	}

	private String[] keyPossibilities(String resourceName) {
		String resourceId = new StringBuilder().append(Character.toLowerCase(resourceName.charAt(0)))
				.append(resourceName.substring(1)).append("Id").toString();

		return new String[] { "businessKey", "naturalId", "guid", "businessId", resourceId, "id" };
	}

	private String inferKeyName(String resourceName, Map<String, PropertySource<JavaClassSource>> propertyMap) {
		String name = null;

		// the first that matches is inferred to be the type's business key.

		for (String keyCandidate : keyPossibilities(resourceName)) {
			PropertySource<JavaClassSource> p = propertyMap.get(keyCandidate);
			if (p != null) {
				if (!isJsonIgnored(p)) {
					name = keyCandidate;
					break;
				}
			}
		}
		return name;
	}

	private Map<String, PropertySource<JavaClassSource>> buildPropertyMap(JavaType<?> javaType) {

		Map<String, PropertySource<JavaClassSource>> map = new HashMap<>();
		if (javaType instanceof JavaClassSource) {
			JavaClassSource c = (JavaClassSource) javaType;

			for (PropertySource<JavaClassSource> p : c.getProperties()) {
				if (p.isMutable() && p.isAccessible()) {
					if (!isJsonIgnored(p)) {
						map.put(jsonPropertyName(p), p);
					}
				}
			}
		}
		return map;
	}

	private boolean isJsonIgnored(PropertySource<JavaClassSource> match) {
		Annotation<JavaClassSource> jsonValue = match.getAnnotation("com.fasterxml.jackson.annotation.JsonIgnore");
		boolean isIgnored = jsonValue != null;
		return isIgnored;
	}

	private String jsonPropertyName(PropertySource<JavaClassSource> p) {
		String result = p.getName();
		Annotation<JavaClassSource> a = p.getAnnotation("com.fasterxml.jackson.annotation.JsonProperty");
		if (a != null) {
			for (ValuePair pair : a.getValues()) {
				if (pair.getName().equals("value")) {
					result = pair.getStringValue();
					break;
				}
			}
		}
		return result;

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

		Set<RestMethod> m = new HashSet<>();
		m.addAll(Lists.toList(methods.getValue()));
		generationContext.setMethods(m);

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
