package br.cdb.feature.f010._0_domain;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/** Porta: algo a fazer quando um usuário é criado (ex.: semear o catálogo default de categorias). */
@NullMarked
public interface UserProvisioningStep {

    void provision(UUID personId);
}
