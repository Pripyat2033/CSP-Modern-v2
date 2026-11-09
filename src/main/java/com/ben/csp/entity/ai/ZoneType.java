package com.ben.csp.entity.ai;

// ============================================================================
// SUPERSEDED by com.ben.csp.radiology.ZoneType
//   This duplicate is not referenced anywhere. It is kept only so that
//   existing work is not destroyed. DO NOT USE IT IN NEW CODE.
//   Ask Ben before deleting.
// ============================================================================

@Deprecated
public enum ZoneType {
    CLEAN,
    CONTROLLED_ACCESS,
    HIGH_RADIATION_AREA,
    CONTAMINATION_AREA;
}