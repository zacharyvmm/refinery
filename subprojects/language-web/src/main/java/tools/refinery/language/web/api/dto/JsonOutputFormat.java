/*
 * SPDX-FileCopyrightText: 2025-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.dto;

import jakarta.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
}
