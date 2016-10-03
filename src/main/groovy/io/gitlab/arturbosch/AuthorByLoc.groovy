package io.gitlab.arturbosch

import groovy.transform.Immutable
import groovy.transform.ToString
import io.gitlab.arturbosch.loc.core.LOC
import io.gitlab.arturbosch.loc.languages.LanguageStrategyFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiFunction

/**
 * Collects from given project path all contributors and assign them the loc count of
 * their written files.
 *
 * As lloc is used for loc calculation only some jvm languages are supported.
 *
 * If the @author's line tokens are separated with a ',', then we split the line
 * and all parts are treated as authors. Brackets within an authors name are deleted
 * with a regex. If the currently scanned sub path (e.g. a file) has not a supported
 * language ending of lloc, it is skipped. This also happens if the sub path contains
 * the word resources as this indicates some source code resources for testing.
 * In the future a concept of name merging is thoughtful.
 *
 * This little program is though to be used as a groovy script, but can also be build
 * to a jar file with the shadow task.
 *
 * @author Artur Bosch
 */
@Grab('io.gitlab.arturbosch:lloc:1.4')
class AuthorByLoc {

	static void main(String... args) {
		if (args == null || args.size() == 0) {
			throw new IllegalArgumentException("One argument: Project Path must be specified!")
		}

		String pathToProject = args[0]
		def path = Paths.get(pathToProject)
		if (Files.notExists(path)) {
			throw new IllegalArgumentException("Given project path is not valid!")
		}

		new Collector(path).invoke().authorByLoc.each { println it }
	}

	private static class Collector {

		private static List<String> languagesAsList = Arrays.asList(LanguageStrategyFactory.languages)
		private static BiFunction<Double, Double, Double> mergeFunc = { Double a, Double b -> a + b }

		private Path project
		Map<String, Double> authorByLoc = new HashMap<>()

		Collector(Path project) {
			this.project = project
		}

		Collector invoke() {
			Files.walk(project)
					.filter { Files.isRegularFile(it) }
					.map { execute(it) }
					.filter { it != null }
					.flatMap { it.stream() }
					.each { fillMap(it) }
			return this
		}

		private void fillMap(Data data) {
			authorByLoc.merge(data.author, data.loc, mergeFunc)
		}

		private static Collection<Data> execute(Path path) {

			def pathAsString = path.toAbsolutePath().toString()
			def ending = pathAsString.substring(pathAsString.lastIndexOf(".") + 1)

			if (!supportedLanguage(ending) || pathAsString.contains("resources")) {
				return null
			}

			def strategy = LanguageStrategyFactory.getInstance(ending)
			def count = LOC.count(strategy, path.toFile())

			def authors = Files.lines(path)
					.filter { it.contains("@author") }
					.map { it.substring(it.indexOf("@author") + 7) }
					.flatMap { Arrays.stream(it.split(",")) }
					.map { it.replaceAll("\\(.*\\)", "") }
					.map { it.trim() }
					.collect()

			def size = authors.size()

			return authors.collect { new Data(it as String, count / size) }
		}

		private static boolean supportedLanguage(String language) {
			languagesAsList.contains(language)
		}

	}

	@Immutable
	@ToString(includePackage = false)
	private static class Data {
		String author
		double loc
	}
}
