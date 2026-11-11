package com.ben.csp.work;

/**
 * Where a {@link WorkType}'s man-hour norm comes from. The point of this enum is
 * that a figure which has not been read out of the norm collection cannot pretend
 * it has: it is marked {@link #PROVISIONAL} and must name the collection it is
 * still waiting on. "Science grade" means the data says what it is.
 */
public enum NormSource {
    /** Read from the named ЕНиР / СНиП collection; safe to trust. */
    ENIR_SOURCED,
    /** A shape-correct figure not yet replaced by a sourced one. Not trustworthy. */
    PROVISIONAL,
    /** No per-unit norm applies (lump / без нормы work). */
    NOT_APPLICABLE
}
