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
	 * Generate a REST resource class based on a context
	 */
	JavaClassSource generateResourceClassFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a REST resource class interface based on a context
	 */
	JavaInterfaceSource generateResourceClassInterfaceFrom(GenerationContext context) throws Exception;

	/**
	 * Generate a REST resource class based on a context
	 */
	JavaClassSource generateResourceControllerFrom(GenerationContext context) throws Exception;

}
