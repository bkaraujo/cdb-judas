package br.commons.platform.provider.windows;

import br.commons.Logger;
import lombok.val;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

import static br.commons.platform.provider.windows.Kernel32.GetLastError;

@NullMarked
public abstract class WinAPI {
    private WinAPI() {
    }

    static final Arena arena = Arena.ofShared();
    static final Linker linker = Linker.nativeLinker();

    // =====================================================================
    // GENERAL CONSTANTS
    // =====================================================================
    public static final int S_OK = 0;
    public static final int S_FALSE = 1;
    public static final int MAX_PATH = 260;
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    static <T> T invoke(MethodHandle method, Object... args) {
        try {
            return (T) method.invokeWithArguments(args);
        } catch (Throwable e) {
            Logger.fatal("Failed to invoke User32 function: %s", e.toString());
            return (T) new Object();
        }
    }

    /** Reads a null-terminated wide string (UTF-16LE) from a MemorySegment. */
    public static String readWideString(MemorySegment segment) {
        // Find the null terminator
        var length = 0;
        while (length < MAX_PATH) {
            val ch = segment.get(ValueLayout.JAVA_SHORT, length * 2L);
            if (ch == 0) break;
            length++;
        }

        // Read the bytes and convert to String
        val bytes = new byte[length * 2];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }

    public static void printError(String functionName) {
        val error = GetLastError();
        if (error != WinError.ERROR_SUCCESS) {
            Logger.warn("%s: %s", functionName, WinAPI.error(error));
        }
    }

    // Mapeamento exaustivo de códigos de erro do Windows → nome: é tabela de dados, não lógica.
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static String error(int code) {
        return switch (code) {
            case WinError.ERROR_SUCCESS -> "ERROR_SUCCESS";
            case WinError.ERROR_INVALID_FUNCTION ->  "ERROR_INVALID_FUNCTION";
            case WinError.ERROR_FILE_NOT_FOUND ->  "ERROR_FILE_NOT_FOUND";
            case WinError.ERROR_PATH_NOT_FOUND ->  "ERROR_PATH_NOT_FOUND";
            case WinError.ERROR_TOO_MANY_OPEN_FILES ->  "ERROR_TOO_MANY_OPEN_FILES";
            case WinError.ERROR_ACCESS_DENIED ->  "ERROR_ACCESS_DENIED";
            case WinError.ERROR_INVALID_HANDLE ->  "ERROR_INVALID_HANDLE";
            case WinError.ERROR_NOT_ENOUGH_MEMORY ->  "ERROR_NOT_ENOUGH_MEMORY";
            case WinError.ERROR_INVALID_DATA ->  "ERROR_INVALID_DATA";
            case WinError.ERROR_OUTOFMEMORY ->  "ERROR_OUTOFMEMORY";
            case WinError.ERROR_INVALID_DRIVE ->  "ERROR_INVALID_DRIVE";
            case WinError.ERROR_NO_MORE_FILES ->  "ERROR_NO_MORE_FILES";
            case WinError.ERROR_WRITE_PROTECT ->  "ERROR_WRITE_PROTECT";
            case WinError.ERROR_NOT_READY ->  "ERROR_NOT_READY";
            case WinError.ERROR_SHARING_VIOLATION ->  "ERROR_SHARING_VIOLATION";
            case WinError.ERROR_LOCK_VIOLATION ->  "ERROR_LOCK_VIOLATION";
            case WinError.ERROR_HANDLE_EOF ->  "ERROR_HANDLE_EOF";
            case WinError.ERROR_NOT_SUPPORTED ->  "ERROR_NOT_SUPPORTED";
            case WinError.ERROR_FILE_EXISTS ->  "ERROR_FILE_EXISTS";
            case WinError.ERROR_INVALID_PARAMETER ->  "ERROR_INVALID_PARAMETER";
            case WinError.ERROR_BROKEN_PIPE ->  "ERROR_BROKEN_PIPE";
            case WinError.ERROR_DISK_FULL ->  "ERROR_DISK_FULL";
            case WinError.ERROR_INSUFFICIENT_BUFFER ->  "ERROR_INSUFFICIENT_BUFFER";
            case WinError.ERROR_INVALID_NAME ->  "ERROR_INVALID_NAME";
            case WinError.ERROR_MOD_NOT_FOUND ->  "ERROR_MOD_NOT_FOUND";
            case WinError.ERROR_PROC_NOT_FOUND ->  "ERROR_PROC_NOT_FOUND";
            case WinError.ERROR_DIR_NOT_EMPTY ->  "ERROR_DIR_NOT_EMPTY";
            case WinError.ERROR_ALREADY_EXISTS ->  "ERROR_ALREADY_EXISTS";
            case WinError.ERROR_ENVVAR_NOT_FOUND ->  "ERROR_ENVVAR_NOT_FOUND";
            case WinError.ERROR_MORE_DATA ->  "ERROR_MORE_DATA";
            case WinError.ERROR_NO_MORE_ITEMS ->  "ERROR_NO_MORE_ITEMS";
            case WinError.ERROR_TIMEOUT ->  "ERROR_TIMEOUT";

            default -> "UNKNOWN";
        };
    }

    @NullMarked
    public static abstract class WinError {
        private WinError() {
        }

        public static final int ERROR_SUCCESS = 0;
        public static final int ERROR_INVALID_FUNCTION = 1;
        public static final int ERROR_FILE_NOT_FOUND = 2;
        public static final int ERROR_PATH_NOT_FOUND = 3;
        public static final int ERROR_TOO_MANY_OPEN_FILES = 4;
        public static final int ERROR_ACCESS_DENIED = 5;
        public static final int ERROR_INVALID_HANDLE = 6;
        public static final int ERROR_NOT_ENOUGH_MEMORY = 8;
        public static final int ERROR_INVALID_DATA = 13;
        public static final int ERROR_OUTOFMEMORY = 14;
        public static final int ERROR_INVALID_DRIVE = 15;
        public static final int ERROR_NO_MORE_FILES = 18;
        public static final int ERROR_WRITE_PROTECT = 19;
        public static final int ERROR_NOT_READY = 21;
        public static final int ERROR_SHARING_VIOLATION = 32;
        public static final int ERROR_LOCK_VIOLATION = 33;
        public static final int ERROR_HANDLE_EOF = 38;
        public static final int ERROR_NOT_SUPPORTED = 50;
        public static final int ERROR_FILE_EXISTS = 80;
        public static final int ERROR_INVALID_PARAMETER = 87;
        public static final int ERROR_BROKEN_PIPE = 109;
        public static final int ERROR_DISK_FULL = 112;
        public static final int ERROR_INSUFFICIENT_BUFFER = 122; // Buffer passado é muito pequeno
        public static final int ERROR_INVALID_NAME = 123;
        public static final int ERROR_MOD_NOT_FOUND = 126; // DLL não encontrada
        public static final int ERROR_PROC_NOT_FOUND = 127; // Função não encontrada na DLL
        public static final int ERROR_DIR_NOT_EMPTY = 145;
        public static final int ERROR_ALREADY_EXISTS = 183;
        public static final int ERROR_ENVVAR_NOT_FOUND = 203;
        public static final int ERROR_MORE_DATA = 234; // Há mais dados disponíveis
        public static final int ERROR_NO_MORE_ITEMS = 259;
        public static final int ERROR_TIMEOUT = 1460;

    }

    @NullMarked
    public static abstract class WinBase {
        private WinBase() {
        }

        // --- File Creation & Management (Criação e Gestão de Arquivos) ---

        // Creation Disposition (CreateFile)
        public static final int CREATE_NEW = 1; // Cria novo, falha se existir
        public static final int CREATE_ALWAYS = 2; // Cria novo, sobrescreve se existir
        public static final int OPEN_EXISTING = 3; // Abre existente, falha se não existir
        public static final int OPEN_ALWAYS = 4; // Abre existente, cria se não existir
        public static final int TRUNCATE_EXISTING = 5; // Abre existente e trunca para 0 bytes

        // File Sharing Modes
        public static final int FILE_SHARE_READ = 0x00000001; // Permite outros lerem
        public static final int FILE_SHARE_WRITE = 0x00000002; // Permite outros escreverem
        public static final int FILE_SHARE_DELETE = 0x00000004; // Permite outros deletarem

        // File Flags (CreateFile)
        public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000; // Escreve direto no disco
        public static final int FILE_FLAG_OVERLAPPED = 0x40000000; // I/O Assíncrono
        public static final int FILE_FLAG_NO_BUFFERING = 0x20000000; // Sem cache do sistema
        public static final int FILE_FLAG_RANDOM_ACCESS = 0x10000000; // Otimiza para acesso aleatório
        public static final int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000; // Otimiza para acesso sequencial
        public static final int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000; // Deleta ao fechar handle
        public static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000; // Necessário para abrir diretórios
        public static final int FILE_FLAG_POSIX_SEMANTICS = 0x01000000; // Case-sensitive (raro)
        public static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000; // Abre o reparse point, não o alvo
        public static final int FILE_FLAG_OPEN_NO_RECALL = 0x00100000; // Não puxar de fita/storage remoto

        // SetFilePointer Move Methods
        public static final int FILE_BEGIN = 0; // Início do arquivo
        public static final int FILE_CURRENT = 1; // Posição atual
        public static final int FILE_END = 2; // Fim do arquivo

        // MoveFileEx Flags
        public static final int MOVEFILE_REPLACE_EXISTING = 0x00000001;
        public static final int MOVEFILE_COPY_ALLOWED = 0x00000002; // Permite mover entre volumes (copia e deleta)
        public static final int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004; // Move no próximo boot (arquivos em uso)
        public static final int MOVEFILE_WRITE_THROUGH = 0x00000008;
        public static final int MOVEFILE_CREATE_HARDLINK = 0x00000010;
        public static final int MOVEFILE_FAIL_IF_NOT_TRACKABLE = 0x00000020;

        // LockFile Flags
        public static final int LOCKFILE_FAIL_IMMEDIATELY = 0x00000001; // Não espera se estiver bloqueado
        public static final int LOCKFILE_EXCLUSIVE_LOCK = 0x00000002; // Bloqueio exclusivo

        // Drive Types (GetDriveType)
        public static final int DRIVE_UNKNOWN = 0;
        public static final int DRIVE_NO_ROOT_DIR = 1; // Caminho inválido
        public static final int DRIVE_REMOVABLE = 2; // USB, Floppy
        public static final int DRIVE_FIXED = 3; // HD, SSD
        public static final int DRIVE_REMOTE = 4; // Rede
        public static final int DRIVE_CDROM = 5;
        public static final int DRIVE_RAMDISK = 6;

        // --- Process & Thread Creation (Criação de Processos e Threads) ---

        // Process Creation Flags (CreateProcess)
        public static final int CREATE_SUSPENDED = 0x00000004; // Processo inicia suspenso
        public static final int CREATE_NEW_CONSOLE = 0x00000010; // Cria novo console
        public static final int CREATE_NEW_PROCESS_GROUP = 0x00000200; // Novo grupo de processos (Ctrl+C)
        public static final int CREATE_UNICODE_ENVIRONMENT = 0x00000400; // Bloco de ambiente é Unicode
        public static final int CREATE_NO_WINDOW = 0x08000000; // Aplicação console sem janela
        public static final int DETACHED_PROCESS = 0x00000008; // Sem console pai

        // Startup Info Flags (STARTUPINFO)
        public static final int STARTF_USESHOWWINDOW = 0x00000001; // Usa wShowWindow
        public static final int STARTF_USESIZE = 0x00000002; // Usa dwXSize/dwYSize
        public static final int STARTF_USEPOSITION = 0x00000004; // Usa dwX/dwY
        public static final int STARTF_USECOUNTCHARS = 0x00000008; // Usa dwXCountChars/dwYCountChars
        public static final int STARTF_USEFILLATTRIBUTE = 0x00000010; // Usa dwFillAttribute
        public static final int STARTF_RUNFULLSCREEN = 0x00000020; // Inicia em tela cheia (x86 apenas)
        public static final int STARTF_FORCEONFEEDBACK = 0x00000040; // Força cursor de "carregando"
        public static final int STARTF_FORCEOFFFEEDBACK = 0x00000080; // Remove cursor de "carregando"
        public static final int STARTF_USESTDHANDLES = 0x00000100; // Usa hStdInput/Output/Error
        public static final int STARTF_USEHOTKEY = 0x00000200;
        public static final int STARTF_TITLEISLINKNAME = 0x00000800;
        public static final int STARTF_TITLEISAPPID = 0x00001000;
        public static final int STARTF_PREVENTPINNING = 0x00002000;

        // Thread Priorities
        public static final int THREAD_PRIORITY_IDLE = -15;
        public static final int THREAD_PRIORITY_LOWEST = -2;
        public static final int THREAD_PRIORITY_BELOW_NORMAL = -1;
        public static final int THREAD_PRIORITY_NORMAL = 0;
        public static final int THREAD_PRIORITY_ABOVE_NORMAL = 1;
        public static final int THREAD_PRIORITY_HIGHEST = 2;
        public static final int THREAD_PRIORITY_TIME_CRITICAL = 15;

        // Priority Classes (SetPriorityClass)
        public static final int IDLE_PRIORITY_CLASS = 0x00000040;
        public static final int BELOW_NORMAL_PRIORITY_CLASS = 0x00004000;
        public static final int NORMAL_PRIORITY_CLASS = 0x00000020;
        public static final int ABOVE_NORMAL_PRIORITY_CLASS = 0x00008000;
        public static final int HIGH_PRIORITY_CLASS = 0x00000080;
        public static final int REALTIME_PRIORITY_CLASS = 0x00000100;

        // Process Exit Code
        public static final int STILL_ACTIVE = 259; // Processo ainda está rodando

        // --- Synchronization (Sincronização) ---

        // WaitForSingleObject Return Values
        public static final int WAIT_OBJECT_0 = 0x00000000; // Objeto sinalizado
        public static final int WAIT_ABANDONED = 0x00000080; // Mutex abandonado (thread morreu)
        public static final int WAIT_TIMEOUT = 0x00000102; // Tempo limite excedido
        public static final int WAIT_FAILED = 0xFFFFFFFF; // Erro na chamada
        public static final int INFINITE = 0xFFFFFFFF; // Esperar para sempre

        // --- Handle Management ---
        public static final int DUPLICATE_CLOSE_SOURCE = 0x00000001; // Fecha handle original após duplicar
        public static final int DUPLICATE_SAME_ACCESS = 0x00000002; // Mantém mesmos direitos de acesso
        public static final int HANDLE_FLAG_INHERIT = 0x00000001; // Handle é herdado por processos filhos
        public static final int HANDLE_FLAG_PROTECT_FROM_CLOSE = 0x00000002; // Impede closing do handle

        // --- Pipes (CreateNamedPipe) ---
        public static final int PIPE_ACCESS_INBOUND = 0x00000001; // Cliente -> Servidor
        public static final int PIPE_ACCESS_OUTBOUND = 0x00000002; // Servidor -> Cliente
        public static final int PIPE_ACCESS_DUPLEX = 0x00000003; // Bidirecional
        public static final int PIPE_TYPE_BYTE = 0x00000000; // Stream de bytes
        public static final int PIPE_TYPE_MESSAGE = 0x00000004; // Stream de mensagens
        public static final int PIPE_READMODE_BYTE = 0x00000000; // Ler como bytes
        public static final int PIPE_READMODE_MESSAGE = 0x00000002; // Ler como mensagem
        public static final int PIPE_WAIT = 0x00000000; // Blocking mode
        public static final int PIPE_NOWAIT = 0x00000001; // Non-blocking mode
        public static final int PIPE_UNLIMITED_INSTANCES = 255;

        // --- Memory Allocation Flags (Heap & Global/Local) ---

        // Heap Flags (HeapCreate, HeapAlloc)
        public static final int HEAP_NO_SERIALIZE = 0x00000001; // Sem thread-safety (mais rápido)
        public static final int HEAP_GROWABLE = 0x00000002;
        public static final int HEAP_GENERATE_EXCEPTIONS = 0x00000004; // Lança exceção SEH em erro
        public static final int HEAP_ZERO_MEMORY = 0x00000008; // Zera memória alocada
        public static final int HEAP_REALLOC_IN_PLACE_ONLY = 0x00000010;
        public static final int HEAP_TAIL_CHECKING_ENABLED = 0x00000020;
        public static final int HEAP_FREE_CHECKING_ENABLED = 0x00000040;
        public static final int HEAP_DISABLE_COALESCE_ON_FREE = 0x00000080;
        public static final int HEAP_CREATE_ENABLE_TRACING = 0x00020000;

        // Global/Local Memory Flags (Legacy)
        public static final int GMEM_FIXED = 0x0000; // Alocação fixa
        public static final int GMEM_MOVEABLE = 0x0002; // Alocação movível (handle)
        public static final int GMEM_ZEROINIT = 0x0040; // Zera memória
        public static final int GPTR = GMEM_FIXED | GMEM_ZEROINIT;
        public static final int GHND = GMEM_MOVEABLE | GMEM_ZEROINIT;
        public static final int LMEM_FIXED = 0x0000;
        public static final int LMEM_MOVEABLE = 0x0002;
        public static final int LMEM_ZEROINIT = 0x0040;
        public static final int LPTR = LMEM_FIXED | LMEM_ZEROINIT;
        public static final int LHND = LMEM_MOVEABLE | LMEM_ZEROINIT;

        // File Mapping (MapViewOfFile / CreateFileMapping)
        public static final int FILE_MAP_COPY = 0x0001; // Copy-on-write
        public static final int FILE_MAP_WRITE = 0x0002; // Leitura/Escrita
        public static final int FILE_MAP_READ = 0x0004; // Apenas leitura
        public static final int FILE_MAP_EXECUTE = 0x0020; // Execução
        public static final int FILE_MAP_ALL_ACCESS = 0x000F001F;

        // --- Dynamic Library Loading (LoadLibraryEx) ---
        public static final int DONT_RESOLVE_DLL_REFERENCES = 0x00000001;
        public static final int LOAD_LIBRARY_AS_DATAFILE = 0x00000002; // Carrega sem executar DllMain
        public static final int LOAD_WITH_ALTERED_SEARCH_PATH = 0x00000008;
        public static final int LOAD_IGNORE_CODE_AUTHZ_LEVEL = 0x00000010;
        public static final int LOAD_LIBRARY_AS_IMAGE_RESOURCE = 0x00000020;
        public static final int LOAD_LIBRARY_AS_DATAFILE_EXCLUSIVE = 0x00000040;
        public static final int LOAD_LIBRARY_REQUIRE_SIGNED_TARGET = 0x00000080;
        public static final int LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR = 0x00000100;
        public static final int LOAD_LIBRARY_SEARCH_APPLICATION_DIR = 0x00000200;
        public static final int LOAD_LIBRARY_SEARCH_USER_DIRS = 0x00000400;
        public static final int LOAD_LIBRARY_SEARCH_SYSTEM32 = 0x00000800;
        public static final int LOAD_LIBRARY_SEARCH_DEFAULT_DIRS = 0x00001000;

        // --- Format Message ---
        public static final int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x00000100; // Sistema aloca buffer
        public static final int FORMAT_MESSAGE_IGNORE_INSERTS = 0x00000200; // Ignora argumentos %1
        public static final int FORMAT_MESSAGE_FROM_STRING = 0x00000400; // Mensagem vem de string
        public static final int FORMAT_MESSAGE_FROM_HMODULE = 0x00000800; // Mensagem vem de DLL/Exe
        public static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000; // Mensagem do sistema (GetLastError)
        public static final int FORMAT_MESSAGE_ARGUMENT_ARRAY = 0x00002000; // Argumentos passados como array
        public static final int FORMAT_MESSAGE_MAX_WIDTH_MASK = 0x000000FF;

        // --- Thread Local Storage (TLS) ---
        public static final int TLS_OUT_OF_INDEXES = 0xFFFFFFFF; // Erro em TlsAlloc

    }

    @NullMarked
    public static abstract class WinNT {
        private WinNT() {
        }


        // --- Generic Access Rights (Direitos de acesso genéricos) ---
        public static final int GENERIC_READ = 0x80000000; // Acesso de leitura
        public static final int GENERIC_WRITE = 0x40000000; // Acesso de escrita
        public static final int GENERIC_EXECUTE = 0x20000000; // Acesso de execução
        public static final int GENERIC_ALL = 0x10000000; // Todos os acessos possíveis

        // --- Standard Access Rights (Direitos padrão para a maioria dos objetos) ---
        public static final int DELETE = 0x00010000; // Direito de deletar o objeto
        public static final int READ_CONTROL = 0x00020000; // Direito de ler informações de segurança/dono
        public static final int WRITE_DAC = 0x00040000; // Direito de modificar a DACL
        public static final int WRITE_OWNER = 0x00080000; // Direito de mudar o dono
        public static final int SYNCHRONIZE = 0x00100000; // Direito de usar o objeto para sincronização
        public static final int STANDARD_RIGHTS_REQUIRED = 0x000F0000; // Combinação de delete, read_control, write_dac/owner
        public static final int STANDARD_RIGHTS_READ = READ_CONTROL;
        public static final int STANDARD_RIGHTS_WRITE = READ_CONTROL;
        public static final int STANDARD_RIGHTS_EXECUTE = READ_CONTROL;
        public static final int STANDARD_RIGHTS_ALL = 0x001F0000;

        // --- Specific Access Rights (Direitos específicos por tipo de objeto) ---

        // Process Access Rights
        public static final int PROCESS_TERMINATE = 0x0001; // Terminar processo
        public static final int PROCESS_CREATE_THREAD = 0x0002; // Criar thread no processo
        public static final int PROCESS_SET_SESSIONID = 0x0004; // Definir ID de sessão
        public static final int PROCESS_VM_OPERATION = 0x0008; // Operação na memória virtual
        public static final int PROCESS_VM_READ = 0x0010; // Ler memória virtual
        public static final int PROCESS_VM_WRITE = 0x0020; // Escrever memória virtual
        public static final int PROCESS_DUP_HANDLE = 0x0040; // Duplicar handle
        public static final int PROCESS_CREATE_PROCESS = 0x0080; // Criar sub-processo
        public static final int PROCESS_SET_QUOTA = 0x0100; // Definir cotas de memória
        public static final int PROCESS_SET_INFORMATION = 0x0200; // Definir informações do processo
        public static final int PROCESS_QUERY_INFORMATION = 0x0400; // Consultar informações
        public static final int PROCESS_SUSPEND_RESUME = 0x0800; // Suspender/Resumir
        public static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000; // Consulta limitada (para serviços)
        public static final int PROCESS_ALL_ACCESS = STANDARD_RIGHTS_REQUIRED | SYNCHRONIZE | 0xFFFF;

        // Thread Access Rights
        public static final int THREAD_TERMINATE = 0x0001; // Terminar thread
        public static final int THREAD_SUSPEND_RESUME = 0x0002; // Suspender/Resumir
        public static final int THREAD_GET_CONTEXT = 0x0008; // Obter contexto (registros)
        public static final int THREAD_SET_CONTEXT = 0x0010; // Definir contexto
        public static final int THREAD_SET_INFORMATION = 0x0020; // Definir info
        public static final int THREAD_QUERY_INFORMATION = 0x0040; // Consultar info
        public static final int THREAD_SET_THREAD_TOKEN = 0x0080; // Definir token de segurança
        public static final int THREAD_IMPERSONATE = 0x0100; // Impersonar usuário
        public static final int THREAD_DIRECT_IMPERSONATION = 0x0200; // Impersonação direta
        public static final int THREAD_SET_LIMITED_INFORMATION = 0x0400; // Definir info limitada
        public static final int THREAD_QUERY_LIMITED_INFORMATION = 0x0800; // Consultar info limitada
        public static final int THREAD_ALL_ACCESS = STANDARD_RIGHTS_REQUIRED | SYNCHRONIZE | 0xFFFF;

        // Event/Mutex/Semaphore Access Rights
        public static final int EVENT_MODIFY_STATE = 0x0002; // Modificar estado do evento (SetEvent)
        public static final int EVENT_ALL_ACCESS = STANDARD_RIGHTS_REQUIRED | SYNCHRONIZE | 0x3;
        public static final int MUTEX_MODIFY_STATE = 0x0001; // Liberar mutex
        public static final int MUTEX_ALL_ACCESS = STANDARD_RIGHTS_REQUIRED | SYNCHRONIZE | MUTEX_MODIFY_STATE;
        public static final int SEMAPHORE_MODIFY_STATE = 0x0002; // Liberar semáforo (ReleaseSemaphore)
        public static final int SEMAPHORE_ALL_ACCESS = STANDARD_RIGHTS_REQUIRED | SYNCHRONIZE | 0x3;

        // --- File Attributes (Atributos de arquivo) ---
        public static final int FILE_ATTRIBUTE_READONLY = 0x00000001;
        public static final int FILE_ATTRIBUTE_HIDDEN = 0x00000002;
        public static final int FILE_ATTRIBUTE_SYSTEM = 0x00000004;
        public static final int FILE_ATTRIBUTE_DIRECTORY = 0x00000010;
        public static final int FILE_ATTRIBUTE_ARCHIVE = 0x00000020;
        public static final int FILE_ATTRIBUTE_DEVICE = 0x00000040;
        public static final int FILE_ATTRIBUTE_NORMAL = 0x00000080;
        public static final int FILE_ATTRIBUTE_TEMPORARY = 0x00000100;
        public static final int FILE_ATTRIBUTE_SPARSE_FILE = 0x00000200;
        public static final int FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400;
        public static final int FILE_ATTRIBUTE_COMPRESSED = 0x00000800;
        public static final int FILE_ATTRIBUTE_OFFLINE = 0x00001000;
        public static final int FILE_ATTRIBUTE_NOT_CONTENT_INDEXED = 0x00002000;
        public static final int FILE_ATTRIBUTE_ENCRYPTED = 0x00004000;

        // --- Memory Management Constants (Constantes de Gerenciamento de Memória) ---

        // Allocation Types (VirtualAlloc)
        public static final int MEM_COMMIT = 0x00001000; // Alocar memória física
        public static final int MEM_RESERVE = 0x00002000; // Reservar espaço de endereçamento
        public static final int MEM_DECOMMIT = 0x00004000; // Desalocar memória física
        public static final int MEM_RELEASE = 0x00008000; // Liberar espaço de endereçamento
        public static final int MEM_RESET = 0x00080000; // Indicar que dados não são mais necessários
        public static final int MEM_TOP_DOWN = 0x00100000; // Alocar no endereço mais alto possível
        public static final int MEM_WRITE_WATCH = 0x00200000; // Rastrear escritas
        public static final int MEM_PHYSICAL = 0x00400000; // Alocar memória física (AWE)
        public static final int MEM_LARGE_PAGES = 0x20000000; // Usar large pages

        // Page Protection Options (VirtualProtect)
        public static final int PAGE_NOACCESS = 0x01; // Sem acesso
        public static final int PAGE_READONLY = 0x02; // Apenas leitura
        public static final int PAGE_READWRITE = 0x04; // Leitura e escrita
        public static final int PAGE_WRITECOPY = 0x08; // Copy-on-write
        public static final int PAGE_EXECUTE = 0x10; // Execução
        public static final int PAGE_EXECUTE_READ = 0x20; // Execução e leitura
        public static final int PAGE_EXECUTE_READWRITE = 0x40; // Execução, leitura e escrita
        public static final int PAGE_EXECUTE_WRITECOPY = 0x80; // Execução e copy-on-write
        public static final int PAGE_GUARD = 0x100; // Página de guarda (exceção ao acessar)
        public static final int PAGE_NOCACHE = 0x200; // Não fazer cache
        public static final int PAGE_WRITECOMBINE = 0x400; // Combinar escritas

        // --- System & Hardware Info ---
        public static final long INVALID_HANDLE_VALUE = -1L; // Valor de handle inválido

        // Processor Architecture
        public static final int PROCESSOR_ARCHITECTURE_INTEL = 0;
        public static final int PROCESSOR_ARCHITECTURE_MIPS = 1;
        public static final int PROCESSOR_ARCHITECTURE_ALPHA = 2;
        public static final int PROCESSOR_ARCHITECTURE_PPC = 3;
        public static final int PROCESSOR_ARCHITECTURE_SHX = 4;
        public static final int PROCESSOR_ARCHITECTURE_ARM = 5;
        public static final int PROCESSOR_ARCHITECTURE_IA64 = 6;
        public static final int PROCESSOR_ARCHITECTURE_ALPHA64 = 7;
        public static final int PROCESSOR_ARCHITECTURE_AMD64 = 9;  // x64 / Intel 64
        public static final int PROCESSOR_ARCHITECTURE_ARM64 = 12; // ARM 64-bit
        public static final int PROCESSOR_ARCHITECTURE_UNKNOWN = 0xFFFF;

        // File Types
        public static final int FILE_TYPE_UNKNOWN = 0x0000;
        public static final int FILE_TYPE_DISK = 0x0001;
        public static final int FILE_TYPE_CHAR = 0x0002; // Console, impressora (LPT)
        public static final int FILE_TYPE_PIPE = 0x0003; // Pipe nomeado ou anônimo
        public static final int FILE_TYPE_REMOTE = 0x8000; // Arquivo não utilizado

    }

    @NullMarked
    public static abstract class WinCon {
        private WinCon() {
        }

        // --- Console Handles ---
        public static final int STD_INPUT_HANDLE = -10; // Handle entrada (stdin)
        public static final int STD_OUTPUT_HANDLE = -11; // Handle saída (stdout)
        public static final int STD_ERROR_HANDLE = -12; // Handle erro (stderr)

        // --- Console Input Flags (SetConsoleMode) ---
        public static final int ENABLE_PROCESSED_INPUT = 0x0001; // Processa backspace, tab, etc.
        public static final int ENABLE_LINE_INPUT = 0x0002; // Lê linhas inteiras (até enter)
        public static final int ENABLE_ECHO_INPUT = 0x0004; // Ecoa caracteres digitados
        public static final int ENABLE_WINDOW_INPUT = 0x0008; // Reporta redimensionamento de buffer
        public static final int ENABLE_MOUSE_INPUT = 0x0010; // Reporta eventos de mouse
        public static final int ENABLE_INSERT_MODE = 0x0020;
        public static final int ENABLE_QUICK_EDIT_MODE = 0x0040; // Permite seleção com mouse
        public static final int ENABLE_EXTENDED_FLAGS = 0x0080;
        public static final int ENABLE_AUTO_POSITION = 0x0100;
        public static final int ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200; // Sequências VT100

        // --- Console Output Flags ---
        public static final int ENABLE_PROCESSED_OUTPUT = 0x0001; // Processa backspace, tab, bell
        public static final int ENABLE_WRAP_AT_EOL_OUTPUT = 0x0002; // Quebra linha no fim do buffer
        public static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004; // Interpreta códigos ANSI/VT
        public static final int DISABLE_NEWLINE_AUTO_RETURN = 0x0008;
        public static final int ENABLE_LVB_GRID_WORLDWIDE = 0x0010;

        // --- Console Attributes (Cores) ---
        // Foreground (Texto)
        public static final short FOREGROUND_BLUE = 0x0001;
        public static final short FOREGROUND_GREEN = 0x0002;
        public static final short FOREGROUND_RED = 0x0004;
        public static final short FOREGROUND_INTENSITY = 0x0008; // Cor brilhante
        // Background (Fundo)
        public static final short BACKGROUND_BLUE = 0x0010;
        public static final short BACKGROUND_GREEN = 0x0020;
        public static final short BACKGROUND_RED = 0x0040;
        public static final short BACKGROUND_INTENSITY = 0x0080; // Fundo brilhante

        // --- Console Events (HandlerRoutine) ---
        public static final int CTRL_C_EVENT = 0; // Ctrl+C pressionado
        public static final int CTRL_BREAK_EVENT = 1; // Ctrl+Break pressionado
        public static final int CTRL_CLOSE_EVENT = 2; // Janela fechada
        public static final int CTRL_LOGOFF_EVENT = 5; // Usuário deslogando
        public static final int CTRL_SHUTDOWN_EVENT = 6; // Sistema desligando

        // Attach Console
        public static final int ATTACH_PARENT_PROCESS = -1; // Anexa ao console do pai

    }

    @NullMarked
    public static abstract class WinUser {
        private WinUser() {
        }

        // Show Window Commands (ShowWindow)
        public static final int SW_HIDE = 0; // Oculta janela
        public static final int SW_SHOWNORMAL = 1; // Mostra normal
        public static final int SW_NORMAL = 1;
        public static final int SW_SHOWMINIMIZED = 2; // Mostra minimizado
        public static final int SW_SHOWMAXIMIZED = 3; // Mostra maximizado
        public static final int SW_MAXIMIZE = 3;
        public static final int SW_SHOWNOACTIVATE = 4; // Mostra sem focar
        public static final int SW_SHOW = 5;
        public static final int SW_MINIMIZE = 6; // Minimiza e foca próxima janela
        public static final int SW_SHOWMINNOACTIVE = 7; // Minimiza sem focar
        public static final int SW_SHOWNA = 8; // Mostra no estado atual sem focar
        public static final int SW_RESTORE = 9; // Restaura tamanho original
        public static final int SW_SHOWDEFAULT = 10; // Usa padrão da criação
        public static final int SW_FORCEMINIMIZE = 11; // Minimiza mesmo se thread estiver travada

    }

    @NullMarked
    public static abstract class WinNls {
        private WinNls() {
        }

        public static final int CP_UTF8 = 65001; // UTF-8 Code Page

    }

    @NullMarked
    public static abstract class ShellObj {
        private ShellObj() {
        }

        public static final int CSIDL_DESKTOP = 0x0000; // <desktop>
        public static final int CSIDL_INTERNET = 0x0001; // Internet Explorer (icon on desktop)
        public static final int CSIDL_PROGRAMS = 0x0002; // Start Menu\Programs
        public static final int CSIDL_CONTROLS = 0x0003; // My Computer\Control Panel
        public static final int CSIDL_PRINTERS = 0x0004; // My Computer\Printers
        public static final int CSIDL_PERSONAL = 0x0005; // My Documents
        public static final int CSIDL_FAVORITES = 0x0006; // <user name>\Favorites
        public static final int CSIDL_STARTUP = 0x0007; // Start Menu\Programs\Startup
        public static final int CSIDL_RECENT = 0x0008; // <user name>\Recent
        public static final int CSIDL_SENDTO = 0x0009; // <user name>\SendTo
        public static final int CSIDL_BITBUCKET = 0x000a; // <desktop>\Recycle Bin
        public static final int CSIDL_STARTMENU = 0x000b; // <user name>\Start Menu
        public static final int CSIDL_MYDOCUMENTS = 0x000c; // logical "My Documents" desktop icon
        public static final int CSIDL_MYMUSIC = 0x000d; // "My Music" folder
        public static final int CSIDL_MYVIDEO = 0x000e; // "My Videos" folder
        public static final int CSIDL_DESKTOPDIRECTORY = 0x0010; // <user name>\Desktop
        public static final int CSIDL_DRIVES = 0x0011; // My Computer
        public static final int CSIDL_NETWORK = 0x0012; // Network Neighborhood (My Network Places)
        public static final int CSIDL_NETHOOD = 0x0013; // <user name>\nethood
        public static final int CSIDL_FONTS = 0x0014; // windows\fonts
        public static final int CSIDL_TEMPLATES = 0x0015;
        public static final int CSIDL_COMMON_STARTMENU = 0x0016; // All Users\Start Menu
        public static final int CSIDL_COMMON_PROGRAMS = 0x0017; // All Users\Start Menu\Programs
        public static final int CSIDL_COMMON_STARTUP = 0x0018; // All Users\Startup
        public static final int CSIDL_COMMON_DESKTOPDIRECTORY = 0x0019; // All Users\Desktop
        public static final int CSIDL_APPDATA = 0x001a; // <user name>\Application Data
        public static final int CSIDL_PRINTHOOD = 0x001b; // <user name>\PrintHood
        public static final int CSIDL_LOCAL_APPDATA = 0x001c; // <user name>\Local Settings\Applicaiton Data (non roaming)
        public static final int CSIDL_ALTSTARTUP = 0x001d; // non localized startup
        public static final int CSIDL_COMMON_ALTSTARTUP = 0x001e; // non localized common startup
        public static final int CSIDL_COMMON_FAVORITES = 0x001f;
        public static final int CSIDL_INTERNET_CACHE = 0x0020;
        public static final int CSIDL_COOKIES = 0x0021;
        public static final int CSIDL_HISTORY = 0x0022;
        public static final int CSIDL_COMMON_APPDATA = 0x0023; // All Users\Application Data
        public static final int CSIDL_WINDOWS = 0x0024; // GetWindowsDirectory()
        public static final int CSIDL_SYSTEM = 0x0025; // GetSystemDirectory()
        public static final int CSIDL_PROGRAM_FILES = 0x0026; // C:\Program Files
        public static final int CSIDL_MYPICTURES = 0x0027; // C:\Program Files\My Pictures
        public static final int CSIDL_PROFILE = 0x0028; // USERPROFILE
        public static final int CSIDL_PROGRAM_FILES_COMMON = 0x002b; // C:\Program Files\Common
        public static final int CSIDL_COMMON_TEMPLATES = 0x002d; // All Users\Templates
        public static final int CSIDL_COMMON_DOCUMENTS = 0x002e; // All Users\Documents
        public static final int CSIDL_COMMON_ADMINTOOLS = 0x002f; // All Users\Start Menu\Programs\Administrative Tools
        public static final int CSIDL_ADMINTOOLS = 0x0030; // <user name>\Start Menu\Programs\Administrative Tools
        public static final int CSIDL_CONNECTIONS = 0x0031; // Network and Dial-up Connections
        public static final int CSIDL_COMMON_MUSIC = 0x0035; // All Users\My Music
        public static final int CSIDL_COMMON_PICTURES = 0x0036; // All Users\My Pictures
        public static final int CSIDL_COMMON_VIDEO = 0x0037; // All Users\My Video
        public static final int CSIDL_RESOURCES = 0x0038; // Resource Direcotry
        public static final int CSIDL_RESOURCES_LOCALIZED = 0x0039; // Localized Resource Direcotry
        public static final int CSIDL_COMMON_OEM_LINKS = 0x003a; // Links to All Users OEM specific apps
        public static final int CSIDL_CDBURN_AREA = 0x003b; // USERPROFILE\Local Settings\Application Data\Microsoft\CD Burning
        public static final int CSIDL_COMPUTERSNEARME = 0x003d; // Computers Near Me (computered from Workgroup membership)

        // CSIDL Flags
        public static final int CSIDL_FLAG_CREATE = 0x8000; // combine with CSIDL_ value to force folder creation in SHGetFolderPath()
        public static final int CSIDL_FLAG_DONT_VERIFY = 0x4000; // combine with CSIDL_ value to return an unverified folder path
        public static final int CSIDL_FLAG_NO_ALIAS = 0x1000; // combine with CSIDL_ value to insure non-alias versions of the pidl
        public static final int CSIDL_FLAG_PER_USER_INIT = 0x0800; // combine with CSIDL_ value to indicate per-user init (eg. upgrade)

        // SHGFP_TYPE constants (used in SHGetFolderPath)
        public static final int SHGFP_TYPE_CURRENT = 0;   // current value for user, verify it exists
        public static final int SHGFP_TYPE_DEFAULT = 1;   // default value, may not exist

    }

    @NullMarked
    public static abstract class ShellApi {
        private ShellApi() {}

        // Operations (wFunc)
        public static final int FO_MOVE = 0x0001;
        public static final int FO_COPY = 0x0002;
        public static final int FO_DELETE = 0x0003;
        public static final int FO_RENAME = 0x0004;

        // Flags (fFlags)
        public static final int FOF_MULTIDESTFILES = 0x0001;
        public static final int FOF_CONFIRMMOUSE = 0x0002;
        public static final int FOF_SILENT = 0x0004;  // don't create progress/report
        public static final int FOF_RENAMEONCOLLISION = 0x0008;
        public static final int FOF_NOCONFIRMATION = 0x0010;  // Don't prompt the user.
        public static final int FOF_WANTMAPPINGHANDLE = 0x0020;  // Fill in SHFILEOPSTRUCT.hNameMappings
        public static final int FOF_ALLOWUNDO = 0x0040;  // Enable undo including Recycle behavior for IFileOperation::Delete()
        public static final int FOF_FILESONLY = 0x0080;  // on *.*, do only files
        public static final int FOF_SIMPLEPROGRESS = 0x0100;  // means don't show names of files
        public static final int FOF_NOCONFIRMMKDIR = 0x0200;  // don't confirm making any needed dirs
        public static final int FOF_NOERRORUI = 0x0400;  // don't put up error UI
        public static final int FOF_NOCOPYSECURITYATTRIBS = 0x0800;  // dont copy NT file security attributes
        public static final int FOF_NORECURSION = 0x1000;  // don't recurse into directories.
        public static final int FOF_NO_CONNECTED_ELEMENTS = 0x2000;  // don't operate on connected elements.
        public static final int FOF_WANTNUKEWARNING = 0x4000;  // during delete operation, warn if nuking instead of recycling (partially overrides FOF_NOCONFIRMATION)
        // Flags (uFlags)
        public static final int SHGFI_ICON = 0x000000100; // get icon
        public static final int SHGFI_DISPLAYNAME = 0x000000200; // get display name
        public static final int SHGFI_TYPENAME = 0x000000400; // get type name
        public static final int SHGFI_ATTRIBUTES = 0x000000800; // get attributes
        public static final int SHGFI_ICONLOCATION = 0x000001000; // get icon location
        public static final int SHGFI_EXETYPE = 0x000002000; // return exe type
        public static final int SHGFI_SYSICONINDEX = 0x000004000; // get system icon index
        public static final int SHGFI_LINKOVERLAY = 0x000008000; // put a link overlay on icon
        public static final int SHGFI_SELECTED = 0x000010000; // show icon in selected state
        public static final int SHGFI_ATTR_SPECIFIED = 0x000020000; // get only specified attributes
        public static final int SHGFI_LARGEICON = 0x000000000; // get large icon
        public static final int SHGFI_SMALLICON = 0x000000001; // get small icon
        public static final int SHGFI_OPENICON = 0x000000002; // get open icon
        public static final int SHGFI_SHELLICONSIZE = 0x000000004; // get shell size icon
        public static final int SHGFI_PIDL = 0x000000008; // pszPath is a pidl
        public static final int SHGFI_USEFILEATTRIBUTES = 0x000000010; // use passed dwFileAttribute

    }
}
