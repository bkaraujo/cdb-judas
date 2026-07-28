package br.cdb.feature.f003._2_infrastructure.web.request;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CardStatusRequest(boolean active) {}
