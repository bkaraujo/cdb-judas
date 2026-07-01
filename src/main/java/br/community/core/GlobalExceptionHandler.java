package br.community.core;
// Obsoleto na migração para Quarkus: substituído por ExceptionMappers JAX-RS em
// br.community.core.web.error (DomainExceptionMapper, ConstraintViolationExceptionMapper,
// GenericExceptionMapper). ResponseStatusException/MethodArgumentNotValidException/
// HttpRequestMethodNotSupportedException eram Spring MVC — sem equivalente necessário.
// Arquivo a remover na limpeza: rm src/main/java/br/community/core/GlobalExceptionHandler.java
