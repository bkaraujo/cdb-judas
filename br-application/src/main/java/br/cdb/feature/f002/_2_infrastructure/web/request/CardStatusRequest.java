package br.cdb.feature.f002._2_infrastructure.web.request;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardStatusRequest(boolean active) {}
