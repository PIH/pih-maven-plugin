package org.pih.maven.plugin;

import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates a report that lists all project non-transitive dependencies and unique sha-1 hash of each artifact
 * This is inspired by <a href="https://github.com/mekomsolutions/dependency-tracker-maven-plugin">Mekom Solutions</a>
 */
@Mojo(name = "dependency-report", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
@Setter
public class DependencyReportMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter(property = "outputFile", defaultValue = "dependencies.txt")
	private File outputFile;

	@Override
	public void execute() throws MojoFailureException {
		try {
			getLog().info("Capturing project dependencies");
			List<Dependency> dependencies = project.getDependencies();
			getLog().info("Found " + dependencies.size() + " dependencies to track");
			getLog().debug("---------------------- Tracked Dependencies ----------------------");
			Map<String, Artifact> dependencyArtifacts = new TreeMap<>();
			for (Dependency d : dependencies) {
				getLog().debug("Dependency: " + d);
				Artifact artifact = getArtifact(d);
				getLog().debug("Resolved dependency to artifact: " + artifact);
				dependencyArtifacts.put(artifact.getId(), artifact);
			}
			List<String> lines = new ArrayList<>();
			for (String key : dependencyArtifacts.keySet()) {
				Artifact artifact = dependencyArtifacts.get(key);
				getLog().debug("Generating sha1 hash for artifact file: " + artifact.getFile());
				byte[] fileBytes = Files.readAllBytes(artifact.getFile().toPath());
				String sha1Hex = DigestUtils.sha1Hex(fileBytes);
				String line = artifact.getId() + ":" + sha1Hex;
				getLog().info(line);
				lines.add(line);
			}
			getLog().debug("------------------------------------------------------------------");
			getLog().info("Saving dependency tracker artifact to " + outputFile);
			Files.write(outputFile.toPath(), lines);
		}
		catch (IOException e) {
			throw new MojoFailureException("An error occurred while tracking dependencies", e);
		}
	}

	/**
	 * Returns the artifact associated with the given dependency defined in the project
	 */
	protected Artifact getArtifact(Dependency d) throws MojoFailureException {
		String type = StringUtils.isBlank(d.getType()) ? "jar" : d.getType();
		for (Artifact a : project.getArtifacts()) {
			if (d.getGroupId().equals(a.getGroupId()) && d.getArtifactId().equals(a.getArtifactId())) {
				if (StringUtils.isBlank(d.getClassifier()) || d.getClassifier().equals(a.getClassifier())) {
					if (type.equals(a.getType())) {
						return a;
					}
				}
			}
		}
		throw new MojoFailureException("Unable to find artifact for dependency: " + d);
	}
}
