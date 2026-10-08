package com.ben.csp.block.entity;

/**
 * Defines the type of a channel within the RBMK reactor core.
 */
public enum ChannelType {
    EMPTY,
    FUEL,
    CONTROL_ROD,
    GRAPHITE // For moderator blocks without fuel or control rods
}