/*
 * SPDX-FileCopyrightText: 2025-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

const OutputFormat = z.object({
  enabled: z.boolean().default(true),
});

export const PartialInterpretationPreservation = z.enum(['keep', 'discard']);

export type PartialInterpretationPreservation = z.infer<
  typeof PartialInterpretationPreservation
>;

export const JsonOutputFormat = OutputFormat.extend({
  nonExistingObjects: PartialInterpretationPreservation.default('discard'),
  shadowPredicates: PartialInterpretationPreservation.default('discard'),
  relations: z.array(z.string()).optional(),
  excludedRelations: z.array(z.string()).optional(),
});

export type JsonOutputFormat = z.infer<typeof JsonOutputFormat>;

export const SourceOutputFormat = OutputFormat;

export type SourceOutputFormat = z.infer<typeof SourceOutputFormat>;

export const OutputFormats = z
  .object({
    json: JsonOutputFormat.prefault({}),
    source: SourceOutputFormat.prefault({}),
  })
  .prefault({});

export type OutputFormats = z.infer<typeof OutputFormats>;
