package org.jjflyboy.forge.addon.rest.commands;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;

public class DecoupledResourceClassGenerator implements ResourceClassGenerator {
	@Inject
	TemplateFactory templateFactory;

	@Inject
	ResourceFactory resourceFactory;

	@Override
	public String getName() {
		return "RR_CLASS";
	}

	@Override
	public String getDescription() {
		return "Expose Resource Representation classes in Resource classes";
	}

	private <T extends JavaSource<T>> T applyTemplateWithContext(Class<T> c, String template, GenerationContext context)
			throws IOException {
		Map<Object, Object> map = populateTemplateContext(context);
		Resource<URL> templateResource = resourceFactory.create(getClass().getResource(template));
		Template processor = templateFactory.create(templateResource, FreemarkerTemplate.class);
		String output = processor.process(map);
		return Roaster.parse(c, output);
	}


	private Map<Object, Object> populateTemplateContext(GenerationContext context) {
		Map<Object, Object> map = new HashMap<>();
		map.put("resourceName", capitalize(context.getResourceName()));
		map.put("resourceRepresentation", context.getRrClass());
		map.put("contentTypes", context.getContentTypes());
		map.put("resourcePath", context.getResourcePath());
		map.put("targetPackage", context.getTargetPackageName());

		map.put("keyPropertyName",
				context.getKeyProperty() == null ? "BusinessKey" : capitalize(context.getKeyProperty().getName()));
		map.put("keyPropertyType",
				context.getKeyProperty() == null ? "String" : context.getKeyProperty().getType().getName());
		map.put("keyProperty", context.getKeyProperty());
		map.put("keyName", context.getKeyName());
		return map;
	}

	private String capitalize(String name) {
		return new StringBuilder().append(Character.toUpperCase(name.charAt(0))).append(name.substring(1)).toString();
	}

	@Override
	public JavaClassSource generateResourceClassFrom(GenerationContext context) throws Exception {
		return applyTemplateWithContext(JavaClassSource.class, "ResourceClass.jv", context);
	}

	@Override
	public JavaInterfaceSource generateResourceClassInterfaceFrom(GenerationContext context) throws Exception {
		return applyTemplateWithContext(JavaInterfaceSource.class, "ResourceClassInterface.jv", context);
	}

	@Override
	public JavaClassSource generateResourceControllerFrom(GenerationContext context) throws Exception {
		return applyTemplateWithContext(JavaClassSource.class, "ResourceControllerClass.jv", context);
	}

	@Override
	public JavaInterfaceSource generateResourceControllerInterfaceFrom(GenerationContext context) throws Exception {
		return applyTemplateWithContext(JavaInterfaceSource.class, "ResourceControllerInterface.jv", context);
	}

	@Override
	public JavaInterfaceSource generateResourcesClassInterfaceFrom(GenerationContext context) throws Exception {
		return applyTemplateWithContext(JavaInterfaceSource.class, "ResourcesClassInterface.jv", context);
	}

}
