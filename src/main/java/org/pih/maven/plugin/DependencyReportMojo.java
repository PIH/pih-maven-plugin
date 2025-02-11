package org.pih.maven.plugin;

import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates a report that lists all project non-transitive dependencies and unique sha-1 hash of each artifact
 * This is inspired by https://github.com/mekomsolutions/dependency-tracker-maven-plugin
 */
@Mojo(name = "dependency-report", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
@Setter
public class DependencyReportMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter(property = "buildDirectory", defaultValue = "${project.build.directory}")
	private File buildDirectory;

	@Parameter(property = "buildFileName", defaultValue = "${project.build.finalName}-dependencies.txt")
	private String buildFileName;

	@Override
	public void execute() throws MojoFailureException {
		try {
			getLog().info("Capturing project dependencies");
			Set<Artifact> artifacts = project.getDependencyArtifacts();
			getLog().info("Found " + artifacts.size() + " dependencies to track");

			getLog().debug("---------------------- Tracked Dependencies ----------------------");
			List<String> lines = new ArrayList<>();
			for (Artifact a : artifacts) {
				getLog().debug("Generating sha1 hash for artifact: " + a);
				byte[] fileBytes = Files.readAllBytes(a.getFile().toPath());
				String sha1Hex = DigestUtils.sha1Hex(fileBytes);
				String line = a.getId() + ":" + sha1Hex;
				getLog().debug(line);
				lines.add(line);
			}
			getLog().debug("------------------------------------------------------------------");

			File artifactFile = new File(buildDirectory, buildFileName);
			getLog().info("Saving dependency tracker artifact to " + artifactFile);
			Files.write(artifactFile.toPath(), lines);
		}
		catch (IOException e) {
			throw new MojoFailureException("An error occurred while tracking dependencies", e);
		}
	}
}
