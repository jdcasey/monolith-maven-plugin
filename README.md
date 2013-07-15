# Monolith Plugin for Apache Maven

This plugin reads the entries in your POM's `dependencyManagement` and `pluginManagement` sections, and generates monolithic, self-contained assemblies for each. That is, self-contained EXCEPT when they reference other monoliths declared in the current project. In the case of plugins, these assemblies are usable as drop-in replacements for the original plugin...just adjust the version to the new, qualified version, and away you go.

## Why??

In cleanroom environments, it's often desirable to avoid mixing built-time / toolchain libraries with the libraries you're building. When you're doing Maven builds, it's not enough to simply keep track of the plugins you're using. Each plugin has its own dependency graph, and if that graph overlaps with something you're trying to build and ship, it can cause a chicken-and-egg scenario that makes it hard to guarantee that what you're shipping is actually the code that you built, and not something you imported for the sake of supporting a plugin.

This is where monoliths come in handy. By creating self-contained plugins (and other libraries used during a Maven build), you avoid questions of dependency-graph overlap.

## How To Use

Imagine you want to create a self-contained version of the Maven Archetype Plugin. However, since you want to manage the archetypes you're planning on shipping, you actually need a self-contained version of the `archetype-packaging` artifact. It's possible this artifact is already self-contained...for the purposes of this example, assume it's not.

Simply create a new Maven project, and in the pom.xml add this:

    <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>org.apache.maven.archetype</groupId>
          <artifactId>archetype-packaging</artifactId>
          <version>2.2</version>
        </dependency>
      </dependencies>
    </dependencyManagement>

    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <artifactId>maven-archetype-plugin</artifactId>
            <version>2.2</version>
          </plugin>
        </plugins>
      </pluginManagement>
    </build>

Then, issue the following:

    mvn org.commonjava.maven.plugins:monolith-maven-plugin:create

When Maven finishes, the basic output (i.e. monolithic artifacts + poms) will be in the `target/monoliths` directory. Additionally, the plugin generates a partial local repository (by `install`ing these artifacts and POMs) in `target/installed`. It also generates a partial remote repository (by `deploy`'ing these artifacts and POMs) in `target/deployed`.

## Making That Last Call Simpler

If you want to simplify the Maven command line you must run, try adding the following to your `settings.xml`:

    <pluginGroups>
      <pluginGroup>org.commonjava.maven.plugins</pluginGroup>
    </pluginGroups>

Then, the command you must issue becomes:

    mvn monolith:create

