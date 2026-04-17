package com.loganalyser.model;

import java.util.List;

/**
 * A ranked list of entries produced by the analyser, together with a flag
 * indicating whether the cut-off position is tied.
 *
 * <p>Co-locating the tie flag with the list it describes avoids the need to
 * carry two separate fields — one for the entries and one for the tie — when
 * passing analysis results between pipeline stages.
 *
 * @param entries the ranked entries, ordered by rank ascending; never null, may be empty
 * @param tied    true when an entry outside the list shares the same count as the
 *                last entry inside it — i.e. the cut-off position is ambiguous
 */
public record RankedList(List<RankedEntry> entries, boolean tied) {
}
