/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.json;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.Nullable;
import tools.refinery.generator.ModelFacade;
import tools.refinery.store.util.CancellationToken;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

@Singleton
public class PartialModel2Json {
	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	public PartialModelJson savePartialInterpretation(ModelFacade facade) {
		return savePartialInterpretation(facade, facade.getModel().getCancellationToken());
	}

	public PartialModelJson savePartialInterpretation(ModelFacade facade, CancellationToken cancellationToken) {
		return savePartialInterpretation(facade, cancellationToken, null);
	}

	/**
	 * Save the partial interpretation of some relations of a model.
	 *
	 * @param facade            The model to save.
	 * @param cancellationToken The cancellation token to check while saving.
	 * @param relationNames     The names of the relations to include, as they appear in the output, or {@code null} to
	 *                          include every relation. Names that don't refer to any relation are ignored.
	 * @return The saved model.
	 */
	public PartialModelJson savePartialInterpretation(ModelFacade facade, CancellationToken cancellationToken,
													  @Nullable Collection<String> relationNames) {
		Predicate<String> includeRelation = relationNames == null ? ignored -> true :
				Set.copyOf(relationNames)::contains;
		cancellationToken.checkCancelled();
		var nodes = facade.getNodesMetadata().list();
		cancellationToken.checkCancelled();
		var relations = facade.getRelationsMetadata();
		if (relationNames != null) {
			relations = relations.stream()
					.filter(relation -> includeRelation.test(relation.name()))
					.toList();
		}
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(facade, cancellationToken,
				includeRelation);
		return new PartialModelJson(facade.getConcreteness(), nodes, relations, partialInterpretation);
	}
}
