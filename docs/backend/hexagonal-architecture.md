# Hexagonal Architecture

Em atendimento a macro-arquitetura hexagonal, o código deve ser formatado em:

- Resource (interface web)
- UseCase (Orquestração de serviços de negócio — recebe Commands/DTOs, delega para Services com parâmetros simples)
- Service (Capacidades do modelo de negócio — acesso a Repository, validações de domínio)
- Repository (Persistências dos registros de negócio)

## Fluxo de dependência

```
Resource → UseCase → Service → Repository
```

- **Resource**: traduz HTTP ↔ Result, sem lógica de negócio
- **UseCase**: recebe Commands, orquestra Services, publica eventos
- **Service**: encapsula acesso a Repository e validações de domínio, recebe parâmetros simples
- **Repository**: interface de persistência definida no domínio
