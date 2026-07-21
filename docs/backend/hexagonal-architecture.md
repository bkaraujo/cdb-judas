# Hexagonal Architecture

Aplica-se duas vezes: dentro de cada **contexto** de negócio (`br.cdb.context.*`, `_0_domain`/
`_1_application`) e, desde a migração `fNNN` (`.claude/refactor.md`), dentro de cada **feature**
(`br.cdb.feature.fNNN`, `_0_domain`/`_1_application`/`_2_infrastructure`) — cada fatia é um
hexágono auto-contido com seu próprio use case, portas e adaptadores, em vez de um único
Resource→god-object central. Em atendimento a macro-arquitetura hexagonal, o código deve ser formatado em:

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
