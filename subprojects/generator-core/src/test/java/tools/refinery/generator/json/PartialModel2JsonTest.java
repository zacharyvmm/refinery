/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.json;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.semantics.metadata.RelationMetadata;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.store.util.CancellationToken;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;

@InjectWithRefinery
class PartialModel2JsonTest {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelSemanticsFactory semanticsFactory;

	@Inject
	private PartialModel2Json partialModel2Json;

	@Test
	void allRelationsTest() throws IOException {
		var json = save(null);
		var names = List.of("builtin::exists", "Person", "Person::friend", "lonely");
		assertThat(json.partialInterpretation().keySet(), hasItems(names.toArray(String[]::new)));
		assertThat(getRelationNames(json), hasItems(names.toArray(String[]::new)));
	}

	@Test
	void selectedRelationsTest() throws IOException {
		var json = save(List.of("Person::friend", "lonely", "nonExistent"));
		assertThat(json.partialInterpretation().keySet(), containsInAnyOrder("Person::friend", "lonely"));
		assertThat(getRelationNames(json), containsInAnyOrder("Person::friend", "lonely"));
	}

	private PartialModelJson save(List<String> relations) throws IOException {
		var problem = loader.loadString("""
				class Person {
					Person[] friend
				}

				pred lonely(Person p) <-> !friend(p, _).

				Person(alice).
				Person(bob).
				friend(alice, bob).
				""");
		try (var semantics = semanticsFactory.createSemantics(problem)) {
			return partialModel2Json.savePartialInterpretation(semantics, CancellationToken.NONE, relations);
		}
	}

	private static List<String> getRelationNames(PartialModelJson json) {
		return json.relations().stream().map(RelationMetadata::name).toList();
	}
}
