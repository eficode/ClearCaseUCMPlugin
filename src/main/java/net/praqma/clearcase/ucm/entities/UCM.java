package net.praqma.clearcase.ucm.entities;

import net.praqma.clearcase.ucm.persistence.UCMContext;
import net.praqma.clearcase.ucm.persistence.UCMStrategyXML;

public abstract class UCM
{
	/* Make sure, that we're using the same instance of the context! */
	protected static UCMContext context = new UCMContext( new UCMStrategyXML() );
}