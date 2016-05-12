package org.jjflyboy.forge.addon.rest.commands;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

public interface ResourceClassGenerator
{
	/**
	 * A readable description for this strategy
	 */
	String getName();

	/**
	 * A human-readable description for this strategy
	 */
	String getDescription();

	/**
	 * Generate a REST resource (singular) class based on a context
	 */
	JavaClassSource generateResourceClassFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a REST resources (plural) class based on a context
	 */
	default JavaClassSource generateResourcesClassFrom(GenerationContext context) throws Exception {
		throw new RuntimeException("this is a future");
	}

	/**
	 * Generate a REST resource (singular) class interface based on a context
	 */
	JavaInterfaceSource generateResourceClassInterfaceFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a REST resources (plural) class interface based on a context
	 */
	JavaInterfaceSource generateResourcesClassInterfaceFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a resource controller implementation from a context
	 */
	JavaClassSource generateResourceControllerFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a resource controller interface from a context.
	 */
	JavaInterfaceSource generateResourceControllerInterfaceFrom(GenerationContext context) throws Exception;

}
