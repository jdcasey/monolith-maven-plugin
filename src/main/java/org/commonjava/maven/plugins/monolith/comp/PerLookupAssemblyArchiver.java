package org.commonjava.maven.plugins.monolith.comp;

import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.archive.DefaultAssemblyArchiver;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = AssemblyArchiver.class, hint = PerLookupAssemblyArchiver.ID, instantiationStrategy = "per-lookup" )
public class PerLookupAssemblyArchiver
    extends DefaultAssemblyArchiver
{

    public static final String ID = "perLookup";
}
