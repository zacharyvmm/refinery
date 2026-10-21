/*
 * SPDX-FileCopyrightText: 2025-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class JsonOutputFormat extends OutputFormat {
	@NotNull
	private PartialInterpretationPreservation nonExistingObjects = PartialInterpretationPreservation.DISCARD;

	@NotNull
	private PartialInterpretationPreservation shadowPredicates = PartialInterpretationPreservation.DISCARD;

	/**
	 * The names of the relations to include in the output, or {@code null} to include every relation.
	 */
	@Nullable
	private List<@NotNull String> relations;

	/**
	 * The names of the relations to leave out of the output, even if they are listed in {@link #relations}.
	 */
	@Nullable
	private List<@NotNull String> excludedRelations;

	public PartialInterpretationPreservation getNonExistingObjects() {
		return isEnabled() && nonExistingObjects != null ? nonExistingObjects :
				PartialInterpretationPreservation.DISCARD;
	}

	public void setNonExistingObjects(PartialInterpretationPreservation nonExistingObjects) {
		this.nonExistingObjects = nonExistingObjects;
	}

	public PartialInterpretationPreservation getShadowPredicates() {
		return isEnabled() && shadowPredicates != null ? shadowPredicates : PartialInterpretationPreservation.DISCARD;
	}

	public void setShadowPredicates(PartialInterpretationPreservation shadowPredicates) {
		this.shadowPredicates = shadowPredicates;
	}

	@Nullable
	public List<String> getRelations() {
		return relations;
	}

	public void setRelations(@Nullable List<String> relations) {
		this.relations = relations;
	}

	@Nullable
	public List<String> getExcludedRelations() {
		return excludedRelations;
	}

	public void setExcludedRelations(@Nullable List<String> excludedRelations) {
		this.excludedRelations = excludedRelations;
	}

	/**
	 * Create a filter that accepts the names of the relations to include in the output.
	 *
	 * @return Whether to include a relation, given its name as it appears in the output.
	 */
	public Predicate<String> createRelationFilter() {
		Predicate<String> listed = relations == null ? ignored -> true : Set.copyOf(relations)::contains;
		if (excludedRelations == null || excludedRelations.isEmpty()) {
			return listed;
		}
		var excluded = Set.copyOf(excludedRelations);
		return name -> listed.test(name) && !excluded.contains(name);
	}
}
