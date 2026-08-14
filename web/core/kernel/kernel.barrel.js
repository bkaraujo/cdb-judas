/* core/kernel/kernel.barrel.js — morto: a lógica de sequenciar os arquivos do kernel foi
 * incorporada direto em core/boot.js (lista única achatada kernel+fatias, ver comentário lá —
 * o padrão de dois níveis de injeção dinâmica <script async=false> quebrava a ordem de execução
 * de qualquer primitiva de kernel usada no nível do módulo por uma fatia, ex. window.cachePage).
 * A ferramenta de edição desta sessão não tem permissão para apagar arquivos — pendente remoção
 * manual: `rm web/core/kernel/kernel.barrel.js`.
 */
