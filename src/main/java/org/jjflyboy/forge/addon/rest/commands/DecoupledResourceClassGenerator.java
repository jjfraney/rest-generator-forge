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
		T resource = Roaster.parse(c, output);
		resource.setPackage(context.getTargetPackageName());
		if (!context.getRrClass().getPackage().equals(context.getTargetPackageName())) {
			resource.addImport(context.getRrClass());
		}
		return resource;
	}


	private Map<Object, Object> populateTemplateContext(GenerationContext context) {
		Map<Object, Object> map = new HashMap<>();
		map.put("resourceName", context.getResourceName());
		map.put("resourceRepresentation", context.getRrClass());
		map.put("contentType", context.getContentType());
		map.put("resourcePath", context.getResourcePath());
		return map;
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

}
