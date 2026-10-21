/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.json;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import tools.refinery.generator.ModelFacade;
import tools.refinery.store.util.CancellationToken;

import java.util.function.Predicate;

@Singleton
public class PartialModel2Json {
	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	public PartialModelJson savePartialInterpretation(ModelFacade facade) {
		return savePartialInterpretation(facade, facade.getModel().getCancellationToken());
	}

	public PartialModelJson savePartialInterpretation(ModelFacade facade, CancellationToken cancellationToken) {
		return savePartialInterpretation(facade, cancellationToken, ignored -> true);
	}

	/**
	 * Save the partial interpretation of some relations of a model.
	 *
	 * @param facade            The model to save.
	 * @param cancellationToken The cancellation token to check while saving.
	 * @param includeRelation   Whether to include a relation, given its name as it appears in the output.
	 * @return The saved model.
	 */
	public PartialModelJson savePartialInterpretation(ModelFacade facade, CancellationToken cancellationToken,
													  Predicate<String> includeRelation) {
		cancellationToken.checkCancelled();
		var nodes = facade.getNodesMetadata().list();
		cancellationToken.checkCancelled();
		var relations = facade.getRelationsMetadata().stream()
				.filter(relation -> includeRelation.test(relation.name()))
				.toList();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(facade, cancellationToken,
				includeRelation);
		return new PartialModelJson(facade.getConcreteness(), nodes, relations, partialInterpretation);
	}
}
