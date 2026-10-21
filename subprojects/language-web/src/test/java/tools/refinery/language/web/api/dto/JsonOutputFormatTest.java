/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class JsonOutputFormatTest {
	private static final List<String> NAMES = List.of("Person", "Person::friend", "lonely", "helper");

	@Test
	void includeAllTest() {
		var format = new JsonOutputFormat();
		assertThat(filter(format.createRelationFilter()), contains("Person", "Person::friend", "lonely", "helper"));
	}

	@Test
	void relationsTest() {
		var format = new JsonOutputFormat();
		format.setRelations(List.of("lonely", "helper", "nonExistent"));
		assertThat(filter(format.createRelationFilter()), contains("lonely", "helper"));
	}

	@Test
	void excludedRelationsTest() {
		var format = new JsonOutputFormat();
		format.setExcludedRelations(List.of("helper", "nonExistent"));
		assertThat(filter(format.createRelationFilter()), contains("Person", "Person::friend", "lonely"));
	}

	@Test
	void relationsAndExcludedRelationsTest() {
		var format = new JsonOutputFormat();
		format.setRelations(List.of("lonely", "helper"));
		format.setExcludedRelations(List.of("helper"));
		assertThat(filter(format.createRelationFilter()), contains("lonely"));
	}

	private static List<String> filter(Predicate<String> includeRelation) {
		return NAMES.stream().filter(includeRelation).toList();
	}
}
