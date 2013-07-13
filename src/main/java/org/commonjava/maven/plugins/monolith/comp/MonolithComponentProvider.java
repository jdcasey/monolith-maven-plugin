package org.commonjava.maven.plugins.monolith.comp;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.filtering.MavenFileFilter;

public interface MonolithComponentProvider
{

    MavenSession getSession();

    MavenFileFilter getMavenFileFilter();

}
